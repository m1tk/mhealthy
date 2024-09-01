
create table userinfo (
    id serial primary key,
    name bytea not null,
    cin bytea not null,
    cin_hash bytea not null,
    account_type integer not null,
    description bytea,
    phone bytea not null,
    enc_nonce bytea not null,
    create_time bigint not null,
    last_seen_time bigint not null
);

create table token (
    id int primary key,
    token bytea not null,
    change_time bigint not null,
    foreign key(id) references userinfo (id) on delete cascade
);

create table cookie (
    id int primary key,
    cookie bytea not null,
    update_time bigint not null,
    foreign key(id) references userinfo (id) on delete cascade
);

create table assigned (
    caregiver int not null,
    patient int not null,
    id serial not null,
    primary key (caregiver, patient),
    foreign key(caregiver) references userinfo (id) on delete cascade,
    foreign key(patient) references userinfo (id) on delete cascade
);

create table caregiver_instruction (
    caregiver int,
    patient int not null,
    id bigserial not null,
    instruction bytea not null,
    enc_nonce bytea not null,
    primary key (caregiver, patient, id),
    foreign key(caregiver, patient) references assigned (caregiver, patient) on delete cascade
);

create table patient_info (
    patient int not null,
    id bigserial not null,
    info bytea not null,
    enc_nonce bytea not null,
    primary key (patient, id),
    foreign key(patient) references userinfo (id) on delete cascade
);

create or replace function new_assigned()
  returns trigger as $$
declare
begin
  perform pg_notify('assigned', format('%s,%s,%s', new.caregiver::text, new.patient::text, new.id::text));
  return new;
end;
$$ language plpgsql;

create or replace trigger trigger_new_assigned
  after insert
  on assigned
  for each row
  execute procedure new_assigned();

create or replace function new_caregiver_instruction()
  returns trigger as $$
declare
begin
  perform pg_notify('instruction', format('%s,%s,%s', new.caregiver::text, new.patient::text, new.id::text));
  return new;
end;
$$ language plpgsql;

create or replace trigger trigger_new_caregiver_instruction
  after insert
  on caregiver_instruction
  for each row
  execute procedure new_caregiver_instruction();

create or replace function new_patient_info()
  returns trigger as $$
declare
begin
  perform pg_notify('patient_info', format('%s,%s', new.patient::text, new.id::text));
  return new;
end;
$$ language plpgsql;

create or replace trigger trigger_patient_info
  after insert
  on patient_info
  for each row
  execute procedure new_patient_info();
