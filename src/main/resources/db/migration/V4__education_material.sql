create table if not exists acf.education_material
(
    id                bigserial primary key,
    title             varchar,
    description       text,
    category          varchar,
    video_path        varchar,
    presentation_path varchar,
    thumbnail_path    varchar,
    ordering          integer                  default 0,
    created_date      timestamp with time zone default now(),
    updated_date      timestamp with time zone default now()
);

create index if not exists idx_education_material_category
    on acf.education_material (category);
