create table user_public_key(
  id serial primary key,
  user_id varchar(255) not null,
  key_pem TEXT NOT NULL,
  key_type varchar(255) not null comment 'RSA, ECDSA, etc',
  created_at datetime not null,
  updated_at datetime not null,
  deleted_at datetime
)
