RENAME TABLE `book_types` TO `ownership_types`;
UPDATE `ownership_types` SET `type` = "Owned" WHERE `id` = 0;
ALTER TABLE `library_items` CHANGE `book_type_id` `ownership_type_id` TINYINT NOT NULL;
ALTER TABLE `library_items` MODIFY `progress_cfi` VARCHAR(255) NULL;
ALTER TABLE `library_items` DROP FOREIGN KEY `library_items_ibfk_1`;
ALTER TABLE `library_items` ADD FOREIGN KEY `fk_library_items_ownership_types` (`ownership_type_id`) references `ownership_types`(`id`);

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (7, '007-change-book-type-to-ownership-type.sql', 'RS-7');