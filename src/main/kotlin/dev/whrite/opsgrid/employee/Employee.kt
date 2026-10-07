package dev.whrite.opsgrid.employee

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "employees")
class Employee(
    @Id
    val id: UUID,
    @Column(name = "employee_number", nullable = false, unique = true)
    val employeeNumber: String,
    @Column(name = "first_name", nullable = false)
    val firstName: String,
    @Column(name = "last_name", nullable = false)
    val lastName: String,
    @Column(nullable = false)
    val department: String,
    @Column(nullable = false)
    var active: Boolean = true
)
