CREATE DATABASE IF NOT EXISTS comaas;
GRANT ALL PRIVILEGES ON comaas.* TO 'mysql_guest'@'%';
USE comaas;
CREATE TABLE IF NOT EXISTS rts2_event_log (
  id                           INT(11)      NOT NULL AUTO_INCREMENT,
  messageId                    VARCHAR(45)  NOT NULL,
  conversationId               VARCHAR(45)  NOT NULL,
  messageDirection             VARCHAR(20)  NOT NULL,
  conversationState            VARCHAR(20)  NOT NULL,
  messageState                 VARCHAR(20)  NOT NULL,
  adId                         VARCHAR(20)  NOT NULL,
  sellerMail                   VARCHAR(100) NOT NULL,
  buyerMail                    VARCHAR(100) NOT NULL,
  numOfMessageInConversation   VARCHAR(20)  NOT NULL,
  logTimestamp                 VARCHAR(25)  NOT NULL,
  conversationCreatedAt        VARCHAR(25)  NOT NULL,
  messageReceivedAt            VARCHAR(25)  NOT NULL,
  conversationLastModifiedDate VARCHAR(25)  NOT NULL,
  custcategoryid               VARCHAR(20)  NOT NULL,
  custip                       VARCHAR(20)           DEFAULT NULL,
  custuseragent                VARCHAR(255)          DEFAULT NULL,
  custreplychannel             VARCHAR(20)           DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_conversation_creation (conversationCreatedAt),
  KEY idx_log_timestamp (logTimestamp)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS userdata (
  id     INT(11)      NOT NULL AUTO_INCREMENT,
  status VARCHAR(20)  NOT NULL,
  email  VARCHAR(100) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS external_user_tns (
  id     INT(11)      NOT NULL AUTO_INCREMENT,
  status VARCHAR(20)  NOT NULL,
  email  VARCHAR(100) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ip_ranges (
  id              INT(11)     NOT NULL AUTO_INCREMENT,
  expiration_date DATETIME    NOT NULL,
  begin_ip        VARCHAR(15) NOT NULL,
  end_ip          VARCHAR(15) NOT NULL,
  PRIMARY KEY (id)
);