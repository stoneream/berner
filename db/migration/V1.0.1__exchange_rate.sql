-- todo コメント書きたい

create table if not exists exchange_rates(
  id serial primary key,
  base_currency varchar(255) not null,
  target_currency varchar(255) not null,
  rate numeric(10, 4) not null,
  target_date datetime not null,
  created_at datetime not null,
  updated_at datetime not null,
  deleted_at datetime
) engine = InnoDB default charset = utf8mb4;
