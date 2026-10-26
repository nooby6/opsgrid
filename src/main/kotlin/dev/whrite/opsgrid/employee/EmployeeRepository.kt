package dev.whrite.opsgrid.employee

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface EmployeeRepository : JpaRepository<Employee, UUID> {
    fun existsByEmployeeNumber(employeeNumber: String): Boolean

    @Query(
        """
        SELECT e FROM Employee e
        WHERE (:active IS NULL OR e.active = :active)
          AND (:department IS NULL OR LOWER(e.department) = LOWER(:department))
          AND (
            :query IS NULL OR
            LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(e.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(e.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """
    )
    fun search(
        @Param("active") active: Boolean?,
        @Param("department") department: String?,
        @Param("query") query: String?,
        pageable: Pageable
    ): Page<Employee>
}
