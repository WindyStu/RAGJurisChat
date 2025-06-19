package org.bigdata.rag_legal.controller;

import org.bigdata.rag_legal.entity.Reference;
import org.bigdata.rag_legal.service.LegalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
public class LegalController {

    @Autowired
    private  LegalService legalService;

    @PostMapping("/ask")
    public Reference askQuestion(@RequestBody String question) throws Exception {
        // 调用向量数据库查询服务

        return legalService.getAnswerFromVectorDB(question);
    }

}
