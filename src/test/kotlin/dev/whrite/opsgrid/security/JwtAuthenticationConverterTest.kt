package dev.whrite.opsgrid.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class JwtAuthenticationConverterTest {
    private val converter = SecurityConfig().jwtAuthenticationConverter()

    @Test
    fun mapsTopLevelRolesToSpringAuthorities() {
        val jwt = token(
            mapOf(
                "roles" to listOf("ADMIN", "SCHEDULER", "ROLE_HR")
            )
        )

        val authorities = converter.convert(jwt)!!.authorities.map { it.authority }.toSet()

        assertEquals(setOf("ROLE_ADMIN", "ROLE_SCHEDULER", "ROLE_HR"), authorities)
    }

    @Test
    fun mapsKeycloakRealmRolesAndDeduplicatesAuthorities() {
        val jwt = token(
            mapOf(
                "roles" to listOf("SCHEDULER"),
                "realm_access" to mapOf("roles" to listOf("SCHEDULER", "HR"))
            )
        )

        val authorities = converter.convert(jwt)!!.authorities.map { it.authority }.toSet()

        assertEquals(setOf("ROLE_SCHEDULER", "ROLE_HR"), authorities)
    }

    private fun token(claims: Map<String, Any>): Jwt {
        val now = Instant.now()
        return Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("test-operator")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .claims { it.putAll(claims) }
            .build()
    }
}
