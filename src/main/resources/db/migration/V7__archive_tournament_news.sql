alter table acf.tournament
    add column if not exists is_archived boolean not null default false,
    add column if not exists archived_date timestamp with time zone;

alter table acf.news
    add column if not exists is_archived boolean not null default false,
    add column if not exists archived_date timestamp with time zone;

create index if not exists idx_tournament_is_archived
    on acf.tournament (is_archived);

create index if not exists idx_news_is_archived
    on acf.news (is_archived);
