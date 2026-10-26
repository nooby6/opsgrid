package dev.whrite.opsgrid.api

import dev.whrite.opsgrid.leave.LeaveConflictException
import dev.whrite.opsgrid.leave.LeaveEmployeeInactiveException
import dev.whrite.opsgrid.leave.LeaveEmployeeNotFoundException
import dev.whrite.opsgrid.leave.LeaveNotFoundException
import dev.whrite.opsgrid.shift.EmployeeInactiveException
import dev.whrite.opsgrid.shift.EmployeeNotFoundException
import dev.whrite.opsgrid.shift.ShiftConflictException
import dev.whrite.opsgrid.shift.ShiftNotFoundException
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.sql.SQLException

data class ApiError(val code: String, val message: String)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ShiftConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun conflict(ex: ShiftConflictException) =
        ApiError("SHIFT_CONFLICT", ex.message ?: "Shift conflict")

    @ExceptionHandler(EmployeeInactiveException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun inactiveEmployee(ex: EmployeeInactiveException) =
        ApiError("EMPLOYEE_INACTIVE", ex.message ?: "Inactive employees cannot be scheduled")

    @ExceptionHandler(EmployeeNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(ex: EmployeeNotFoundException) =
        ApiError("EMPLOYEE_NOT_FOUND", ex.message ?: "Employee not found")

    @ExceptionHandler(ShiftNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun shiftNotFound(ex: ShiftNotFoundException) =
        ApiError("SHIFT_NOT_FOUND", ex.message ?: "Shift not found")

    @ExceptionHandler(LeaveEmployeeNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun leaveEmployeeNotFound(ex: LeaveEmployeeNotFoundException) =
        ApiError("EMPLOYEE_NOT_FOUND", ex.message ?: "Employee not found")

    @ExceptionHandler(LeaveEmployeeInactiveException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun inactiveLeaveEmployee(ex: LeaveEmployeeInactiveException) =
        ApiError("EMPLOYEE_INACTIVE", ex.message ?: "Inactive employees cannot receive leave")

    @ExceptionHandler(LeaveNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun leaveNotFound(ex: LeaveNotFoundException) =
        ApiError("LEAVE_NOT_FOUND", ex.message ?: "Leave not found")

    @ExceptionHandler(LeaveConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun leaveConflict(ex: LeaveConflictException) =
        ApiError("LEAVE_CONFLICT", ex.message ?: "Leave conflict")

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun dataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<ApiError> {
        val causes = generateSequence<Throwable>(ex) { it.cause }.toList()
        val sqlState = causes.filterIsInstance<SQLException>().firstOrNull()?.sqlState
        val constraintName = causes
            .filterIsInstance<ConstraintViolationException>()
            .firstOrNull()
            ?.constraintName

        val error = when {
            sqlState == "23P01" && constraintName == SHIFT_EXCLUSION_CONSTRAINT ->
                ApiError("SHIFT_CONFLICT", "The shift overlaps an existing scheduled shift")

            sqlState == "23P01" && constraintName == LEAVE_EXCLUSION_CONSTRAINT ->
                ApiError("LEAVE_CONFLICT", "The leave window overlaps existing approved leave")

            sqlState == "23P01" ->
                ApiError("SCHEDULE_CONFLICT", "The requested time window conflicts with an existing schedule")

            else ->
                ApiError("DATA_INTEGRITY_VIOLATION", "The request violates a data integrity constraint")
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(ex: IllegalArgumentException) =
        ApiError("INVALID_REQUEST", ex.message ?: "Invalid request")

    private companion object {
        const val SHIFT_EXCLUSION_CONSTRAINT = "shifts_no_overlapping_employee_windows"
        const val LEAVE_EXCLUSION_CONSTRAINT = "leave_no_overlapping_employee_windows"
    }
}
