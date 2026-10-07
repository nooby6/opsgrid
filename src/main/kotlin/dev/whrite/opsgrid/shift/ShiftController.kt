package dev.whrite.opsgrid.shift

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.UUID

data class CreateShiftRequest(@field:NotNull val employeeId: UUID?, @field:NotNull val startsAt: OffsetDateTime?, @field:NotNull val endsAt: OffsetDateTime?)
data class UpdateShiftRequest(@field:NotNull val startsAt: OffsetDateTime?, @field:NotNull val endsAt: OffsetDateTime?)
data class ShiftResponse(val id: UUID, val employeeId: UUID, val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val status: ShiftStatus)

@RestController
@RequestMapping("/api/v1/shifts")
class ShiftController(private val service: ShiftService) {
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateShiftRequest) = service.schedule(CreateShiftCommand(request.employeeId!!, request.startsAt!!, request.endsAt!!)).toResponse()
    @GetMapping("/{id}") fun get(@PathVariable id: UUID) = service.find(id).toResponse()
    @PutMapping("/{id}") fun update(@PathVariable id: UUID, @Valid @RequestBody request: UpdateShiftRequest) = service.update(id, UpdateShiftCommand(request.startsAt!!, request.endsAt!!)).toResponse()
    @DeleteMapping("/{id}") fun cancel(@PathVariable id: UUID) = service.cancel(id).toResponse()
    @GetMapping("/employee/{employeeId}")
    fun schedule(@PathVariable employeeId: UUID, @RequestParam startsAt: OffsetDateTime, @RequestParam endsAt: OffsetDateTime) = service.findEmployeeSchedule(employeeId, startsAt, endsAt).map { it.toResponse() }
}

private fun Shift.toResponse() = ShiftResponse(id, employee.id, startsAt, endsAt, status)
