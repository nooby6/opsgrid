package dev.whrite.opsgrid.availability

import dev.whrite.opsgrid.employee.EmployeeRepository
import dev.whrite.opsgrid.shift.EmployeeNotFoundException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

data class CreateAvailabilityRequest(
    @field:NotNull val startsAt: OffsetDateTime?,
    @field:NotNull val endsAt: OffsetDateTime?
)

data class AvailabilityResponse(
    val id: UUID,
    val employeeId: UUID,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime
)

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/availability")
class EmployeeAvailabilityController(
    private val employees: EmployeeRepository,
    private val service: EmployeeAvailabilityService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable employeeId: UUID,
        @Valid @RequestBody request: CreateAvailabilityRequest
    ): AvailabilityResponse {
        if (!employees.existsById(employeeId)) {
            throw EmployeeNotFoundException("Employee $employeeId was not found")
        }
        return service.create(employeeId, request.startsAt!!, request.endsAt!!).toResponse()
    }

    @GetMapping
    fun list(@PathVariable employeeId: UUID): List<AvailabilityResponse> {
        if (!employees.existsById(employeeId)) {
            throw EmployeeNotFoundException("Employee $employeeId was not found")
        }
        return service.list(employeeId).map { it.toResponse() }
    }
}

private fun EmployeeAvailability.toResponse() = AvailabilityResponse(
    id, employee.id, startsAt, endsAt
)
