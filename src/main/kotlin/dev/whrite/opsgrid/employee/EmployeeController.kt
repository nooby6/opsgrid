package dev.whrite.opsgrid.employee

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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

data class EmployeePageResponse(
    val content: List<EmployeeResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
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
        return employee.toResponse()
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) query: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): EmployeePageResponse {
        require(page >= 0) { "page must be zero or greater" }
        require(size in 1..100) { "size must be between 1 and 100" }

        val results = repository.search(
            active = active,
            department = department?.trim()?.takeIf(String::isNotEmpty),
            query = query?.trim()?.takeIf(String::isNotEmpty),
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName", "firstName", "employeeNumber"))
        )
        return EmployeePageResponse(
            content = results.content.map { it.toResponse() },
            page = results.number,
            size = results.size,
            totalElements = results.totalElements,
            totalPages = results.totalPages
        )
    }
}

private fun Employee.toResponse() = EmployeeResponse(
    id, employeeNumber, firstName, lastName, department, active
)
