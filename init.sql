create schema if not exists master;

create table if not exists master.locations (
    id bigint not null,
    primary key (id),
    latitude double precision not null,
    longitude double precision not null
);

create table if not exists master.location_links (
    start bigint not null,
    finish bigint not null,
    primary key (start, finish),
    length double precision not null
);