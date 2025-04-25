create table user_public_keys(
  id serial primary key,
  user_id varchar(255) not null,
  key_value TEXT NOT NULL,
  key_algorithm varchar(255) not null comment 'RSA, ECDSA',
  key_type varchar(255) not null comment 'PEM, OpenSSH',
  created_at datetime not null,
  updated_at datetime not null,
  deleted_at datetime
)
