CREATE TABLE IF NOT EXISTS `QUARANTINED_EMAILS` (
  `EMAIL` VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci NOT NULL,
  `ETAG` CHAR(36) NOT NULL,
  `CREATED_ON` TIMESTAMP(3) NOT NULL,
  `UPDATED_ON` TIMESTAMP(3) NOT NULL,
  `EXPIRES_ON` TIMESTAMP(3) DEFAULT NULL,
  `REASON` ENUM('PERMANENT_BOUNCE', 'TRANSIENT_BOUNCE', 'COMPLAINT', 'OTHER') NOT NULL,
  `REASON_DETAILS` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `SES_MESSAGE_ID` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`EMAIL`),
  INDEX `QUARANTINED_EMAILS_SES_MESSAGE_ID_INDEX` (`SES_MESSAGE_ID`),
  INDEX `QUARANTINED_EMAILS_TIMEOUT_INDEX` (`EXPIRES_ON`)
)
