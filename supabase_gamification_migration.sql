-- ============================================================
-- Watermelon Music — Gamified Profile System Migration
-- Run this AFTER the base supabase_setup.sql in Supabase Dashboard
-- ============================================================

-- ------------------------------------------------------------
-- 1. EXTEND PROFILES TABLE WITH GAMIFICATION FIELDS
-- ------------------------------------------------------------
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

-- Rank tier index for fast leaderboard sorting
CREATE INDEX IF NOT EXISTS idx_profiles_rank ON public.profiles(xp_total DESC);
CREATE INDEX IF NOT EXISTS idx_profiles_streak ON public.profiles(streak_days DESC);

-- Reset RLS profiles policies to include new gamification fields
DROP POLICY IF EXISTS "Profiles: users view own" ON public.profiles;
DROP POLICY IF EXISTS "Profiles: users update own" ON public.profiles;
DROP POLICY IF EXISTS "Profiles: users insert own" ON public.profiles;

CREATE POLICY "Profiles: users view own" ON public.profiles
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Profiles: users update own" ON public.profiles
  FOR UPDATE USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

CREATE POLICY "Profiles: users insert own" ON public.profiles
  FOR INSERT WITH CHECK (auth.uid() = id);

-- Public profile view: other users can see limited profile info
DROP POLICY IF EXISTS "Profiles: public view" ON public.profiles;
CREATE POLICY "Profiles: public view" ON public.profiles
  FOR SELECT USING (is_profile_public = TRUE OR auth.uid() = id);

-- ------------------------------------------------------------
-- 2. USER ACHIEVEMENTS (BADGES) TABLE
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_achievements (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id uuid REFERENCES auth.users ON DELETE CASCADE NOT NULL,
  badge_id TEXT NOT NULL,
  badge_name TEXT NOT NULL,
  badge_emoji TEXT NOT NULL,
  description TEXT,
  unlocked_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
  UNIQUE (user_id, badge_id)
);

ALTER TABLE public.user_achievements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Achievements: users view own" ON public.user_achievements
  FOR SELECT USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_user_achievements_user ON public.user_achievements(user_id);

-- ------------------------------------------------------------
-- 3. PLAY SESSIONS (Valid Play Tracking)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.play_sessions (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id uuid REFERENCES auth.users ON DELETE CASCADE NOT NULL,
  song_id TEXT NOT NULL,
  title TEXT NOT NULL,
  artist TEXT,
  cover_url TEXT,
  listen_duration_ms BIGINT DEFAULT 0,
  song_duration_ms BIGINT DEFAULT 0,
  started_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
  ended_at TIMESTAMP WITH TIME ZONE,
  is_valid BOOLEAN DEFAULT FALSE
);

ALTER TABLE public.play_sessions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Sessions: users CRUD own" ON public.play_sessions
  FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_play_sessions_user_valid ON public.play_sessions(user_id, is_valid);
CREATE INDEX IF NOT EXISTS idx_play_sessions_user_date ON public.play_sessions(user_id, started_at DESC);

-- Auto-trim play_sessions to last 30 days per user (keep recent activity)
CREATE OR REPLACE FUNCTION public.trim_play_sessions()
RETURNS TRIGGER AS $$
BEGIN
  DELETE FROM public.play_sessions
  WHERE user_id = NEW.user_id
    AND id NOT IN (
      SELECT id FROM public.play_sessions
      WHERE user_id = NEW.user_id
      ORDER BY started_at DESC
      LIMIT 200
    );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_play_session_inserted ON public.play_sessions;
CREATE TRIGGER on_play_session_inserted
  AFTER INSERT ON public.play_sessions
  FOR EACH ROW EXECUTE PROCEDURE public.trim_play_sessions();

-- ------------------------------------------------------------
-- 4. XP AWARD TRIGGER (12 XP per valid play = 2 for play + 10 for complete)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.award_xp_on_valid_play()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.is_valid = TRUE THEN
    UPDATE public.profiles SET
      xp_total = COALESCE(xp_total, 0) + 12,
      minutes_listened = COALESCE(minutes_listened, 0) + (NEW.listen_duration_ms / 60000.0),
      hours_listened = COALESCE(hours_listened, 0) + (NEW.listen_duration_ms / 3600000.0),
      songs_completed = COALESCE(songs_completed, 0) + 1,
      songs_played = COALESCE(songs_played, 0) + 1
    WHERE id = NEW.user_id;
  ELSE
    UPDATE public.profiles SET
      songs_played = COALESCE(songs_played, 0) + 1
    WHERE id = NEW.user_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_valid_play_xp ON public.play_sessions;
CREATE TRIGGER on_valid_play_xp
  AFTER INSERT ON public.play_sessions
  FOR EACH ROW EXECUTE PROCEDURE public.award_xp_on_valid_play();

-- ------------------------------------------------------------
-- 5. STREAK TRACKING TRIGGER
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.update_streak_on_play()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.is_valid = TRUE AND NEW.user_id IS NOT NULL THEN
    UPDATE public.profiles SET
      streak_days = CASE
        WHEN streak_last_played = CURRENT_DATE - 1 THEN COALESCE(streak_days, 0) + 1
        WHEN streak_last_played = CURRENT_DATE THEN COALESCE(streak_days, 0)
        ELSE 1
      END,
      streak_last_played = CURRENT_DATE,
      longest_streak = GREATEST(COALESCE(longest_streak, 0), COALESCE(streak_days, 0) + CASE WHEN streak_last_played = CURRENT_DATE - 1 THEN 1 ELSE 0 END)
    WHERE id = NEW.user_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_valid_play_streak ON public.play_sessions;
CREATE TRIGGER on_valid_play_streak
  AFTER INSERT ON public.play_sessions
  FOR EACH ROW WHEN (NEW.is_valid = TRUE)
  EXECUTE PROCEDURE public.update_streak_on_play();

-- ------------------------------------------------------------
-- 6. LEVEL-UP FUNCTION (XP → Level)
-- XP required per level = level² × 100
-- e.g. Level 1→2 needs 100, 5→6 needs 2500, 10→11 needs 10000
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.calculate_level(xp BIGINT)
RETURNS INT AS $$
BEGIN
  RETURN GREATEST(1, FLOOR(SQRT(xp::FLOAT / 100.0))::INT);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Auto-level-up after XP insert
CREATE OR REPLACE FUNCTION public.auto_level_up()
RETURNS TRIGGER AS $$
BEGIN
  NEW.xp_level := public.calculate_level(COALESCE(NEW.xp_total, 0));
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_profile_level_up ON public.profiles;
CREATE TRIGGER on_profile_level_up
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW EXECUTE PROCEDURE public.auto_level_up();

-- ------------------------------------------------------------
-- 7. RANK ASSIGNMENT FUNCTION (16 music-themed tiers)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.assign_user_rank(target_user_id uuid)
RETURNS TEXT AS $$
DECLARE
  hours FLOAT;
  tier TEXT;
BEGIN
  SELECT COALESCE(hours_listened, 0) INTO hours FROM public.profiles WHERE id = target_user_id;
  tier := CASE
    WHEN hours >= 2000 THEN '👑 Eternal Echo'
    WHEN hours >= 1600 THEN '🌈 Spectrum Lord'
    WHEN hours >= 1200 THEN '🎼 Wave Architect'
    WHEN hours >= 950  THEN '🌠 Celestia Tone'
    WHEN hours >= 700  THEN '⚡ Soundrift'
    WHEN hours >= 500  THEN '🔥 Reverb X'
    WHEN hours >= 350  THEN '💿 Harmonic Flow'
    WHEN hours >= 250  THEN '🌌 NovaBeat'
    WHEN hours >= 160  THEN '🎵 Frequency Soul'
    WHEN hours >= 100  THEN '📀 Vinyl Hunter'
    WHEN hours >= 60   THEN '🎶 Resonance'
    WHEN hours >= 35   THEN '🌊 Echo Drift'
    WHEN hours >= 15   THEN '🎧 Pulse Rider'
    WHEN hours >= 5    THEN '🍃 Sprout Wave'
    ELSE '🌱 Seed Listener'
  END;
  UPDATE public.profiles SET rank_tier = tier WHERE id = target_user_id;
  RETURN tier;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger: auto-assign rank after hours reach a new threshold
CREATE OR REPLACE FUNCTION public.auto_assign_rank_on_hours()
RETURNS TRIGGER AS $$
BEGIN
  NEW.rank_tier := public.assign_user_rank(NEW.id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_profile_rank_update ON public.profiles;
CREATE TRIGGER on_profile_rank_update
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW WHEN (NEW.hours_listened IS DISTINCT FROM OLD.hours_listened)
  EXECUTE PROCEDURE public.auto_assign_rank_on_hours();

-- ------------------------------------------------------------
-- 8. XP FROM NON-PLAY EVENTS (Generic XP function)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.add_xp(target_user_id uuid, xp_amount INT)
RETURNS void AS $$
BEGIN
  UPDATE public.profiles
  SET xp_total = COALESCE(xp_total, 0) + xp_amount
  WHERE id = target_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Events table for tracking XP sources
CREATE TABLE IF NOT EXISTS public.xp_events (
  id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id uuid REFERENCES auth.users ON DELETE CASCADE NOT NULL,
  event_type TEXT NOT NULL,  -- 'play_start', 'play_complete', 'playlist_create', 'like', 'share', 'login', 'streak_7', 'streak_30', 'artist_discover'
  xp_amount INT NOT NULL,
  metadata JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.xp_events ENABLE ROW LEVEL SECURITY;
CREATE POLICY "XP events: users view own" ON public.xp_events
  FOR SELECT USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_xp_events_user ON public.xp_events(user_id);

-- Protected insert: service_role only (called from backend)
CREATE POLICY "XP events: block insert" ON public.xp_events FOR INSERT WITH CHECK (false);

-- ------------------------------------------------------------
-- 9. BADGE CHECK & UNLOCK FUNCTIONS
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.unlock_badge(
  target_user_id uuid,
  badge_id TEXT,
  badge_name TEXT,
  badge_emoji TEXT,
  badge_desc TEXT
)
RETURNS BOOLEAN AS $$
BEGIN
  INSERT INTO public.user_achievements (user_id, badge_id, badge_name, badge_emoji, description)
  VALUES (target_user_id, badge_id, badge_name, badge_emoji, badge_desc)
  ON CONFLICT (user_id, badge_id) DO NOTHING;
  RETURN FOUND;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Batch check + unlock all possible badges for a user
CREATE OR REPLACE FUNCTION public.check_and_award_badges(target_user_id uuid)
RETURNS TABLE (badge_id TEXT, badge_name TEXT, unlocked BOOLEAN) AS $$
DECLARE
  p RECORD;
BEGIN
  SELECT * INTO p FROM public.profiles WHERE id = target_user_id;

  RETURN QUERY
  SELECT 'night_listener'::TEXT,
         '🌙 Night Listener'::TEXT,
         public.unlock_badge(target_user_id, 'night_listener', '🌙 Night Listener', '🌙', '100+ hours after midnight')
  WHERE COALESCE(p.night_hours, 0) >= 100

  UNION ALL
  SELECT 'streak_god', '🔥 Streak God',
         public.unlock_badge(target_user_id, 'streak_god', '🔥 Streak God', '🔥', '90-day streak')
  WHERE COALESCE(p.streak_days, 0) >= 90

  UNION ALL
  SELECT 'playlist_architect', '📀 Playlist Architect',
         public.unlock_badge(target_user_id, 'playlist_architect', '📀 Playlist Architect', '📀', '100 playlists created')
  WHERE COALESCE(p.playlists_created, 0) >= 100;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ------------------------------------------------------------
-- 10. LEADERBOARD VIEWS
-- ------------------------------------------------------------
DROP VIEW IF EXISTS public.leaderboard_global;
CREATE VIEW public.leaderboard_global AS
SELECT
  id AS user_id,
  username,
  display_name,
  avatar_url,
  xp_total,
  xp_level,
  rank_tier,
  streak_days,
  hours_listened,
  songs_completed,
  ROW_NUMBER() OVER (ORDER BY xp_total DESC) AS rank_position
FROM public.profiles
WHERE is_banned = FALSE
ORDER BY xp_total DESC;

DROP VIEW IF EXISTS public.leaderboard_streaks;
CREATE VIEW public.leaderboard_streaks AS
SELECT
  id AS user_id,
  username,
  display_name,
  avatar_url,
  streak_days,
  longest_streak,
  ROW_NUMBER() OVER (ORDER BY streak_days DESC) AS rank_position
FROM public.profiles
WHERE is_banned = FALSE
ORDER BY streak_days DESC;

-- RLS for leaderboard views (public read)
CREATE POLICY "Leaderboard: public view" ON public.profiles
  FOR SELECT USING (true);

-- ------------------------------------------------------------
-- 11. DAILY LOGIN STREAK XP FUNCTION (called via backend/cron)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.process_daily_login(target_user_id uuid)
RETURNS TABLE (xp_earned INT, streak_bonus INT, total_xp INT, current_streak INT) AS $$
DECLARE
  p RECORD;
  streak7_bonus INT := 0;
  streak30_bonus INT := 0;
BEGIN
  SELECT * INTO p FROM public.profiles WHERE id = target_user_id;

  -- Check if already counted today
  IF p.last_login = CURRENT_DATE THEN
    RETURN QUERY SELECT 0, 0, COALESCE(p.xp_total, 0)::INT, COALESCE(p.streak_days, 0)::INT;
    RETURN;
  END IF;

  -- Streak bonuses
  IF COALESCE(p.streak_days, 0) > 0 AND MOD(COALESCE(p.streak_days, 0), 30) = 0 THEN
    streak30_bonus := 700;
  ELSIF COALESCE(p.streak_days, 0) > 0 AND MOD(COALESCE(p.streak_days, 0), 7) = 0 THEN
    streak7_bonus := 150;
  END IF;

  UPDATE public.profiles SET
    last_login = CURRENT_DATE
  WHERE id = target_user_id;

  RETURN QUERY
  SELECT
    (20 + streak7_bonus + streak30_bonus)::INT,  -- base daily + streak bonuses
    (streak7_bonus + streak30_bonus)::INT,
    COALESCE(xp_total, 0)::INT,
    COALESCE(streak_days, 0)::INT
  FROM public.profiles WHERE id = target_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ------------------------------------------------------------
-- 12. HOURLY XP AWARD FUNCTION (batch job for 1hr milestones)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.award_hourly_xp()
RETURNS void AS $$
BEGIN
  INSERT INTO public.xp_events (user_id, event_type, xp_amount, metadata)
  SELECT
    p.id,
    'hourly_milestone',
    80,
    jsonb_build_object('hours', FLOOR(p.hours_listened))
  FROM public.profiles p
  WHERE p.hours_listened >= 1
    AND NOT EXISTS (
      SELECT 1 FROM public.xp_events e
      WHERE e.user_id = p.id
        AND e.event_type = 'hourly_milestone'
        AND e.metadata->>'hours' = FLOOR(p.hours_listened)::TEXT
    );

  UPDATE public.profiles p
  SET xp_total = COALESCE(p.xp_total, 0) + 80
  FROM public.xp_events e
  WHERE e.user_id = p.id
    AND e.event_type = 'hourly_milestone'
    AND e.created_at > now() - interval '1 minute';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ------------------------------------------------------------
-- DONE — Gamification schema is ready.
-- Backend and Android app can now use these tables/functions.
-- ============================================================
