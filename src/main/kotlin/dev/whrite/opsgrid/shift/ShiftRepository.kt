package dev.whrite.opsgrid.shift

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface ShiftRepository : JpaRepository<Shift, UUID> {

    @Query("""
        select case when count(s) > 0 then true else false end
        from Shift s
        where s.employee.id = :employeeId
          and s.status = dev.whrite.opsgrid.shift.ShiftStatus.SCHEDULED
          and s.startsAt < :endsAt
          and s.endsAt > :startsAt
    """)
    fun hasConflict(
        @Param("employeeId") employeeId: UUID,
        @Param("startsAt") startsAt: OffsetDateTime,
        @Param("endsAt") endsAt: OffsetDateTime
    ): Boolean

    @Query("""
        select case when count(s) > 0 then true else false end
        from Shift s
        where s.employee.id = :employeeId
          and s.id <> :excludedShiftId
          and s.status = dev.whrite.opsgrid.shift.ShiftStatus.SCHEDULED
          and s.startsAt < :endsAt
          and s.endsAt > :startsAt
    """)
    fun hasConflict(
        @Param("employeeId") employeeId: UUID,
        @Param("startsAt") startsAt: OffsetDateTime,
        @Param("endsAt") endsAt: OffsetDateTime,
        @Param("excludedShiftId") excludedShiftId: UUID
    ): Boolean

    fun findAllByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(
        employeeId: UUID,
        endsAt: OffsetDateTime,
        startsAt: OffsetDateTime
    ): List<Shift>
}
