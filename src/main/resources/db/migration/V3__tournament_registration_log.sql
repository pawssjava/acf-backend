create table if not exists acf.tournament_registration_log
(
    id            bigserial primary key,
    tournament_id bigint                   not null,
    user_id       bigint                   not null,
    action        varchar                  not null,
    psn           varchar,
    created_date  timestamp with time zone not null default now()
);

create index if not exists idx_reg_log_tournament_date
    on acf.tournament_registration_log (tournament_id, created_date desc);

create table if not exists acf.sms_log
(
    id           bigserial primary key,
    phone_number varchar                  not null,
    code         varchar                  not null,
    action       varchar                  not null,
    sent_at      timestamp with time zone not null,
    expires_at   timestamp with time zone not null,
    verified_at  timestamp with time zone,
    used         boolean                  not null default false
);

create index if not exists idx_sms_log_phone_sent on acf.sms_log (phone_number, sent_at desc);

create table if not exists acf.partner
(
    id           bigserial primary key,
    name         varchar,
    logo         varchar,
    description  text,
    hyperlink    varchar,
    created_date timestamp with time zone,
    updated_date timestamp with time zone
);
