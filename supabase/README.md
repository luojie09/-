# Supabase setup

The Android app now uses an anonymous Supabase Auth session plus a private pairing code. The selected role (`sheep` or `chick`) is bound to the authenticated user and all application tables are restricted by `couple_id`.

## Existing project

1. In Supabase Dashboard, open **Authentication > Providers > Anonymous Sign-Ins** and enable anonymous sign-ins.
2. Open **SQL Editor** and run `secure_couple_auth_migration.sql`.
3. Confirm the migration created the pairing RPCs:

   ```sql
   select
     to_regprocedure('public.configure_secret_base_pairing_code(text)') as configure_rpc,
     to_regprocedure('public.join_secret_base(text,text)') as join_rpc;
   ```

   Both result columns must contain a function name. If either is `null`, rerun
   the complete migration and fix its first reported error before continuing.

4. In SQL Editor, set a private pairing code with at least 8 characters:

   ```sql
   select public.configure_secret_base_pairing_code('your-private-pairing-code'::text);
   ```

5. Build and install the new APK.
6. On each phone, select `小羊` or `小耶` and enter the same pairing code once.

Never add the pairing code to GitHub Actions, `BuildConfig`, source code, or a committed properties file.

## New project

1. Run `full_app_setup.sql` to create the original tables and seed data.
2. Immediately run `secure_couple_auth_migration.sql` before distributing an APK.
3. Follow the pairing-code steps above.

`full_app_setup.sql` contains legacy demo policies so that old development databases can be recreated. The secure migration removes every policy on the application tables and replaces it with authenticated couple-scoped policies.

## Recovery

Running `configure_secret_base_pairing_code(...)` again rotates the pairing code. A valid pairing code can rebind a role after reinstalling the app or replacing a phone; the previous anonymous session for that role will lose access.
