package dev.whrite.opsgrid.shift
import dev.whrite.opsgrid.employee.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID
data class CreateShiftCommand(val employeeId: UUID, val startsAt: OffsetDateTime, val endsAt: OffsetDateTime)
class ShiftConflictException(message: String) : RuntimeException(message)
class EmployeeNotFoundException(message: String) : RuntimeException(message)
@Service
class ShiftService(private val employees: EmployeeRepository, private val shifts: ShiftRepository) {
    @Transactional
    fun schedule(command: CreateShiftCommand): Shift {
        require(command.endsAt.isAfter(command.startsAt)) { "Shift end time must be after start time" }
        val employee = employees.findById(command.employeeId).orElseThrow {
            EmployeeNotFoundException("Employee ${command.employeeId} was not found")
        }
        check(employee.active) { "Inactive employees cannot be scheduled" }
        if (shifts.hasConflict(employee.id, command.startsAt, command.endsAt)) {
            throw ShiftConflictException("Employee ${employee.employeeNumber} already has a shift overlapping this time window")
        }
        return shifts.save(Shift(UUID.randomUUID(), employee, command.startsAt, command.endsAt))
    }
}
