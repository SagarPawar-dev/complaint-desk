-- ==============================================================================
-- College Complaint Management System - Supabase PostgreSQL Schema (Phase 3)
-- ==============================================================================
-- Description : Complete DDL and initial demo seed data.
-- Database    : PostgreSQL (Supabase)
-- Instructions: Copy and paste this script into the Supabase SQL Editor and run it.
-- ==============================================================================

-- 1. CLEANUP (Drop tables if they exist in reverse dependency order)
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS complaint_timeline CASCADE;
DROP TABLE IF EXISTS complaints CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ==============================================================================
-- 2. TABLE CREATION
-- ==============================================================================

-- Table: users
-- Stores profiles and credentials for both Students and Administrators
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    department VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'ADMIN')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: complaints
-- Stores grievance tickets with status, priority and category
CREATE TABLE complaints (
    id VARCHAR(20) PRIMARY KEY, -- e.g. CMP-00001, CMP-00002
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('Low', 'Medium', 'High', 'Urgent')),
    location VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUBMITTED', 'IN PROGRESS', 'RESOLVED', 'CLOSED')),
    student_id INT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: complaint_timeline
-- Stores the chronological activity log and administrative responses for each ticket
CREATE TABLE complaint_timeline (
    id BIGSERIAL PRIMARY KEY,
    complaint_id VARCHAR(20) NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL,       -- e.g. 'Complaint Submitted', 'Status Updated to IN PROGRESS', 'Admin Response'
    performed_by VARCHAR(100) NOT NULL, -- e.g. 'Om Sharma (Student)', 'System Administrator'
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    note TEXT,                          -- Admin remarks or remarks accompanying the action
    status VARCHAR(25) NOT NULL,        -- Lifecycle state at the time of the event
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table: notifications
-- Tracks alerts for students and administrators, including unread/read state
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    complaint_id VARCHAR(20) REFERENCES complaints(id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_role VARCHAR(20) NOT NULL CHECK (recipient_role IN ('STUDENT', 'ADMIN')),
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,          -- e.g. 'NEW_SUBMISSION', 'PENDING', 'IN PROGRESS', 'RESOLVED', 'RESPONSE'
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 3. INDEXES FOR PERFORMANCE
-- ==============================================================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_complaints_student_id ON complaints(student_id);
CREATE INDEX idx_complaints_status ON complaints(status);
CREATE INDEX idx_timeline_complaint_id ON complaint_timeline(complaint_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_role_read ON notifications(recipient_role, is_read);

-- ==============================================================================
-- 4. DEMO SEED DATA (Matching UI from Phase 1 & 2)
-- ==============================================================================

-- A. Seed Users
-- Password for student@test.com is 'student123'
-- Password for admin@test.com   is 'admin123'
-- Hashed using SHA-256 for secure, portable verification
INSERT INTO users (id, full_name, email, password_hash, department, role, status) VALUES
(1, 'Om Sharma', 'student@test.com', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'Computer Science & Engineering', 'STUDENT', 'ACTIVE'),
(2, 'System Administrator', 'admin@test.com', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'College Administration', 'ADMIN', 'ACTIVE');

-- Synchronize serial sequence for users table
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- B. Seed Complaints
INSERT INTO complaints (id, title, category, priority, location, description, status, student_id, created_at, updated_at) VALUES
(
    'CMP-00001',
    'Classroom Projector Not Working',
    'IT / Equipment',
    'High',
    'Block B, Room 204',
    'Projector lamp is not turning on and HDMI cable socket is damaged.',
    'CLOSED',
    1,
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '9 days'
),
(
    'CMP-00002',
    'fan not working',
    'Electrical',
    'Medium',
    'Library 1st Floor, Reading Hall (Fan #3)',
    'Ceiling fan #3 makes a loud rattling noise and stopped spinning completely since yesterday.',
    'PENDING',
    1,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'
);

-- C. Seed Complaint Timeline
INSERT INTO complaint_timeline (complaint_id, action, performed_by, user_id, note, status, created_at) VALUES
-- Timeline for CMP-00001
('CMP-00001', 'Complaint Submitted', 'Om Sharma (Student)', 1, 'Initial report of projector failure.', 'PENDING', NOW() - INTERVAL '10 days'),
('CMP-00001', 'Status Updated to IN PROGRESS', 'System Administrator', 2, 'Technician assigned to inspect the projector in Room 204.', 'IN PROGRESS', NOW() - INTERVAL '9 days 20 hours'),
('CMP-00001', 'Complaint Resolved & Closed', 'System Administrator', 2, 'Replaced lamp module and renewed HDMI wall socket. Verified functional.', 'CLOSED', NOW() - INTERVAL '9 days 16 hours'),

-- Timeline for CMP-00002
('CMP-00002', 'Complaint Submitted', 'Om Sharma (Student)', 1, 'Initial grievance submitted with Medium priority.', 'PENDING', NOW() - INTERVAL '2 days');

-- D. Seed Notifications
-- NOTIF-1: For student (Resolved notification)
INSERT INTO notifications (complaint_id, user_id, recipient_role, title, message, type, is_read, created_at) VALUES
('CMP-00001', 1, 'STUDENT', 'Complaint Resolved (CMP-00001)', 'Admin closed ''Classroom Projector Not Working''. Remark: Replaced lamp module and renewed HDMI socket.', 'RESOLVED', FALSE, NOW() - INTERVAL '9 days 16 hours'),

-- NOTIF-2: For admin (New submission notification)
('CMP-00002', 2, 'ADMIN', 'New Complaint Submitted (CMP-00002)', 'Om Sharma submitted ''fan not working'' in Library 1st Floor.', 'NEW_SUBMISSION', FALSE, NOW() - INTERVAL '2 days'),

-- NOTIF-3: For student (Review status notification)
('CMP-00002', 1, 'STUDENT', 'Complaint Under Review (CMP-00002)', 'Admin started reviewing ''fan not working'' in Library 1st Floor.', 'PENDING', FALSE, NOW() - INTERVAL '1 day 23 hours');

-- ==============================================================================
-- 5. VERIFICATION QUERY (Run this to confirm counts after execution)
-- ==============================================================================
SELECT 'users' AS table_name, COUNT(*) AS total_rows FROM users
UNION ALL
SELECT 'complaints', COUNT(*) FROM complaints
UNION ALL
SELECT 'complaint_timeline', COUNT(*) FROM complaint_timeline
UNION ALL
SELECT 'notifications', COUNT(*) FROM notifications;
