CREATE TABLE `library_items` (
  `isbn` CHAR(13) NOT NULL,
  `user_id` INT NOT NULL,
  `book_type_id` TINYINT NOT NULL,
  `reading_status_id` TINYINT NOT NULL,
  `progress_cfi` VARCHAR(255) NOT NULL,
  `progress_percentage` TINYINT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`isbn`,`user_id`),
  FOREIGN KEY `fk_library_items_book_types` (`book_type_id`) references `book_types`(`id`),
  FOREIGN KEY `fk_library_items_reading_statuses` (`reading_status_id`) references `reading_statuses`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (4, '004-create-library-items-table.sql', 'RS-5');