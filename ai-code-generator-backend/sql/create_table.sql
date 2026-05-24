# 创建数据库
create database if not exists ai_code_generator;
# 使用数据库
use ai_code_generator;

# 创建用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '用户账号',
    userPassword varchar(512)                           not null comment '用户密码',
    userName     varchar(256) comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色: user-普通用户 admin-管理员',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除: 0-未删除 1-已删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

# 创建应用表
create table if not exists app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               null comment '应用初始提示的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型(枚举)',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    priority     int      default 0                 not null comment '应用优先级',
    userId       bigint                             not null comment '创建者 id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除: 0-未删除 1-已删除',
    UNIQUE KEY uk_deployKey (deployKey),
    INDEX idx_appName (appName),
    INDEX idx_userId (userId)
) comment '应用' collate = utf8mb4_unicode_ci;