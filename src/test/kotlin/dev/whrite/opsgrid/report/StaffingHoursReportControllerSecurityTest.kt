package dev.whrite.opsgrid.report

import dev.whrite.opsgrid.security.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

@WebMvcTest(controllers = [StaffingHoursReportController::class])
@Import(SecurityConfig::class)
class StaffingHoursReportControllerSecurityTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockBean
    private lateinit var service: StaffingHoursReportService

    @Test
    fun rejectsUnauthenticatedReportRequest() {
        mockMvc.perform(
            get("/api/v1/reports/staffing-hours")
                .param("startsAt", "2026-10-20T08:00:00Z")
                .param("endsAt", "2026-10-20T16:00:00Z")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun authenticatedUserCanReadStaffingReport() {
        val startsAt = OffsetDateTime.parse("2026-10-20T08:00:00Z")
        val endsAt = OffsetDateTime.parse("2026-10-20T16:00:00Z")
        whenever(service.find(startsAt, endsAt)).thenReturn(
            listOf(DepartmentStaffingHours("Operations", 3, 5, 27.5))
        )

        mockMvc.perform(
            get("/api/v1/reports/staffing-hours")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                .param("startsAt", startsAt.toString())
                .param("endsAt", endsAt.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].department").value("Operations"))
            .andExpect(jsonPath("$[0].employeeCount").value(3))
            .andExpect(jsonPath("$[0].shiftCount").value(5))
            .andExpect(jsonPath("$[0].scheduledHours").value(27.5))
    }
}
