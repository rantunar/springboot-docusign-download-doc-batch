package com.batch.docusign.step;

import java.util.List;
import java.util.stream.Collectors;

import com.batch.docusign.entity.MstDocument;
import com.batch.docusign.repository.MstDocumentRepo;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Reader implements ItemReader<MstDocument> {
    
    private String[] messages = { "javainuse.com",
			"Welcome to Spring Batch Example",
			"We use H2 Database for this example" };

	private int count = 0;
	
	MstDocumentRepo mstDocumentRepo;

	public Reader(MstDocumentRepo mstDocumentRepo){
		this.mstDocumentRepo = mstDocumentRepo;
	}

	@Override
	public MstDocument read() throws Exception, UnexpectedInputException,
			ParseException, NonTransientResourceException {

		List<MstDocument> docList = mstDocumentRepo.findAll().stream().filter(x -> x.getIsGenerated().equals("N")).collect(Collectors.toList());
		
		if (count < docList.size()) {
			return docList.get(count++);
		} else {
			count = 0;
		}
		return null;
	}
}
