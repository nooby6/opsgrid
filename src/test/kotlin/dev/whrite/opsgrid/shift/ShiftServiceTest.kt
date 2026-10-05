package dev.whrite.opsgrid.shift
import dev.whrite.opsgrid.employee.Employee
import dev.whrite.opsgrid.employee.EmployeeRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
class ShiftServiceTest {
    private val employees = mock<EmployeeRepository>()
    private val shifts = mock<ShiftRepository>()
    private val service = ShiftService(employees, shifts)

    @Test
    fun rejectsOverlappingScheduledShift() {
        val employeeId = UUID.randomUUID()
        val employee = Employee(employeeId, "EMP-001", "Ada", "Lovelace", "Operations")
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee))
        whenever(shifts.hasConflict(any(), any(), any())).thenReturn(true)
        val ex = assertThrows(ShiftConflictException::class.java) {
            service.schedule(CreateShiftCommand(
                employeeId,
                OffsetDateTime.parse("2026-10-06T08:00:00+03:00"),
                OffsetDateTime.parse("2026-10-06T16:00:00+03:00")
            ))
        }
        assertEquals("Employee EMP-001 already has a shift overlapping this time window", ex.message)
        verify(shifts, never()).save(any())
    }

    @Test
    fun createsShiftWhenThereIsNoConflict() {
        val employeeId = UUID.randomUUID()
        val employee = Employee(employeeId, "EMP-002", "Grace", "Hopper", "Engineering")
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee))
        whenever(shifts.hasConflict(any(), any(), any())).thenReturn(false)
        whenever(shifts.save(any())).thenAnswer { it.arguments[0] as Shift }
        val shift = service.schedule(CreateShiftCommand(
            employeeId,
            OffsetDateTime.parse("2026-10-06T08:00:00+03:00"),
            OffsetDateTime.parse("2026-10-06T16:00:00+03:00")
        ))
        assertEquals(employeeId, shift.employee.id)
        verify(shifts).save(any())
    }
}
