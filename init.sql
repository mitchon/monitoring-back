create schema if not exists master;

create table if not exists master.locations (
    id bigint not null,
    primary key (id),
    latitude double precision not null,
    longitude double precision not null,
    district text not null,
    type text not null
);

create table if not exists master.location_links (
    start bigint not null,
    finish bigint not null,
    primary key (start, finish),
    length double precision not null,
    max_speed double precision not null,
    foreign key (start) references master.locations(id),
    foreign key (finish) references master.locations(id)
);

create table if not exists master.borders (
    id uuid not null,
    from_district text not null,
    to_district text not null,
    primary key (id),
    location_id bigint not null,
    foreign key (location_id) references master.locations(id)
);

create index location_links_start_id_index on master.location_links using btree (start);