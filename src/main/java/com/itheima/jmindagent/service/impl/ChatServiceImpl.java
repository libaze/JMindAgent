package com.itheima.jmindagent.service.impl;

import com.itheima.jmindagent.core.sse.SseEmitterManager;
import com.itheima.jmindagent.entity.TChatSession;
import com.itheima.jmindagent.entity.TRagChunkVector;
import com.itheima.jmindagent.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

import static org.springframework.jdbc.datasource.init.ScriptStatementFailedException.buildErrorMessage;


/**
 * 聊天服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

    private final ChatClient chatClient;
    private final SseEmitterManager sseEmitterManager;
    private final ITChatHistoryService chatHistoryService;
    private final ITChatSessionService chatSessionService;
    private final ITCustomAgentService customAgentService;
    private final ITRagKnowledgeService ragKnowledgeService;
    private final ITRagChunkVectorService ragChunkVectorService;
    private final ITMcpToolService mcpToolService;

    private static final Long SSE_TIMEOUT = 60L * 1000L; // 60 秒超时
    private static final Double MIN_SIMILARITY = 0.1;
    private static final Integer RAG_TOP_K = 5;

    @Override
    public SseEmitter unifiedChatStream(Long userId, String sessionId, String content, String agentId, String kbId) {
        // 关键修改：检查会话是否存在，不存在则先创建
        TChatSession session = chatSessionService.getById(sessionId);
        if (session == null) {
            log.info("会话不存在，自动创建新会话：sessionId={}, userId={}", sessionId, userId);
            session = createNewSession(sessionId, userId, agentId, kbId);
        }

        SseEmitter emitter = sseEmitterManager.createEmitter(sessionId, SSE_TIMEOUT);

        try {
            updateSessionTime(sessionId, agentId, kbId);

            if (agentId != null && !agentId.isBlank()) {
                handleAgentChat(emitter, sessionId, userId, content, agentId, kbId);
            } else {
                handleNormalChat(emitter, sessionId, userId, content, kbId);
            }

        }  catch (Exception e) {
            log.error("创建聊天流失败：userId={}, sessionId={}, agentId={}, kbId={}, error={}",
                    userId, sessionId, agentId, kbId, e.getMessage());
            try {
                chatHistoryService.saveUserMessage(sessionId, userId, content);
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"msg\":\"创建聊天失败：" + e.getMessage() + "\"}")
                        .build());
                sseEmitterManager.closeEmitter(sessionId);
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
        }

        return emitter;
    }

    /**
     * 创建新会话
     */
    private TChatSession createNewSession(String sessionId, Long userId, String agentId, String kbId) {
        TChatSession session = new TChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setSessionName("新对话");
        session.setAgentId(agentId);
        session.setKbId(kbId);
        session.setCreateTime(java.time.LocalDateTime.now());
        session.setUpdateTime(java.time.LocalDateTime.now());

        chatSessionService.save(session);
        log.info("新会话创建成功：sessionId={}, userId={}, topic={}", sessionId, userId, session.getSessionName());

        return session;
    }

    private void updateSessionTime(String sessionId, String agentId, String kbId) {
        TChatSession session = chatSessionService.getById(sessionId);
        if (session != null) {
            session.setUpdateTime(java.time.LocalDateTime.now());
            if (agentId != null) {
                session.setAgentId(agentId);
            }
            if (kbId != null) {
                session.setKbId(kbId);
            }
            chatSessionService.updateById(session);
        }
    }

    private void handleNormalChat(SseEmitter emitter, String sessionId, Long userId, String content, String kbId) {
        log.info("处理普通对话（降级模式）：sessionId={}, kbId={}", sessionId, kbId);

        // 发送 RAG 检索状态
        if (kbId != null && !kbId.isBlank()) {
            sendAgentStatus(emitter, sessionId, "🔍 正在检索知识库...");
        }

        String userContent = buildRagPrompt(content, kbId);

        List<Message> historicalMessages = chatHistoryService.loadChatMemory(sessionId, userId, 20);
        log.info("加载了{}条历史消息", historicalMessages.size());

        // 发送思考状态
        sendAgentStatus(emitter, sessionId, "🤔 正在思考...");

        Flux<String> responseFlux;
        if (!historicalMessages.isEmpty()) {
            log.info("使用历史上下文模式");
            responseFlux = chatClient.prompt()
                    .user(userContent)
                    .messages(historicalMessages)
                    .stream()
                    .content();
        } else {
            log.info("无历史消息，使用简单模式");
            responseFlux = chatClient.prompt()
                    .user(userContent)
                    .stream()
                    .content();
        }

        // 传入用户原始内容用于后续保存
        subscribeToResponse(emitter, sessionId, userId, responseFlux, kbId != null ? 2 : 1, content);
    }

    private void handleAgentChat(SseEmitter emitter, String sessionId, Long userId, String content, String agentId, String kbId) {
        log.info("处理自定义 Agent  对话：sessionId={}, agentId={}, kbId={}", sessionId, agentId, kbId);

        sendAgentStatus(emitter, sessionId, "🤖 正在加载智能体...");

        try {
            var agent = customAgentService.getById(agentId);
            if (agent == null) {
                throw new IllegalArgumentException("Agent 不存在：" + agentId);
            }

            sendAgentStatus(emitter, sessionId, "✅ 智能体已加载：" + agent.getAgentName());

            List<org.springframework.ai.tool.ToolCallback> availableTools = Collections.emptyList();
            if (agent.getToolIds() != null && !agent.getToolIds().isBlank()) {
                sendAgentStatus(emitter, sessionId, "🔧 准备调用工具...");
                availableTools = mcpToolService.getAvailableTools(agent.getToolIds());
                log.info("Agent 绑定了{}个工具", availableTools.size());

                if (!availableTools.isEmpty()) {
                    sendAgentStatus(emitter, sessionId, "🛠️ 已加载 " + availableTools.size() + " 个工具");
                }
            }

            String systemPrompt = agent.getAgentPrompt() != null ? agent.getAgentPrompt() : agent.getAgentDesc();

            // RAG 检索状态
            if (kbId != null && !kbId.isBlank()) {
                sendAgentStatus(emitter, sessionId, "📚 正在检索知识库...");
            }
            String userContent = buildRagPrompt(content, kbId);

            // 告知 RAG 检索结果
            if (kbId != null && !kbId.isBlank()) {
                sendAgentStatus(emitter, sessionId, "✅ 知识库检索完成");
            }

            List<Message> historicalMessages = chatHistoryService.loadChatMemory(sessionId, userId, 20);
            log.info("加载了{}条历史消息", historicalMessages.size());

            // 关键修改：根据是否有工具选择调用方式
            if (!availableTools.isEmpty()) {
                sendAgentStatus(emitter, sessionId, "⚙️ 正在分析并调用工具...");
                log.info("使用工具增强模式，共{}个可用工具", availableTools.size());
                // 工具调用时使用非流式模式，避免冲突
                handleToolCallingWithStreaming(emitter, sessionId, userId, systemPrompt,
                        historicalMessages, userContent, availableTools, kbId != null ? 2 : 3, content);
            } else {
                sendAgentStatus(emitter, sessionId, "💬 正在生成回答...");
                log.info("无可用工具，使用普通流式模式");
                Flux<String> responseFlux;
                if (!historicalMessages.isEmpty()) {
                    responseFlux = chatClient.prompt()
                            .system(systemPrompt)
                            .messages(historicalMessages)
                            .user(userContent)
                            .stream()
                            .content();
                } else {
                    responseFlux = chatClient.prompt()
                            .system(systemPrompt)
                            .user(userContent)
                            .stream()
                            .content();
                }
                subscribeToResponse(emitter, sessionId, userId, responseFlux, kbId != null ? 2 : 3, content);
            }

        } catch (Exception e) {
            log.error("Agent 对话失败：sessionId={}, agentId={}, kbId={}, error={}", sessionId, agentId, kbId, e.getMessage());
            sendError(emitter, sessionId, "Agent 调用失败：" + e.getMessage());
        }
    }


    /**
     * 处理工具调用（非流式获取完整响应，然后流式输出给用户）
     */
    private void handleToolCallingWithStreaming(SseEmitter emitter, String sessionId, Long userId,
                                                String systemPrompt, List<Message> historicalMessages,
                                                String userContent, List<org.springframework.ai.tool.ToolCallback> availableTools,
                                                Integer msgType, String rawUserContent) {

        StringBuilder fullContent = new StringBuilder();

        try {
            // 发送工具执行状态
            sendAgentStatus(emitter, sessionId, "🔍 正在分析问题...");
            // 构建请求
            var promptBuilder = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userContent)
                    .toolCallbacks(availableTools.toArray(new org.springframework.ai.tool.ToolCallback[0]));

            if (!historicalMessages.isEmpty()) {
                promptBuilder.messages(historicalMessages);
            }

            // 使用非流式调用获取完整响应（确保工具调用正常工作）
            String completeResponse = promptBuilder.call().content();

            log.info("工具调用完成，完整响应长度：{}", completeResponse.length());

            // 发送开始回答状态
            sendAgentStatus(emitter, sessionId, "✍️ 正在整理答案...");

            // 将完整响应拆分成字符流式发送
            for (char c : completeResponse.toCharArray()) {
                try {
                    sseEmitterManager.sendText(sessionId, String.valueOf(c));
                    fullContent.append(c);
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("发送 SSE 片段失败：sessionId={}, error={}", sessionId, e.getMessage());
                    break;
                }
            }

            // 保存消息
            chatHistoryService.saveUserMessage(sessionId, userId, rawUserContent);
            chatHistoryService.saveAiMessage(sessionId, userId, fullContent.toString());

            // 发送完成信号
            sendAgentStatus(emitter, sessionId, "✅ 回答完成");
            sseEmitterManager.sendComplete(sessionId);

            log.info("工具调用响应发送完成：sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("工具调用处理失败：sessionId={}, error={}", sessionId, e.getMessage(), e);
            try {
                if (fullContent.length() > 0) {
                    chatHistoryService.saveUserMessage(sessionId, userId, rawUserContent);
                    chatHistoryService.saveAiMessage(sessionId, userId, fullContent.toString());
                }
                sendError(emitter, sessionId, "工具调用失败：" + e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
        }
    }

    private String buildRagPrompt(String content, String kbId) {
        if (kbId == null || kbId.isBlank()) {
            log.debug("未传入知识库 ID，使用原始问题");
            return content;
        }

        try {
            var knowledge = ragKnowledgeService.getById(kbId);
            if (knowledge == null) {
                log.warn("知识库不存在：kbId={}", kbId);
                return content;
            }

            // 检查知识库状态，只允许启用状态的知识库进行检索
            if (knowledge.getKbStatus() == null || knowledge.getKbStatus() != 1) {
                log.warn("知识库已禁用，无法检索：kbId={}, kbName={}, kbStatus={}",
                        kbId, knowledge.getKbName(), knowledge.getKbStatus());
                return content;
            }

            log.info("开始 RAG 检索：kbId={}, kbName={}, question={}", kbId, knowledge.getKbName(), content);

            List<TRagChunkVector> chunks = ragChunkVectorService.retrieveChunks(kbId, content, RAG_TOP_K, MIN_SIMILARITY);

            if (chunks.isEmpty()) {
                log.info("RAG 检索结果为空，使用原始问题");
                return content;
            }

            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                TRagChunkVector chunk = chunks.get(i);
                contextBuilder.append("[片段").append(i + 1).append("]\n");
                contextBuilder.append(chunk.getChunkContent()).append("\n\n");
            }

            String context = contextBuilder.toString().trim();
            log.info("RAG 检索到{}个相关片段，总字符数：{}", chunks.size(), context.length());

            String ragPrompt = "请基于以下参考资料回答问题。不要随意发挥。\n\n" +
                    "=== 参考资料 ===\n" +
                    context +
                    "=== 结束 ===\n\n" +
                    "问题：" + content;

            return ragPrompt;

        } catch (Exception e) {
            log.error("RAG 检索失败：kbId={}, error={}", kbId, e.getMessage());
            return content;
        }
    }

    private void subscribeToResponse(SseEmitter emitter, String sessionId, Long userId,
                                     Flux<String> responseFlux, Integer msgType, String userContent) {
        StringBuilder fullContent = new StringBuilder();

        // 发送开始回答状态
        try {
            sendAgentStatus(emitter, sessionId, "💬 正在组织语言...");
        } catch (Exception e) {
            log.error("发送状态失败：sessionId={}, error={}", sessionId, e.getMessage());
        }

        responseFlux.subscribe(
                chunk -> {
                    try {
                        log.debug("SSE 发送片段：sessionId={}, chunk={}", sessionId, chunk);
                        sseEmitterManager.sendText(sessionId, chunk);
                        fullContent.append(chunk);
                    } catch (Exception e) {
                        log.error("发送 SSE 片段失败：sessionId={}, error={}", sessionId, e.getMessage());
                    }
                },
                error -> {
                    log.error("AI 响应流错误：sessionId={}, error={}", sessionId, error.getMessage());
                    try {
                        chatHistoryService.saveUserMessage(sessionId, userId, userContent);
                        if (fullContent.length() > 0) {
                            chatHistoryService.saveAiMessage(sessionId, userId, fullContent.toString());
                        }
                        sendError(emitter, sessionId, "AI 响应错误：" + error.getMessage());
                    } catch (Exception e) {
                        log.warn("发送错误消息失败（连接可能已关闭）：sessionId={}", sessionId);
                    }
                },
                () -> {
                    log.info("AI 响应完成：sessionId={}, msgType={}", sessionId, msgType);
                    try {
                        chatHistoryService.saveUserMessage(sessionId, userId, userContent);
                        chatHistoryService.saveAiMessage(sessionId, userId, fullContent.toString());

                        // 发送完成状态
                        sendAgentStatus(emitter, sessionId, "✅ 回答完成");
                        sseEmitterManager.sendComplete(sessionId);
                    } catch (Exception e) {
                        log.warn("发送完成消息失败（连接可能已关闭）：sessionId={}", sessionId);
                        try {
                            sseEmitterManager.closeEmitter(sessionId);
                        } catch (Exception ex) {
                            log.warn("关闭连接失败：sessionId={}", sessionId);
                        }
                    }
                }
        );
    }

    private void sendAgentStatus(SseEmitter emitter, String sessionId, String status) {
        try {
            sseEmitterManager.sendStatus(sessionId, status);
            log.debug("发送 Agent 状态：sessionId={}, status={}", sessionId, status);
        } catch (Exception e) {
            log.error("发送 Agent 状态失败：sessionId={}, error={}", sessionId, e.getMessage());
        }
    }


    private void sendError(SseEmitter emitter, String sessionId, String errorMsg) {
        try {
            sseEmitterManager.sendText(sessionId, "错误：" + errorMsg);
            sseEmitterManager.closeEmitter(sessionId);
        } catch (Exception e) {
            log.error("发送错误消息失败：sessionId={}, error={}", sessionId, e.getMessage());
        }
    }
}
