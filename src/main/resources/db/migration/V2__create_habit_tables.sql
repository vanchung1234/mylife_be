-- Bảng habits
CREATE TABLE IF NOT EXISTS habits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(7),
    type VARCHAR(20) NOT NULL,
    goal DOUBLE PRECISION,
    unit VARCHAR(50),
    frequency_type VARCHAR(20) NOT NULL,
    frequency_data JSONB,
    start_date DATE NOT NULL,
    end_date DATE,
    paused BOOLEAN DEFAULT FALSE,
    reminder_time TIME,
    remind_end_of_day BOOLEAN DEFAULT FALSE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Bảng habit_logs
CREATE TABLE IF NOT EXISTS habit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id UUID NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    log_date DATE NOT NULL,
    completed BOOLEAN,
    value DOUBLE PRECISION,
    note TEXT,
    is_makeup BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_habit_log_date UNIQUE (habit_id, log_date)
);

-- Bảng habit_streaks
CREATE TABLE IF NOT EXISTS habit_streaks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id UUID NOT NULL UNIQUE REFERENCES habits(id) ON DELETE CASCADE,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    last_log_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_habits_user_id ON habits(user_id);
CREATE INDEX idx_habit_logs_habit_id ON habit_logs(habit_id);
CREATE INDEX idx_habit_logs_log_date ON habit_logs(log_date);
CREATE INDEX idx_habit_streaks_habit_id ON habit_streaks(habit_id);