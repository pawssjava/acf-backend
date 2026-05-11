insert into acf.d_tournament_type (name, is_active, created_date, updated_date)
values ('Региональные турниры', true, now(), now()),
       ('Социальная лига', true, now(), now()),
       ('Школьная лига', true, now(), now()),
       ('Студенческая лига', true, now(), now()),
       ('EKPL', true, now(), now())
on conflict do nothing;

insert into acf.d_tournament_status (name, is_active, created_date, updated_date)
values ('Активные', true, now(), now()),
       ('Будущие', true, now(), now()),
       ('Завершенные', true, now(), now())
on conflict do nothing;
