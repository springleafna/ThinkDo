package com.springleaf.thinkdo.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.springleaf.thinkdo.config.AIModelProperties;
import com.springleaf.thinkdo.enums.ModelCapability;
import com.springleaf.thinkdo.enums.ModelProvider;
import com.springleaf.thinkdo.http.HttpMediaTypes;
import com.springleaf.thinkdo.http.ModelClientErrorType;
import com.springleaf.thinkdo.http.ModelClientException;
import com.springleaf.thinkdo.http.ModelUrlResolver;
import com.springleaf.thinkdo.model.ModelTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiliconFlowChatClient implements ChatClient {

    private final OkHttpClient httpClient;

    private final Gson gson = new Gson();

    @Override
    public String provider() {
        return ModelProvider.SILICON_FLOW.getId();
    }

    @Override
    public String chat(ChatRequest request, ModelTarget target) {
        AIModelProperties.ProviderConfig provider = requireProvider(target);
        JsonObject reqBody = buildRequestBody(request, target, false);
        Request requestHttp = new Request.Builder()
                .url(resolveUrl(provider, target))
                .post(RequestBody.create(reqBody.toString(), HttpMediaTypes.JSON))
                .addHeader("Content-Type", HttpMediaTypes.JSON_UTF8_HEADER)
                .addHeader("Authorization", "Bearer " + provider.getApiKey())
                .build();

        JsonObject respJson;
        try (Response response = httpClient.newCall(requestHttp).execute()) {
            if (!response.isSuccessful()) {
                String body = readBody(response.body());
                log.warn("SiliconFlow 同步请求失败: status={}, body={}", response.code(), body);
                throw new ModelClientException(
                        "SiliconFlow 同步请求失败: HTTP " + response.code(),
                        classifyStatus(response.code()),
                        response.code()
                );
            }
            respJson = parseJsonBody(response.body());
        } catch (IOException e) {
            throw new ModelClientException("SiliconFlow 同步请求失败: " + e.getMessage(),
                    ModelClientErrorType.NETWORK_ERROR, null, e);
        }

        return extractChatContent(respJson);
    }

    private JsonObject buildRequestBody(ChatRequest request, ModelTarget target, boolean stream) {
        JsonObject reqBody = new JsonObject();
        reqBody.addProperty("model", requireModel(target));
        if (stream) {
            reqBody.addProperty("stream", true);
        }

        JsonArray messages = buildMessages(request);
        reqBody.add("messages", messages);

        if (request.getTemperature() != null) {
            reqBody.addProperty("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            reqBody.addProperty("top_p", request.getTopP());
        }
        if (request.getTopK() != null) {
            reqBody.addProperty("top_k", request.getTopK());
        }
        if (request.getMaxTokens() != null) {
            reqBody.addProperty("max_tokens", request.getMaxTokens());
        }
        if (request.getThinking() != null && request.getThinking()) {
            reqBody.addProperty("enable_thinking", true);
        }
        return reqBody;
    }

    private JsonArray buildMessages(ChatRequest request) {
        JsonArray arr = new JsonArray();

        List<ChatMessage> messages = request.getMessages();
        if (!CollectionUtils.isEmpty(messages)) {
            for (ChatMessage m : messages) {
                JsonObject msg = new JsonObject();
                msg.addProperty("role", toOpenAiRole(m.getRole()));
                msg.addProperty("content", m.getContent());
                arr.add(msg);
            }
        }

        return arr;
    }

    private String toOpenAiRole(ChatMessage.Role role) {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
        };
    }

    private AIModelProperties.ProviderConfig requireProvider(ModelTarget target) {
        if (target == null || target.provider() == null) {
            throw new IllegalStateException("SiliconFlow 提供商配置缺失");
        }
        if (target.provider().getApiKey() == null || target.provider().getApiKey().isBlank()) {
            throw new IllegalStateException("SiliconFlow API密钥缺失");
        }
        return target.provider();
    }

    private String requireModel(ModelTarget target) {
        if (target == null || target.candidate() == null || target.candidate().getModel() == null) {
            throw new IllegalStateException("SiliconFlow 模型名称缺失");
        }
        return target.candidate().getModel();
    }

    private JsonObject parseJsonBody(ResponseBody body) throws IOException {
        if (body == null) {
            throw new ModelClientException("SiliconFlow 响应为空", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        String content = body.string();
        return gson.fromJson(content, JsonObject.class);
    }

    private String readBody(ResponseBody body) throws IOException {
        if (body == null) {
            return "";
        }
        return new String(body.bytes(), StandardCharsets.UTF_8);
    }

    private String resolveUrl(AIModelProperties.ProviderConfig provider, ModelTarget target) {
        return ModelUrlResolver.resolveUrl(provider, target.candidate(), ModelCapability.CHAT);
    }

    private String extractChatContent(JsonObject respJson) {
        if (respJson == null || !respJson.has("choices")) {
            throw new ModelClientException("SiliconFlow 响应缺少 choices", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        JsonArray choices = respJson.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new ModelClientException("SiliconFlow 响应 choices 为空", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        JsonObject choice0 = choices.get(0).getAsJsonObject();
        if (choice0 == null || !choice0.has("message")) {
            throw new ModelClientException("SiliconFlow 响应缺少 message", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        JsonObject message = choice0.getAsJsonObject("message");
        if (message == null || !message.has("content") || message.get("content").isJsonNull()) {
            throw new ModelClientException("SiliconFlow 响应缺少 content", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        return message.get("content").getAsString();
    }

    private ModelClientErrorType classifyStatus(int status) {
        if (status == 401 || status == 403) {
            return ModelClientErrorType.UNAUTHORIZED;
        }
        if (status == 429) {
            return ModelClientErrorType.RATE_LIMITED;
        }
        if (status >= 500) {
            return ModelClientErrorType.SERVER_ERROR;
        }
        return ModelClientErrorType.CLIENT_ERROR;
    }
}
