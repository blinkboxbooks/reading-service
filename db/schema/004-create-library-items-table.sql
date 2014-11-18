CREATE TABLE `library_items` (
  `isbn` CHAR(13) NOT NULL,
  `user_id` INT NOT NULL,
  `book_type` TINYINT NOT NULL,
  `reading_status` TINYINT NOT NULL,
  `progress_cfi` VARCHAR(254) NOT NULL,
  `progress_percentage` TINYINT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `library_items` ADD CONSTRAINT `pk_library_items` PRIMARY KEY (`isbn`,`user_id`);
ALTER TABLE `library_items` ADD CONSTRAINT `fk_library_items_book_types` FOREIGN KEY (`book_type`) references `book_types`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE `library_items` ADD CONSTRAINT `fk_library_items_reading_statuses` FOREIGN KEY (`reading_status`) references `reading_statuses`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION;

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (4, '004-create-library-items-table.sql', 'RS-5');