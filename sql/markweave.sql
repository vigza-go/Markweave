drop table if exists user;
create table user (
  id bigint PRIMARY key,
  account varchar(32) not null ,
  password VARCHAR(128) not null,
  salt varchar(32) not null,
  nick_name varchar(32) not null,
  head_url varchar(100) ,
  create_time timestamp not null,
  user_space_node_id bigint comment '用户个人空间根节点id',
  user_share_space_node_id bigint comment '用户共享空间根节点id',
  type int default 1 comment '0 - 被禁用 1 - 普通用户 2 - 管理员 3 - 超级管理员', 
  unique key (account)
);


drop table if exists doc;
create table doc(
  id bigint primary key,
  content longtext
);

drop table if exists collaboration;
create table collaboration(
  id char(64) PRIMARY key,
  user_id bigint not null,
  doc_id bigint not null,
  permission int not null comment '1 - 创建者 2 - 读写 3 - 只读',
  unique key uk_user_doc (user_id, doc_id),
  key idx_user_id (user_id),
  key idx_doc_id (doc_id)
) comment '文档权限协作表';

drop table if exists fs_node;
create table fs_node(
  id bigint primary key,
  user_id bigint not null comment '创建者',
  doc_owner varchar(32) comment '文档所有者',
  doc_id bigint comment '文档id',
  name varchar(128) comment '文件(夹)名',
  size bigint default 0 comment '文件大小',
  fa_id bigint not null comment '父节点名',
  pt_id bigint  comment '指针节点（快捷方式）指向的真实节点id',
  path varchar(256) comment '/0/userId/.../fs_id/xxx/xxx',
  type int not null comment '1 - 文件 2 - 文件夹 3 - 指针节点（快捷方式）',
  recycled tinyint(1) default 0 comment '是否已经回收',
  last_view_time timestamp comment '方便实现最近查看',
  create_time TIMESTAMP not null ,
  update_time TIMESTAMP 
);
