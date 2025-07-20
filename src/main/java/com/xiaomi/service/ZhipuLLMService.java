package com.xiaomi.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ZhipuLLMService {
    @Value("${llm.api.key}")
    private String apiKey;

    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient;

    public ZhipuLLMService() {
        // 为了兼容您本地的旧版JDK环境，继续使用不安全的OkHttpClient
        this.httpClient = createInsecureOkHttpClient();
    }

    public void streamChatResponse(String question, SseEmitter emitter, StringBuilder fullAnswer, List<Map<String, String>> contextMessages, Runnable onCompleteCallback) {
        executor.submit(() -> {
            try {
                // 1. 生成智谱AI需要的JWT Token
                String token = generateToken(apiKey, 3600 * 1000); // Token有效期1小时

                // 2. 构建包含上下文的请求体
                List<Map<String, String>> messages = new ArrayList<>(contextMessages != null ? contextMessages : Collections.emptyList());
                Map<String, String> currentMessage = new HashMap<>();
                currentMessage.put("role", "user");
                currentMessage.put("content", question);
                messages.add(currentMessage);

                Map<String, Object> payload = new HashMap<>();
                payload.put("model", "glm-4"); // 使用智谱AI的GLM-4模型
                payload.put("messages", messages);
                payload.put("stream", true);

                String jsonPayload = objectMapper.writeValueAsString(payload);
                RequestBody body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), jsonPayload);

                // 3. 创建 Request 对象
                Request request = new Request.Builder()
                        .url(API_URL)
                        .header("Authorization", "Bearer " + token)
                        .post(body)
                        .build();

                // 4. 执行请求并处理流式响应
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("请求失败: " + response.code() + " " + (response.body() != null ? response.body().string() : ""));
                    }

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data:")) {
                                String jsonData = line.substring(5).trim();
                                String content = parseContentFromSseData(jsonData);
                                if (content != null) {
                                    emitter.send(SseEmitter.event().data(content));
                                    fullAnswer.append(content);
                                }
                            }
                        }
                    }
                }

                if (onCompleteCallback != null) {
                    onCompleteCallback.run();
                }
                emitter.complete();
            } catch (Exception e) {
                System.err.println("调用智谱AI API时出错: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    // 辅助方法：从智谱AI返回的SSE JSON中解析出内容
//    private String parseContentFromSseData(String jsonData) throws IOException {
//        Map<String, Object> dataMap = objectMapper.readValue(jsonData, Map.class);
//        List<?> choices = (List<?>) dataMap.get("choices");
//        if (choices != null && !choices.isEmpty()) {
//            Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
//            Map<String, Object> delta = (Map<String, Object>) firstChoice.get("delta");
//            if (delta != null && delta.containsKey("content")) {
//                String content = (String) delta.get("content");
//                // 智谱AI返回的内容是带引号的字符串，我们需要去掉首尾的引号
//                if (content.startsWith("\"") && content.endsWith("\"")) {
//                    return content.substring(1, content.length() - 1);
//                }
//                return content;
//            }
//        }
//        return null;
//    }
    // 在ZhipuLLMService中修改parseContentFromSseData方法
    private String parseContentFromSseData(String jsonData) throws IOException {
        // 处理结束标记
        if ("[DONE]".equals(jsonData)) {
            return null;
        }

        Map<String, Object> dataMap = objectMapper.readValue(jsonData, Map.class);
        List<?> choices = (List<?>) dataMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
            Map<String, Object> delta = (Map<String, Object>) firstChoice.get("delta");
            if (delta != null && delta.containsKey("content")) {
                String content = (String) delta.get("content");
                // 处理可能的转义字符
                return content.replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
        }
        return null;
    }
    // 辅助方法：生成JWT Token
    private String generateToken(String apiKey, long expMillis) {
        String[] parts = apiKey.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("无效的API Key格式");
        }
        String id = parts[0];
        String secret = parts[1];

        Map<String, Object> headerClaims = new HashMap<>();
        headerClaims.put("alg", "HS256");
        headerClaims.put("sign_type", "SIGN");

        Map<String, Object> payloadClaims = new HashMap<>();
        payloadClaims.put("api_key", id);
        payloadClaims.put("exp", System.currentTimeMillis() + expMillis);
        payloadClaims.put("timestamp", System.currentTimeMillis());

        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withHeader(headerClaims)
                .withPayload(payloadClaims)
                .sign(algorithm);
    }

    // 创建不安全的 OkHttpClient 的方法 (为兼容您的本地环境而保留)
    private static OkHttpClient createInsecureOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            builder.connectTimeout(30, TimeUnit.SECONDS);
            builder.readTimeout(10, TimeUnit.MINUTES);
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
