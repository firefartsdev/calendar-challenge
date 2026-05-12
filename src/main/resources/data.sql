-- Seed data — all inserts are idempotent (safe to run on every startup)

-- -----------------------------------------------------------------------
-- Meetings
-- -----------------------------------------------------------------------
INSERT INTO meetings (id, title, description, version)
VALUES
    ('01960000-0000-7000-8000-000000000001', 'Team Sync',       'Weekly team sync',          0),
    ('01960000-0000-7000-8000-000000000002', 'Design Review',   'Q2 design review session',  0)
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------
-- Meeting participants
-- -----------------------------------------------------------------------
INSERT INTO meeting_participants (meeting_id, participant)
SELECT '01960000-0000-7000-8000-000000000001', 'alice'
WHERE NOT EXISTS (
    SELECT 1 FROM meeting_participants
    WHERE meeting_id = '01960000-0000-7000-8000-000000000001' AND participant = 'alice'
);
INSERT INTO meeting_participants (meeting_id, participant)
SELECT '01960000-0000-7000-8000-000000000001', 'bob'
WHERE NOT EXISTS (
    SELECT 1 FROM meeting_participants
    WHERE meeting_id = '01960000-0000-7000-8000-000000000001' AND participant = 'bob'
);
INSERT INTO meeting_participants (meeting_id, participant)
SELECT '01960000-0000-7000-8000-000000000002', 'carol'
WHERE NOT EXISTS (
    SELECT 1 FROM meeting_participants
    WHERE meeting_id = '01960000-0000-7000-8000-000000000002' AND participant = 'carol'
);
INSERT INTO meeting_participants (meeting_id, participant)
SELECT '01960000-0000-7000-8000-000000000002', 'dave'
WHERE NOT EXISTS (
    SELECT 1 FROM meeting_participants
    WHERE meeting_id = '01960000-0000-7000-8000-000000000002' AND participant = 'dave'
);

-- -----------------------------------------------------------------------
-- Time slots
-- -----------------------------------------------------------------------
INSERT INTO time_slots (id, owner, start_at, end_at, busy, meeting_id, version)
VALUES
    -- alice: two free slots + one busy (Team Sync with bob)
    ('01960001-0000-7000-8000-000000000001', 'alice', '2026-05-13 08:00:00Z', '2026-05-13 09:00:00Z', false, NULL,                                         0),
    ('01960001-0000-7000-8000-000000000002', 'alice', '2026-05-13 09:00:00Z', '2026-05-13 10:00:00Z', true,  '01960000-0000-7000-8000-000000000001', 0),
    ('01960001-0000-7000-8000-000000000003', 'alice', '2026-05-13 10:00:00Z', '2026-05-13 12:00:00Z', false, NULL,                                         0),
    ('01960001-0000-7000-8000-000000000004', 'alice', '2026-05-14 09:00:00Z', '2026-05-14 11:00:00Z', false, NULL,                                         0),
    ('01960001-0000-7000-8000-000000000005', 'alice', '2026-05-15 10:00:00Z', '2026-05-15 12:00:00Z', false, NULL,                                         0),

    -- bob: two free slots + one busy (Team Sync with alice)
    ('01960002-0000-7000-8000-000000000001', 'bob',   '2026-05-13 08:00:00Z', '2026-05-13 09:00:00Z', false, NULL,                                         0),
    ('01960002-0000-7000-8000-000000000002', 'bob',   '2026-05-13 09:00:00Z', '2026-05-13 10:00:00Z', true,  '01960000-0000-7000-8000-000000000001', 0),
    ('01960002-0000-7000-8000-000000000003', 'bob',   '2026-05-13 14:00:00Z', '2026-05-13 16:00:00Z', false, NULL,                                         0),
    ('01960002-0000-7000-8000-000000000004', 'bob',   '2026-05-14 10:00:00Z', '2026-05-14 12:00:00Z', false, NULL,                                         0),

    -- carol: one free slot + one busy (Design Review with dave)
    ('01960003-0000-7000-8000-000000000001', 'carol', '2026-05-13 10:00:00Z', '2026-05-13 12:00:00Z', false, NULL,                                         0),
    ('01960003-0000-7000-8000-000000000002', 'carol', '2026-05-14 14:00:00Z', '2026-05-14 16:00:00Z', true,  '01960000-0000-7000-8000-000000000002', 0),
    ('01960003-0000-7000-8000-000000000003', 'carol', '2026-05-15 09:00:00Z', '2026-05-15 11:00:00Z', false, NULL,                                         0),

    -- dave: one free slot + one busy (Design Review with carol)
    ('01960004-0000-7000-8000-000000000001', 'dave',  '2026-05-13 09:00:00Z', '2026-05-13 11:00:00Z', false, NULL,                                         0),
    ('01960004-0000-7000-8000-000000000002', 'dave',  '2026-05-14 14:00:00Z', '2026-05-14 16:00:00Z', true,  '01960000-0000-7000-8000-000000000002', 0),
    ('01960004-0000-7000-8000-000000000003', 'dave',  '2026-05-15 09:00:00Z', '2026-05-15 11:00:00Z', false, NULL,                                         0)
ON CONFLICT (id) DO NOTHING;
