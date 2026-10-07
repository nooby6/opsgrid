package dev.whrite.opsgrid.leave

import dev.whrite.opsgrid.employee.Employee
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

enum class LeaveStatus { APPROVED, CANCELLED }

@Entity
@Table(name = "employee_leave")
class EmployeeLeave(
    @Id val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    val employee: Employee,
    @Column(name = "starts_at", nullable = false)
    var startsAt: OffsetDateTime,
    @Column(name = "ends_at", nullable = false)
    var endsAt: OffsetDateTime,
    @Column(nullable = false)
    var reason: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: LeaveStatus = LeaveStatus.APPROVED
)
