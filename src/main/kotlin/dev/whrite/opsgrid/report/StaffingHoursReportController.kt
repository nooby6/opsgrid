package dev.whrite.opsgrid.report

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/reports")
class StaffingHoursReportController(
    private val service: StaffingHoursReportService
) {
    @GetMapping("/staffing-hours")
    fun staffingHours(
        @RequestParam startsAt: OffsetDateTime,
        @RequestParam endsAt: OffsetDateTime
    ): List<DepartmentStaffingHours> = service.find(startsAt, endsAt)
}
