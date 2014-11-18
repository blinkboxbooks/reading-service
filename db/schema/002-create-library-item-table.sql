CREATE TABLE `library_item` (
  `isbn` CHAR(13) NOT NULL,
  `user_id` INT NOT NULL,
  `book_type` INT NOT NULL,
  `reading_status` INT NOT NULL,
  `progress_cfi` VARCHAR(254) NOT NULL,
  `progress_percentage` INT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `library_item` ADD CONSTRAINT `pk_library_item` PRIMARY KEY(`isbn`,`user_id`);

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (2, '002-create-library-item-table.sql', 'RS-5');