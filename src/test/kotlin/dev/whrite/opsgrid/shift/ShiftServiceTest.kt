package dev.whrite.opsgrid.shift

import dev.whrite.opsgrid.audit.AuditLogService
import dev.whrite.opsgrid.availability.EmployeeAvailabilityConflictException
import dev.whrite.opsgrid.availability.EmployeeAvailabilityRepository
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
    private val availability = mock<EmployeeAvailabilityRepository>()
    private val auditLog = mock<AuditLogService>()
    private val service = ShiftService(employees, shifts, leaves, availability, auditLog)

    private val morning = OffsetDateTime.parse("2026-10-06T08:00:00+03:00")
    private val afternoon = OffsetDateTime.parse("2026-10-06T16:00:00+03:00")
    private val evening = OffsetDateTime.parse("2026-10-06T20:00:00+03:00")

    @Test
    fun rejectsOverlappingScheduledShift() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-001")))
        whenever(shifts.hasConflict(any(), any(), any())).thenReturn(true)

        val ex = assertThrows(ShiftConflictException::class.java) {
            service.schedule(CreateShiftCommand(employeeId, morning, afternoon), "operator-1")
        }

        assertEquals("Employee EMP-001 has a scheduling conflict in this time window", ex.message)
        verify(shifts, never()).save(any())
        verify(leaves, never()).hasConflict(any(), any(), any())
        verifyNoInteractions(auditLog)
    }

    @Test
    fun rejectsShiftThatOverlapsApprovedLeave() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-002")))
        whenever(shifts.hasConflict(any(), any(), any())).thenReturn(false)
        whenever(leaves.hasConflict(any(), any(), any())).thenReturn(true)

        assertThrows(ShiftConflictException::class.java) {
            service.schedule(CreateShiftCommand(employeeId, morning, afternoon), "operator-1")
        }

        verify(shifts, never()).save(any())
        verifyNoInteractions(auditLog)
    }

    @Test
    fun rejectsShiftOutsideConfiguredAvailability() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-008")))
        whenever(availability.existsByEmployeeId(employeeId)).thenReturn(true)
        whenever(availability.existsByEmployeeIdAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
            employeeId, morning, afternoon
        )).thenReturn(false)

        assertThrows(EmployeeAvailabilityConflictException::class.java) {
            service.schedule(CreateShiftCommand(employeeId, morning, afternoon), "operator-1")
        }

        verify(shifts, never()).save(any())
        verifyNoInteractions(auditLog)
    }

    @Test
    fun rejectsUpdateOutsideConfiguredAvailability() {
        val employeeId = UUID.randomUUID()
        val shiftId = UUID.randomUUID()
        val employee = employee(employeeId, "EMP-009")
        whenever(shifts.findById(shiftId)).thenReturn(Optional.of(Shift(shiftId, employee, morning, afternoon)))
        whenever(availability.existsByEmployeeId(employeeId)).thenReturn(true)
        whenever(availability.existsByEmployeeIdAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
            employeeId, morning, evening
        )).thenReturn(false)

        assertThrows(EmployeeAvailabilityConflictException::class.java) {
            service.update(shiftId, UpdateShiftCommand(morning, evening), "operator-1")
        }

        verify(shifts, never()).save(any())
        verifyNoInteractions(auditLog)
    }

    @Test
    fun rejectsSchedulingForInactiveEmployee() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-004", false)))
        assertThrows(EmployeeInactiveException::class.java) {
            service.schedule(CreateShiftCommand(employeeId, morning, afternoon), "operator-1")
        }
        verify(shifts, never()).save(any())
        verifyNoInteractions(auditLog)
    }

    @Test
    fun rejectsInvalidTimeWindowBeforeQueryingRepositories() {
        assertThrows(IllegalArgumentException::class.java) {
            service.schedule(CreateShiftCommand(UUID.randomUUID(), afternoon, morning), "operator-1")
        }
        verifyNoInteractions(employees, shifts, leaves, availability, auditLog)
    }

    @Test
    fun allowsAdjacentNonOverlappingWindow() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-005")))
        whenever(shifts.hasConflict(employeeId, afternoon, evening)).thenReturn(false)
        whenever(leaves.hasConflict(employeeId, afternoon, evening)).thenReturn(false)
        whenever(shifts.save(any())).thenAnswer { it.arguments[0] as Shift }

        val shift = service.schedule(CreateShiftCommand(employeeId, afternoon, evening), "operator-1")
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

        val shift = service.schedule(CreateShiftCommand(employeeId, morning, afternoon), "operator-1")
        assertEquals(employeeId, shift.employee.id)
        verify(shifts).save(any())
    }

    @Test
    fun createsShiftAndWritesAuditEvent() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, "EMP-007")))
        whenever(shifts.hasConflict(any(), any(), any())).thenReturn(false)
        whenever(leaves.hasConflict(any(), any(), any())).thenReturn(false)
        whenever(shifts.save(any())).thenAnswer { it.arguments[0] as Shift }

        val shift = service.schedule(CreateShiftCommand(employeeId, morning, afternoon), "operator-1")
        assertEquals(employeeId, shift.employee.id)
        verify(shifts).save(any())
        verify(auditLog).record(
            "operator-1", "SHIFT_CREATED", "SHIFT", shift.id, null,
            mapOf("employeeId" to employeeId, "startsAt" to morning, "endsAt" to afternoon, "status" to "SCHEDULED")
        )
    }

    private fun employee(id: UUID, employeeNumber: String, active: Boolean = true) =
        Employee(id, employeeNumber, "Test", "Employee", "Operations", active)
}
