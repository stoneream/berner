create table if not exists hub_message_mappings(
  id serial primary key,
  guild_id varchar(255) not null,
  source_guild_message_channel_id varchar(255) not null,
  source_thread_message_channel_id varchar(255),
  source_message_id varchar(255) not null,
  hub_guild_message_channel_id varchar(255) not null,
  hub_message_id varchar(255) not null,
  created_at datetime not null,
  updated_at datetime not null,
  deleted_at datetime
) engine = InnoDB default charset = utf8mb4;
