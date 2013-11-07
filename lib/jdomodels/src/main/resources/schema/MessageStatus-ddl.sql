CREATE TABLE `MESSAGE_STATUS` (
  `MESSAGE_ID` bigint(20) NOT NULL,
  `RECIPIENT_ID` bigint(20) NOT NULL,
  `STATUS` ENUM('READ', 'UNREAD', 'ARCHIVED') NOT NULL, 
  PRIMARY KEY (`MESSAGE_ID`, `RECIPIENT_ID`),
  CONSTRAINT `MESSAGE_ID_FK` FOREIGN KEY (`MESSAGE_ID`) REFERENCES `MESSAGE` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `RECIPIENT_ID_FK` FOREIGN KEY (`RECIPIENT_ID`) REFERENCES `JDOUSERGROUP` (`ID`) ON DELETE CASCADE
)