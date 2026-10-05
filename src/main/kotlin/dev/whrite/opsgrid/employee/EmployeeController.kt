package dev.whrite.opsgrid.employee

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class CreateEmployeeRequest(
    @field:NotBlank val employeeNumber: String,
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:NotBlank val department: String
)

data class EmployeeResponse(
    val id: UUID,
    val employeeNumber: String,
    val firstName: String,
    val lastName: String,
    val department: String,
    val active: Boolean
)

@RestController
@RequestMapping("/api/v1/employees")
class EmployeeController(private val repository: EmployeeRepository) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateEmployeeRequest): EmployeeResponse {
        require(!repository.existsByEmployeeNumber(request.employeeNumber)) {
            "Employee number already exists"
        }
        val employee = repository.save(
            Employee(UUID.randomUUID(), request.employeeNumber, request.firstName, request.lastName, request.department)
        )
        return EmployeeResponse(
            employee.id, employee.employeeNumber, employee.firstName,
            employee.lastName, employee.department, employee.active
        )
    }
}
