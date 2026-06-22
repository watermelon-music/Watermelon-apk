-- ============================================================
-- Fix: Change BEFORE triggers to AFTER triggers on profiles
-- This allows backfill UPDATEs to work without tuple-modified errors
-- ============================================================

DROP TRIGGER IF EXISTS on_profile_level_up ON public.profiles;
CREATE TRIGGER on_profile_level_up
  AFTER UPDATE ON public.profiles
  FOR EACH ROW EXECUTE PROCEDURE public.auto_level_up();

DROP TRIGGER IF EXISTS on_profile_rank_update ON public.profiles;
CREATE TRIGGER on_profile_rank_update
  AFTER UPDATE ON public.profiles
  FOR EACH ROW WHEN (NEW.hours_listened IS DISTINCT FROM OLD.hours_listened)
  EXECUTE PROCEDURE public.auto_assign_rank_on_hours();

-- ============================================================
-- Backfill gamification stats from listening_history
-- ============================================================

-- 1. Backfill core stats from listening_history
WITH user_stats AS (
  SELECT
    user_id,
    COUNT(*) as total_plays,
    COALESCE(SUM(duration_ms), 0.0) / 3600000.0 as total_hours
  FROM public.listening_history
  GROUP BY user_id
)
UPDATE public.profiles
SET
  songs_played = COALESCE(user_stats.total_plays, 0),
  songs_completed = COALESCE(user_stats.total_plays, 0),
  hours_listened = COALESCE(user_stats.total_hours, 0.0),
  minutes_listened = COALESCE(user_stats.total_hours, 0.0) * 60,
  xp_total = COALESCE(user_stats.total_plays, 0) * 12
FROM user_stats
WHERE public.profiles.id = user_stats.user_id;

-- 2. Compute rank tiers from hours
UPDATE public.profiles
SET rank_tier = CASE
    WHEN hours_listened >= 2000 THEN '👑 Eternal Echo'
    WHEN hours_listened >= 1600 THEN '🌈 Spectrum Lord'
    WHEN hours_listened >= 1200 THEN '🎼 Wave Architect'
    WHEN hours_listened >= 950  THEN '🌠 Celestia Tone'
    WHEN hours_listened >= 700  THEN '⚡ Soundrift'
    WHEN hours_listened >= 500  THEN '🔥 Reverb X'
    WHEN hours_listened >= 350  THEN '💿 Harmonic Flow'
    WHEN hours_listened >= 250  THEN '🌌 NovaBeat'
    WHEN hours_listened >= 160  THEN '🎵 Frequency Soul'
    WHEN hours_listened >= 100  THEN '📀 Vinyl Hunter'
    WHEN hours_listened >= 60   THEN '🎶 Resonance'
    WHEN hours_listened >= 35   THEN '🌊 Echo Drift'
    WHEN hours_listened >= 15   THEN '🎧 Pulse Rider'
    WHEN hours_listened >= 5    THEN '🍃 Sprout Wave'
    ELSE '🌱 Seed Listener'
END;

-- 3. Compute listening streaks from history (gaps-and-islands)
WITH user_dates AS (
  SELECT DISTINCT user_id, DATE(played_at) as d
  FROM public.listening_history
),
streaks AS (
  SELECT user_id, streak_length, last_date
  FROM (
    SELECT
      user_id,
      COUNT(*) as streak_length,
      MAX(d) as last_date,
      ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY MAX(d) DESC) as rn
    FROM (
      SELECT user_id, d, d - (ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY d))::int as grp
      FROM user_dates
    ) grouped
    GROUP BY user_id, grp
  ) ranked
  WHERE rn = 1  -- most recent streak
)
UPDATE public.profiles
SET
  streak_days = COALESCE(streaks.streak_length, 0),
  streak_last_played = streaks.last_date
FROM streaks
WHERE public.profiles.id = streaks.user_id;

-- 4. Compute level from XP (trigger handles this but explicit for safety)
UPDATE public.profiles
SET xp_level = GREATEST(1, FLOOR(SQRT(xp_total::FLOAT / 100.0))::INT);

-- 5. Award 'First Listener' badge to every user with history
INSERT INTO public.user_achievements (user_id, badge_id, badge_name, badge_emoji, description)
SELECT DISTINCT
    user_id,
    'first_listener',
    'First Listener',
    '🎧',
    'Started your music journey'
FROM public.listening_history
ON CONFLICT (user_id, badge_id) DO NOTHING;
