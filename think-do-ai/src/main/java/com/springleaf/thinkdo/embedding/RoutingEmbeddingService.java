package com.springleaf.thinkdo.embedding;

import com.springleaf.thinkdo.enums.ModelCapability;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.model.ModelHealthStore;
import com.springleaf.thinkdo.model.ModelRoutingExecutor;
import com.springleaf.thinkdo.model.ModelSelector;
import com.springleaf.thinkdo.model.ModelTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 向量化服务路由实现
 * 根据配置的模型路由到对应的 EmbeddingClient
 */
@Slf4j
@Service
public class RoutingEmbeddingService implements EmbeddingService {

    private final ModelSelector selector;
    private final ModelHealthStore healthStore;
    private final ModelRoutingExecutor executor;
    private final Map<String, EmbeddingClient> clientsByProvider;

    public RoutingEmbeddingService(
            ModelSelector selector,
            ModelHealthStore healthStore,
            ModelRoutingExecutor executor,
            List<EmbeddingClient> clients) {
        this.selector = selector;
        this.healthStore = healthStore;
        this.executor = executor;
        this.clientsByProvider = clients.stream()
                .collect(Collectors.toMap(EmbeddingClient::provider, Function.identity()));
    }

    @Override
    public List<Float> embed(String text) {
        return executor.executeWithFallback(
                ModelCapability.EMBEDDING,
                selector.selectEmbeddingCandidates(),
                target -> clientsByProvider.get(target.candidate().getProvider()),
                (client, target) -> client.embed(text, target)
        );
    }

    @Override
    public List<Float> embed(String text, String modelId) {
        ModelTarget target = resolveTarget(modelId);
        EmbeddingClient client = resolveClient(target);
        if (!healthStore.allowCall(target.id())) {
            throw new BusinessException("Embedding 模型暂不可用: " + target.id());
        }
        try {
            List<Float> vector = client.embed(text, target);
            healthStore.markSuccess(target.id());
            return vector;
        } catch (Exception e) {
            healthStore.markFailure(target.id());
            throw new BusinessException("Embedding 模型调用失败: " + target.id(), e);
        }
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        return executor.executeWithFallback(
                ModelCapability.EMBEDDING,
                selector.selectEmbeddingCandidates(),
                target -> clientsByProvider.get(target.candidate().getProvider()),
                (client, target) -> client.embedBatch(texts, target)
        );
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts, String modelId) {
        ModelTarget target = resolveTarget(modelId);
        EmbeddingClient client = resolveClient(target);
        if (!healthStore.allowCall(target.id())) {
            throw new BusinessException("Embedding 模型暂不可用: " + target.id());
        }
        try {
            List<List<Float>> vectors = client.embedBatch(texts, target);
            healthStore.markSuccess(target.id());
            return vectors;
        } catch (Exception e) {
            healthStore.markFailure(target.id());
            throw new BusinessException("Embedding 模型调用失败: " + target.id(), e);
        }
    }

    @Override
    public int dimension() {
        ModelTarget target = selector.selectDefaultEmbedding();
        if (target == null || target.candidate().getDimension() == null) {
            return 0;
        }
        return target.candidate().getDimension();
    }

    private ModelTarget resolveTarget(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            throw new BusinessException("Embedding 模型ID不能为空");
        }
        return selector.selectEmbeddingCandidates().stream()
                .filter(target -> modelId.equals(target.id()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Embedding 模型不可用: " + modelId));
    }

    private EmbeddingClient resolveClient(ModelTarget target) {
        EmbeddingClient client = clientsByProvider.get(target.candidate().getProvider());
        if (client == null) {
            throw new BusinessException("Embedding 模型客户端不存在: " + target.candidate().getProvider());
        }
        return client;
    }
}
