-- Tip catalog. Rows carry i18n keys; the translated text lives in the
-- frontend bundles (es.json / en.json). Phase tips are deliberately neutral:
-- they suggest asking and caring, never assert how someone feels.

INSERT INTO daily_tips (message_key, category, cycle_phase, relationship_stage) VALUES
-- Stage: NEW (0-3 months)
('tips.stage.new.communication',  'COMMUNICATION', NULL, 'NEW'),
('tips.stage.new.boundaries',     'GENERAL',       NULL, 'NEW'),
('tips.stage.new.expectations',   'GENERAL',       NULL, 'NEW'),
('tips.stage.new.consent',        'INTIMACY',      NULL, 'NEW'),
-- Stage: DEVELOPING (3-12 months)
('tips.stage.developing.deeper',      'COMMUNICATION', NULL, 'DEVELOPING'),
('tips.stage.developing.disagreements','CONFLICT',     NULL, 'DEVELOPING'),
('tips.stage.developing.newActivity', 'DATE_IDEA',     NULL, 'DEVELOPING'),
('tips.stage.developing.routines',    'GENERAL',       NULL, 'DEVELOPING'),
-- Stage: ESTABLISHED (1-3 years)
('tips.stage.established.routine',  'DATE_IDEA',     NULL, 'ESTABLISHED'),
('tips.stage.established.goals',    'GENERAL',       NULL, 'ESTABLISHED'),
('tips.stage.established.details',  'AFFECTION',     NULL, 'ESTABLISHED'),
('tips.stage.established.deepTalk', 'COMMUNICATION', NULL, 'ESTABLISHED'),
-- Stage: LONG_TERM (3-7 years)
('tips.stage.longTerm.growth',        'GENERAL',   NULL, 'LONG_TERM'),
('tips.stage.longTerm.individuality', 'SELF_CARE', NULL, 'LONG_TERM'),
('tips.stage.longTerm.newExperience', 'DATE_IDEA', NULL, 'LONG_TERM'),
('tips.stage.longTerm.intimacy',      'INTIMACY',  NULL, 'LONG_TERM'),
-- Stage: VERY_LONG_TERM (7+ years)
('tips.stage.veryLongTerm.memories',  'AFFECTION',     NULL, 'VERY_LONG_TERM'),
('tips.stage.veryLongTerm.changes',   'COMMUNICATION', NULL, 'VERY_LONG_TERM'),
('tips.stage.veryLongTerm.projects',  'GENERAL',       NULL, 'VERY_LONG_TERM'),
('tips.stage.veryLongTerm.curiosity', 'COMMUNICATION', NULL, 'VERY_LONG_TERM'),
-- Phase: MENSTRUATION
('tips.phase.menstruation.comfort', 'SUPPORT', 'MENSTRUATION', NULL),
('tips.phase.menstruation.ask',     'GENERAL', 'MENSTRUATION', NULL),
-- Phase: FOLLICULAR
('tips.phase.follicular.plan',    'DATE_IDEA', 'FOLLICULAR', NULL),
('tips.phase.follicular.checkIn', 'GENERAL',   'FOLLICULAR', NULL),
-- Phase: OVULATION
('tips.phase.ovulation.estimate', 'GENERAL', 'OVULATION', NULL),
-- Phase: LUTEAL
('tips.phase.luteal.patience', 'SUPPORT',   'LUTEAL', NULL),
('tips.phase.luteal.selfCare', 'SELF_CARE', 'LUTEAL', NULL),
-- Generic fallbacks
('tips.general.listen',    'COMMUNICATION', NULL, NULL),
('tips.general.smallGesture', 'AFFECTION',  NULL, NULL),
('tips.general.time',      'GENERAL',       NULL, NULL),
('tips.general.gratitude', 'GENERAL',       NULL, NULL);
