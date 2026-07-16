-- Run this migration in Supabase SQL Editor before installing the secured APK.
-- Then set the private pairing code once:
-- select public.configure_secret_base_pairing_code('replace-with-a-private-code');
-- The plaintext code is never stored; only a bcrypt hash is retained.

create schema if not exists extensions;
create extension if not exists pgcrypto with schema extensions;

-- Supabase normally installs pgcrypto in `extensions`. Include both schemas so
-- this migration also works when an existing project installed it in `public`.
set search_path = public, extensions;

create table if not exists public.secret_base_couples (
    id uuid primary key,
    pairing_code_hash text not null,
    created_at timestamptz not null default now()
);

insert into public.secret_base_couples (id, pairing_code_hash)
values (
    '5ec6d4d3-6fa9-45c8-9506-202605060001'::uuid,
    crypt(gen_random_uuid()::text, gen_salt('bf'))
)
on conflict (id) do nothing;

create table if not exists public.secret_base_members (
    user_id uuid primary key references auth.users(id) on delete cascade,
    couple_id uuid not null references public.secret_base_couples(id) on delete cascade,
    role text not null check (role in ('sheep', 'chick')),
    bound_at timestamptz not null default now(),
    unique (couple_id, role)
);

create or replace function public.configure_secret_base_pairing_code(p_pairing_code text)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
begin
    if length(trim(p_pairing_code)) < 8 then
        raise exception '配对码至少需要 8 个字符';
    end if;

    update public.secret_base_couples
    set pairing_code_hash = crypt(trim(p_pairing_code), gen_salt('bf'))
    where id = '5ec6d4d3-6fa9-45c8-9506-202605060001'::uuid;
end;
$$;

revoke all on function public.configure_secret_base_pairing_code(text) from public, anon, authenticated;

create or replace function public.current_secret_base_couple_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
    select couple_id
    from public.secret_base_members
    where user_id = auth.uid()
    limit 1
$$;

create or replace function public.current_secret_base_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
    select role
    from public.secret_base_members
    where user_id = auth.uid()
    limit 1
$$;

revoke all on function public.current_secret_base_couple_id() from public, anon;
revoke all on function public.current_secret_base_role() from public, anon;
grant execute on function public.current_secret_base_couple_id() to authenticated;
grant execute on function public.current_secret_base_role() to authenticated;

create or replace function public.join_secret_base(
    p_pairing_code text,
    p_role text
)
returns jsonb
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
    selected_couple_id uuid;
begin
    if auth.uid() is null then
        raise exception '请先完成 Supabase 身份认证';
    end if;
    if p_role not in ('sheep', 'chick') then
        raise exception '身份无效';
    end if;

    select id into selected_couple_id
    from public.secret_base_couples
    where pairing_code_hash = crypt(trim(p_pairing_code), pairing_code_hash)
    limit 1;

    if selected_couple_id is null then
        raise exception '配对码不正确';
    end if;

    -- A valid pairing code can rebind a role after reinstalling or replacing a phone.
    delete from public.secret_base_members
    where user_id = auth.uid()
       or (couple_id = selected_couple_id and role = p_role);

    insert into public.secret_base_members (user_id, couple_id, role)
    values (auth.uid(), selected_couple_id, p_role);

    return jsonb_build_object(
        'couple_id', selected_couple_id::text,
        'role', p_role
    );
end;
$$;

revoke all on function public.join_secret_base(text, text) from public, anon;
grant execute on function public.join_secret_base(text, text) to authenticated;

alter table public.anniversaries add column if not exists couple_id uuid;
alter table public.wishes add column if not exists couple_id uuid;
alter table public.messages add column if not exists couple_id uuid;
alter table public.message_replies add column if not exists couple_id uuid;
alter table public.message_drafts add column if not exists couple_id uuid;
alter table public.user_moods add column if not exists couple_id uuid;
alter table public.home_quick_notes add column if not exists couple_id uuid;

update public.anniversaries set couple_id = '5ec6d4d3-6fa9-45c8-9506-202605060001'::uuid where couple_id is null;
update public.wishes set couple_id = '5ec6d4d3-6fa9-45c8-9506-202605060001'::uuid where couple_id is null;
update public.messages set couple_id = '5ec6d4d3-6fa9-45c8-9506-202605060001'::uuid where couple_id is null;
update public.message_replies set couple_id = '5ec6d4d3-6fa9-45c8-9506-202605060001'::uuid where couple_id is null;
update public.message_drafts set couple_id = '5ec6d4d3-6fa9-45c8-9506-202605060001'::uuid where couple_id is null;
update public.user_moods set couple_id = '5ec6d4d3-6fa9-45c8-9506-202605060001'::uuid where couple_id is null;
update public.home_quick_notes set couple_id = '5ec6d4d3-6fa9-45c8-9506-202605060001'::uuid where couple_id is null;

alter table public.anniversaries alter column couple_id set default public.current_secret_base_couple_id();
alter table public.wishes alter column couple_id set default public.current_secret_base_couple_id();
alter table public.messages alter column couple_id set default public.current_secret_base_couple_id();
alter table public.message_replies alter column couple_id set default public.current_secret_base_couple_id();
alter table public.message_drafts alter column couple_id set default public.current_secret_base_couple_id();
alter table public.user_moods alter column couple_id set default public.current_secret_base_couple_id();
alter table public.home_quick_notes alter column couple_id set default public.current_secret_base_couple_id();

alter table public.anniversaries alter column couple_id set not null;
alter table public.wishes alter column couple_id set not null;
alter table public.messages alter column couple_id set not null;
alter table public.message_replies alter column couple_id set not null;
alter table public.message_drafts alter column couple_id set not null;
alter table public.user_moods alter column couple_id set not null;
alter table public.home_quick_notes alter column couple_id set not null;

revoke all on table public.anniversaries from anon;
revoke all on table public.wishes from anon;
revoke all on table public.messages from anon;
revoke all on table public.message_replies from anon;
revoke all on table public.message_drafts from anon;
revoke all on table public.user_moods from anon;
revoke all on table public.home_quick_notes from anon;
revoke all on table public.secret_base_couples from anon, authenticated;
revoke all on table public.secret_base_members from anon, authenticated;

grant select, insert, update, delete on table public.anniversaries to authenticated;
grant select, insert, update, delete on table public.wishes to authenticated;
grant select, insert, update, delete on table public.messages to authenticated;
grant select, insert, update, delete on table public.message_replies to authenticated;
grant select, insert, update, delete on table public.message_drafts to authenticated;
grant select, insert, update, delete on table public.user_moods to authenticated;
grant select, insert, update, delete on table public.home_quick_notes to authenticated;
grant select on table public.secret_base_members to authenticated;

do $$
declare
    target_table text;
begin
    foreach target_table in array array[
        'anniversaries',
        'wishes',
        'messages',
        'message_replies',
        'message_drafts',
        'user_moods',
        'home_quick_notes',
        'secret_base_couples',
        'secret_base_members'
    ] loop
        execute format('alter table public.%I enable row level security', target_table);
    end loop;
end;
$$;

do $$
declare
    policy_record record;
begin
    for policy_record in
        select schemaname, tablename, policyname
        from pg_policies
        where schemaname = 'public'
          and tablename in (
              'anniversaries', 'wishes', 'messages', 'message_replies',
              'message_drafts', 'user_moods', 'home_quick_notes',
              'secret_base_couples', 'secret_base_members'
          )
    loop
        execute format(
            'drop policy if exists %I on %I.%I',
            policy_record.policyname,
            policy_record.schemaname,
            policy_record.tablename
        );
    end loop;
end;
$$;

create policy "members_select_self"
on public.secret_base_members for select to authenticated
using (user_id = auth.uid());

create policy "anniversaries_couple_all"
on public.anniversaries for all to authenticated
using (couple_id = public.current_secret_base_couple_id())
with check (couple_id = public.current_secret_base_couple_id());

create policy "wishes_couple_all"
on public.wishes for all to authenticated
using (couple_id = public.current_secret_base_couple_id())
with check (couple_id = public.current_secret_base_couple_id());

create policy "messages_couple_select"
on public.messages for select to authenticated
using (couple_id = public.current_secret_base_couple_id());
create policy "messages_couple_insert"
on public.messages for insert to authenticated
with check (
    couple_id = public.current_secret_base_couple_id()
    and author_id = public.current_secret_base_role()
);
create policy "messages_couple_update"
on public.messages for update to authenticated
using (couple_id = public.current_secret_base_couple_id())
with check (couple_id = public.current_secret_base_couple_id());
create policy "messages_author_delete"
on public.messages for delete to authenticated
using (
    couple_id = public.current_secret_base_couple_id()
    and author_id = public.current_secret_base_role()
);

create policy "replies_couple_select"
on public.message_replies for select to authenticated
using (couple_id = public.current_secret_base_couple_id());
create policy "replies_couple_insert"
on public.message_replies for insert to authenticated
with check (
    couple_id = public.current_secret_base_couple_id()
    and author_id = public.current_secret_base_role()
);
create policy "replies_couple_update"
on public.message_replies for update to authenticated
using (couple_id = public.current_secret_base_couple_id())
with check (couple_id = public.current_secret_base_couple_id());
create policy "replies_author_delete"
on public.message_replies for delete to authenticated
using (
    couple_id = public.current_secret_base_couple_id()
    and author_id = public.current_secret_base_role()
);

create policy "drafts_own_all"
on public.message_drafts for all to authenticated
using (
    couple_id = public.current_secret_base_couple_id()
    and user_id = public.current_secret_base_role()
)
with check (
    couple_id = public.current_secret_base_couple_id()
    and user_id = public.current_secret_base_role()
);

create policy "moods_couple_select"
on public.user_moods for select to authenticated
using (couple_id = public.current_secret_base_couple_id());
create policy "moods_own_insert"
on public.user_moods for insert to authenticated
with check (
    couple_id = public.current_secret_base_couple_id()
    and user_id = public.current_secret_base_role()
);
create policy "moods_own_update"
on public.user_moods for update to authenticated
using (
    couple_id = public.current_secret_base_couple_id()
    and user_id = public.current_secret_base_role()
)
with check (
    couple_id = public.current_secret_base_couple_id()
    and user_id = public.current_secret_base_role()
);

create policy "quick_notes_couple_all"
on public.home_quick_notes for all to authenticated
using (couple_id = public.current_secret_base_couple_id())
with check (couple_id = public.current_secret_base_couple_id());

create index if not exists anniversaries_couple_id_idx on public.anniversaries(couple_id);
create index if not exists wishes_couple_id_idx on public.wishes(couple_id);
create index if not exists messages_couple_id_idx on public.messages(couple_id);
create index if not exists replies_couple_id_idx on public.message_replies(couple_id);
create index if not exists drafts_couple_id_idx on public.message_drafts(couple_id);
create index if not exists moods_couple_id_idx on public.user_moods(couple_id);
create index if not exists quick_notes_couple_id_idx on public.home_quick_notes(couple_id);

drop policy if exists "secretbase_images_select_all" on storage.objects;
drop policy if exists "secretbase_images_insert_all" on storage.objects;
drop policy if exists "secretbase_images_update_all" on storage.objects;
drop policy if exists "secretbase_images_delete_all" on storage.objects;
drop policy if exists "secretbase_images_couple_select" on storage.objects;
drop policy if exists "secretbase_images_couple_insert" on storage.objects;
drop policy if exists "secretbase_images_couple_update" on storage.objects;
drop policy if exists "secretbase_images_couple_delete" on storage.objects;

create policy "secretbase_images_couple_select"
on storage.objects for select to authenticated
using (
    bucket_id = 'secretbase-images'
    and (storage.foldername(name))[1] = public.current_secret_base_couple_id()::text
);
create policy "secretbase_images_couple_insert"
on storage.objects for insert to authenticated
with check (
    bucket_id = 'secretbase-images'
    and (storage.foldername(name))[1] = public.current_secret_base_couple_id()::text
);
create policy "secretbase_images_couple_update"
on storage.objects for update to authenticated
using (
    bucket_id = 'secretbase-images'
    and (storage.foldername(name))[1] = public.current_secret_base_couple_id()::text
)
with check (
    bucket_id = 'secretbase-images'
    and (storage.foldername(name))[1] = public.current_secret_base_couple_id()::text
);
create policy "secretbase_images_couple_delete"
on storage.objects for delete to authenticated
using (
    bucket_id = 'secretbase-images'
    and (storage.foldername(name))[1] = public.current_secret_base_couple_id()::text
);

notify pgrst, 'reload schema';
