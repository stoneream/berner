-- todo コメント書きたい

create table if not exists hub_messages(
  id serial primary key,
  source_message_id varchar(255) not null,
  source_channel_id varchar(255) not null,
  message_id varchar(255) not null,
  channel_id varchar(255) not null,
  guild_id varchar(255) not null,
  created_at datetime not null,
  updated_at datetime not null,
  deleted_at datetime
) engine = InnoDB default charset = utf8mb4;
