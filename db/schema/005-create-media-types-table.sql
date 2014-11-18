CREATE TABLE `media_types` (
  `id` TINYINT NOT NULL PRIMARY KEY,
  `type` VARCHAR(15) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO media_types (id, type) VALUES (0, "EpubKey");
INSERT INTO media_types (id, type) VALUES (1, "FullEpub");

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (5, '005-create-media-types-table.sql', 'RS-5');