package com.batch.docusign.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="document")
public class MstDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name="doc_name")
    private String name;
    @Column(name="envelope_id")
    private String envelopeId;
    @Column(name="document_id")
    private String documentId;
    @Column(name="is_generated")
    private String isGenerated;
    @Column(name="modified_file_name")
    private String modifiedFileName;
    @Column(name="modified_file_path")
    private String modifiedFilePath;
}
