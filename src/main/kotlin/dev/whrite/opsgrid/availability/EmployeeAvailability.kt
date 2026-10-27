package dev.whrite.opsgrid.availability

import dev.whrite.opsgrid.employee.Employee
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "employee_availability")
class EmployeeAvailability(
    @Id val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    val employee: Employee,
    @Column(name = "starts_at", nullable = false)
    val startsAt: OffsetDateTime,
    @Column(name = "ends_at", nullable = false)
    val endsAt: OffsetDateTime
)
