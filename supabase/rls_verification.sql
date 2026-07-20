-- Read-only production verification for the secured couple schema.
-- Run after secure_couple_auth_migration.sql and phase4_features.sql.

do $$
declare
    table_name text;
    expected_tables text[] := array[
        'anniversaries',
        'wishes',
        'messages',
        'message_replies',
        'message_drafts',
        'message_likes',
        'user_moods',
        'home_quick_notes',
        'secret_base_members',
        'secret_base_couples'
    ];
begin
    foreach table_name in array expected_tables loop
        if not exists (
            select 1
            from pg_class c
            join pg_namespace n on n.oid = c.relnamespace
            where n.nspname = 'public'
              and c.relname = table_name
              and c.relrowsecurity
        ) then
            raise exception 'RLS is missing or disabled on public.%', table_name;
        end if;

        if has_table_privilege('anon', format('public.%I', table_name), 'select')
            or has_table_privilege('anon', format('public.%I', table_name), 'insert')
            or has_table_privilege('anon', format('public.%I', table_name), 'update')
            or has_table_privilege('anon', format('public.%I', table_name), 'delete') then
            raise exception 'anon still has application privileges on public.%', table_name;
        end if;
    end loop;

    if not exists (
        select 1
        from pg_policies
        where schemaname = 'public'
          and tablename = 'message_likes'
          and policyname = 'message_likes_own_insert'
    ) then
        raise exception 'message_likes owner insert policy is missing';
    end if;

    if not exists (
        select 1
        from pg_policies
        where schemaname = 'storage'
          and tablename = 'objects'
          and policyname = 'secretbase_images_couple_insert'
        and 'authenticated'::name = any(roles)
    ) then
        raise exception 'Couple-scoped storage insert policy is missing';
    end if;

    if has_function_privilege(
        'anon',
        'public.join_secret_base(text,text)',
        'execute'
    ) then
        raise exception 'anon must not execute join_secret_base';
    end if;
end
$$;

select
    'RLS verification passed' as result,
    now() as verified_at;
