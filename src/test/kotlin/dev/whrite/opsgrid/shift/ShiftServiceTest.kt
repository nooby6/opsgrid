package dev.whrite.opsgrid.shift

import dev.whrite.opsgrid.employee.Employee
import dev.whrite.opsgrid.employee.EmployeeRepository
import dev.whrite.opsgrid.leave.EmployeeLeaveRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class ShiftServiceTest {
    private val employees = mock<EmployeeRepository>()
    private val shifts = mock<ShiftRepository>()
    private val leaves = mock<EmployeeLeaveRepository>()
    private val service = ShiftService(employees, shifts, leaves)

    private val morning = OffsetDateTime.parse("2026-10-06T08:00:00+03:00")
    private val afternoon = OffsetDateTime.parse("2026-10-06T16:00:00+03:00")
    private val evening = OffsetDateTime.parse("2026-10-06T20:00:00+03:00")

    @Test
    fun rejectsOverlappingScheduledShift() {
        val employeeId = UUID.randomUUID()
        val employee = employee(employeeId, "EMP-001")
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee))
        whenever(shifts.hasConflict(any(), any(), any())).thenReturn(true)

        val ex = assertThrows(ShiftConflictException::class.java) {
            service.schedule(CreateShiftCommand(employeeId, morning, afternoon))
        }

        assertEquals("Employee EMP-001 has a scheduling conflict in this time window", ex.message)
        verify(shifts, never()).save(any())
        verify(leaves, never()).hasConflict(any(), any(), any())
    }

    @Test
    fun rejectsShiftThatOverlapsApprovedLeave() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-002")))
        whenever(shifts.hasConflict(any(), any(), any())).thenReturn(false)
        whenever(leaves.hasConflict(any(), any(), any())).thenReturn(true)

        assertThrows(ShiftConflictException::class.java) {
            service.schedule(CreateShiftCommand(employeeId, morning, afternoon))
        }

        verify(shifts, never()).save(any())
    }

    @Test
    fun rejectsUpdateThatWouldOverlapApprovedLeave() {
        val employeeId = UUID.randomUUID()
        val shiftId = UUID.randomUUID()
        val employee = employee(employeeId, "EMP-003")
        val existing = Shift(shiftId, employee, morning, afternoon)
        whenever(shifts.findById(shiftId)).thenReturn(Optional.of(existing))
        whenever(shifts.hasConflict(any(), any(), any(), any())).thenReturn(false)
        whenever(leaves.hasConflict(employeeId, morning, evening)).thenReturn(true)

        assertThrows(ShiftConflictException::class.java) {
            service.update(shiftId, UpdateShiftCommand(morning, evening))
        }

        verify(shifts, never()).save(any())
    }

    @Test
    fun rejectsSchedulingForInactiveEmployee() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(
            Optional.of(employee(employeeId, "EMP-004", active = false))
        )

        assertThrows(EmployeeInactiveException::class.java) {
            service.schedule(CreateShiftCommand(employeeId, morning, afternoon))
        }

        verify(shifts, never()).save(any())
        verify(shifts, never()).hasConflict(any(), any(), any())
        verify(leaves, never()).hasConflict(any(), any(), any())
    }

    @Test
    fun rejectsInvalidTimeWindowBeforeQueryingRepositories() {
        val employeeId = UUID.randomUUID()

        assertThrows(IllegalArgumentException::class.java) {
            service.schedule(CreateShiftCommand(employeeId, afternoon, morning))
        }

        verifyNoInteractions(employees, shifts, leaves)
    }

    @Test
    fun allowsAdjacentNonOverlappingWindow() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-005")))
        whenever(shifts.hasConflict(employeeId, afternoon, evening)).thenReturn(false)
        whenever(leaves.hasConflict(employeeId, afternoon, evening)).thenReturn(false)
        whenever(shifts.save(any())).thenAnswer { it.arguments[0] as Shift }

        val shift = service.schedule(CreateShiftCommand(employeeId, afternoon, evening))

        assertEquals(afternoon, shift.startsAt)
        assertEquals(evening, shift.endsAt)
        verify(shifts).save(any())
    }

    @Test
    fun allowsOverlappingWindowForDifferentEmployee() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-006")))
        whenever(shifts.hasConflict(employeeId, morning, afternoon)).thenReturn(false)
        whenever(leaves.hasConflict(employeeId, morning, afternoon)).thenReturn(false)
        whenever(shifts.save(any())).thenAnswer { it.arguments[0] as Shift }

        val shift = service.schedule(CreateShiftCommand(employeeId, morning, afternoon))

        assertEquals(employeeId, shift.employee.id)
        verify(shifts).save(any())
    }

    @Test
    fun createsShiftWhenThereIsNoConflict() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-007")))
        whenever(shifts.hasConflict(any(), any(), any())).thenReturn(false)
        whenever(leaves.hasConflict(any(), any(), any())).thenReturn(false)
        whenever(shifts.save(any())).thenAnswer { it.arguments[0] as Shift }

        val shift = service.schedule(CreateShiftCommand(employeeId, morning, afternoon))

        assertEquals(employeeId, shift.employee.id)
        verify(shifts).save(any())
    }

    private fun employee(
        id: UUID,
        employeeNumber: String,
        active: Boolean = true
    ) = Employee(id, employeeNumber, "Test", "Employee", "Operations", active)
}
