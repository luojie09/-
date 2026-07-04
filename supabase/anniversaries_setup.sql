create table if not exists public.anniversaries (
    id text primary key,
    title text not null,
    date date not null,
    repeat_yearly boolean not null default true,
    reminder_type text not null default 'NONE',
    created_at timestamptz not null default now()
);

alter table public.anniversaries enable row level security;

drop policy if exists "anniversaries_select_all" on public.anniversaries;
create policy "anniversaries_select_all"
on public.anniversaries
for select
to anon, authenticated
using (true);

drop policy if exists "anniversaries_insert_all" on public.anniversaries;
create policy "anniversaries_insert_all"
on public.anniversaries
for insert
to anon, authenticated
with check (true);

drop policy if exists "anniversaries_update_all" on public.anniversaries;
create policy "anniversaries_update_all"
on public.anniversaries
for update
to anon, authenticated
using (true)
with check (true);

drop policy if exists "anniversaries_delete_all" on public.anniversaries;
create policy "anniversaries_delete_all"
on public.anniversaries
for delete
to anon, authenticated
using (true);

insert into public.anniversaries (id, title, date, repeat_yearly, reminder_type, created_at)
values
    ('anniversary-love', '在一起的日子', '2023-01-01', false, 'NONE', '2025-02-12T08:00:00Z'),
    ('anniversary-first-date', '第一次约会', '2023-01-15', true, 'SAME_DAY', '2025-03-24T08:00:00Z'),
    ('anniversary-first-trip', '第一次旅行', '2023-05-20', true, 'ONE_DAY_BEFORE', '2025-06-02T08:00:00Z'),
    ('anniversary-first-growth', '第一次见家长', '2023-10-01', false, 'THREE_DAYS_BEFORE', '2025-10-10T08:00:00Z'),
    ('anniversary-sheep-bday', '生日 - 小羊', '2023-08-15', true, 'ONE_DAY_BEFORE', '2025-09-01T08:00:00Z'),
    ('anniversary-chick-bday', '生日 - 小耶', '2023-03-22', true, 'THREE_DAYS_BEFORE', '2025-09-01T08:00:00Z')
on conflict (id) do nothing;
