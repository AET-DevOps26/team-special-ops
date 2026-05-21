-- Flyway migration: seed a demo user for local development and demos.
--
-- Login credentials:
--   email:    demo@example.com
--   password: password123      (plaintext — DEMO USE ONLY)
--
-- The password is stored as a BCrypt hash because the app never persists
-- plaintext passwords. The hash below was produced by the application's own
-- encoder (Spring BCryptPasswordEncoder, $2a, strength 10) and verified to
-- match "password123", so it works against POST /user-progress/auth/login.
-- Do NOT hand-edit the hash: regenerate it with the same encoder if you change
-- the password, otherwise login will fail.
--
-- NOTE: this ships a known credential to every environment the migration runs
-- in. That's fine for the MVP demo; rotate or remove it before any real
-- production deployment.

INSERT INTO users (id, email, password_hash)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'demo@example.com',
    '$2a$10$ZEa/SAorMo.Xzzp1aRui9ODUCimvcv23ueMZubN5QDzGvb3uQpcyG'
)
ON CONFLICT (email) DO NOTHING;
