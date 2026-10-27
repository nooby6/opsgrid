package dev.whrite.opsgrid.report

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

data class DepartmentStaffingHours(
    val department: String,
    val employeeCount: Long,
    val shiftCount: Long,
    val scheduledHours: Double
)

@Service
class StaffingHoursReportService(
    private val reports: StaffingHoursReportRepository
) {
    @Transactional(readOnly = true)
    fun find(startsAt: OffsetDateTime, endsAt: OffsetDateTime): List<DepartmentStaffingHours> {
        require(endsAt.isAfter(startsAt)) { "Report end time must be after start time" }
        return reports.findStaffingHours(startsAt, endsAt).map {
            DepartmentStaffingHours(
                department = it.getDepartment(),
                employeeCount = it.getEmployeeCount(),
                shiftCount = it.getShiftCount(),
                scheduledHours = it.getScheduledHours()
            )
        }
    }
}
