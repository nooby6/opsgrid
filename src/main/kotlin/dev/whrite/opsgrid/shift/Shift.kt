package dev.whrite.opsgrid.shift

import dev.whrite.opsgrid.employee.Employee
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

enum class ShiftStatus { SCHEDULED, CANCELLED }

@Entity
@Table(name = "shifts")
class Shift(
    @Id val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    val employee: Employee,
    @Column(name = "starts_at", nullable = false)
    var startsAt: OffsetDateTime,
    @Column(name = "ends_at", nullable = false)
    var endsAt: OffsetDateTime,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ShiftStatus = ShiftStatus.SCHEDULED
)
