package com.batch.docusign.controller;

import java.util.Map;

import com.batch.docusign.service.MstDocumentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/docusign")
public class DocusignController {
    
    @Autowired
    MstDocumentService docuSignService;

    @CrossOrigin
    @PostMapping(value = "/docusignEvent", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> docusignEvent(@RequestBody Map<String,Object> jsonBody){
        boolean flag = docuSignService.docusignEventTriggered(jsonBody);
        if(!flag) return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
