package dev.whrite.opsgrid.report

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime

class StaffingHoursReportServiceTest {
    private val reports = mock<StaffingHoursReportRepository>()
    private val service = StaffingHoursReportService(reports)

    private val startsAt = OffsetDateTime.parse("2026-10-20T08:00:00Z")
    private val endsAt = OffsetDateTime.parse("2026-10-20T16:00:00Z")

    @Test
    fun mapsDepartmentAggregatesToApiModel() {
        val projection = mock<StaffingHoursProjection>()
        whenever(projection.getDepartment()).thenReturn("Operations")
        whenever(projection.getEmployeeCount()).thenReturn(3L)
        whenever(projection.getShiftCount()).thenReturn(5L)
        whenever(projection.getScheduledHours()).thenReturn(27.5)
        whenever(reports.findStaffingHours(startsAt, endsAt)).thenReturn(listOf(projection))

        assertEquals(
            listOf(DepartmentStaffingHours("Operations", 3, 5, 27.5)),
            service.find(startsAt, endsAt)
        )
        verify(reports).findStaffingHours(startsAt, endsAt)
    }

    @Test
    fun returnsEmptyListWhenNoDepartmentHasScheduledShifts() {
        whenever(reports.findStaffingHours(startsAt, endsAt)).thenReturn(emptyList())

        assertEquals(emptyList<DepartmentStaffingHours>(), service.find(startsAt, endsAt))
        verify(reports).findStaffingHours(startsAt, endsAt)
    }

    @Test
    fun rejectsInvalidWindowBeforeQueryingDatabase() {
        assertThrows(IllegalArgumentException::class.java) {
            service.find(endsAt, startsAt)
        }
        verifyNoInteractions(reports)
    }
}
