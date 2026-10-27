package dev.whrite.opsgrid.report

import dev.whrite.opsgrid.shift.Shift
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface StaffingHoursProjection {
    fun getDepartment(): String
    fun getEmployeeCount(): Long
    fun getShiftCount(): Long
    fun getScheduledHours(): Double
}

interface StaffingHoursReportRepository : JpaRepository<Shift, UUID> {
    @Query(
        value = """
            SELECT
                e.department AS department,
                COUNT(DISTINCT e.id) AS "employeeCount",
                COUNT(s.id) AS "shiftCount",
                CAST(
                    ROUND(
                        COALESCE(
                            SUM(
                                EXTRACT(
                                    EPOCH FROM (
                                        LEAST(s.ends_at, CAST(:endsAt AS TIMESTAMPTZ))
                                        - GREATEST(s.starts_at, CAST(:startsAt AS TIMESTAMPTZ))
                                    )
                                ) / 3600.0
                            ),
                            0
                        )::numeric,
                        2
                    ) AS double precision
                ) AS "scheduledHours"
            FROM shifts s
            JOIN employees e ON e.id = s.employee_id
            WHERE s.status = 'SCHEDULED'
              AND s.starts_at < :endsAt
              AND s.ends_at > :startsAt
            GROUP BY e.department
            ORDER BY e.department ASC
        """,
        nativeQuery = true
    )
    fun findStaffingHours(
        @Param("startsAt") startsAt: OffsetDateTime,
        @Param("endsAt") endsAt: OffsetDateTime
    ): List<StaffingHoursProjection>
}
