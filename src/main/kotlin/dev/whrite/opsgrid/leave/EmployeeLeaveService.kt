package dev.whrite.opsgrid.leave

import dev.whrite.opsgrid.audit.AuditLogService
import dev.whrite.opsgrid.employee.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class CreateLeaveCommand(
    val employeeId: UUID,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val reason: String
)
class LeaveConflictException(message: String) : RuntimeException(message)
class LeaveNotFoundException(message: String) : RuntimeException(message)

@Service
class EmployeeLeaveService(
    private val employees: EmployeeRepository,
    private val leaves: EmployeeLeaveRepository,
    private val auditLog: AuditLogService
) {
    @Transactional
    fun approve(command: CreateLeaveCommand, actorId: String): EmployeeLeave {
        validateWindow(command.startsAt, command.endsAt)
        require(command.reason.isNotBlank()) { "Leave reason is required" }
        val employee = employees.findById(command.employeeId)
            .orElseThrow { RuntimeException("Employee ${command.employeeId} was not found") }
        check(employee.active) { "Inactive employees cannot receive leave" }
        if (leaves.hasConflict(employee.id, command.startsAt, command.endsAt)) {
            throw LeaveConflictException("Employee already has approved leave overlapping this time window")
        }

        val leave = leaves.save(
            EmployeeLeave(
                UUID.randomUUID(),
                employee,
                command.startsAt,
                command.endsAt,
                command.reason.trim()
            )
        )
        auditLog.record(actorId, "LEAVE_APPROVED", "EMPLOYEE_LEAVE", leave.id, null, snapshot(leave))
        return leave
    }

    @Transactional(readOnly = true)
    fun find(id: UUID) = leaves.findById(id)
        .orElseThrow { LeaveNotFoundException("Leave $id was not found") }

    @Transactional
    fun cancel(id: UUID, actorId: String): EmployeeLeave {
        val leave = leaves.findById(id)
            .orElseThrow { LeaveNotFoundException("Leave $id was not found") }
        val before = snapshot(leave)
        leave.status = LeaveStatus.CANCELLED
        val cancelled = leaves.save(leave)
        auditLog.record(actorId, "LEAVE_CANCELLED", "EMPLOYEE_LEAVE", cancelled.id, before, snapshot(cancelled))
        return cancelled
    }

    private fun snapshot(leave: EmployeeLeave) = mapOf(
        "employeeId" to leave.employee.id,
        "startsAt" to leave.startsAt,
        "endsAt" to leave.endsAt,
        "reason" to leave.reason,
        "status" to leave.status.name
    )

    private fun validateWindow(startsAt: OffsetDateTime, endsAt: OffsetDateTime) {
        require(endsAt.isAfter(startsAt)) { "Leave end time must be after start time" }
    }
}
