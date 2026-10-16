CREATE FUNCTION prevent_shift_leave_overlap()
RETURNS trigger
LANGUAGE plpgsql
AS '
BEGIN
    -- Both tables use the same transaction-scoped lock for each employee.
    -- This serializes cross-table checks under concurrent shift/leave writes.
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.employee_id::text, 0));

    IF TG_TABLE_NAME = ''shifts'' AND NEW.status = ''SCHEDULED'' THEN
        IF EXISTS (
            SELECT 1
            FROM employee_leave l
            WHERE l.employee_id = NEW.employee_id
              AND l.status = ''APPROVED''
              AND tstzrange(l.starts_at, l.ends_at, ''[)'')
                  && tstzrange(NEW.starts_at, NEW.ends_at, ''[)'')
        ) THEN
            RAISE EXCEPTION ''Scheduled shift overlaps approved leave''
                USING ERRCODE = ''23P01'',
                      CONSTRAINT = ''shifts_no_overlapping_employee_windows'';
        END IF;
    ELSIF TG_TABLE_NAME = ''employee_leave'' AND NEW.status = ''APPROVED'' THEN
        IF EXISTS (
            SELECT 1
            FROM shifts s
            WHERE s.employee_id = NEW.employee_id
              AND s.status = ''SCHEDULED''
              AND tstzrange(s.starts_at, s.ends_at, ''[)'')
                  && tstzrange(NEW.starts_at, NEW.ends_at, ''[)'')
        ) THEN
            RAISE EXCEPTION ''Approved leave overlaps a scheduled shift''
                USING ERRCODE = ''23P01'',
                      CONSTRAINT = ''leave_no_overlapping_employee_windows'';
        END IF;
    END IF;

    RETURN NEW;
END;
';

CREATE TRIGGER prevent_shift_leave_overlap_on_shift
BEFORE INSERT OR UPDATE ON shifts
FOR EACH ROW EXECUTE FUNCTION prevent_shift_leave_overlap();

CREATE TRIGGER prevent_shift_leave_overlap_on_leave
BEFORE INSERT OR UPDATE ON employee_leave
FOR EACH ROW EXECUTE FUNCTION prevent_shift_leave_overlap();
