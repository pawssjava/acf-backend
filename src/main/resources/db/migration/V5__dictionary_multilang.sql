alter table acf.d_tournament_status
    add column if not exists name_ru varchar,
    add column if not exists name_kk varchar,
    add column if not exists name_en varchar;

update acf.d_tournament_status
set name_ru = name
where name_ru is null;

alter table acf.d_tournament_type
    add column if not exists name_ru varchar,
    add column if not exists name_kk varchar,
    add column if not exists name_en varchar;

update acf.d_tournament_type
set name_ru = name
where name_ru is null;


-- tournament statuses
update acf.d_tournament_status
set name_kk = 'Белсенді',
    name_en = 'Active'
where id = 1;
update acf.d_tournament_status
set name_kk = 'Алдағы',
    name_en = 'Upcoming'
where id = 2;
update acf.d_tournament_status
set name_kk = 'Аяқталған',
    name_en = 'Completed'
where id = 3;

-- tournament types
update acf.d_tournament_type
set name_kk = 'Аймақтық турнирлер',
    name_en = 'Regional Tournaments'
where id = 1;
update acf.d_tournament_type
set name_kk = 'Әлеуметтік лига',
    name_en = 'Social League'
where id = 2;
update acf.d_tournament_type
set name_kk = 'Мектеп лигасы',
    name_en = 'School League'
where id = 3;
update acf.d_tournament_type
set name_kk = 'Студенттік лига',
    name_en = 'Student League'
where id = 4;
update acf.d_tournament_type
set name_kk = 'EKPL',
    name_en = 'EKPL'
where id = 5;