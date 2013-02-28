CREATE TABLE `JDODOUBLEANNOTATION` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ATTRIBUTE` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
  `OWNER_ID` bigint(20) NOT NULL,
  `VALUE` double DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `DOUBLE_ANNO_UNIQUE` (`OWNER_ID`,`ATTRIBUTE, `VALUE`),
  KEY `JDODOUBLEANNOTATION_N49` (`OWNER_ID`)
)