package com.batch.docusign.step;

import java.util.List;

import com.batch.docusign.entity.MstDocument;
import com.batch.docusign.repository.MstDocumentRepo;
import com.batch.docusign.service.MstDocumentService;

import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Value;

import com.amazonaws.services.s3.AmazonS3;

public class Writer implements ItemWriter<MstDocument> {

	MstDocumentRepo mstDocumentRepo;
	MstDocumentService mstDocumentService;
	ResourceLoader resourceLoader;
	AmazonS3 s3client;
	String bucketFolder = null;

	public Writer(MstDocumentRepo mstDocumentRepo,MstDocumentService mstDocumentService,ResourceLoader resourceLoader,AmazonS3 s3client,String bucketFolder){
		this.mstDocumentRepo = mstDocumentRepo;
		this.mstDocumentService = mstDocumentService;
		this.resourceLoader = resourceLoader;
		this.s3client = s3client;
		this.bucketFolder = bucketFolder;
	}
	
    @Override
	public void write(List<? extends MstDocument> mstDocuments) throws Exception {
		String accountId = mstDocumentService.getDocusignAccountId(resourceLoader);
		System.out.println("ACCOUNT ID:: "+accountId);
		boolean flag = false;
		for (MstDocument doc : mstDocuments) {
			if(accountId != null){
				flag = mstDocumentService.embeddedDownload(accountId, doc, resourceLoader, s3client);
				if(flag){
					doc.setIsGenerated("Y");
					doc.setModifiedFileName(doc.getDocumentId()+".pdf");
					doc.setModifiedFilePath(bucketFolder + "/" + doc.getDocumentId()+".pdf");
					mstDocumentRepo.save(doc);
				}
			}
		}
	}
}
