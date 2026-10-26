package dev.whrite.opsgrid.api

import dev.whrite.opsgrid.leave.LeaveEmployeeInactiveException
import dev.whrite.opsgrid.leave.LeaveEmployeeNotFoundException
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import java.sql.SQLException

class ApiExceptionHandlerTest {
    private val handler = ApiExceptionHandler()

    @Test
    fun mapsShiftExclusionViolationToShiftConflict() {
        val response = handler.dataIntegrityViolation(
            dataIntegrityFailure("23P01", "shifts_no_overlapping_employee_windows")
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("SHIFT_CONFLICT", response.body?.code)
        assertEquals("The shift overlaps an existing scheduled shift", response.body?.message)
    }

    @Test
    fun mapsLeaveExclusionViolationToLeaveConflict() {
        val response = handler.dataIntegrityViolation(
            dataIntegrityFailure("23P01", "leave_no_overlapping_employee_windows")
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("LEAVE_CONFLICT", response.body?.code)
    }

    @Test
    fun doesNotExposeDatabaseDetailsForOtherIntegrityViolations() {
        val response = handler.dataIntegrityViolation(
            dataIntegrityFailure("23505", "employees_employee_number_key")
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("DATA_INTEGRITY_VIOLATION", response.body?.code)
        assertEquals(
            "The request violates a data integrity constraint",
            response.body?.message
        )
    }


    @Test
    fun mapsMissingEmployeeDuringLeaveApprovalToNotFound() {
        val error = handler.leaveEmployeeNotFound(
            LeaveEmployeeNotFoundException("Employee was not found")
        )

        assertEquals("EMPLOYEE_NOT_FOUND", error.code)
        assertEquals("Employee was not found", error.message)
    }

    @Test
    fun mapsInactiveEmployeeDuringLeaveApprovalToConflict() {
        val error = handler.inactiveLeaveEmployee(
            LeaveEmployeeInactiveException("Inactive employees cannot receive leave")
        )

        assertEquals("EMPLOYEE_INACTIVE", error.code)
        assertEquals("Inactive employees cannot receive leave", error.message)
    }

    private fun dataIntegrityFailure(sqlState: String, constraint: String) =
        DataIntegrityViolationException(
            "database write failed",
            ConstraintViolationException(
                "constraint violation",
                SQLException("database detail must not reach the client", sqlState),
                constraint
            )
        )
}
