package org.bigdata.rag_legal.utils;

import org.bigdata.rag_legal.utils.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;


public class InsertLegalKnowledgeBase {
    public static void main(String[] args) throws Exception {
        // 步骤 3：创建集合
        int vectorDim = 1024;
        MilvusService.createCollectionIfNotExists("law_articles", vectorDim);

        // 步骤 2：创建索引
        String vectorField = "embedding";
        MilvusService.createBasicIndex("law_articles", vectorField);

        // 步骤 3：读取本地法律文档并向量化
        String folderPath = "D:\\data\\宪法\\test";
        List<String> legalTexts = readDocxFiles(folderPath);

        // 对文本进行分块处理（根据需要进行调整）
        List<String> chunks = chunkLegalTexts(legalTexts);

        final int MAX_TEXT_LENGTH = 512;
        List<String> processedChunks = new ArrayList<>();

        for (String chunk : chunks) {
            System.out.println(chunk.length());
            if (chunk.length() <= MAX_TEXT_LENGTH) {
                processedChunks.add(chunk);
            } else {
                List<String> segments = splitText(chunk, MAX_TEXT_LENGTH);
                processedChunks.addAll(segments);
            }
        }
        System.out.println(processedChunks);
        System.out.println("处理前文本块数量: " + chunks.size());
        System.out.println("处理后文本块数量: " + processedChunks.size());


        List<List<Float>> vectors = EmbeddingClient.embedInBatches(processedChunks);

        // 步骤 6：插入数据
        MilvusService.insertAndPrepare("law_articles", processedChunks, vectors);

        // 关闭连接
        MilvusService.close();
    }

    private static List<String> readDocxFiles(String folderPath) throws Exception {
        List<String> contents = new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".docx"));

        if (files != null) {
            for (File file : files) {
                try (FileInputStream fis = new FileInputStream(file);
                     XWPFDocument document = new XWPFDocument(fis)) {

                    StringBuilder content = new StringBuilder();
                    for (XWPFParagraph para : document.getParagraphs()) {
                        String text = para.getText().trim();
                        if (!text.isEmpty()) {
                            content.append(text).append("\n");
                        }
                    }
                    contents.add(content.toString());
                }
            }
        }
        return contents;
    }

    private static List<String> chunkLegalTexts(List<String> texts) {
        List<String> chunks = new ArrayList<>();

        for (String text : texts) {
            // 按章节分割
            String[] chapters = text.split("第[一二三四五六七八九十]+章");
            for (String chapter : chapters) {
                if (chapter.trim().isEmpty()) continue;

                // 按条款分割
                String[] articles = chapter.split("第[零一二三四五六七八九十百]+条");
                for (int i = 0; i < articles.length; i++) {
                    if (articles[i].trim().isEmpty()) continue;

                    // 构建完整条款文本
                    String chunk;
                    if (i == 0) {
                        // 章节开头部分
                        chunk = chapter.split("第[零一二三四五六七八九十百]+条")[0].trim();
                    } else {
                        // 正常条款
                        chunk = "第" + ChineseNumberUtil.toChineseNumber(i) + "条 " + articles[i].trim();
                    }

                    // 确保分块有意义且不过长
                    if (!chunk.isEmpty() && chunk.length() < 1000) {
                        chunks.add(chunk);
                    } else if (chunk.length() >= 1000) {
                        // 对超长条款进行二次分割
                        splitLongArticle(chunk, chunks);
                    }
                }
            }
        }
        return chunks;
    }

    private static void splitLongArticle(String article, List<String> chunks) {
        String[] sentences = article.split("[；。]");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() < 800) {
                currentChunk.append(sentence).append("。");
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                currentChunk.append(sentence).append("。");
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
    }

    private static List<String> splitText(String text, int maxLength) {
        List<String> segments = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());

            // 尝试在句子或段落边界处分割
            if (end < text.length()) {
                // 优先在句号、问号、感叹号后分割
                int lastPunctuation = Math.max(
                        text.lastIndexOf('。', end),
                        Math.max(text.lastIndexOf('?', end), text.lastIndexOf('，', end))
                );

                // 其次在段落标记或换行处分段
                if (lastPunctuation < start) {
                    lastPunctuation = text.lastIndexOf('\n', end);
                }

                // 如果找到合适的分割点，则调整end位置
                if (lastPunctuation > start) {
                    end = lastPunctuation + 1; // 包含标点符号
                }
            }

            segments.add(text.substring(start, end));
            start = end;
        }

        return segments;
    }

}

class ChineseNumberUtil {
    private static final String[] CHINESE_NUMBERS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

    public static String toChineseNumber(int num) {
        if (num <= 10) {
            return CHINESE_NUMBERS[num];
        } else if (num < 20) {
            return "十" + CHINESE_NUMBERS[num - 10];
        } else if (num < 100) {
            return CHINESE_NUMBERS[num / 10] + "十" + (num % 10 == 0 ? "" : CHINESE_NUMBERS[num % 10]);
        } else {
            return String.valueOf(num);
        }
    }
}


