package com.batch.docusign.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import com.batch.docusign.step.Processor;
import com.batch.docusign.step.Reader;
import com.batch.docusign.step.Writer;
import com.batch.docusign.entity.MstDocument;
import com.batch.docusign.listner.JobCompletionListener;
import com.batch.docusign.repository.MstDocumentRepo;
import com.batch.docusign.service.MstDocumentService;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class BatchConfig {
    
    @Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	MstDocumentRepo mstDocumentRepo;

	@Autowired
	MstDocumentService mstDocumentService;

	@Autowired
    ResourceLoader resourceLoader;
	
	private AmazonS3 s3client;

	@Value("${storage.aws.s3.accessKey}")
	private String accessKey;
	@Value("${storage.aws.s3.secretKey}")
	private String secretKey;
	@Value("${storage.aws.s3.region}")
	private String region;
	@Value("${storage.aws.s3.bucketFolder}")
	private String bucketFolder;

	@PostConstruct
	private void initializeAmazon() {

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
		this.s3client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withRegion(region).build();
	}

	@Bean
	public Job processJob() {
		return jobBuilderFactory.get("processJob")
				.incrementer(new RunIdIncrementer()).listener(listener())
				.flow(orderStep1()).end().build();
	}

	@Bean
	public Step orderStep1() {
		return stepBuilderFactory.get("orderStep1").<MstDocument, MstDocument> chunk(10)
				.reader(new Reader(mstDocumentRepo))
				.writer(new Writer(mstDocumentRepo,mstDocumentService,resourceLoader,s3client,bucketFolder)).build();
	}

	@Bean
	public JobExecutionListener listener() {
		return new JobCompletionListener();
	}
}
