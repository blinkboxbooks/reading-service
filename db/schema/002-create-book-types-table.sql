CREATE TABLE `book_types` (
  `id` TINYINT NOT NULL,
  `type` VARCHAR(15) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `book_types` ADD CONSTRAINT `pk_book_types` PRIMARY KEY(`id`);

INSERT INTO book_types (id, type) VALUES (0, "Full");
INSERT INTO book_types (id, type) VALUES (1, "Sample");

INSERT INTO db_version (db_version_id, file_name, jira_issue)
VALUES (2, '002-create-book-types-table.sql', 'RS-5');