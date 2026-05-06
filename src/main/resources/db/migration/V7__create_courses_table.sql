CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    level VARCHAR(10) NOT NULL,
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(255),
    file_size BIGINT,
    professor_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_courses_professor FOREIGN KEY (professor_id) REFERENCES professors(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_courses_professor_id ON courses(professor_id);
CREATE INDEX IF NOT EXISTS idx_courses_level ON courses(level);
