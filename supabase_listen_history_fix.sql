-- ============================================================
-- FIX: Gamification triggers on listening_history
-- Run this in Supabase SQL Editor
--
-- Problem: App writes to listening_history, but gamification
-- triggers were on play_sessions. Profiles never auto-update.
-- This script creates triggers on listening_history.
-- ============================================================

-- 0. Ensure gamification columns exist (safe to re-run)
ALTER TABLE public.profiles
ADD COLUMN IF NOT EXISTS xp_total BIGINT DEFAULT 0,
ADD COLUMN IF NOT EXISTS xp_level INT DEFAULT 1,
ADD COLUMN IF NOT EXISTS rank_tier TEXT DEFAULT '🌱 Seed Listener',
ADD COLUMN IF NOT EXISTS hours_listened FLOAT DEFAULT 0,
ADD COLUMN IF NOT EXISTS minutes_listened FLOAT DEFAULT 0,
ADD COLUMN IF NOT EXISTS streak_days INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS streak_last_played DATE,
ADD COLUMN IF NOT EXISTS longest_streak INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS songs_completed INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS songs_played INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS artists_discovered INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS playlists_created INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS liked_songs_count INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS shared_tracks_count INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS top_genre TEXT,
ADD COLUMN IF NOT EXISTS top_artist TEXT,
ADD COLUMN IF NOT EXISTS last_login DATE,
ADD COLUMN IF NOT EXISTS weekly_hours FLOAT DEFAULT 0,
ADD COLUMN IF NOT EXISTS monthly_hours FLOAT DEFAULT 0,
ADD COLUMN IF NOT EXISTS night_hours FLOAT DEFAULT 0,
ADD COLUMN IF NOT EXISTS is_profile_public BOOLEAN DEFAULT TRUE;

-- 1. Drop old listening_history trigger if it only trims
DROP TRIGGER IF EXISTS on_history_inserted ON public.listening_history;

-- 2. Create a combined trigger: trim history + update gamification stats
CREATE OR REPLACE FUNCTION public.handle_listening_history_insert()
RETURNS TRIGGER AS $$
DECLARE
    play_duration_minutes FLOAT;
    play_duration_hours FLOAT;
    current_streak INT;
    current_last_played DATE;
BEGIN
    -- Trim history to last 50 rows per user (copied from original)
    DELETE FROM public.listening_history
    WHERE user_id = NEW.user_id
      AND id NOT IN (
        SELECT id FROM public.listening_history
        WHERE user_id = NEW.user_id
        ORDER BY played_at DESC
        LIMIT 50
      );

    -- Calculate duration
    play_duration_hours := COALESCE(NEW.duration_ms, 0) / 3600000.0;
    play_duration_minutes := COALESCE(NEW.duration_ms, 0) / 60000.0;

    -- Get current streak info
    SELECT streak_days, streak_last_played
    INTO current_streak, current_last_played
    FROM public.profiles WHERE id = NEW.user_id;

    -- Update profile gamification stats
    UPDATE public.profiles SET
      songs_played = COALESCE(songs_played, 0) + 1,
      songs_completed = COALESCE(songs_completed, 0) + 1,
      xp_total = COALESCE(xp_total, 0) + 12,
      minutes_listened = COALESCE(minutes_listened, 0) + play_duration_minutes,
      hours_listened = COALESCE(hours_listened, 0) + play_duration_hours,
      streak_days = CASE
        WHEN current_last_played = CURRENT_DATE - 1 THEN COALESCE(current_streak, 0) + 1
        WHEN current_last_played = CURRENT_DATE THEN COALESCE(current_streak, 0)
        ELSE 1
      END,
      streak_last_played = CURRENT_DATE,
      longest_streak = GREATEST(
        COALESCE(longest_streak, 0),
        CASE
          WHEN current_last_played = CURRENT_DATE - 1 THEN COALESCE(current_streak, 0) + 1
          WHEN current_last_played = CURRENT_DATE THEN COALESCE(current_streak, 0)
          ELSE 1
        END
      )
    WHERE id = NEW.user_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_history_inserted
  AFTER INSERT ON public.listening_history
  FOR EACH ROW EXECUTE PROCEDURE public.handle_listening_history_insert();

-- 3. Auto-assign rank based on hours_listened after any profile update
CREATE OR REPLACE FUNCTION public.auto_assign_rank_on_hours()
RETURNS TRIGGER AS $$
BEGIN
  NEW.rank_tier := CASE
    WHEN NEW.hours_listened >= 2000 THEN '👑 Eternal Echo'
    WHEN NEW.hours_listened >= 1600 THEN '🌈 Spectrum Lord'
    WHEN NEW.hours_listened >= 1200 THEN '🎼 Wave Architect'
    WHEN NEW.hours_listened >= 950  THEN '🌠 Celestia Tone'
    WHEN NEW.hours_listened >= 700  THEN '⚡ Soundrift'
    WHEN NEW.hours_listened >= 500  THEN '🔥 Reverb X'
    WHEN NEW.hours_listened >= 350  THEN '💿 Harmonic Flow'
    WHEN NEW.hours_listened >= 250  THEN '🌌 NovaBeat'
    WHEN NEW.hours_listened >= 160  THEN '🎵 Frequency Soul'
    WHEN NEW.hours_listened >= 100  THEN '📀 Vinyl Hunter'
    WHEN NEW.hours_listened >= 60   THEN '🎶 Resonance'
    WHEN NEW.hours_listened >= 35   THEN '🌊 Echo Drift'
    WHEN NEW.hours_listened >= 15   THEN '🎧 Pulse Rider'
    WHEN NEW.hours_listened >= 5    THEN '🍃 Sprout Wave'
    ELSE '🌱 Seed Listener'
  END;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_profile_rank_update ON public.profiles;
CREATE TRIGGER on_profile_rank_update
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW WHEN (NEW.hours_listened IS DISTINCT FROM OLD.hours_listened)
  EXECUTE PROCEDURE public.auto_assign_rank_on_hours();

-- 4. Auto-level up based on XP
CREATE OR REPLACE FUNCTION public.auto_level_up()
RETURNS TRIGGER AS $$
DECLARE
  new_level INT;
BEGIN
  new_level := GREATEST(1, FLOOR(SQRT(GREATEST(0, NEW.xp_total) / 100.0))::INT);
  IF new_level > COALESCE(NEW.xp_level, 1) THEN
    NEW.xp_level := new_level;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_profile_level_up ON public.profiles;
CREATE TRIGGER on_profile_level_up
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW EXECUTE PROCEDURE public.auto_level_up();

-- 5. ONE-TIME BACKFILL from existing listening_history
UPDATE public.profiles
SET songs_played = COALESCE(stats.plays, 0),
    songs_completed = COALESCE(stats.plays, 0),
    hours_listened = COALESCE(stats.hours, 0.0),
    minutes_listened = COALESCE(stats.hours, 0.0) * 60,
    xp_total = COALESCE(stats.plays, 0) * 12
FROM (
  SELECT user_id, COUNT(*) as plays, COALESCE(SUM(duration_ms), 0.0) / 3600000.0 as hours
  FROM public.listening_history
  GROUP BY user_id
) stats
WHERE public.profiles.id = stats.user_id;

-- 6. Compute rank for all profiles after backfill
UPDATE public.profiles SET rank_tier = CASE
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

-- 7. Compute streaks from listening_history
WITH user_dates AS (
  SELECT DISTINCT user_id, DATE(played_at) as d FROM public.listening_history
),
streaks AS (
  SELECT user_id, streak_length, last_date
  FROM (
    SELECT user_id, COUNT(*) as streak_length, MAX(d) as last_date,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY MAX(d) DESC) as rn
    FROM (
      SELECT user_id, d,
             d - (ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY d))::int as grp
      FROM user_dates
    ) grouped
    GROUP BY user_id, grp
  ) ranked
  WHERE rn = 1
)
UPDATE public.profiles
SET streak_days = s.streak_length,
    streak_last_played = s.last_date
FROM streaks s
WHERE public.profiles.id = s.user_id;

-- 8. Award first_listener badge to users with history
INSERT INTO public.user_achievements (user_id, badge_id, badge_name, badge_emoji, description)
SELECT DISTINCT user_id, 'first_listener', 'First Listener', '🎧', 'Started your listening journey'
FROM public.listening_history
ON CONFLICT (user_id, badge_id) DO NOTHING;

-- 9. RLS for profiles policies (ensure gamification fields accessible)
DROP POLICY IF EXISTS "Profiles: users view own" ON public.profiles;
DROP POLICY IF EXISTS "Profiles: users update own" ON public.profiles;
DROP POLICY IF EXISTS "Profiles: users insert own" ON public.profiles;

CREATE POLICY "Profiles: users view own" ON public.profiles
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Profiles: users update own" ON public.profiles
  FOR UPDATE USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

CREATE POLICY "Profiles: users insert own" ON public.profiles
  FOR INSERT WITH CHECK (auth.uid() = id);

-- DONE
