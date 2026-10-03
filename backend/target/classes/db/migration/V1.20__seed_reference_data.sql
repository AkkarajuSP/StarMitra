-- V1.20  Baseline reference/configuration data — idempotent seeds only.
-- No business/test data. IDs are fixed UUIDs so seeds are deterministic.

-- System roles (capability roles required by the accepted authz model)
INSERT INTO system_roles (id, name, description) VALUES
    ('00000000-0000-7000-8000-000000000001', 'USER',        'Standard platform user'),
    ('00000000-0000-7000-8000-000000000002', 'JUDGE',       'Judge capability — scope comes from judge_assignments'),
    ('00000000-0000-7000-8000-000000000003', 'ADMIN',       'Administrative capability'),
    ('00000000-0000-7000-8000-000000000004', 'SUPER_ADMIN', 'Super administrative capability')
ON CONFLICT (name) DO NOTHING;
-- NOTE: MODERATOR role taxonomy remains an open product decision — not seeded.

-- Optional proficiency levels (baseline-proposed example scale; configurable)
INSERT INTO skill_proficiencies (id, code, label, ordinal) VALUES
    ('00000000-0000-7000-8000-000000000101', 'BEGINNER',      'Beginner',      1),
    ('00000000-0000-7000-8000-000000000102', 'INTERMEDIATE',  'Intermediate',  2),
    ('00000000-0000-7000-8000-000000000103', 'ADVANCED',      'Advanced',      3),
    ('00000000-0000-7000-8000-000000000104', 'PROFESSIONAL',  'Professional',  4)
ON CONFLICT (code) DO NOTHING;
