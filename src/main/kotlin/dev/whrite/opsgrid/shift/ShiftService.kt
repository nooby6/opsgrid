package dev.whrite.opsgrid.shift

import dev.whrite.opsgrid.employee.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class CreateShiftCommand(val employeeId: UUID, val startsAt: OffsetDateTime, val endsAt: OffsetDateTime)
data class UpdateShiftCommand(val startsAt: OffsetDateTime, val endsAt: OffsetDateTime)
class ShiftConflictException(message: String) : RuntimeException(message)
class EmployeeNotFoundException(message: String) : RuntimeException(message)
class ShiftNotFoundException(message: String) : RuntimeException(message)

@Service
class ShiftService(private val employees: EmployeeRepository, private val shifts: ShiftRepository) {
    @Transactional
    fun schedule(command: CreateShiftCommand): Shift {
        validateWindow(command.startsAt, command.endsAt)
        val employee = employees.findById(command.employeeId).orElseThrow { EmployeeNotFoundException("Employee "+command.employeeId+" was not found") }
        check(employee.active) { "Inactive employees cannot be scheduled" }
        if (shifts.hasConflict(employee.id, command.startsAt, command.endsAt)) {
            throw ShiftConflictException("Employee "+employee.employeeNumber+" already has a shift overlapping this time window")
        }
        return shifts.save(Shift(UUID.randomUUID(), employee, command.startsAt, command.endsAt))
    }
    @Transactional
    fun update(id: UUID, command: UpdateShiftCommand): Shift {
        validateWindow(command.startsAt, command.endsAt)
        val shift = shifts.findById(id).orElseThrow { ShiftNotFoundException("Shift "+id+" was not found") }
        check(shift.status == ShiftStatus.SCHEDULED) { "Cancelled shifts cannot be updated" }
        if (shifts.hasConflict(shift.employee.id, command.startsAt, command.endsAt, shift.id)) {
            throw ShiftConflictException("Employee "+shift.employee.employeeNumber+" already has a shift overlapping this time window")
        }
        shift.startsAt = command.startsAt
        shift.endsAt = command.endsAt
        return shifts.save(shift)
    }
    @Transactional
    fun cancel(id: UUID): Shift {
        val shift = shifts.findById(id).orElseThrow { ShiftNotFoundException("Shift "+id+" was not found") }
        shift.status = ShiftStatus.CANCELLED
        return shifts.save(shift)
    }
    @Transactional(readOnly = true)
    fun find(id: UUID): Shift = shifts.findById(id).orElseThrow { ShiftNotFoundException("Shift "+id+" was not found") }
    @Transactional(readOnly = true)
    fun findEmployeeSchedule(employeeId: UUID, startsAt: OffsetDateTime, endsAt: OffsetDateTime): List<Shift> {
        validateWindow(startsAt, endsAt)
        if (!employees.existsById(employeeId)) throw EmployeeNotFoundException("Employee "+employeeId+" was not found")
        return shifts.findAllByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(employeeId, endsAt, startsAt)
    }
    private fun validateWindow(startsAt: OffsetDateTime, endsAt: OffsetDateTime) { require(endsAt.isAfter(startsAt)) { "Shift end time must be after start time" } }
}
