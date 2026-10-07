CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE shifts
    ADD CONSTRAINT shifts_no_overlapping_employee_windows
    EXCLUDE USING gist (
        employee_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status = 'SCHEDULED');

ALTER TABLE employee_leave
    ADD CONSTRAINT leave_no_overlapping_employee_windows
    EXCLUDE USING gist (
        employee_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status = 'APPROVED');
