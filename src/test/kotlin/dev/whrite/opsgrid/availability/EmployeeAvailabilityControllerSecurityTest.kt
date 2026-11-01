package dev.whrite.opsgrid.availability

import dev.whrite.opsgrid.employee.Employee
import dev.whrite.opsgrid.employee.EmployeeRepository
import dev.whrite.opsgrid.security.SecurityConfig
import org.junit.jupiter.api.Test
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

@WebMvcTest(controllers = [EmployeeAvailabilityController::class])
@Import(SecurityConfig::class)
class EmployeeAvailabilityControllerSecurityTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockBean
    private lateinit var employees: EmployeeRepository

    @MockBean
    private lateinit var service: EmployeeAvailabilityService

    @Test
    fun rejectsUnauthenticatedAvailabilityMutation() {
        mockMvc.perform(
            post(path())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody())
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun employeeRoleCannotChangeAvailability() {
        mockMvc.perform(
            post(path())
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody())
        ).andExpect(status().isForbidden)
    }

    @Test
    fun schedulerCanCreateAvailabilityWindow() {
        val employeeId = UUID.randomUUID()
        val employee = Employee(employeeId, "EMP-AVL-01", "Test", "Employee", "Operations")
        val startsAt = OffsetDateTime.parse("2026-11-02T08:00:00Z")
        val endsAt = OffsetDateTime.parse("2026-11-02T16:00:00Z")
        whenever(employees.existsById(employeeId)).thenReturn(true)
        whenever(service.create(employeeId, startsAt, endsAt)).thenReturn(
            EmployeeAvailability(UUID.randomUUID(), employee, startsAt, endsAt)
        )

        mockMvc.perform(
            post(path(employeeId))
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_SCHEDULER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(startsAt, endsAt))
        ).andExpect(status().isCreated)
    }

    private fun path(employeeId: UUID = UUID.randomUUID()) =
        "/api/v1/employees/$employeeId/availability"

    private fun requestBody(
        startsAt: OffsetDateTime = OffsetDateTime.parse("2026-11-02T08:00:00Z"),
        endsAt: OffsetDateTime = OffsetDateTime.parse("2026-11-02T16:00:00Z")
    ) = """{"startsAt":"$startsAt","endsAt":"$endsAt"}"""
}
