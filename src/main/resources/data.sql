DROP TABLE IF EXISTS document;

CREATE TABLE document (
  id INT AUTO_INCREMENT  PRIMARY KEY,
  doc_name VARCHAR(250) NOT NULL,
  envelope_id VARCHAR(250) NOT NULL,
  document_id VARCHAR(250) DEFAULT NULL,
  is_generated VARCHAR(250) DEFAULT NULL,
  modified_file_name VARCHAR(250) DEFAULT NULL,
  modified_file_path VARCHAR(250) DEFAULT NULL
);

INSERT INTO document (doc_name, envelope_id, document_id, is_generated, modified_file_name, modified_file_path) VALUES
  ('Aliko', '9f08199e-5872-4bf4-90c9-fd0df3149ef0', '1', 'N','1.pdf','test/1.pdf'),
  ('Bill', '4b9e4702-092c-44ba-a586-4ae720c631f6', '1', 'N','1.jpg','dev/1.jpg');