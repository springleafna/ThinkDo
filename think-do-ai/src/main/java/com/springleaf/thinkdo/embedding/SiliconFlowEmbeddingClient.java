package com.springleaf.thinkdo.embedding;

import com.google.gson.Gson;
import com.springleaf.thinkdo.enums.ModelProvider;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.model.ModelTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiliconFlowEmbeddingClient implements EmbeddingClient {

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private final EmbeddingModel embeddingModel;

    // 向量化最大批处理大小，避免单次请求过大
    private static final int MAX_BATCH = 32;

    @Override
    public String provider() {
        return ModelProvider.SILICON_FLOW.getId();
    }

    @Override
    public float[] embed(String text, ModelTarget target) {
        return embedBatch(List.of(text), target).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts, ModelTarget target) {
        if (CollectionUtils.isEmpty(texts)) {
            return Collections.emptyList();
        }

        List<float[]> results = new ArrayList<>(Collections.nCopies(texts.size(), null));
        for (int i = 0, n = texts.size(); i < n; i += MAX_BATCH) {
            int end = Math.min(i + MAX_BATCH, n);
            List<String> slice = texts.subList(i, end);
            try {
                List<float[]> part = embeddingModel.embed(slice);
                for (int k = 0; k < part.size(); k++) {
                    results.set(i + k, part.get(k));
                }
            } catch (Exception e) {
                log.error("SiliconFlow embeddings 调用失败", e);
                throw new RuntimeException("调用 SiliconFlow Embedding 失败: " + e.getMessage(), e);
            }
        }

        for (int i = 0; i < results.size(); i++) {
            if (results.get(i) == null) {
                throw new BusinessException("Embedding 结果缺失，index=" + i);
            }
        }
        return results;
    }
}
