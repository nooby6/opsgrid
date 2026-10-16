package dev.whrite.opsgrid.leave

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import dev.whrite.opsgrid.security.requireActorId
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

data class CreateLeaveRequest(
    @field:NotNull val employeeId: UUID?,
    @field:NotNull val startsAt: OffsetDateTime?,
    @field:NotNull val endsAt: OffsetDateTime?,
    @field:NotBlank val reason: String
)

data class LeaveResponse(
    val id: UUID,
    val employeeId: UUID,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val reason: String,
    val status: LeaveStatus
)

@RestController
@RequestMapping("/api/v1/leave")
class EmployeeLeaveController(private val service: EmployeeLeaveService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateLeaveRequest
    ) = service.approve(
        CreateLeaveCommand(request.employeeId!!, request.startsAt!!, request.endsAt!!, request.reason),
        jwt.requireActorId()
    ).toResponse()

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID) = service.find(id).toResponse()

    @DeleteMapping("/{id}")
    fun cancel(
        @PathVariable id: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ) = service.cancel(id, jwt.requireActorId()).toResponse()
}

private fun EmployeeLeave.toResponse() =
    LeaveResponse(id, employee.id, startsAt, endsAt, reason, status)
