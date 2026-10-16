package dev.whrite.opsgrid.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.config.annotation.web.builders.HttpSecurity

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter
    ): SecurityFilterChain {
        http.csrf { it.disable() }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.authorizeHttpRequests {
            it.requestMatchers(HttpMethod.POST, "/api/v1/employees").hasAnyRole("ADMIN", "HR")
            it.requestMatchers(
                HttpMethod.POST,
                "/api/v1/shifts"
            ).hasAnyRole("ADMIN", "SCHEDULER")
            it.requestMatchers(
                HttpMethod.PUT,
                "/api/v1/shifts/**"
            ).hasAnyRole("ADMIN", "SCHEDULER")
            it.requestMatchers(
                HttpMethod.DELETE,
                "/api/v1/shifts/**"
            ).hasAnyRole("ADMIN", "SCHEDULER")
            it.requestMatchers(
                HttpMethod.POST,
                "/api/v1/leave"
            ).hasAnyRole("ADMIN", "HR")
            it.requestMatchers(
                HttpMethod.DELETE,
                "/api/v1/leave/**"
            ).hasAnyRole("ADMIN", "HR")
            it.anyRequest().authenticated()
        }
        http.oauth2ResourceServer {
            it.jwt { jwt ->
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
            }
        }
        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(
            Converter<Jwt, Collection<GrantedAuthority>> { jwt ->
                val directRoles = (jwt.claims["roles"] as? Collection<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                val realmAccess = jwt.claims["realm_access"] as? Map<*, *>
                val realmRoles = (realmAccess?.get("roles") as? Collection<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()

                (directRoles + realmRoles)
                    .mapNotNull { role ->
                        role.trim()
                            .removePrefix("ROLE_")
                            .takeIf(String::isNotBlank)
                            ?.let { SimpleGrantedAuthority("ROLE_$it") }
                    }
                    .distinct()
            }
        )
        return converter
    }
}
