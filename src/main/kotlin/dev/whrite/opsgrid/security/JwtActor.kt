package dev.whrite.opsgrid.security

import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.server.ResponseStatusException

fun Jwt.requireActorId(): String =
    subject?.takeIf { it.isNotBlank() }
        ?: throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "The access token must contain a subject claim"
        )
