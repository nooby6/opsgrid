package dev.whrite.opsgrid.report

import dev.whrite.opsgrid.employee.Employee
import dev.whrite.opsgrid.employee.EmployeeRepository
import dev.whrite.opsgrid.shift.Shift
import dev.whrite.opsgrid.shift.ShiftRepository
import dev.whrite.opsgrid.shift.ShiftStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
class StaffingHoursReportIntegrationTest @Autowired constructor(
    private val employees: EmployeeRepository,
    private val shifts: ShiftRepository,
    private val reports: StaffingHoursReportRepository
) {
    @Test
    fun aggregatesOnlyScheduledOverlappingShiftsAndClipsHoursToRequestedWindow() {
        val operationsA = createEmployee("Operations")
        val operationsB = createEmployee("Operations")
        val hr = createEmployee("HR")
        val operationsOutsideWindow = createEmployee("Operations")
        val start = OffsetDateTime.parse("2026-10-20T10:00:00Z")
        val end = OffsetDateTime.parse("2026-10-20T14:00:00Z")

        try {
            shifts.save(Shift(UUID.randomUUID(), operationsA, start.minusHours(2), start.plusHours(2)))
            shifts.save(Shift(UUID.randomUUID(), operationsB, end.minusHours(2), end.plusHours(2)))
            shifts.save(Shift(UUID.randomUUID(), hr, start, end, ShiftStatus.CANCELLED))
            shifts.save(Shift(UUID.randomUUID(), operationsOutsideWindow, end.plusHours(1), end.plusHours(2)))

            val result = reports.findStaffingHours(start, end)

            assertEquals(1, result.size)
            assertEquals("Operations", result.single().getDepartment())
            assertEquals(2L, result.single().getEmployeeCount())
            assertEquals(2L, result.single().getShiftCount())
            assertEquals(4.0, result.single().getScheduledHours(), 0.001)
        } finally {
            shifts.deleteAll(
                shifts.findAll().filter {
                    it.employee.id in setOf(
                        operationsA.id,
                        operationsB.id,
                        hr.id,
                        operationsOutsideWindow.id
                    )
                }
            )
            employees.deleteAll(listOf(operationsA, operationsB, hr, operationsOutsideWindow))
        }
    }

    private fun createEmployee(department: String) = employees.save(
        Employee(
            UUID.randomUUID(),
            "RPT-" + UUID.randomUUID().toString().take(8),
            "Report",
            "Test",
            department
        )
    )
}
