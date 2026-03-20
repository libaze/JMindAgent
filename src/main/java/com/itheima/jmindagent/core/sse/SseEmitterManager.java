package com.itheima.jmindagent.core.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 发射器管理器
 * 管理所有用户的 SSE 连接
 */
@Slf4j
@Component
public class SseEmitterManager {

    /**
     * 存储所有 SSE 发射器，key 为 sessionId
     */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建并注册新的 SSE 发射器
     * @param sessionId 会话ID
     * @param timeout 超时时间（毫秒）
     * @return SSE 发射器
     */
    public SseEmitter createEmitter(String sessionId, Long timeout) {
        // 如果已存在，先关闭旧的连接
        closeEmitter(sessionId);

        SseEmitter emitter = new SseEmitter(timeout);
        emitterMap.put(sessionId, emitter);

        // 注册完成回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成：sessionId={}", sessionId);
            emitterMap.remove(sessionId);
        });

        // 注册超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时：sessionId={}, timeout={}ms", sessionId, timeout);
            emitterMap.remove(sessionId);
            try {
                emitter.send(SseEmitter.event()
                        .name("timeout")
                        .data("{\"msg\":\"连接超时\"}")
                        .build());
            } catch (IOException e) {
                log.error("发送超时消息失败", e);
            }
        });

        // 注册错误回调
        emitter.onError(throwable -> {
            log.error("SSE 连接错误：sessionId={}, error={}", sessionId, throwable.getMessage());
            emitterMap.remove(sessionId);
        });

        log.info("创建 SSE 连接：sessionId={}", sessionId);
        return emitter;
    }

    /**
     * 获取指定会话的 SSE 发射器
     * @param sessionId 会话ID
     * @return SSE 发射器，不存在则返回 null
     */
    public SseEmitter getEmitter(String sessionId) {
        return emitterMap.get(sessionId);
    }

    /**
     * 向指定会话发送消息
     * @param sessionId 会话ID
     * @param data 消息数据
     * @throws IOException 发送失败异常
     */
    public void send(String sessionId, Object data) throws IOException {
        SseEmitter emitter = getEmitter(sessionId);
        if (emitter != null) {
            emitter.send(data);
        } else {
            throw new IOException("SSE 连接不存在：sessionId=" + sessionId);
        }
    }

    /**
     * 向指定会话发送文本消息
     * @param sessionId 会话ID
     * @param message 消息内容
     * @throws IOException 发送失败异常
     */
    public void sendText(String sessionId, String message) throws IOException {
        SseEmitter emitter = getEmitter(sessionId);
        if (emitter != null) {
            // 将消息包装成 JSON 格式
            String jsonData = "{\"data\":\"" + escapeJson(message) + "\"}";
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(jsonData)
                    .build());
        } else {
            throw new IOException("SSE 连接不存在：sessionId=" + sessionId);
        }
    }

    public void sendStatus(String sessionId, String message) throws IOException {
        SseEmitter emitter = getEmitter(sessionId);
        if (emitter != null) {
            // 将消息包装成 JSON 格式
            String jsonData = "{\"data\":\"" + escapeJson(message) + "\"}";
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(jsonData)
                    .build());
        } else {
            throw new IOException("SSE 连接不存在：sessionId=" + sessionId);
        }
    }
    /**
     * JSON 字符串转义（处理特殊字符）
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    /**
     * 发送结束标识
     * @param sessionId 会话ID
     * @throws IOException 发送失败异常
     */
    public void sendComplete(String sessionId) throws IOException {
        SseEmitter emitter = getEmitter(sessionId);
        if (emitter != null) {
            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data("[DONE]")
                    .build());
            emitter.complete();
        }
    }


    /**
     * 关闭指定会话的 SSE 连接
     * @param sessionId 会话 ID
     */
    public void closeEmitter(String sessionId) {
        SseEmitter emitter = emitterMap.remove(sessionId);
        if (emitter != null) {
            try {
                // 先完成 emitter，阻止新的消息发送
                emitter.complete();
                log.info("关闭 SSE 连接：sessionId={}", sessionId);
            } catch (Exception e) {
                log.warn("关闭 SSE 连接异常（可能已断开）：sessionId={}, error={}", sessionId, e.getMessage());
            }
        }
    }

    /**
     * 关闭所有 SSE 连接
     */
    public void closeAll() {
        emitterMap.forEach((sessionId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("关闭 SSE 连接失败：sessionId={}", sessionId);
            }
        });
        emitterMap.clear();
        log.info("关闭所有 SSE 连接");
    }
}
