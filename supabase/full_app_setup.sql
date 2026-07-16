-- LEGACY BOOTSTRAP ONLY.
-- This file creates the original schema and seed data, including permissive demo policies.
-- Do not use it by itself for a production installation. Immediately run
-- secure_couple_auth_migration.sql afterwards to replace all demo policies.

create table if not exists public.anniversaries (
    id text primary key,
    title text not null,
    date date not null,
    repeat_yearly boolean not null default true,
    reminder_type text not null default 'NONE',
    created_at timestamptz not null default now()
);

create table if not exists public.wishes (
    id text primary key,
    title text not null,
    description text not null default '',
    cover_image_path text,
    planned_date date,
    created_at timestamptz not null default now(),
    status text not null default 'UNREALIZED',
    completion_text text,
    completion_image_paths jsonb not null default '[]'::jsonb,
    completed_at timestamptz
);

create table if not exists public.messages (
    id text primary key,
    author_id text not null,
    author_name text not null,
    content text not null default '',
    image_paths jsonb not null default '[]'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz,
    is_read boolean not null default false,
    read_at timestamptz,
    counterpart_read_at timestamptz
);

create table if not exists public.message_replies (
    id text primary key,
    message_id text not null references public.messages(id) on delete cascade,
    author_id text not null,
    author_name text not null,
    content text not null,
    created_at timestamptz not null default now(),
    is_read boolean not null default false,
    read_at timestamptz
);

create table if not exists public.message_drafts (
    user_id text primary key,
    content text not null default '',
    image_paths jsonb not null default '[]'::jsonb,
    updated_at timestamptz not null default now()
);

create table if not exists public.user_moods (
    user_id text primary key,
    mood_label text not null,
    recorded_date date not null,
    updated_at timestamptz not null default now()
);

create table if not exists public.home_quick_notes (
    id text primary key,
    title text not null,
    icon_slot text not null default 'activityNote',
    created_at timestamptz not null default now(),
    click_message text not null default '待接入动态详情页'
);

insert into storage.buckets (
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
)
values (
    'secretbase-images',
    'secretbase-images',
    true,
    10485760,
    array['image/jpeg', 'image/png', 'image/webp', 'image/gif']
)
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "secretbase_images_select_all" on storage.objects;
create policy "secretbase_images_select_all"
on storage.objects
for select
to anon, authenticated
using (bucket_id = 'secretbase-images');

drop policy if exists "secretbase_images_insert_all" on storage.objects;
create policy "secretbase_images_insert_all"
on storage.objects
for insert
to anon, authenticated
with check (bucket_id = 'secretbase-images');

drop policy if exists "secretbase_images_update_all" on storage.objects;
create policy "secretbase_images_update_all"
on storage.objects
for update
to anon, authenticated
using (bucket_id = 'secretbase-images')
with check (bucket_id = 'secretbase-images');

drop policy if exists "secretbase_images_delete_all" on storage.objects;
create policy "secretbase_images_delete_all"
on storage.objects
for delete
to anon, authenticated
using (bucket_id = 'secretbase-images');

alter table public.anniversaries enable row level security;
alter table public.wishes enable row level security;
alter table public.messages enable row level security;
alter table public.message_replies enable row level security;
alter table public.message_drafts enable row level security;
alter table public.user_moods enable row level security;
alter table public.home_quick_notes enable row level security;

drop policy if exists "anniversaries_select_all" on public.anniversaries;
create policy "anniversaries_select_all" on public.anniversaries for select to anon, authenticated using (true);
drop policy if exists "anniversaries_insert_all" on public.anniversaries;
create policy "anniversaries_insert_all" on public.anniversaries for insert to anon, authenticated with check (true);
drop policy if exists "anniversaries_update_all" on public.anniversaries;
create policy "anniversaries_update_all" on public.anniversaries for update to anon, authenticated using (true) with check (true);
drop policy if exists "anniversaries_delete_all" on public.anniversaries;
create policy "anniversaries_delete_all" on public.anniversaries for delete to anon, authenticated using (true);

drop policy if exists "wishes_select_all" on public.wishes;
create policy "wishes_select_all" on public.wishes for select to anon, authenticated using (true);
drop policy if exists "wishes_insert_all" on public.wishes;
create policy "wishes_insert_all" on public.wishes for insert to anon, authenticated with check (true);
drop policy if exists "wishes_update_all" on public.wishes;
create policy "wishes_update_all" on public.wishes for update to anon, authenticated using (true) with check (true);
drop policy if exists "wishes_delete_all" on public.wishes;
create policy "wishes_delete_all" on public.wishes for delete to anon, authenticated using (true);

drop policy if exists "messages_select_all" on public.messages;
create policy "messages_select_all" on public.messages for select to anon, authenticated using (true);
drop policy if exists "messages_insert_all" on public.messages;
create policy "messages_insert_all" on public.messages for insert to anon, authenticated with check (true);
drop policy if exists "messages_update_all" on public.messages;
create policy "messages_update_all" on public.messages for update to anon, authenticated using (true) with check (true);
drop policy if exists "messages_delete_all" on public.messages;
create policy "messages_delete_all" on public.messages for delete to anon, authenticated using (true);

drop policy if exists "message_replies_select_all" on public.message_replies;
create policy "message_replies_select_all" on public.message_replies for select to anon, authenticated using (true);
drop policy if exists "message_replies_insert_all" on public.message_replies;
create policy "message_replies_insert_all" on public.message_replies for insert to anon, authenticated with check (true);
drop policy if exists "message_replies_update_all" on public.message_replies;
create policy "message_replies_update_all" on public.message_replies for update to anon, authenticated using (true) with check (true);
drop policy if exists "message_replies_delete_all" on public.message_replies;
create policy "message_replies_delete_all" on public.message_replies for delete to anon, authenticated using (true);

drop policy if exists "message_drafts_select_all" on public.message_drafts;
create policy "message_drafts_select_all" on public.message_drafts for select to anon, authenticated using (true);
drop policy if exists "message_drafts_insert_all" on public.message_drafts;
create policy "message_drafts_insert_all" on public.message_drafts for insert to anon, authenticated with check (true);
drop policy if exists "message_drafts_update_all" on public.message_drafts;
create policy "message_drafts_update_all" on public.message_drafts for update to anon, authenticated using (true) with check (true);
drop policy if exists "message_drafts_delete_all" on public.message_drafts;
create policy "message_drafts_delete_all" on public.message_drafts for delete to anon, authenticated using (true);

drop policy if exists "user_moods_select_all" on public.user_moods;
create policy "user_moods_select_all" on public.user_moods for select to anon, authenticated using (true);
drop policy if exists "user_moods_insert_all" on public.user_moods;
create policy "user_moods_insert_all" on public.user_moods for insert to anon, authenticated with check (true);
drop policy if exists "user_moods_update_all" on public.user_moods;
create policy "user_moods_update_all" on public.user_moods for update to anon, authenticated using (true) with check (true);
drop policy if exists "user_moods_delete_all" on public.user_moods;
create policy "user_moods_delete_all" on public.user_moods for delete to anon, authenticated using (true);

drop policy if exists "home_quick_notes_select_all" on public.home_quick_notes;
create policy "home_quick_notes_select_all" on public.home_quick_notes for select to anon, authenticated using (true);
drop policy if exists "home_quick_notes_insert_all" on public.home_quick_notes;
create policy "home_quick_notes_insert_all" on public.home_quick_notes for insert to anon, authenticated with check (true);
drop policy if exists "home_quick_notes_update_all" on public.home_quick_notes;
create policy "home_quick_notes_update_all" on public.home_quick_notes for update to anon, authenticated using (true) with check (true);
drop policy if exists "home_quick_notes_delete_all" on public.home_quick_notes;
create policy "home_quick_notes_delete_all" on public.home_quick_notes for delete to anon, authenticated using (true);

insert into public.anniversaries (id, title, date, repeat_yearly, reminder_type, created_at)
values
    ('anniversary-love', '在一起的日子', '2023-01-01', false, 'NONE', '2025-02-12T08:00:00Z'),
    ('anniversary-first-date', '第一次约会', '2023-01-15', true, 'SAME_DAY', '2025-03-24T08:00:00Z'),
    ('anniversary-first-trip', '第一次旅行', '2023-05-20', true, 'ONE_DAY_BEFORE', '2025-06-02T08:00:00Z'),
    ('anniversary-first-growth', '第一次见家长', '2023-10-01', false, 'THREE_DAYS_BEFORE', '2025-10-10T08:00:00Z'),
    ('anniversary-sheep-bday', '生日 - 小羊', '2023-08-15', true, 'ONE_DAY_BEFORE', '2025-09-01T08:00:00Z'),
    ('anniversary-chick-bday', '生日 - 小耶', '2023-03-22', true, 'THREE_DAYS_BEFORE', '2025-09-01T08:00:00Z')
on conflict (id) do nothing;

insert into public.wishes (
    id, title, description, cover_image_path, planned_date, created_at, status,
    completion_text, completion_image_paths, completed_at
)
values
    ('wish-paris', '一起去旅行', '去看海，去看日出日落，去我们从未去过的城市。', 'mock://wish-paris', current_date + 180, now() - interval '38 day', 'UNREALIZED', null, '[]'::jsonb, null),
    ('wish-couple-photo', '拍一组情侣写真', '记录下最具勇气的我们，把镜头里的笑也好好珍藏。', 'mock://wish-photo', current_date + 65, now() - interval '22 day', 'UNREALIZED', null, '[]'::jsonb, null),
    ('wish-escape', '一起玩一次密室逃脱', '谁先被吓到就请晚饭吧。', 'mock://wish-game', current_date + 12, now() - interval '7 day', 'UNREALIZED', null, '[]'::jsonb, null),
    ('wish-sunrise', '一起去看日出', '去海边，看一场属于我们的清晨。', 'mock://wish-sunrise', current_date - 210, now() - interval '400 day', 'REALIZED', '我们在天还没亮的时候出发，看到了最温柔的天光。', '["mock://wish-sunrise-1","mock://wish-sunrise-2","mock://wish-sunrise-3"]'::jsonb, now() - interval '26 day'),
    ('wish-cake', '一起做小蛋糕', '想试一次把奶油抹得歪歪扭扭也很开心的下午。', 'mock://wish-cake', current_date - 45, now() - interval '120 day', 'REALIZED', '虽然奶油裱花翻车了，但你笑得太可爱了，所以一切都值得。', '["mock://wish-cake-1"]'::jsonb, now() - interval '18 day'),
    ('wish-movie', '一起看一场露天电影', '带上小毯子和汽水，找一个晚风舒服的夜晚。', 'mock://wish-movie', current_date - 15, now() - interval '90 day', 'REALIZED', '电影已经记不太清了，但肩膀靠在一起的那一刻特别清楚。', '["mock://wish-movie-1","mock://wish-movie-2"]'::jsonb, now() - interval '8 day')
on conflict (id) do nothing;

insert into public.messages (
    id, author_id, author_name, content, image_paths, created_at, updated_at, is_read, read_at, counterpart_read_at
)
values
    ('message-sheep-blossom', 'sheep', '小羊', '今天看到一朵超美的樱花，第一时间就想分享给你。', '["mock://blossom-sky"]'::jsonb, now() - interval '10 minute', null, true, now() - interval '10 minute', null),
    ('message-chick-sunset', 'chick', '小耶', '一起去看了日落和海，好治愈呀～', '["mock://sunset-gold","mock://sunset-shore"]'::jsonb, now() - interval '16 hour', null, false, null, now() - interval '15 hour'),
    ('message-sheep-morning', 'sheep', '小羊', '早安～新的一天也要元气满满哦！', '[]'::jsonb, now() - interval '32 hour', null, true, now() - interval '32 hour', now() - interval '28 hour'),
    ('message-chick-text', 'chick', '小耶', '回家路上给你带了热乎乎的小甜点，等会记得开门收惊喜。', '[]'::jsonb, now() - interval '46 hour', null, false, null, now() - interval '45 hour')
on conflict (id) do nothing;

insert into public.message_replies (
    id, message_id, author_id, author_name, content, created_at, is_read, read_at
)
values
    ('reply-chick-blossom', 'message-sheep-blossom', 'chick', '小耶', '好想和你一起去看，光是照片就已经心动啦。', now() - interval '5 minute', false, null),
    ('reply-sheep-morning-1', 'message-sheep-morning', 'sheep', '小羊', '记得喝温水，别空着肚子出门。', now() - interval '31 hour', true, now() - interval '31 hour'),
    ('reply-chick-morning-2', 'message-sheep-morning', 'chick', '小耶', '收到收到，今天也会很想你。', now() - interval '30 hour', true, now() - interval '29 hour'),
    ('reply-sheep-morning-3', 'message-sheep-morning', 'sheep', '小羊', '那我等你下班，晚上一起吃小蛋糕。', now() - interval '29 hour', true, now() - interval '29 hour'),
    ('reply-chick-morning-4', 'message-sheep-morning', 'chick', '小耶', '好呀，想吃草莓奶油味的！', now() - interval '28 hour', true, now() - interval '28 hour')
on conflict (id) do nothing;
