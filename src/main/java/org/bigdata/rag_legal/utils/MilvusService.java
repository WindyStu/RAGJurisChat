package org.bigdata.rag_legal.utils;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @describe: 封装了 Milvus 的基本操作，简化使用
 * @Author JasonZhang
 * @Date 2025/4/14
 */
public class MilvusService {
    private static MilvusServiceClient client;

    /**
     * 静态初始化块确保 client 在类加载时初始化
     */
    static {
        client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("localhost")
                        .withPort(19530)
                        .build()
        );
        System.out.println("🟢 Milvus 客户端已初始化");
    }

    /**
     * @param collectionName 集合名称
     * @param texts          文本数据列表
     * @param vectors        向量数据列表
     *                       插入数据（基础版本，不自动刷新和加载集合）
     */
    public static void insert(String collectionName, List<String> texts, List<List<Float>> vectors) {
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(Arrays.asList(
                        new InsertParam.Field("embedding", vectors),
                        new InsertParam.Field("text", texts)
                ))
                .build();

        R<MutationResult> insertResult = client.insert(insertParam);
        System.out.println("✅ 插入数据行数: " + insertResult.getData().getInsertCnt());
    }

    /**
     * @param collectionName 集合名称
     * @param texts          文本数据列表
     * @param vectors        向量数据列表
     *                       插入数据并刷新 + 加载集合（推荐使用）
     */
    public static long insertAndPrepare(String collectionName, List<String> texts, List<List<Float>> vectors) {
        System.out.println("📥 正在插入数据到集合: " + collectionName + "，共 " + texts.size() + " 条");

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(Arrays.asList(
                        new InsertParam.Field("embedding", vectors),
                        new InsertParam.Field("text", texts)
                ))
                .build();

        R<MutationResult> insertResult = client.insert(insertParam);
        long insertCount = insertResult.getData().getInsertCnt();
        System.out.println("✅ 插入成功: " + insertCount + " 条数据");

        // 刷新集合
        FlushParam flushParam = FlushParam.newBuilder()
                .withCollectionNames(Collections.singletonList(collectionName))
                .build();
        client.flush(flushParam);
        System.out.println("🔄 集合已刷新: " + collectionName);

        // 加载集合
        LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        client.loadCollection(loadParam);
        System.out.println("📦 集合已加载进内存: " + collectionName);

        return insertCount;
    }

    /**
     * @param queryVector    查询向量
     * @param collectionName 集合名称
     *                       检索向量，返回 Top K 匹配结果
     */
    public static List<VectorSearchResult> search(String collectionName, List<Float> queryVector) {
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.L2)
                .withOutFields(Arrays.asList("id", "text"))
                .withTopK(3)
                .withVectors(Collections.singletonList(queryVector))
                .withVectorFieldName("embedding")
                .withParams("{\"nprobe\": 10}")
                .build();

        R<SearchResults> search = client.search(searchParam);
        SearchResultsWrapper wrapper = new SearchResultsWrapper(search.getData().getResults());

        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        List<String> texts = (List<String>) wrapper.getFieldWrapper("text").getFieldData();

        List<VectorSearchResult> results = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            long id = scores.get(i).getLongID();
            float score = scores.get(i).getScore();
            String text = texts.get(i);
            results.add(new VectorSearchResult(id, score, text));
        }

        return results;
    }

    /**
     * @param dim            向量维度
     * @param collectionName 集合名称
     *                       创建集合（如不存在）
     */
    public static void createCollectionIfNotExists(String collectionName, int dim) {
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        R<Boolean> hasCollection = client.hasCollection(hasCollectionParam);
//        if (Boolean.TRUE.equals(hasCollection.getData())) {
//            System.out.println("✅ 集合已存在: " + collectionName);
//            return;
//        }
        if (Boolean.TRUE.equals(hasCollection.getData())) {
            // 集合存在，删除它
            DropCollectionParam dropCollectionParam = DropCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();
            client.dropCollection(dropCollectionParam);
            System.out.println("⚠️ 已删除旧集合: " + collectionName);
        }

        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDescription("主键ID")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName("embedding")
                .withDescription("向量字段")
                .withDataType(DataType.FloatVector)
                .withDimension(dim)
                .build();

        FieldType textField = FieldType.newBuilder()
                .withName("text")
                .withDescription("原始文本")
                .withDataType(DataType.VarChar)
                .withMaxLength(2000)
                .build();

        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("自动创建集合")
                .withShardsNum(2)
                .addFieldType(idField)
                .addFieldType(vectorField)
                .addFieldType(textField)
                .build();

        client.createCollection(createCollectionParam);
        System.out.println("✅ 成功创建集合: " + collectionName);
    }

    /**
     * @param vectorField    向量字段名称
     * @param collectionName 集合名称
     *                       为向量字段创建基础索引
     */
    public static void createBasicIndex(String collectionName, String vectorField) {
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(vectorField)
                .withIndexName("basic_index")
                .withIndexType(IndexType.FLAT)
                .withMetricType(MetricType.L2)
                .withExtraParam("{}")
                .build();

        client.createIndex(indexParam);
        System.out.println("✅ 向量字段创建索引完成: " + vectorField);
    }

    /**
     * @param collectionName 集合名称
     *                       加载集合到内存
     */
    public static void loadCollection(String collectionName) {
        LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        client.loadCollection(loadParam);
        System.out.println("✅ 集合已加载进内存: " + collectionName);
    }



    /**
     * 关闭连接
     */
    public static void close() {
        client.close();
    }

    /**
     * @describe: 单一职责，生成向量
     * @Author JasonZhang
     * @Date 2025/4/14
     **/
    public static class EmbeddingClient {

        private static final Logger logger = LoggerFactory.getLogger(EmbeddingClient.class);

        private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
        private static final String API_KEY = System.getenv("DASHSCOPE_API_KEY");

        private static final HttpClient httpClient = HttpClient.newHttpClient();

        public EmbeddingClient() {
            if (API_KEY == null || API_KEY.isEmpty()) {
                throw new RuntimeException("❌ 未检测到 DASHSCOPE_API_KEY，请设置环境变量");
            }
            logger.info("✅ 成功获取 API_KEY，长度: {}", API_KEY.length());
        }

        /**
         * 单条文本向量化
         */
        public static List<Float> embed(String text) throws Exception {
            List<List<Float>> results = embedInBatches(Collections.singletonList(text));
            return results.isEmpty() ? Collections.emptyList() : results.get(0);
        }

        /**
         * 多条文本向量化
         */
    //    public static List<List<Float>> embed(List<String> texts) throws Exception {
    //        logger.info("📨 准备向量化文本，共计: {} 条", texts.size());
    //
    //        JSONObject requestBody = new JSONObject();
    //        requestBody.put("model", "text-embedding-v3");
    //        requestBody.put("input", new JSONArray(texts));
    //        requestBody.put("dimensions", 1024);
    //        requestBody.put("encoding_format", "float");
    //
    //        HttpRequest request = HttpRequest.newBuilder()
    //                .uri(URI.create(API_URL))
    //                .header("Authorization", "Bearer " + API_KEY)
    //                .header("Content-Type", "application/json")
    //                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
    //                .build();
    //
    //        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    //
    //        if (response.statusCode() != 200) {
    //            logger.error("❌ 向量化请求失败，状态码: {}, 响应体: {}", response.statusCode(), response.body());
    //            throw new RuntimeException("向量请求失败: " + response.statusCode());
    //        }
    //
    //        String responseBody = response.body();
    //        logger.info("✅ 请求成功，响应预览: {}", responseBody.length() > 60 ? responseBody.substring(0, 60) + "..." : responseBody);
    //
    //        return extractEmbeddings(responseBody);
    //    }
        public static List<List<Float>> embedInBatches(List<String> texts) throws Exception {
            logger.info("📨 准备向量化文本，共计: {} 条", texts.size());
            final int BATCH_SIZE = 10;  // 每次最多处理10条文本
            List<List<Float>> allEmbeddings = new ArrayList<>();

            // 拆分批次并循环处理
            for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, texts.size());
                List<String> batchTexts = texts.subList(i, end);
                logger.info("处理批次 {}: 处理 {} 条文本", (i/BATCH_SIZE + 1), batchTexts.size());

                // 调用原始向量化方法处理当前批次
                List<List<Float>> batchEmbeddings = embedBatch(batchTexts);
                allEmbeddings.addAll(batchEmbeddings);
            }

            logger.info("✅ 向量化处理完成，共生成 {} 个向量", allEmbeddings.size());
            return allEmbeddings;
        }

        // 单次向量化处理（限制10条以内）
        private static List<List<Float>> embedBatch(List<String> batchTexts) throws Exception {
            if (batchTexts.size() > 10) {
                throw new IllegalArgumentException("批次大小不能超过10，当前传入: " + batchTexts.size());
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "text-embedding-v3");
            requestBody.put("input", new JSONArray(batchTexts));
            requestBody.put("dimensions", 1024);
            requestBody.put("encoding_format", "float");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("❌ 向量化批次请求失败，状态码: {}, 响应体: {}",
                        response.statusCode(), response.body());
                throw new RuntimeException("向量批次请求失败: " + response.statusCode());
            }

            String responseBody = response.body();
            logger.info("✅ 批次请求成功，响应预览: {}",
                    responseBody.length() > 60 ? responseBody.substring(0, 60) + "..." : responseBody);

            return extractEmbeddings(responseBody);
        }

        /**
         * 从响应中提取向量
         */
        private static List<List<Float>> extractEmbeddings(String json) {
            logger.info("🔍 正在提取向量...");
            List<List<Float>> vectors = new ArrayList<>();
            JSONObject jsonObject = new JSONObject(json);
            JSONArray dataArray = jsonObject.getJSONArray("data");

            for (int i = 0; i < dataArray.length(); i++) {
                JSONArray vectorArray = dataArray.getJSONObject(i).getJSONArray("embedding");
                List<Float> vector = new ArrayList<>();
                for (int j = 0; j < vectorArray.length(); j++) {
                    vector.add((float) vectorArray.getDouble(j));
                }
                vectors.add(vector);
            }

            logger.info("📦 提取完成，共计向量条数: {}", vectors.size());
            return vectors;
        }

        /**
         * 示例主方法（可删除）
         */
        public static void main(String[] args) throws Exception {
            List<String> inputs = Arrays.asList(
                    "中华人民共和国刑法（第232条）：故意杀人的，处死刑、无期徒刑或者十年以上有期徒刑。",
                    "中华人民共和国民法典（第123条）：侵权责任法是调整侵权行为所产生的法律关系的法律。",
                    "中华人民共和国公司法（第45条）：股东会是公司的最高权力机构。"
            );
            List<List<Float>> vectors = embedInBatches(inputs);
            System.out.println("✔ 向量维度：" + vectors.get(0).size());
        }
    }
}
