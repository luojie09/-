-- Enable Postgres Changes for the tables observed by the Android client.
-- Safe to run more than once after full_app_setup.sql and secure_couple_auth_migration.sql.

do $$
declare
    table_name text;
begin
    if not exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        create publication supabase_realtime;
    end if;

    foreach table_name in array array[
        'messages',
        'message_replies',
        'message_drafts',
        'message_likes',
        'wishes',
        'anniversaries',
        'user_moods',
        'home_quick_notes'
    ] loop
        if to_regclass('public.' || table_name) is not null
            and not exists (
                select 1
                from pg_publication_tables
                where pubname = 'supabase_realtime'
                  and schemaname = 'public'
                  and tablename = table_name
            ) then
            execute format('alter publication supabase_realtime add table public.%I', table_name);
        end if;
    end loop;
end
$$;
