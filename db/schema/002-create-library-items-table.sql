CREATE TABLE `library_items` (
  `isbn` CHAR(13) NOT NULL,
  `user_id` INT NOT NULL,
  `book_type` INT NOT NULL,
  `reading_status` INT NOT NULL,
  `progress_cfi` VARCHAR(254) NOT NULL,
  `progress_percentage` INT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `library_items` ADD CONSTRAINT `library_items_id` PRIMARY KEY(`isbn`,`user_id`);

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (2, '002-create-library-items-table.sql', 'RS-5');