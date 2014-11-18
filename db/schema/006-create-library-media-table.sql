CREATE TABLE `library_media` (
  `isbn` CHAR(13) NOT NULL,
  `media_type_id` TINYINT NOT NULL,
  `uri` VARCHAR(254) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL
);

ALTER TABLE `library_media` ADD CONSTRAINT `pk_library_media` PRIMARY KEY (`isbn`,`media_type_id`);
ALTER TABLE `library_media` ADD CONSTRAINT `fk_library_media_media_types` FOREIGN KEY (`media_type_id`) REFERENCES `media_types`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION;

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (6, '006-create-library-media-table.sql', 'RS-5');