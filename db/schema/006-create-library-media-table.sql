CREATE TABLE `library_media` (
  `isbn` CHAR(13) NOT NULL,
  `media_type_id` TINYINT NOT NULL,
  `uri` VARCHAR(254) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`isbn`,`media_type_id`),
  FOREIGN KEY `fk_library_media_media_types` (`media_type_id`) REFERENCES `media_types`(`id`)
);

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (6, '006-create-library-media-table.sql', 'RS-5');