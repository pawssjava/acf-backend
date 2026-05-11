create schema if not exists acf;

create table if not exists acf.d_city
(
    id           bigserial primary key,
    name_ru      varchar,
    name_kk      varchar,
    name_en      varchar,
    is_active    boolean                  default true,
    created_date timestamp with time zone default now(),
    updated_date timestamp with time zone default now()
);

INSERT INTO acf.d_city (name_ru,
                        name_kk,
                        name_en,
                        is_active,
                        created_date,
                        updated_date)
VALUES ('Алматы', 'Алматы', 'Almaty', true, NOW(), NOW()),
       ('Астана', 'Астана', 'Astana', true, NOW(), NOW()),
       ('Шымкент', 'Шымкент', 'Shymkent', true, NOW(), NOW()),
       ('Караганда', 'Қарағанды', 'Karaganda', true, NOW(), NOW()),
       ('Актобе', 'Ақтөбе', 'Aktobe', true, NOW(), NOW()),
       ('Тараз', 'Тараз', 'Taraz', true, NOW(), NOW()),
       ('Павлодар', 'Павлодар', 'Pavlodar', true, NOW(), NOW()),
       ('Усть-Каменогорск', 'Өскемен', 'Ust-Kamenogorsk', true, NOW(), NOW()),
       ('Семей', 'Семей', 'Semey', true, NOW(), NOW()),
       ('Атырау', 'Атырау', 'Atyrau', true, NOW(), NOW()),
       ('Костанай', 'Қостанай', 'Kostanay', true, NOW(), NOW()),
       ('Кызылорда', 'Қызылорда', 'Kyzylorda', true, NOW(), NOW()),
       ('Уральск', 'Орал', 'Uralsk', true, NOW(), NOW()),
       ('Петропавловск', 'Петропавл', 'Petropavl', true, NOW(), NOW()),
       ('Актау', 'Ақтау', 'Aktau', true, NOW(), NOW()),
       ('Темиртау', 'Теміртау', 'Temirtau', true, NOW(), NOW()),
       ('Туркестан', 'Түркістан', 'Turkistan', true, NOW(), NOW()),
       ('Кокшетау', 'Көкшетау', 'Kokshetau', true, NOW(), NOW()),
       ('Талдыкорган', 'Талдықорған', 'Taldykorgan', true, NOW(), NOW()),
       ('Экибастуз', 'Екібастұз', 'Ekibastuz', true, NOW(), NOW()),
       ('Рудный', 'Рудный', 'Rudny', true, NOW(), NOW()),
       ('Жезказган', 'Жезқазған', 'Zhezkazgan', true, NOW(), NOW()),
       ('Кентау', 'Кентау', 'Kentau', true, NOW(), NOW()),
       ('Сатпаев', 'Сәтбаев', 'Satpayev', true, NOW(), NOW()),
       ('Балхаш', 'Балқаш', 'Balkhash', true, NOW(), NOW())
on conflict do nothing;

create table if not exists acf.d_club
(
    id           bigserial primary key,
    name_ru      varchar,
    name_kk      varchar,
    name_en      varchar,
    is_active    boolean                  default true,
    created_date timestamp with time zone default now(),
    updated_date timestamp with time zone default now()
);
create table if not exists acf.user
(
    id                    bigserial primary key,
    username              varchar unique not null,
    phone_number          bigint unique  not null,
    first_name            varchar,
    last_name             varchar,
    birth_date            date,
    is_admin              boolean default false,
    photo                 varchar,
    city_id               bigint,
    is_verified           boolean default false,
    verification_document varchar,
    club_id               bigint,
    updated_date          timestamp with time zone,
    created_date          timestamp with time zone
);

create table if not exists acf.d_tournament_type
(
    id           bigserial primary key,
    name         varchar,
    is_active    boolean default true,
    updated_date timestamp with time zone,
    created_date timestamp with time zone
);

create table if not exists acf.d_tournament_status
(
    id           bigserial primary key,
    name         varchar,
    is_active    boolean default true,
    updated_date timestamp with time zone,
    created_date timestamp with time zone
);

create table if not exists acf.news
(
    id           bigserial primary key,
    title        varchar,
    description  text,
    image        varchar,
    updated_date timestamp with time zone,
    created_date timestamp with time zone
);

create table if not exists acf.tournament
(
    id                bigserial primary key,
    name              varchar,
    logo              varchar,
    start_date        date,
    end_date          date,
    capacity          integer,
    prize_money       numeric,
    tournament_status bigint,
    tournament_type   bigint,
    format            varchar,
    phase             varchar,
    total_rounds      integer,
    updated_date      timestamp with time zone,
    created_date      timestamp with time zone
);

create table if not exists acf.tournament_registration
(
    id              bigserial primary key,
    tournament_id   bigint                   not null,
    user_id         bigint                   not null,
    psn             varchar                  not null default '',
    registered_date timestamp with time zone not null default now(),
    constraint uq_tournament_registration unique (tournament_id, user_id)
);

create table if not exists acf.tournament_result
(
    id            bigserial primary key,
    tournament_id bigint not null,
    user_id       bigint not null,
    place         integer,
    score         numeric,
    created_date  timestamp with time zone,
    constraint uq_tournament_result unique (tournament_id, user_id)
);

create table if not exists acf.tournament_match
(
    id              bigserial primary key,
    tournament_id   bigint  not null,
    phase           varchar not null,
    round_number    integer,
    match_number    integer,
    group_name      varchar,
    participant1_id bigint,
    participant2_id bigint,
    score1          integer default 0,
    score2          integer default 0,
    winner_id       bigint,
    loser_id        bigint,
    status          varchar default 'PENDING',
    next_match_id   bigint,
    next_match_slot integer,
    created_date    timestamp with time zone,
    updated_date    timestamp with time zone
);