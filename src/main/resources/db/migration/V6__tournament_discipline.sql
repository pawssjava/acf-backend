create table if not exists acf.d_discipline
(
    id           bigserial primary key,
    name_ru      varchar,
    name_kk      varchar,
    name_en      varchar,
    is_active    boolean default true,
    updated_date timestamp with time zone,
    created_date timestamp with time zone
);

insert into acf.d_discipline (name_ru, name_kk, name_en, is_active, created_date, updated_date)
values ('FC 24', 'FC 24', 'FC 24', true, now(), now()),
       ('eFootball', 'eFootball', 'eFootball', true, now(), now()),
       ('UFL', 'UFL', 'UFL', true, now(), now())
on conflict do nothing;

alter table acf.tournament
    add column if not exists discipline_id bigint;

create index if not exists idx_tournament_discipline
    on acf.tournament (discipline_id);
