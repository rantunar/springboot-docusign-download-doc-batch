package com.batch.docusign.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.Configuration;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.client.auth.OAuth.UserInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

import com.batch.docusign.entity.MstDocument;
import com.batch.docusign.repository.MstDocumentRepo;

@Service
public class MstDocumentService {

    @Value("${docusign.server.url}")
    String docusignurl;

    @Value("${docusign.server.integrationkey}")
    String integrationkey;

    @Value("${docusign.server.username}")
    String username;

    @Value("${docusign.server.returnurl}")
    String returnurl;

    @Value("${storage.aws.s3.bucketName}")
	private String bucketName;
    
    @Value("${storage.aws.s3.bucketFolder}")
	private String bucketFolder;

    @Value("${storage.aws.s3.accessKey}")
	private String accessKey;
	@Value("${storage.aws.s3.secretKey}")
	private String secretKey;
	@Value("${storage.aws.s3.region}")
	private String region;

    @Autowired
    MstDocumentRepo mstDocumentRepo;

    public String getDocusignAccountId(ResourceLoader resourceLoader){
        String accountId = null;
        try{
            //String currentDir = System.getProperty("user.dir");
            Resource resource = resourceLoader.getResource("classpath:static/privatekey.txt");
            File privateKeyFile = resource.getFile();
            FileInputStream fin = new FileInputStream(privateKeyFile);
            byte fileContent[] = new byte[(int)privateKeyFile.length()];
            fin.read(fileContent);
            fin.close();
            ApiClient apiClient = new ApiClient(docusignurl);

            // IMPORTANT NOTE:
            // the first time you ask for a JWT access token, you should grant access by making the following call
            // get DocuSign OAuth authorization url:
            //String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
            // open DocuSign OAuth authorization url in the browser, login and grant access
            //Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
            // END OF NOTE

            java.util.List<String> scopes = new ArrayList<String>();
            scopes.add(OAuth.Scope_SIGNATURE);

            OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(integrationkey, username, scopes, fileContent, 3600);
            // now that the API client has an OAuth token, let's use it in all
            // DocuSign APIs
            apiClient.setAccessToken(oAuthToken.getAccessToken(), oAuthToken.getExpiresIn());
            UserInfo userInfo = apiClient.getUserInfo(oAuthToken.getAccessToken());

            // parse first account's baseUrl
            // below code required for production, no effect in demo (same
            // domain)
            apiClient.setBasePath(userInfo.getAccounts().get(0).getBaseUri() + "/restapi");
            Configuration.setDefaultApiClient(apiClient);
            accountId = userInfo.getAccounts().get(0).getAccountId();
        }catch(Exception e){
            e.printStackTrace();
        }       
        return accountId;
    }

    public boolean embeddedDownload(String accountId, MstDocument doc, ResourceLoader resourceLoader, AmazonS3 s3client){
        boolean flag = false;
        try{
            if(s3client.doesObjectExist(bucketName, bucketFolder + "/" + doc.getModifiedFileName())){
                EnvelopesApi envelopesApi = new EnvelopesApi();
                byte[] results = envelopesApi.getDocument(accountId, doc.getEnvelopeId(), doc.getDocumentId());
                
                String strModFileName = doc.getDocumentId()+".pdf";
                File file = new File(strModFileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(results);
                fos.close();
                
                String fileUrl = bucketFolder + "/" + strModFileName;
                PutObjectRequest request = new PutObjectRequest(bucketName, fileUrl, file);

                // Request server-side encryption.
                //ObjectMetadata objectMetadata = new ObjectMetadata();
                //objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
                //request.setMetadata(objectMetadata);

                //delete existing object
                s3client.deleteObject(bucketName, bucketFolder + "/" + doc.getModifiedFileName());
                // Upload the object and check its encryption status.
                PutObjectResult putResult = s3client.putObject(request);
                file.delete();
                flag = true;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return flag;
	}

    public boolean docusignEventTriggered(Map<String,Object> jsonBody){
        boolean flag = false;
        try{
            String envelopeId = (String)jsonBody.get("envelopeId");
            System.out.println(envelopeId);
            List<Map<String,Object>> docList = (List<Map<String,Object>>)jsonBody.get("envelopeDocuments");
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
		    AmazonS3 s3client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withRegion(region).build();

            List<MstDocument> mstDocuments = mstDocumentRepo.findAllByEnvelopeId(envelopeId);
            Map<String, Long> docMap = mstDocuments.stream().collect(Collectors.toMap(MstDocument::getDocumentId, MstDocument::getId));
            String strModFileName = null,fileUrl=null;
            MstDocument mstDocument = null;
            for(Map<String,Object> map : docList){
                String documentId = (String)map.get("documentId");
                if(docMap.get(documentId) != null){
                    mstDocument = mstDocumentRepo.getById(docMap.get(documentId));
                    if(s3client.doesObjectExist(bucketName, bucketFolder + "/" + mstDocument.getModifiedFileName())){
                        byte[] results = Base64.getDecoder().decode((String)map.get("PDFBytes"));
                        
                        strModFileName = docMap.get(documentId)+".pdf";
                        File file = new File(strModFileName);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(results);
                        fos.close();
                        
                        fileUrl = bucketFolder + "/" + strModFileName;
                        PutObjectRequest request = new PutObjectRequest(bucketName, fileUrl, file);

                        //delete existing object
                        s3client.deleteObject(bucketName, bucketFolder + "/" + mstDocument.getModifiedFileName());
                        
                        // Upload the object and check its encryption status.
                        PutObjectResult putResult = s3client.putObject(request);
                        
                        mstDocument.setModifiedFileName(strModFileName);
                        mstDocument.setModifiedFilePath(fileUrl);
                        mstDocument.setIsGenerated("Y");
                        mstDocumentRepo.save(mstDocument);
                        file.delete();
                        flag = true;
                    }
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return flag;
    }
}
