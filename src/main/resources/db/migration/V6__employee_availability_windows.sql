CREATE TABLE employee_availability (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT employee_availability_valid_window CHECK (ends_at > starts_at)
);

CREATE INDEX idx_employee_availability_employee_window
    ON employee_availability(employee_id, starts_at, ends_at);

CREATE FUNCTION enforce_employee_availability()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    -- Serialize availability and shift writes per employee. Existing shifts are
    -- grandfathered when availability is first configured; subsequent shift
    -- inserts/updates must fit inside a configured window.
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.employee_id::text, 0));

    IF TG_TABLE_NAME = 'shifts' THEN
        IF NEW.status = 'SCHEDULED' THEN
            IF EXISTS (
                SELECT 1 FROM employee_availability a
                WHERE a.employee_id = NEW.employee_id
            ) AND NOT EXISTS (
                SELECT 1 FROM employee_availability a
                WHERE a.employee_id = NEW.employee_id
                  AND a.starts_at <= NEW.starts_at
                  AND a.ends_at >= NEW.ends_at
            ) THEN
                RAISE EXCEPTION 'Scheduled shift falls outside employee availability'
                    USING ERRCODE = '23514',
                          CONSTRAINT = 'shifts_within_employee_availability';
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER enforce_shift_employee_availability
BEFORE INSERT OR UPDATE ON shifts
FOR EACH ROW EXECUTE FUNCTION enforce_employee_availability();

-- This trigger takes the same lock as shift writes. It intentionally does not
-- reject legacy shifts when an employee first receives availability windows.
CREATE TRIGGER serialize_employee_availability_writes
BEFORE INSERT OR UPDATE ON employee_availability
FOR EACH ROW EXECUTE FUNCTION enforce_employee_availability();
