package dev.whrite.opsgrid.shift

import dev.whrite.opsgrid.employee.Employee
import dev.whrite.opsgrid.security.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.util.UUID

@WebMvcTest(controllers = [ShiftController::class])
@Import(SecurityConfig::class)
class ShiftControllerSecurityTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockBean
    private lateinit var service: ShiftService

    @Test
    fun rejectsUnauthenticatedShiftMutation() {
        mockMvc.perform(
            post("/api/v1/shifts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody())
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun employeeRoleCannotCreateShift() {
        mockMvc.perform(
            post("/api/v1/shifts")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody())
        ).andExpect(status().isForbidden)
    }

    @Test
    fun schedulerCanCreateShiftAndAuditActorComesFromTokenSubject() {
        val employeeId = UUID.randomUUID()
        val employee = Employee(employeeId, "EMP-SEC-01", "Test", "Scheduler", "Operations")
        val startsAt = OffsetDateTime.parse("2026-10-20T08:00:00Z")
        val endsAt = OffsetDateTime.parse("2026-10-20T16:00:00Z")
        val shift = Shift(UUID.randomUUID(), employee, startsAt, endsAt)
        whenever(service.schedule(any(), eq("scheduler-1"))).thenReturn(shift)

        mockMvc.perform(
            post("/api/v1/shifts")
                .with(
                    jwt()
                        .jwt { it.subject("scheduler-1") }
                        .authorities(SimpleGrantedAuthority("ROLE_SCHEDULER"))
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(employeeId))
        ).andExpect(status().isCreated)

        verify(service).schedule(any(), eq("scheduler-1"))
    }

    private fun requestBody(employeeId: UUID = UUID.randomUUID()) =
        """{"employeeId":"$employeeId","startsAt":"2026-10-20T08:00:00Z","endsAt":"2026-10-20T16:00:00Z"}"""
}
