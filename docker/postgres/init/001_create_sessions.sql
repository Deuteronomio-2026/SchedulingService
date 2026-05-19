CREATE TABLE IF NOT EXISTS sessions (
  id UUID PRIMARY KEY,
  patient_id UUID,
  psychologist_id UUID,
  date DATE,
  start_time TIME,
  end_time TIME,
  type VARCHAR(255),
  attention_type VARCHAR(255),
  status VARCHAR(255),
  created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sessions_psychologist_date_start
  ON sessions (psychologist_id, date, start_time);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sessions_psychologist_slot
  ON sessions (psychologist_id, date, start_time);
