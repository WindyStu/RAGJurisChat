package org.bigdata.rag_legal.utils;

/**
 * @describe: 向量搜索结果封装类
 * @Author JasonZhang
 * @Date 2025/4/14
**/
public record VectorSearchResult(long id, float score, String text) {}
