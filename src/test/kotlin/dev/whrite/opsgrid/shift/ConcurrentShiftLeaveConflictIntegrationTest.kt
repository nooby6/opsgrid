package dev.whrite.opsgrid.shift

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConcurrentShiftLeaveConflictIntegrationTest {
    @Test
    fun concurrentOverlappingShiftAndLeaveAllowOnlyOneCommit() {
        val url = System.getenv("TEST_DATABASE_URL")
        assumeTrue(!url.isNullOrBlank(), "TEST_DATABASE_URL must point to a disposable PostgreSQL test database")
        val user = System.getenv("TEST_DATABASE_USER") ?: "opsgrid"
        val password = System.getenv("TEST_DATABASE_PASSWORD") ?: "opsgrid"
        val schema = "opsgrid_cross_table_test_" + UUID.randomUUID().toString().replace("-", "")
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

                val crossTableMigration = Files.readString(
                    Path.of("src/main/resources/db/migration/V5__prevent_shift_leave_overlap.sql")
                )
                connection.createStatement().use { it.execute(crossTableMigration) }

                connection.prepareStatement(
                    """INSERT INTO employees (id, employee_number, first_name, last_name, department, active)
                       VALUES (?, ?, ?, ?, ?, TRUE)"""
                ).use { statement ->
                    statement.setObject(1, employeeId)
                    statement.setString(2, "EMP-${employeeId.toString().take(8)}")
                    statement.setString(3, "Cross-table")
                    statement.setString(4, "Integration")
                    statement.setString(5, "Operations")
                    statement.executeUpdate()
                }
            }

            val barrier = CyclicBarrier(2)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val operations = listOf("shift", "leave")
                val results = operations.map { operation ->
                    executor.submit<String> {
                        DriverManager.getConnection(url, user, password).use { connection ->
                            setSearchPath(connection, schema)
                            connection.autoCommit = false
                            try {
                                barrier.await(10, TimeUnit.SECONDS)
                                val startsAt = OffsetDateTime.parse("2026-10-20T08:00:00Z")
                                val endsAt = OffsetDateTime.parse("2026-10-20T16:00:00Z")
                                if (operation == "shift") {
                                    connection.prepareStatement(
                                        """INSERT INTO shifts (id, employee_id, starts_at, ends_at, status)
                                           VALUES (?, ?, ?, ?, 'SCHEDULED')"""
                                    ).use { statement ->
                                        statement.setObject(1, UUID.randomUUID())
                                        statement.setObject(2, employeeId)
                                        statement.setObject(3, startsAt)
                                        statement.setObject(4, endsAt)
                                        statement.executeUpdate()
                                    }
                                } else {
                                    connection.prepareStatement(
                                        """INSERT INTO employee_leave (id, employee_id, starts_at, ends_at, reason, status)
                                           VALUES (?, ?, ?, ?, 'annual leave', 'APPROVED')"""
                                    ).use { statement ->
                                        statement.setObject(1, UUID.randomUUID())
                                        statement.setObject(2, employeeId)
                                        statement.setObject(3, startsAt)
                                        statement.setObject(4, endsAt)
                                        statement.executeUpdate()
                                    }
                                }
                                connection.commit()
                                "$operation:success"
                            } catch (error: SQLException) {
                                connection.rollback()
                                if (error.sqlState == "23P01") "$operation:conflict" else throw error
                            } catch (error: Exception) {
                                connection.rollback()
                                throw error
                            }
                        }
                    }
                }.map { it.get(20, TimeUnit.SECONDS) }

                assertEquals(1, results.count { it.endsWith(":success") }, "exactly one operation should commit")
                assertEquals(1, results.count { it.endsWith(":conflict") }, "the cross-table overlap must be rejected")

                DriverManager.getConnection(url, user, password).use { connection ->
                    setSearchPath(connection, schema)
                    connection.createStatement().use { statement ->
                        statement.executeQuery(
                            "SELECT (SELECT COUNT(*) FROM shifts WHERE employee_id = '${employeeId}' AND status = 'SCHEDULED') + " +
                                "(SELECT COUNT(*) FROM employee_leave WHERE employee_id = '${employeeId}' AND status = 'APPROVED')"
                        ).use { result ->
                            result.next()
                            assertEquals(1, result.getInt(1), "only one scheduled shift or approved leave should remain")
                        }
                    }
                }
            } finally {
                executor.shutdownNow()
            }
        } finally {
            DriverManager.getConnection(url, user, password).use { admin ->
                admin.createStatement().use { it.execute("DROP SCHEMA IF EXISTS \"$schema\" CASCADE") }
            }
        }
    }

    private fun setSearchPath(connection: Connection, schema: String) {
        connection.createStatement().use { it.execute("SET search_path TO \"$schema\", public") }
    }

    private fun executeMigration(connection: Connection, filename: String) {
        val sql = Files.readString(Path.of("src/main/resources/db/migration", filename))
        sql.split(';')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { statement ->
                connection.createStatement().use { it.execute(statement) }
            }
    }
}
