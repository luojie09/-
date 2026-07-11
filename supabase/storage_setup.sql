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
