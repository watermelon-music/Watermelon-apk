-- ============================================================
-- Watermelon Music - Complete Supabase Schema
-- Copy-paste the ENTIRE contents into Supabase Dashboard → SQL Editor → New Query → Run
-- ============================================================



-- ------------------------------------------------------------
-- 1. PROFILES TABLE
-- ------------------------------------------------------------
create table if not exists public.profiles (
  id uuid references auth.users on delete cascade,
  email text,
  username text,
  display_name text,
  plan text default 'FREE' not null,
  avatar_url text,
  is_banned boolean default false,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  primary key (id)
);

alter table public.profiles enable row level security;

create policy "Profiles: users view own" on public.profiles
  for select using (auth.uid() = id);

create policy "Profiles: users update own" on public.profiles
  for update using (auth.uid() = id) with check (auth.uid() = id);

create policy "Profiles: users insert own" on public.profiles
  for insert with check (auth.uid() = id);

-- Trigger: auto-create profile on auth signup (robust version)
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, email, username, display_name, plan, is_banned)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data->>'username', split_part(new.email, '@', 1)),
    coalesce(new.raw_user_meta_data->>'display_name', split_part(new.email, '@', 1)),
    'FREE',
    false
  )
  on conflict (id) do update
    set email        = excluded.email,
        username     = coalesce(excluded.username, public.profiles.username),
        display_name = coalesce(excluded.display_name, public.profiles.display_name);
  return new;
exception when others then
  raise warning 'handle_new_user failed: %', sqlerrm;
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- ------------------------------------------------------------
-- 2. PLAYLISTS TABLE
-- ------------------------------------------------------------
create table if not exists public.playlists (
  id uuid default gen_random_uuid(),
  user_id uuid references auth.users on delete cascade not null,
  name text not null,
  description text,
  cover_url text,
  share_code text,
  is_public boolean default false,
  share_count bigint default 0,
  save_count bigint default 0,
  copy_count bigint default 0,
  created_at timestamp with time zone default timezone('utc'::text, now()),
  updated_at timestamp with time zone default timezone('utc'::text, now()),
  primary key (id)
);

alter table public.playlists enable row level security;

create policy "Playlists: users CRUD own" on public.playlists
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ------------------------------------------------------------
-- 3. PLAYLIST SONGS TABLE
-- ------------------------------------------------------------
create table if not exists public.playlist_songs (
  id uuid default gen_random_uuid(),
  playlist_id uuid references public.playlists on delete cascade not null,
  song_id text not null,
  title text not null,
  artist text,
  cover_url text,
  audio_url text,
  position int default 0,
  created_at timestamp with time zone default timezone('utc'::text, now()),
  primary key (id)
);

alter table public.playlist_songs enable row level security;

create policy "PlaylistSongs: users CRUD own" on public.playlist_songs
  for all using (
    auth.uid() in (
      select user_id from public.playlists where id = playlist_id
    )
  );

-- ------------------------------------------------------------
-- 4. FAVORITES TABLE (songs)
-- ------------------------------------------------------------
create table if not exists public.favorites (
  id uuid default gen_random_uuid(),
  user_id uuid references auth.users on delete cascade not null,
  song_id text not null,
  title text not null,
  artist text,
  cover_url text,
  audio_url text,
  created_at timestamp with time zone default timezone('utc'::text, now()),
  unique (user_id, song_id)
);

alter table public.favorites enable row level security;

create policy "Favorites: users CRUD own" on public.favorites
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ------------------------------------------------------------
-- 5. LISTENING HISTORY (Analytics)
-- ------------------------------------------------------------
create table if not exists public.listening_history (
  id uuid default gen_random_uuid(),
  user_id uuid references auth.users on delete cascade not null,
  song_id text not null,
  title text not null,
  artist text,
  cover_url text,
  audio_url text,
  duration_ms bigint default 0,
  played_at timestamp with time zone default timezone('utc'::text, now()) not null
);

alter table public.listening_history enable row level security;

create policy "History: users insert own" on public.listening_history
  for insert with check (auth.uid() = user_id);

create policy "History: users view own" on public.listening_history
  for select using (auth.uid() = user_id);

-- Auto-trim to last 50 rows per user
create or replace function public.trim_listening_history()
returns trigger as $$
begin
  delete from public.listening_history
  where user_id = new.user_id
    and id not in (
      select id from public.listening_history
      where user_id = new.user_id
      order by played_at desc
      limit 50
    );
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists on_history_inserted on public.listening_history;
create trigger on_history_inserted
  after insert on public.listening_history
  for each row execute procedure public.trim_listening_history();

-- ------------------------------------------------------------
-- 6. ADMIN STATS TABLE
-- ------------------------------------------------------------
create table if not exists public.admin_stats (
  total_users bigint,
  paid_users bigint,
  free_users bigint,
  total_playlists bigint,
  total_favorites bigint,
  total_plays bigint,
  refreshed_at timestamp with time zone
);

-- Only service_role can touch this table (RLS blocks everything else)
alter table public.admin_stats enable row level security;
create policy "AdminStats: block all" on public.admin_stats for all using (false);

-- Refresh helper
create or replace function public.refresh_admin_stats()
returns void as $$
begin
  delete from public.admin_stats;
  insert into public.admin_stats
  select
    (select count(*) from auth.users) as total_users,
    (select count(*) from public.profiles where plan != 'FREE') as paid_users,
    (select count(*) from public.profiles where plan = 'FREE') as free_users,
    (select count(*) from public.playlists) as total_playlists,
    (select count(*) from public.favorites) as total_favorites,
    (select count(*) from public.listening_history) as total_plays,
    now() as refreshed_at;
end;
$$ language plpgsql security definer;

-- Populate once
select public.refresh_admin_stats();

-- ------------------------------------------------------------
-- 7. PREMIUM REQUESTS TABLE
-- ------------------------------------------------------------
create table if not exists public.premium_requests (
  id uuid default gen_random_uuid(),
  user_id uuid references auth.users on delete cascade not null,
  email text not null,
  plan text not null default 'PREMIUM_INDIVIDUAL' check (plan in ('PREMIUM_INDIVIDUAL', 'PREMIUM_FAMILY', 'STUDENT')),
  order_id text not null,
  payment_id text not null,
  amount integer not null, -- in paise
  currency text not null default 'INR',
  status text not null default 'pending' check (status in ('pending', 'approved', 'rejected')),
  created_at timestamp with time zone default timezone('utc'::text, now()),
  updated_at timestamp with time zone default timezone('utc'::text, now()),
  primary key (id),
  unique (payment_id)
);

-- Lock down — only backend service_role should read/insert
alter table public.premium_requests enable row level security;
create policy "PremiumRequests: block all" on public.premium_requests for all using (false);

-- Trim pending requests older than 7 days
create or replace function public.clean_old_premium_requests()
returns trigger as $$
begin
  delete from public.premium_requests
  where status = 'pending' and created_at < now() - interval '7 days';
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists on_premium_request_inserted on public.premium_requests;
create trigger on_premium_request_inserted
  after insert on public.premium_requests
  for each row execute procedure public.clean_old_premium_requests();

-- ------------------------------------------------------------
-- 8. BROADCASTS TABLE
-- ------------------------------------------------------------
create table if not exists public.broadcasts (
  id bigint generated by default as identity,
  message text not null,
  sender text not null,
  active boolean default true not null,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  primary key (id)
);

alter table public.broadcasts enable row level security;
create policy "Broadcasts: anyone select active" on public.broadcasts
  for select using (active = true);

-- ------------------------------------------------------------
-- 9. RADIO FAVORITES TABLE (sync liked radio stations)
-- ------------------------------------------------------------
create table if not exists public.radio_favorites (
  id uuid default gen_random_uuid(),
  user_id uuid references auth.users on delete cascade not null,
  station_uuid text not null,
  name text,
  url text,
  favicon text,
  country text,
  tags text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  unique (user_id, station_uuid)
);

alter table public.radio_favorites enable row level security;
create policy "RadioFavorites: users CRUD own" on public.radio_favorites
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ------------------------------------------------------------
-- 10. REMOTE CONFIG TABLE (kill-switch / feature flags)
-- ------------------------------------------------------------
create table if not exists public.remote_config (
  id serial primary key,
  maintenance_mode boolean default false,
  disable_youtube boolean default false,
  disable_audius boolean default false,
  disable_jamendo boolean default false,
  free_max_playlists int default 3,
  updated_at timestamp with time zone default timezone('utc'::text, now())
);

-- Seed default row
insert into public.remote_config (id, maintenance_mode, disable_youtube, disable_audius, disable_jamendo, free_max_playlists)
values (1, false, false, false, false, 3)
on conflict (id) do nothing;

-- Only service_role can read/write
alter table public.remote_config enable row level security;
create policy "RemoteConfig: block all" on public.remote_config for all using (false);

-- ============================================================
-- DONE — all tables, RLS policies, triggers, and functions created.
-- ============================================================
