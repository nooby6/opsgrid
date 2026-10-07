CREATE TABLE employee_leave (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT employee_leave_valid_window CHECK (ends_at > starts_at)
);
CREATE INDEX idx_employee_leave_window ON employee_leave(employee_id, starts_at, ends_at);
