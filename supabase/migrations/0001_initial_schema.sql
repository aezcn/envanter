-- Ev Envanteri — başlangıç şeması
--
-- Supabase panelinde SQL Editor'e yapıştırıp çalıştırın. Tek seferde,
-- baştan sona çalışacak şekilde yazıldı (idempotent değil — bir kez çalıştırın).

create extension if not exists pgcrypto;


-- ---------------------------------------------------------------------------
-- Ev ve üyeler
-- ---------------------------------------------------------------------------

create table public.households (
    id          uuid primary key default gen_random_uuid(),
    name        text not null,
    invite_code text not null unique,
    created_at  timestamptz not null default now()
);

create table public.household_members (
    household_id uuid not null references public.households (id) on delete cascade,
    user_id      uuid not null references auth.users (id) on delete cascade,
    display_name text not null default '',
    joined_at    timestamptz not null default now(),
    primary key (household_id, user_id)
);

create index household_members_user_idx on public.household_members (user_id);


-- ---------------------------------------------------------------------------
-- Üyelik kontrolü
--
-- RLS politikaları doğrudan household_members'ı sorgulasaydı, o tablonun kendi
-- politikası da aynı sorguyu tetikleyip sonsuz özyinelemeye girerdi. security
-- definer bir fonksiyon RLS'i atlayarak bu döngüyü kırar.
-- ---------------------------------------------------------------------------

create or replace function public.is_member(hid uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
    select exists (
        select 1
        from public.household_members
        where household_id = hid
          and user_id = auth.uid()
    );
$$;

revoke all on function public.is_member(uuid) from public;
grant execute on function public.is_member(uuid) to authenticated;


-- ---------------------------------------------------------------------------
-- Envanter tabloları
--
-- Ortak kurallar:
--   * id'ler istemcide üretilir — çevrimdışı eklenen kayıt sunucu yanıtını
--     beklemeden kullanılabilsin diye.
--   * deleted_at ile yumuşak silme. Sert silme diğer cihaza senkronize
--     olmazdı: satır kaybolur, karşı taraf bunu asla öğrenemezdi.
--   * updated_at'i istemci gönderir. Bir gün çevrimdışı kalan telefon senkron
--     olduğunda, değişikliğin sunucuya VARDIĞI an değil YAPILDIĞI an
--     karşılaştırılsın istiyoruz; last-write-wins ancak böyle doğru çalışır.
-- ---------------------------------------------------------------------------

create table public.locations (
    id           uuid primary key,
    household_id uuid not null references public.households (id) on delete cascade,
    name         text not null,
    icon         text not null default 'cabinet',
    sort_order   int  not null default 0,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    deleted_at   timestamptz
);

create index locations_household_idx on public.locations (household_id, updated_at);

create table public.items (
    id            uuid primary key,
    household_id  uuid not null references public.households (id) on delete cascade,
    location_id   uuid not null references public.locations (id) on delete cascade,
    name          text not null,
    quantity      numeric(10, 3) not null default 1,
    unit          text not null default 'adet',
    expiry_date   date,           -- opsiyonel
    low_threshold numeric(10, 3), -- opsiyonel: "azaldı" eşiği
    barcode       text,
    note          text not null default '',
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    deleted_at    timestamptz,
    updated_by    uuid references auth.users (id) on delete set null
);

create index items_household_idx on public.items (household_id, updated_at);
create index items_location_idx  on public.items (location_id) where deleted_at is null;
create index items_expiry_idx    on public.items (household_id, expiry_date)
    where deleted_at is null and expiry_date is not null;

create table public.shopping_items (
    id             uuid primary key,
    household_id   uuid not null references public.households (id) on delete cascade,
    name           text not null,
    quantity       numeric(10, 3) not null default 1,
    unit           text not null default 'adet',
    -- Envanterdeki bir üründen eklendiyse, "alındı" işaretlenince miktarın
    -- hangi satıra geri yazılacağını bilmek için.
    source_item_id uuid references public.items (id) on delete set null,
    is_done        boolean not null default false,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    deleted_at     timestamptz
);

create index shopping_items_household_idx on public.shopping_items (household_id, updated_at);

-- Barkod hafızası: hazır barkod veritabanlarının Türkiye kapsamı zayıf olduğu
-- için, ev kendi okuttuğu barkodları öğrenir. İkinci okutmadan itibaren isim
-- otomatik dolar.
create table public.barcode_memory (
    household_id uuid not null references public.households (id) on delete cascade,
    barcode      text not null,
    name         text not null,
    unit         text not null default 'adet',
    last_used_at timestamptz not null default now(),
    primary key (household_id, barcode)
);

-- Yalnızca ekleme yapılan kayıt defteri: "bunu sen mi bitirdin" sorusunu çözer.
create table public.activity_log (
    id           uuid primary key default gen_random_uuid(),
    household_id uuid not null references public.households (id) on delete cascade,
    user_id      uuid references auth.users (id) on delete set null,
    entity_type  text not null,  -- item | location | shopping_item
    entity_id    uuid,
    action       text not null,  -- create | update | delete | quantity_change
    payload      jsonb not null default '{}'::jsonb,
    created_at   timestamptz not null default now()
);

create index activity_log_household_idx on public.activity_log (household_id, created_at desc);


-- ---------------------------------------------------------------------------
-- RLS
--
-- Tek kural: kullanıcı, satırın ait olduğu evin üyesiyse okur ve yazar.
-- ---------------------------------------------------------------------------

alter table public.households        enable row level security;
alter table public.household_members enable row level security;
alter table public.locations         enable row level security;
alter table public.items             enable row level security;
alter table public.shopping_items    enable row level security;
alter table public.barcode_memory    enable row level security;
alter table public.activity_log      enable row level security;

-- households: üyesi olduğun evi görürsün. Oluşturma ve katılma yalnızca
-- aşağıdaki RPC'ler üzerinden — doğrudan insert yok, aksi halde herkes
-- istediği evi okumaya çalışabilirdi.
create policy households_select on public.households
    for select to authenticated
    using (public.is_member(id));

create policy households_update on public.households
    for update to authenticated
    using (public.is_member(id))
    with check (public.is_member(id));

-- household_members: kendi evinin üyelerini görürsün, kendi satırını
-- düzenleyebilirsin (görünen ad).
create policy household_members_select on public.household_members
    for select to authenticated
    using (public.is_member(household_id));

create policy household_members_update on public.household_members
    for update to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- Envanter tabloları: hepsi aynı kalıp.
create policy locations_all on public.locations
    for all to authenticated
    using (public.is_member(household_id))
    with check (public.is_member(household_id));

create policy items_all on public.items
    for all to authenticated
    using (public.is_member(household_id))
    with check (public.is_member(household_id));

create policy shopping_items_all on public.shopping_items
    for all to authenticated
    using (public.is_member(household_id))
    with check (public.is_member(household_id));

create policy barcode_memory_all on public.barcode_memory
    for all to authenticated
    using (public.is_member(household_id))
    with check (public.is_member(household_id));

-- activity_log: yalnızca oku ve ekle. Geçmişin sonradan değiştirilememesi
-- kaydın anlamının bir parçası.
create policy activity_log_select on public.activity_log
    for select to authenticated
    using (public.is_member(household_id));

create policy activity_log_insert on public.activity_log
    for insert to authenticated
    with check (public.is_member(household_id) and user_id = auth.uid());


-- ---------------------------------------------------------------------------
-- Ev oluşturma / katılma
--
-- İkisi de security definer: davet kodunu doğrulamak için, henüz üyesi
-- olmadığın bir evin satırını okumak gerekiyor — RLS bunu bilerek engelliyor.
-- ---------------------------------------------------------------------------

-- Karıştırılabilir karakterler (0/O, 1/I/L) alfabede yok.
create or replace function public.generate_invite_code()
returns text
language plpgsql
volatile
as $$
declare
    alphabet constant text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
    code text;
    i int;
begin
    loop
        code := '';
        for i in 1..8 loop
            code := code || substr(alphabet, 1 + floor(random() * length(alphabet))::int, 1);
        end loop;
        exit when not exists (select 1 from public.households where invite_code = code);
    end loop;
    return code;
end;
$$;

create or replace function public.create_household(household_name text, member_name text default '')
returns public.households
language plpgsql
security definer
set search_path = public
as $$
declare
    new_household public.households;
begin
    if auth.uid() is null then
        raise exception 'Oturum açmanız gerekiyor';
    end if;

    insert into public.households (name, invite_code)
    values (
        coalesce(nullif(trim(household_name), ''), 'Evimiz'),
        public.generate_invite_code()
    )
    returning * into new_household;

    insert into public.household_members (household_id, user_id, display_name)
    values (new_household.id, auth.uid(), trim(member_name));

    -- Boş bir uygulama karşılamasın diye tipik ev konumları hazır gelsin;
    -- kullanıcı bunları ayarlardan silebilir veya yeniden adlandırabilir.
    insert into public.locations (id, household_id, name, icon, sort_order)
    values
        (gen_random_uuid(), new_household.id, 'Buzdolabı',       'fridge',  1),
        (gen_random_uuid(), new_household.id, 'Buzluk',          'freezer', 2),
        (gen_random_uuid(), new_household.id, 'Bakliyat Dolabı', 'grain',   3),
        (gen_random_uuid(), new_household.id, 'Baharat Dolabı',  'spice',   4),
        (gen_random_uuid(), new_household.id, 'Takviye Dolabı',  'pill',    5),
        (gen_random_uuid(), new_household.id, 'Alt Dolap',       'cabinet', 6),
        (gen_random_uuid(), new_household.id, 'Yüklük',          'closet',  7);

    return new_household;
end;
$$;

create or replace function public.join_household(code text, member_name text default '')
returns public.households
language plpgsql
security definer
set search_path = public
as $$
declare
    target public.households;
begin
    if auth.uid() is null then
        raise exception 'Oturum açmanız gerekiyor';
    end if;

    select * into target
    from public.households
    where invite_code = upper(trim(code));

    if target.id is null then
        raise exception 'Davet kodu geçersiz';
    end if;

    insert into public.household_members (household_id, user_id, display_name)
    values (target.id, auth.uid(), trim(member_name))
    on conflict (household_id, user_id) do nothing;

    return target;
end;
$$;

revoke all on function public.create_household(text, text) from public;
revoke all on function public.join_household(text, text)   from public;
grant execute on function public.create_household(text, text) to authenticated;
grant execute on function public.join_household(text, text)   to authenticated;


-- ---------------------------------------------------------------------------
-- Realtime
--
-- İstemci bu tablolara abone olup uzak değişiklikleri anında Room'a yazar.
-- ---------------------------------------------------------------------------

alter publication supabase_realtime add table public.locations;
alter publication supabase_realtime add table public.items;
alter publication supabase_realtime add table public.shopping_items;
alter publication supabase_realtime add table public.activity_log;
