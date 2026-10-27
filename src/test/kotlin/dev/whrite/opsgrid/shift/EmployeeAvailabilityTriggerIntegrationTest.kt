package dev.whrite.opsgrid.shift

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.UUID

class EmployeeAvailabilityTriggerIntegrationTest {
    @Test
    fun databaseEnforcesAvailabilityAndPreservesExistingShifts() {
        val url = System.getenv("TEST_DATABASE_URL")
        assumeTrue(!url.isNullOrBlank(), "TEST_DATABASE_URL must point to a disposable PostgreSQL test database")
        val user = System.getenv("TEST_DATABASE_USER") ?: "opsgrid"
        val password = System.getenv("TEST_DATABASE_PASSWORD") ?: "opsgrid"
        val schema = "opsgrid_availability_test_" + UUID.randomUUID().toString().replace("-", "")
        val employeeId = UUID.randomUUID()

        DriverManager.getConnection(url, user, password).use { admin ->
            admin.createStatement().use { it.execute("CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public") }
            admin.createStatement().use { it.execute("CREATE SCHEMA \"$schema\"") }
        }

        try {
            DriverManager.getConnection(url, user, password).use { connection ->
                setSearchPath(connection, schema)
                listOf(
                    "V1__initial_schema.sql",
                    "V2__employee_leave.sql",
                    "V3__schedule_consistency.sql"
                ).forEach { executeMigration(connection, it) }
                connection.createStatement().use {
                    it.execute(Files.readString(Path.of("src/main/resources/db/migration/V5__prevent_shift_leave_overlap.sql")))
                    it.execute(Files.readString(Path.of("src/main/resources/db/migration/V6__employee_availability_windows.sql")))
                }

                connection.prepareStatement(
                    """INSERT INTO employees (id, employee_number, first_name, last_name, department, active)
                       VALUES (?, ?, ?, ?, ?, TRUE)"""
                ).use { statement ->
                    statement.setObject(1, employeeId)
                    statement.setString(2, "EMP-${employeeId.toString().take(8)}")
                    statement.setString(3, "Integration")
                    statement.setString(4, "Test")
                    statement.setString(5, "Operations")
                    statement.executeUpdate()
                }

                insertShift(
                    connection, employeeId,
                    "2026-10-12T06:00:00Z", "2026-10-12T07:00:00Z"
                )

                connection.prepareStatement(
                    """INSERT INTO employee_availability (id, employee_id, starts_at, ends_at)
                       VALUES (?, ?, ?, ?)"""
                ).use { statement ->
                    statement.setObject(1, UUID.randomUUID())
                    statement.setObject(2, employeeId)
                    statement.setObject(3, OffsetDateTime.parse("2026-10-12T08:00:00Z"))
                    statement.setObject(4, OffsetDateTime.parse("2026-10-12T16:00:00Z"))
                    assertEquals(1, statement.executeUpdate(), "availability should be configurable despite legacy shifts")
                }

                var rejected = false
                try {
                    insertShift(
                        connection, employeeId,
                        "2026-10-12T16:00:00Z", "2026-10-12T17:00:00Z"
                    )
                } catch (error: SQLException) {
                    rejected = error.sqlState == "23514"
                    if (!rejected) throw error
                }
                assertTrue(rejected, "a scheduled shift outside configured availability must be rejected")

                insertShift(
                    connection, employeeId,
                    "2026-10-12T09:00:00Z", "2026-10-12T10:00:00Z"
                )
            }
        } finally {
            DriverManager.getConnection(url, user, password).use { admin ->
                admin.createStatement().use { it.execute("DROP SCHEMA IF EXISTS \"$schema\" CASCADE") }
            }
        }
    }

    private fun insertShift(
        connection: java.sql.Connection,
        employeeId: UUID,
        startsAt: String,
        endsAt: String
    ) {
        connection.prepareStatement(
            """INSERT INTO shifts (id, employee_id, starts_at, ends_at, status)
               VALUES (?, ?, ?, ?, 'SCHEDULED')"""
        ).use { statement ->
            statement.setObject(1, UUID.randomUUID())
            statement.setObject(2, employeeId)
            statement.setObject(3, OffsetDateTime.parse(startsAt))
            statement.setObject(4, OffsetDateTime.parse(endsAt))
            statement.executeUpdate()
        }
    }

    private fun setSearchPath(connection: java.sql.Connection, schema: String) {
        connection.createStatement().use { it.execute("SET search_path TO \"$schema\", public") }
    }

    private fun executeMigration(connection: java.sql.Connection, filename: String) {
        val sql = Files.readString(Path.of("src/main/resources/db/migration", filename))
        sql.split(';')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { statement -> connection.createStatement().use { it.execute(statement) } }
    }
}
