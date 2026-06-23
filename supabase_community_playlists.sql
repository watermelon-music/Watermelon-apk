-- ============================================================
-- Watermelon Music — Community Playlists Migration
-- Run this in Supabase Dashboard SQL Editor
-- ============================================================

-- 1. Extend playlists table for community features
ALTER TABLE public.playlists
ADD COLUMN IF NOT EXISTS tags TEXT[] DEFAULT '{}',
ADD COLUMN IF NOT EXISTS like_count BIGINT DEFAULT 0;

-- Creator display name denormalization (no JOIN needed for public listing)
ALTER TABLE public.playlists
ADD COLUMN IF NOT EXISTS creator_display_name TEXT;

-- Index for fast public playlist discovery
CREATE INDEX IF NOT EXISTS idx_playlists_public ON public.playlists(is_public, like_count DESC);
CREATE INDEX IF NOT EXISTS idx_playlists_tags ON public.playlists USING GIN(tags);

-- 2. Playlist likes table (many-to-many user ↔ playlist)
CREATE TABLE IF NOT EXISTS public.playlist_likes (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id uuid REFERENCES auth.users ON DELETE CASCADE NOT NULL,
    playlist_id uuid REFERENCES public.playlists ON DELETE CASCADE NOT NULL,
    liked_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    UNIQUE (user_id, playlist_id)
);

ALTER TABLE public.playlist_likes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Playlist likes: users view own" ON public.playlist_likes
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Playlist likes: users insert own" ON public.playlist_likes
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Playlist likes: users delete own" ON public.playlist_likes
    FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_playlist_likes_user ON public.playlist_likes(user_id);
CREATE INDEX IF NOT EXISTS idx_playlist_likes_playlist ON public.playlist_likes(playlist_id);

-- 3. Function: toggle like on a playlist (+1/-1 like_count)
CREATE OR REPLACE FUNCTION public.toggle_playlist_like(target_playlist_id uuid, target_user_id uuid)
RETURNS BOOLEAN AS $$
DECLARE
    already_liked BOOLEAN;
BEGIN
    SELECT EXISTS(
        SELECT 1 FROM public.playlist_likes
        WHERE playlist_id = target_playlist_id AND user_id = target_user_id
    ) INTO already_liked;

    IF already_liked THEN
        DELETE FROM public.playlist_likes
        WHERE playlist_id = target_playlist_id AND user_id = target_user_id;

        UPDATE public.playlists
        SET like_count = GREATEST(0, like_count - 1)
        WHERE id = target_playlist_id;
        RETURN FALSE;
    ELSE
        INSERT INTO public.playlist_likes (user_id, playlist_id)
        VALUES (target_user_id, target_playlist_id);

        UPDATE public.playlists
        SET like_count = like_count + 1
        WHERE id = target_playlist_id;
        RETURN TRUE;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. RLS for public playlists: anyone can view public playlists
DROP POLICY IF EXISTS "Playlists: public view" ON public.playlists;
CREATE POLICY "Playlists: public view" ON public.playlists
    FOR SELECT USING (is_public = TRUE OR auth.uid() = user_id);

-- 5. DONE
