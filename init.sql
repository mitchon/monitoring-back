create schema if not exists master;

create table if not exists master.users (
    id uuid default gen_random_uuid(),
    login text not null,
    password text not null,
    constraint users$pk primary key (id)
);

insert into master.users (login, password)
values ('admin', 'cm9vdA==') on conflict do nothing;

create table if not exists master.tokens (
    id uuid,
    user_id uuid,
    foreign key (user_id) references master.users(id),
    valid bool,
    primary key (id)
);

create table if not exists master.ways (
    id uuid,
    osm_id bigint not null,
    primary key (id)
);

create table if not exists master.nodes (
    id uuid,
    osm_id bigint not null,
    way_id bigint not null,
    primary key (id),
    latitude double precision not null,
    longitude double precision not null,
    index integer not null
);