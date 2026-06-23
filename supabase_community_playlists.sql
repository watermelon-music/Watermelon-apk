 -- Add missing columns (safe even if already run)
   ALTER TABLE public.playlists
   ADD COLUMN IF NOT EXISTS tags TEXT[] DEFAULT '{}',
   ADD COLUMN IF NOT EXISTS like_count BIGINT DEFAULT 0,
   ADD COLUMN IF NOT EXISTS creator_display_name TEXT;

   -- Recreate policies (drop first)
   DROP POLICY IF EXISTS "Playlist likes: users view own" ON public.playlist_likes;
   CREATE POLICY "Playlist likes: users view own" ON public.playlist_likes
       FOR SELECT USING (auth.uid() = user_id);

   DROP POLICY IF EXISTS "Playlist likes: users insert own" ON public.playlist_likes;
   CREATE POLICY "Playlist likes: users insert own" ON public.playlist_likes
       FOR INSERT WITH CHECK (auth.uid() = user_id);

   DROP POLICY IF EXISTS "Playlist likes: users delete own" ON public.playlist_likes;
   CREATE POLICY "Playlist likes: users delete own" ON public.playlist_likes
       FOR DELETE USING (auth.uid() = user_id);

   -- Public playlist view policy
   DROP POLICY IF EXISTS "Playlists: public view" ON public.playlists;
   CREATE POLICY "Playlists: public view" ON public.playlists
       FOR SELECT USING (is_public = TRUE OR auth.uid() = user_id);

   -- Indexes
   CREATE INDEX IF NOT EXISTS idx_playlists_public ON public.playlists(is_public, like_count DESC);
   CREATE INDEX IF NOT EXISTS idx_playlists_tags ON public.playlists USING GIN(tags);
   CREATE INDEX IF NOT EXISTS idx_playlist_likes_user ON public.playlist_likes(user_id);
   CREATE INDEX IF NOT EXISTS idx_playlist_likes_playlist ON public.playlist_likes(playlist_id);

   -- Toggle like function
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
