CREATE TABLE IF NOT EXISTS `DISCUSSION_THREAD_STATS` (
  `THREAD_ID` bigint(20) NOT NULL,
  `NUMBER_OF_VIEWS` bigint(20) DEFAULT NULL,
  `NUMBER_OF_REPLIES` bigint(20) DEFAULT NULL,
  `LAST_ACTIVITY` TIMESTAMP NULL,
  `ACTIVE_AUTHORS` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`THREAD_ID`),
  CONSTRAINT `DISCUSSION_THREAD_STATS_THREAD_ID_FK` FOREIGN KEY (`THREAD_ID`) REFERENCES `DISCUSSION_THREAD` (`ID`) ON DELETE CASCADE
)