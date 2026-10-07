package dev.whrite.opsgrid.leave

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface EmployeeLeaveRepository : JpaRepository<EmployeeLeave, UUID> {
    @Query("""
        select count(l) > 0 from EmployeeLeave l
        where l.employee.id = :employeeId
          and l.status = dev.whrite.opsgrid.leave.LeaveStatus.APPROVED
          and l.startsAt < :endsAt
          and l.endsAt > :startsAt
    """)
    fun hasConflict(@Param("employeeId") employeeId: UUID, @Param("startsAt") startsAt: OffsetDateTime, @Param("endsAt") endsAt: OffsetDateTime): Boolean
}
