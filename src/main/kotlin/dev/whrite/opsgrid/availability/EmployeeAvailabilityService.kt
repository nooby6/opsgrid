package dev.whrite.opsgrid.availability

import dev.whrite.opsgrid.employee.EmployeeRepository
import dev.whrite.opsgrid.shift.EmployeeInactiveException
import dev.whrite.opsgrid.shift.EmployeeNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

class InvalidAvailabilityWindowException(message: String) : IllegalArgumentException(message)

@Service
class EmployeeAvailabilityService(
    private val employees: EmployeeRepository,
    private val availability: EmployeeAvailabilityRepository
) {
    @Transactional
    fun create(employeeId: UUID, startsAt: OffsetDateTime, endsAt: OffsetDateTime): EmployeeAvailability {
        validateWindow(startsAt, endsAt)
        val employee = employees.findById(employeeId)
            .orElseThrow { EmployeeNotFoundException("Employee $employeeId was not found") }
        if (!employee.active) {
            throw EmployeeInactiveException("Inactive employees cannot be assigned availability")
        }
        return availability.save(EmployeeAvailability(UUID.randomUUID(), employee, startsAt, endsAt))
    }

    @Transactional(readOnly = true)
    fun list(employeeId: UUID): List<EmployeeAvailability> =
        availability.findAllByEmployeeIdOrderByStartsAtAsc(employeeId)

    private fun validateWindow(startsAt: OffsetDateTime, endsAt: OffsetDateTime) {
        if (!endsAt.isAfter(startsAt)) {
            throw InvalidAvailabilityWindowException("Availability end time must be after start time")
        }
    }
}
