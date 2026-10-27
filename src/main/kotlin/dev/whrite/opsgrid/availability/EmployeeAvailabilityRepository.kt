package dev.whrite.opsgrid.availability

import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface EmployeeAvailabilityRepository : JpaRepository<EmployeeAvailability, UUID> {
    fun existsByEmployeeId(employeeId: UUID): Boolean

    fun existsByEmployeeIdAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
        employeeId: UUID,
        startsAt: OffsetDateTime,
        endsAt: OffsetDateTime
    ): Boolean

    fun findAllByEmployeeIdOrderByStartsAtAsc(employeeId: UUID): List<EmployeeAvailability>
}
