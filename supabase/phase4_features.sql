-- Phase 4: persistent message likes.
-- Run after secure_couple_auth_migration.sql. Safe to run more than once.

create table if not exists public.message_likes (
    couple_id uuid not null default public.current_secret_base_couple_id(),
    message_id text not null references public.messages(id) on delete cascade,
    user_id text not null check (user_id in ('sheep', 'chick')),
    created_at timestamptz not null default now(),
    primary key (couple_id, message_id, user_id)
);

alter table public.message_likes enable row level security;
revoke all on table public.message_likes from anon;
grant select, insert, update, delete on table public.message_likes to authenticated;

drop policy if exists "message_likes_couple_select" on public.message_likes;
drop policy if exists "message_likes_own_insert" on public.message_likes;
drop policy if exists "message_likes_own_update" on public.message_likes;
drop policy if exists "message_likes_own_delete" on public.message_likes;

create policy "message_likes_couple_select"
on public.message_likes for select to authenticated
using (couple_id = public.current_secret_base_couple_id());

create policy "message_likes_own_insert"
on public.message_likes for insert to authenticated
with check (
    couple_id = public.current_secret_base_couple_id()
    and user_id = public.current_secret_base_role()
);

create policy "message_likes_own_update"
on public.message_likes for update to authenticated
using (
    couple_id = public.current_secret_base_couple_id()
    and user_id = public.current_secret_base_role()
)
with check (
    couple_id = public.current_secret_base_couple_id()
    and user_id = public.current_secret_base_role()
);

create policy "message_likes_own_delete"
on public.message_likes for delete to authenticated
using (
    couple_id = public.current_secret_base_couple_id()
    and user_id = public.current_secret_base_role()
);

create index if not exists message_likes_message_id_idx
on public.message_likes(message_id);

do $$
begin
    if not exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        create publication supabase_realtime;
    end if;
    if not exists (
        select 1
        from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'message_likes'
    ) then
        alter publication supabase_realtime add table public.message_likes;
    end if;
end
$$;

notify pgrst, 'reload schema';
