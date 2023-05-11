create schema if not exists master;

create table if not exists master.locations (
    id bigint not null,
    primary key (id),
    latitude double precision not null,
    longitude double precision not null,
    district text not null
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
    from_district text not null,
    to_district text not null,
    primary key (from_district, to_district),
    location_id bigint not null,
    foreign key (location_id) references master.locations(id)
);

-- explain analyse (select
--     s.id as s_id, s.latitude as s_latitude, s.longitude as s_longitude,
--     f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude,
--     ll.length
--     from master.location_links ll join master.locations s on ll.start = s.id join master.locations f on ll.finish = f.id
--     where s.id = 11111);

create index location_links_start_id_index on master.location_links using btree (start);