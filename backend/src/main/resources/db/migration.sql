-- Idempotent schema migration — safe to run multiple times.
-- Adds columns introduced after the initial schema was created.

-- jobs: status column (application tracking)
ALTER TABLE "jobs"
    ADD COLUMN IF NOT EXISTS "status" VARCHAR(255) NOT NULL DEFAULT 'SAVED';

-- jobs: notes column (free-text notes per target)
ALTER TABLE "jobs"
    ADD COLUMN IF NOT EXISTS "notes" TEXT;
