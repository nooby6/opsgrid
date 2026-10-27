package dev.whrite.opsgrid.shift

import dev.whrite.opsgrid.audit.AuditLogService
import dev.whrite.opsgrid.availability.EmployeeAvailabilityConflictException
import dev.whrite.opsgrid.availability.EmployeeAvailabilityRepository
import dev.whrite.opsgrid.employee.EmployeeRepository
import dev.whrite.opsgrid.leave.EmployeeLeaveRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class CreateShiftCommand(val employeeId: UUID, val startsAt: OffsetDateTime, val endsAt: OffsetDateTime)
data class UpdateShiftCommand(val startsAt: OffsetDateTime, val endsAt: OffsetDateTime)
class ShiftConflictException(message: String) : RuntimeException(message)
class EmployeeNotFoundException(message: String) : RuntimeException(message)
class EmployeeInactiveException(message: String) : RuntimeException(message)
class ShiftNotFoundException(message: String) : RuntimeException(message)

@Service
class ShiftService(
    private val employees: EmployeeRepository,
    private val shifts: ShiftRepository,
    private val leaves: EmployeeLeaveRepository,
    private val availability: EmployeeAvailabilityRepository,
    private val auditLog: AuditLogService
) {
    @Transactional
    fun schedule(command: CreateShiftCommand, actorId: String): Shift {
        validateWindow(command.startsAt, command.endsAt)
        val employee = employees.findById(command.employeeId)
            .orElseThrow { EmployeeNotFoundException("Employee ${command.employeeId} was not found") }
        if (!employee.active) {
            throw EmployeeInactiveException("Inactive employees cannot be scheduled")
        }
        validateAvailability(employee.id, command.startsAt, command.endsAt)
        if (shifts.hasConflict(employee.id, command.startsAt, command.endsAt) ||
            leaves.hasConflict(employee.id, command.startsAt, command.endsAt)
        ) {
            throw ShiftConflictException(
                "Employee ${employee.employeeNumber} has a scheduling conflict in this time window"
            )
        }

        val shift = shifts.save(Shift(UUID.randomUUID(), employee, command.startsAt, command.endsAt))
        auditLog.record(actorId, "SHIFT_CREATED", "SHIFT", shift.id, null, snapshot(shift))
        return shift
    }

    @Transactional
    fun update(id: UUID, command: UpdateShiftCommand, actorId: String): Shift {
        validateWindow(command.startsAt, command.endsAt)
        val shift = shifts.findById(id)
            .orElseThrow { ShiftNotFoundException("Shift $id was not found") }
        check(shift.status == ShiftStatus.SCHEDULED) { "Cancelled shifts cannot be updated" }
        validateAvailability(shift.employee.id, command.startsAt, command.endsAt)

        val hasShiftConflict = shifts.hasConflict(shift.employee.id, command.startsAt, command.endsAt, shift.id)
        val hasLeaveConflict = leaves.hasConflict(shift.employee.id, command.startsAt, command.endsAt)
        if (hasShiftConflict || hasLeaveConflict) {
            throw ShiftConflictException(
                "Employee ${shift.employee.employeeNumber} has a scheduling conflict in this time window"
            )
        }

        val before = snapshot(shift)
        shift.startsAt = command.startsAt
        shift.endsAt = command.endsAt
        val updated = shifts.save(shift)
        auditLog.record(actorId, "SHIFT_UPDATED", "SHIFT", updated.id, before, snapshot(updated))
        return updated
    }

    @Transactional
    fun cancel(id: UUID, actorId: String): Shift {
        val shift = shifts.findById(id)
            .orElseThrow { ShiftNotFoundException("Shift $id was not found") }
        val before = snapshot(shift)
        shift.status = ShiftStatus.CANCELLED
        val cancelled = shifts.save(shift)
        auditLog.record(actorId, "SHIFT_CANCELLED", "SHIFT", cancelled.id, before, snapshot(cancelled))
        return cancelled
    }

    @Transactional(readOnly = true)
    fun find(id: UUID): Shift = shifts.findById(id)
        .orElseThrow { ShiftNotFoundException("Shift $id was not found") }

    @Transactional(readOnly = true)
    fun findEmployeeSchedule(employeeId: UUID, startsAt: OffsetDateTime, endsAt: OffsetDateTime): List<Shift> {
        validateWindow(startsAt, endsAt)
        if (!employees.existsById(employeeId)) {
            throw EmployeeNotFoundException("Employee $employeeId was not found")
        }
        return shifts.findAllByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(
            employeeId, endsAt, startsAt
        )
    }

<<<<<<< HEAD
    @Transactional(readOnly = true)
    fun findSchedule(startsAt: OffsetDateTime, endsAt: OffsetDateTime): List<Shift> {
        validateWindow(startsAt, endsAt)
        return shifts.findAllByStartsAtLessThanAndEndsAtGreaterThanAndStatusOrderByStartsAtAsc(
            endsAt,
            startsAt,
            ShiftStatus.SCHEDULED
        )
=======
    private fun validateAvailability(employeeId: UUID, startsAt: OffsetDateTime, endsAt: OffsetDateTime) {
        // Existing employees without configured availability retain their current behaviour.
        // Once availability windows exist, every scheduled shift must fit wholly inside one window.
        if (availability.existsByEmployeeId(employeeId) &&
            !availability.existsByEmployeeIdAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
                employeeId, startsAt, endsAt
            )
        ) {
            throw EmployeeAvailabilityConflictException(
                "Shift must fit entirely within an availability window for employee $employeeId"
            )
        }
>>>>>>> origin/main
    }

    private fun snapshot(shift: Shift) = mapOf(
        "employeeId" to shift.employee.id,
        "startsAt" to shift.startsAt,
        "endsAt" to shift.endsAt,
        "status" to shift.status.name
    )

    private fun validateWindow(startsAt: OffsetDateTime, endsAt: OffsetDateTime) {
        require(endsAt.isAfter(startsAt)) { "Shift end time must be after start time" }
    }
}
