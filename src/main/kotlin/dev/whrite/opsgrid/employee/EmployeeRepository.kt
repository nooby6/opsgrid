package dev.whrite.opsgrid.employee

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmployeeRepository : JpaRepository<Employee, UUID> {
    fun existsByEmployeeNumber(employeeNumber: String): Boolean
}
