create table if not exists reate table hub_message_delete_queue(
  id serial primary key,
  hub_message_mapping_id int not null,
  status int not null comment '0: pending, 1: success, 2: failed',
  created_at datetime not null,
  updated_at datetime not null,
  deleted_at datetime
) engine = InnoDB default charset = utf8mb4;
