ALTER TABLE employee_availability
    ADD CONSTRAINT employee_availability_no_overlapping_windows
    EXCLUDE USING gist (
        employee_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    );
