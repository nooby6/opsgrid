package dev.whrite.opsgrid.shift
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID
data class CreateShiftRequest(
    @field:NotNull val employeeId: UUID?,
    @field:NotNull val startsAt: OffsetDateTime?,
    @field:NotNull val endsAt: OffsetDateTime?
)
data class ShiftResponse(val id: UUID, val employeeId: UUID, val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val status: ShiftStatus)
@RestController
@RequestMapping("/api/v1/shifts")
class ShiftController(private val service: ShiftService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateShiftRequest): ShiftResponse {
        val shift = service.schedule(CreateShiftCommand(request.employeeId!!, request.startsAt!!, request.endsAt!!))
        return ShiftResponse(shift.id, shift.employee.id, shift.startsAt, shift.endsAt, shift.status)
    }
}
