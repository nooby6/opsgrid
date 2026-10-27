package dev.whrite.opsgrid.leave

import dev.whrite.opsgrid.audit.AuditLogService
import dev.whrite.opsgrid.employee.Employee
import dev.whrite.opsgrid.employee.EmployeeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class EmployeeLeaveServiceTest {
    private val employees = mock<EmployeeRepository>()
    private val leaves = mock<EmployeeLeaveRepository>()
    private val auditLog = mock<AuditLogService>()
    private val service = EmployeeLeaveService(employees, leaves, auditLog)

    private val startsAt = OffsetDateTime.parse("2026-11-02T08:00:00Z")
    private val endsAt = OffsetDateTime.parse("2026-11-02T16:00:00Z")

    @Test
    fun approvalWritesAnAuditEvent() {
        val employeeId = UUID.randomUUID()
        val employee = employee(employeeId)
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee))
        whenever(leaves.hasConflict(employeeId, startsAt, endsAt)).thenReturn(false)
        whenever(leaves.save(any())).thenAnswer { it.arguments[0] as EmployeeLeave }

        val leave = service.approve(
            CreateLeaveCommand(employeeId, startsAt, endsAt, "  Annual leave  "),
            "operator-2"
        )

        assertEquals("Annual leave", leave.reason)
        verify(auditLog).record(
            "operator-2",
            "LEAVE_APPROVED",
            "EMPLOYEE_LEAVE",
            leave.id,
            null,
            mapOf(
                "employeeId" to employeeId,
                "startsAt" to startsAt,
                "endsAt" to endsAt,
                "reason" to "Annual leave",
                "status" to "APPROVED"
            )
        )
    }

    @Test
    fun rejectedLeaveDoesNotWriteAnAuditEvent() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)))
        whenever(leaves.hasConflict(employeeId, startsAt, endsAt)).thenReturn(true)

        assertThrows(LeaveConflictException::class.java) {
            service.approve(
                CreateLeaveCommand(employeeId, startsAt, endsAt, "Annual leave"),
                "operator-2"
            )
        }

        verify(leaves, never()).save(any())
        verifyNoInteractions(auditLog)
    }

    @Test
    fun cancellationAuditsBeforeAndAfterState() {
        val employeeId = UUID.randomUUID()
        val leaveId = UUID.randomUUID()
        val employee = employee(employeeId)
        val leave = EmployeeLeave(leaveId, employee, startsAt, endsAt, "Annual leave")
        whenever(leaves.findById(leaveId)).thenReturn(Optional.of(leave))
        whenever(leaves.save(any())).thenAnswer { it.arguments[0] as EmployeeLeave }

        val cancelled = service.cancel(leaveId, "operator-2")

        assertEquals(LeaveStatus.CANCELLED, cancelled.status)
        verify(auditLog).record(
            "operator-2",
            "LEAVE_CANCELLED",
            "EMPLOYEE_LEAVE",
            leaveId,
            mapOf(
                "employeeId" to employeeId,
                "startsAt" to startsAt,
                "endsAt" to endsAt,
                "reason" to "Annual leave",
                "status" to "APPROVED"
            ),
            mapOf(
                "employeeId" to employeeId,
                "startsAt" to startsAt,
                "endsAt" to endsAt,
                "reason" to "Annual leave",
                "status" to "CANCELLED"
            )
        )
    }


    @Test
    fun approvalRejectsMissingEmployeeWithDomainException() {
        val employeeId = UUID.randomUUID()
        whenever(employees.findById(employeeId)).thenReturn(Optional.empty())

        assertThrows(LeaveEmployeeNotFoundException::class.java) {
            service.approve(
                CreateLeaveCommand(employeeId, startsAt, endsAt, "Annual leave"),
                "operator-2"
            )
        }

        verifyNoInteractions(leaves, auditLog)
    }

    @Test
    fun approvalRejectsInactiveEmployeeWithDomainException() {
        val employeeId = UUID.randomUUID()
        val inactiveEmployee = employee(employeeId).apply { active = false }
        whenever(employees.findById(employeeId)).thenReturn(Optional.of(inactiveEmployee))

        assertThrows(LeaveEmployeeInactiveException::class.java) {
            service.approve(
                CreateLeaveCommand(employeeId, startsAt, endsAt, "Annual leave"),
                "operator-2"
            )
        }

        verify(leaves, never()).save(any())
        verifyNoInteractions(auditLog)
    }

    private fun employee(id: UUID) =
        Employee(id, "EMP-" + id.toString().take(8), "Test", "Employee", "Operations")
}
