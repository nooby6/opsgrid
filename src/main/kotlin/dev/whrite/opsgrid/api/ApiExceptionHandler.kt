package dev.whrite.opsgrid.api
import dev.whrite.opsgrid.shift.EmployeeNotFoundException
import dev.whrite.opsgrid.shift.ShiftConflictException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
data class ApiError(val code: String, val message: String)
@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ShiftConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun conflict(ex: ShiftConflictException) = ApiError("SHIFT_CONFLICT", ex.message ?: "Shift conflict")
    @ExceptionHandler(EmployeeNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(ex: EmployeeNotFoundException) = ApiError("EMPLOYEE_NOT_FOUND", ex.message ?: "Employee not found")
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(ex: IllegalArgumentException) = ApiError("INVALID_REQUEST", ex.message ?: "Invalid request")
}
