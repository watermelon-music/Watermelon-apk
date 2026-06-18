-- Run this in Supabase Dashboard → SQL Editor

-- 1. PROFILES TABLE
-- Stores user plan, display name, avatar. Auto-created on signup via trigger.
create table public.profiles (
  id uuid references auth.users on delete cascade,
  email text,
  username text,
  display_name text,
  plan text default 'FREE' not null,
  avatar_url text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  primary key (id)
);

alter table public.profiles enable row level security;
create policy "Users can view own profile" on public.profiles for select using (auth.uid() = id);
create policy "Users can update own profile" on public.profiles for update using (auth.uid() = id);
create policy "Users can insert own profile" on public.profiles for insert with check (auth.uid() = id);

-- Auto-create profile when auth user signs up
create function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, email, username, display_name, plan)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data->>'username', split_part(new.email, '@', 1)),
    coalesce(new.raw_user_meta_data->>'display_name', split_part(new.email, '@', 1)),
    'FREE'
  );
  return new;
end;
$$ language plpgsql security definer;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();


-- 2. PLAYLISTS TABLE
create table public.playlists (
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
create policy "Users can CRUD own playlists" on public.playlists
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);


-- 3. PLAYLIST SONGS TABLE
create table public.playlist_songs (
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
create policy "Users can CRUD own playlist songs" on public.playlist_songs
  for all using (
    auth.uid() in (
      select user_id from public.playlists where id = playlist_id
    )
  );


-- 4. FAVORITES TABLE
create table public.favorites (
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
create policy "Users can CRUD own favorites" on public.favorites
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);


-- 5. LISTENING HISTORY (Analytics)
-- Stores last 50 plays per user for sync. Delete old rows to keep DB small.
create table public.listening_history (
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
create policy "Users can insert own history" on public.listening_history
  for insert with check (auth.uid() = user_id);
create policy "Users can view own history" on public.listening_history
  for select using (auth.uid() = user_id);

-- Auto-trim to last 50 rows per user
create function public.trim_listening_history()
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

create trigger on_history_inserted
  after insert on public.listening_history
  for each row execute procedure public.trim_listening_history();


-- 6. ADMIN STATS TABLE (refreshed periodically, not a mat view — avoids RLS issues)
create table public.admin_stats (
  total_users bigint,
  paid_users bigint,
  free_users bigint,
  total_playlists bigint,
  total_favorites bigint,
  total_plays bigint,
  refreshed_at timestamp with time zone
);

-- Only service_role can read this table (RLS blocks everything else)
alter table public.admin_stats enable row level security;
create policy "Block all" on public.admin_stats for all using (false);

-- Refresh function (run this whenever you want updated stats)
create function public.refresh_admin_stats()
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

-- Run once to populate
select public.refresh_admin_stats();

-- 7. PREMIUM REQUESTS TABLE (pending payment verifications awaiting admin approval)
-- The backend writes rows here after successful Razorpay signature check.
-- The admin Telegram bot reads this and approves via /verify <email>.
create table public.premium_requests (
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

-- Lock this down — only backend service_role should read/insert
alter table public.premium_requests enable row level security;
create policy "Block all" on public.premium_requests for all using (false);

-- Optional: trim pending requests older than 7 days so the table stays small
create function public.clean_old_premium_requests()
returns trigger as $$
begin
  delete from public.premium_requests
  where status = 'pending' and created_at < now() - interval '7 days';
  return new;
end;
$$ language plpgsql security definer;

create trigger on_premium_request_inserted
  after insert on public.premium_requests
  for each row execute procedure public.clean_old_premium_requests();
