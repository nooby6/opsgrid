package dev.whrite.opsgrid.employee

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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(controllers = [EmployeeController::class])
@Import(SecurityConfig::class)
class EmployeeControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockBean
    private lateinit var repository: EmployeeRepository

    @Test
    fun requiresAuthenticationToListEmployees() {
        mockMvc.perform(get("/api/v1/employees"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun listsEmployeesWithFiltersAndPaginationMetadata() {
        val employee = Employee(UUID.randomUUID(), "EMP-001", "Ada", "Otieno", "Operations", true)
        whenever(repository.search(eq(true), eq("Operations"), eq("Ada"), any<Pageable>()))
            .thenReturn(PageImpl(listOf(employee), PageRequest.of(0, 10), 1))

        mockMvc.perform(
            get("/api/v1/employees")
                .param("active", "true")
                .param("department", " Operations ")
                .param("query", " Ada ")
                .param("page", "0")
                .param("size", "10")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_HR")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP-001"))
            .andExpect(jsonPath("$.content[0].department").value("Operations"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))

        verify(repository).search(eq(true), eq("Operations"), eq("Ada"), any<Pageable>())
    }

    @Test
    fun rejectsPageSizesAboveMaximum() {
        mockMvc.perform(
            get("/api/v1/employees")
                .param("size", "101")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_HR")))
        )
            .andExpect(status().isBadRequest)
    }
}
