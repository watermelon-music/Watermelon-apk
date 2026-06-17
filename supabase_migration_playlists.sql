-- Run this snippet in your Supabase SQL Editor to fix the Playlist creation error.
-- It only adds the missing columns to the existing table, preventing the "relation already exists" error.

ALTER TABLE public.playlists 
ADD COLUMN IF NOT EXISTS share_code TEXT,
ADD COLUMN IF NOT EXISTS is_public BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS share_count BIGINT DEFAULT 0,
ADD COLUMN IF NOT EXISTS save_count BIGINT DEFAULT 0,
ADD COLUMN IF NOT EXISTS copy_count BIGINT DEFAULT 0;
