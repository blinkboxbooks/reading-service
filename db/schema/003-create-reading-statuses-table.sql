CREATE TABLE `reading_statuses` (
  `id` TINYINT NOT NULL,
  `status` VARCHAR(15) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `reading_statuses` ADD CONSTRAINT `pk_reading_statuses` PRIMARY KEY(`id`);

INSERT INTO reading_statuses (id, status) VALUES (0, "NotStarted");
INSERT INTO reading_statuses (id, status) VALUES (1, "Reading");
INSERT INTO reading_statuses (id, status) VALUES (2, "Finished");

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (3, '003-create-reading-statuses-table.sql', 'RS-5');