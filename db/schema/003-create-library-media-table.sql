CREATE TABLE `library_media` (
  `isbn` CHAR(13) NOT NULL,
  `media_type` INT NOT NULL,
  `uri` VARCHAR(254) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE `library_item_links` ADD CONSTRAINT `library_item_links_isbn` PRIMARY KEY (`isbn`,`media_type`);

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (3, '003-create-library-media-table.sql', 'RS-5');