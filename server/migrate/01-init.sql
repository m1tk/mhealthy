
create table userinfo (
    id serial primary key,
    name varchar(60) not null,
    cin varchar(15) not null,
    account_type integer not null,
    description varchar(100)
);

create table account (
    id int primary key,
    secret bytea not null,
    foreign key(id) references userinfo (id) on delete cascade
);

create table token (
    id int primary key,
    token bytea not null,
    foreign key(id) references userinfo (id) on delete cascade
);

create table assigned (
    caregiver int primary key,
    patient int primary key,
    foreign key(caregiver) references userinfo (id) on delete cascade,
    foreign key(patient) references userinfo (id) on delete cascade
);

create table caregiver_instruction (
    caregiver int not null,
    patient int not null,
    id int not null,
    instruction bytea not null,
    foreign key(caregiver) references userinfo (id) on delete cascade,
    foreign key(patient) references userinfo (id) on delete cascade
);

create table patient_info (
    patient int not null,
    id int not null,
    info bytea not null,
    foreign key(patient) references userinfo (id) on delete cascade
);
