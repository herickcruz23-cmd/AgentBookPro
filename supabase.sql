-- ============================================================
-- IMPORTANTE: ejecuta TODO este bloque en el SQL Editor de Supabase
-- (Dashboard → SQL Editor → New query → pega todo → Run)
-- ============================================================

-- 1. Asegura que la tabla tenga EXACTAMENTE estas columnas.
--    Si ya existe con otras columnas, las añadimos.
create table if not exists public.appointments (
    id bigserial primary key,
    nombre text not null,
    telefono text not null,
    fecha date not null,
    hora time not null,
    servicio text not null,
    costo numeric(10,2),
    created_at timestamptz not null default now()
);

-- 2. Por si la tabla ya existía con otro esquema, añade las que falten.
alter table public.appointments add column if not exists nombre text;
alter table public.appointments add column if not exists telefono text;
alter table public.appointments add column if not exists fecha date;
alter table public.appointments add column if not exists hora time;
alter table public.appointments add column if not exists servicio text;
alter table public.appointments add column if not exists costo numeric(10,2);
alter table public.appointments add column if not exists created_at timestamptz default now();

-- 3. Recarga el schema cache de PostgREST (evita el error PGRST204).
notify pgrst, 'reload schema';

-- 4. RLS y políticas para que la anon key pueda insertar y leer.
alter table public.appointments enable row level security;

drop policy if exists "anon_insert" on public.appointments;
create policy "anon_insert" on public.appointments
    for insert
    to anon
    with check (true);

drop policy if exists "anon_select" on public.appointments;
create policy "anon_select" on public.appointments
    for select
    to anon
    using (true);
