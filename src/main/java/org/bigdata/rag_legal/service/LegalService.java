package org.bigdata.rag_legal.service;

import org.bigdata.rag_legal.entity.Reference;
import org.bigdata.rag_legal.utils.MilvusService;
import org.bigdata.rag_legal.utils.QwenClient;
import org.bigdata.rag_legal.utils.VectorSearchResult;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LegalService {
    public Reference getAnswerFromVectorDB(String query) throws Exception {
        List<Float> queryVector = MilvusService.EmbeddingClient.embed(query);
        // 向量检索
        List<VectorSearchResult> results = MilvusService.search("law_articles", queryVector);

        // 获取检索结果文本内容
        List<String> topTexts = results.stream()
                .map(VectorSearchResult::text)
                .collect(Collectors.toList());

        // 构建上下文：法律条文拼接
        String context = String.join("\n", topTexts);

        System.out.println(context);

        // 构建系统提示 + 提问内容
        String systemPrompt = "请根据以下宪法内容回答用户问题，并适当结合一些中国相关法律信息最后总结给予建议进行回答：\n" + context;

        // 调用大模型生成回答
        String reply = QwenClient.chat(systemPrompt, query);

        // 打印结果
        System.out.println("🤖 AI 回答：\n" + reply);

        MilvusService.close();

        return new Reference(reply);
    }

}
