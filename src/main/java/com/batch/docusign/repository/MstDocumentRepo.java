package com.batch.docusign.repository;

import com.batch.docusign.entity.MstDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MstDocumentRepo extends JpaRepository<MstDocument,Long> {
    
    List<MstDocument> findAllByEnvelopeId(String envelopeId);
}
