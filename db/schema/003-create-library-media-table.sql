CREATE TABLE `library_media` (
  `isbn` CHAR(13) NOT NULL,
  `media_type` INT NOT NULL,
  `uri` VARCHAR(254) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL
);

ALTER TABLE `library_media` ADD CONSTRAINT `pk_library_media` PRIMARY KEY (`isbn`,`media_type`);

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (3, '003-create-library-media-table.sql', 'RS-5');