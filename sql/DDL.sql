CREATE DATABASE mydb;
USE mydb;


-- ##################### 유저 #####################
create table member(
    id VARCHAR(30) NOT NULL UNIQUE,
    pw VARCHAR(150) NOT NULL,
    display_name VARCHAR(30) NOT NULL,
    status_message VARCHAR(100),
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    PRIMARY KEY(id)
);


-- ##################### 1:1 채팅 #####################
CREATE TABLE personal_chat (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_user_id VARCHAR(30) NOT NULL,
    receiver_user_id VARCHAR(30) NOT NULL,
    group_id VARCHAR(61) NOT NULL,
    content VARCHAR(5000) NOT NULL DEFAULT '',
    read_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(id)
);

CREATE TABLE personal_chat_backup (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_user_id VARCHAR(30) NOT NULL,
    receiver_user_id VARCHAR(30) NOT NULL,
    group_id VARCHAR(61) NOT NULL,
    content VARCHAR(5000) NOT NULL DEFAULT '',
    read_at DATETIME,
    created_at DATETIME,
    deleted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(id)
);


-- ##################### 그룹 채팅 #####################
CREATE TABLE group_room (
    id BIGINT NOT NULL UNIQUE AUTO_INCREMENT,
    PRIMARY KEY (id)
);

CREATE TABLE group_room_members (
    room_id BIGINT      NOT NULL,
    user_id VARCHAR(30) NOT NULL,
    PRIMARY KEY (room_id, user_id),
    FOREIGN KEY (room_id) REFERENCES group_room(id),
    FOREIGN KEY (user_id) REFERENCES member(id)
);

CREATE TABLE group_chat (
    id             BIGINT        NOT NULL UNIQUE AUTO_INCREMENT,
    sender_user_id VARCHAR(30)   NOT NULL,
    room_id        BIGINT        NOT NULL,
    content        VARCHAR(5000) NOT NULL DEFAULT '',
    created_at     DATETIME               DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (sender_user_id) REFERENCES member(id),
    FOREIGN KEY (room_id) REFERENCES group_room(id)
);

CREATE TABLE group_chat_read_time (
    chat_id BIGINT      NOT NULL,
    user_id VARCHAR(30) NOT NULL,
    read_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chat_id, user_id),
    FOREIGN KEY (chat_id) REFERENCES group_chat(id),
    FOREIGN KEY (user_id) REFERENCES member(id)
);

CREATE TABLE group_chat_backup (
    id             BIGINT        NOT NULL UNIQUE AUTO_INCREMENT,
    sender_user_id VARCHAR(30)   NOT NULL,
    target_room_id BIGINT        NOT NULL,
    content        VARCHAR(5000) NOT NULL DEFAULT '',
    created_at     DATETIME,
    deleted_at     DATETIME               DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);


-- ##################### Index #####################
CREATE INDEX idx_group_id ON personal_chat (group_id, id);
CREATE INDEX idx_receiver_id ON personal_chat (receiver_user_id, id);
CREATE INDEX idx_sender_id ON personal_chat (sender_user_id, id);
CREATE INDEX idx_receiver_sender_id ON personal_chat (receiver_user_id, sender_user_id, id);


-- ##################### Trigger #####################
DELIMITER $$

DROP TRIGGER IF EXISTS `BACKUP_DELETED_PERSONAL_CHAT`;
CREATE TRIGGER `BACKUP_DELETED_PERSONAL_CHAT`
    BEFORE DELETE ON personal_chat
    FOR EACH ROW
BEGIN
    INSERT INTO personal_chat_backup(id, sender_user_id, receiver_user_id, group_id, content, read_at, created_at)
        VALUE (OLD.id, OLD.sender_user_id, OLD.receiver_user_id, OLD.group_id, OLD.content, OLD.read_at, OLD.created_at);
END $$

DELIMITER ;
