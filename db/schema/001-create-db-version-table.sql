CREATE TABLE `db_version` (
  `db_version_id` int(2) NOT NULL,
  `applied_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `file_name` varchar(100) NOT NULL,
  `jira_issue` varchar(12) NOT NULL,
  PRIMARY KEY (`db_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (1, '001-create-db-version-table.sql', 'RS-5');