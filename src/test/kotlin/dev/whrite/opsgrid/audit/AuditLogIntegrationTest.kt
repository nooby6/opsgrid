package dev.whrite.opsgrid.audit

import dev.whrite.opsgrid.employee.Employee
import dev.whrite.opsgrid.employee.EmployeeRepository
import dev.whrite.opsgrid.shift.CreateShiftCommand
import dev.whrite.opsgrid.shift.ShiftService
import dev.whrite.opsgrid.shift.ShiftRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
class AuditLogIntegrationTest {
    @Autowired
    private lateinit var employees: EmployeeRepository

    @Autowired
    private lateinit var shifts: ShiftRepository

    @Autowired
    private lateinit var shiftService: ShiftService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun successfulShiftMutationPersistsItsAuditEvent() {
        val employee = createEmployee()
        try {
            val shift = shiftService.schedule(
                CreateShiftCommand(
                    employee.id,
                    OffsetDateTime.parse("2026-10-20T08:00:00Z"),
                    OffsetDateTime.parse("2026-10-20T16:00:00Z")
                ),
                "integration-test"
            )

            val count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE entity_id = ? AND actor_id = ?",
                Long::class.javaObjectType,
                shift.id,
                "integration-test"
            )
            val status = jdbcTemplate.queryForObject(
                "SELECT after_state ->> 'status' FROM audit_events WHERE entity_id = ?",
                String::class.java,
                shift.id
            )

            assertEquals(1L, count)
            assertEquals("SCHEDULED", status)
        } finally {
            shifts.deleteAll(shifts.findAll().filter { it.employee.id == employee.id })
            employees.deleteById(employee.id)
        }
    }

    @Test
    fun failedAuditWriteRollsBackTheShiftMutation() {
        val employee = createEmployee()
        val startsAt = OffsetDateTime.parse("2026-10-21T08:00:00Z")
        val endsAt = OffsetDateTime.parse("2026-10-21T16:00:00Z")
        try {
            assertThrows(IllegalArgumentException::class.java) {
                shiftService.schedule(
                    CreateShiftCommand(employee.id, startsAt, endsAt),
                    "x".repeat(121)
                )
            }

            val shiftCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shifts WHERE employee_id = ?",
                Long::class.javaObjectType,
                employee.id
            )
            val auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE actor_id = ?",
                Long::class.javaObjectType,
                "x".repeat(121)
            )

            assertEquals(0L, shiftCount)
            assertEquals(0L, auditCount)
        } finally {
            employees.deleteById(employee.id)
        }
    }

    private fun createEmployee(): Employee = employees.save(
        Employee(
            UUID.randomUUID(),
            "AUD-" + UUID.randomUUID().toString().take(8),
            "Audit",
            "Test",
            "Operations"
        )
    )
}
