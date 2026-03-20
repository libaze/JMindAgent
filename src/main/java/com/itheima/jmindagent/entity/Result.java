package com.itheima.jmindagent.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一响应结果工具类
 * 适配常规JSON响应 + SSE流式响应，符合接口规范要求
 */
@Data
public class Result {

    /**
     * 状态码
     */
    private int code;

    /**
     * 响应信息
     */
    private String msg;

    /**
     * 响应数据
     */
    private Object data;

    // 私有化构造器，通过静态方法创建实例
    private Result(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ====================== 状态码常量（符合接口规范） ======================
    /**
     * 操作成功
     */
    public static final int SUCCESS_CODE = 200;
    /**
     * 参数错误
     */
    public static final int PARAM_ERROR_CODE = 400;
    /**
     * 未授权/登录过期
     */
    public static final int UNAUTHORIZED_CODE = 401;
    /**
     * 无权限
     */
    public static final int FORBIDDEN_CODE = 403;
    /**
     * 服务器异常
     */
    public static final int SERVER_ERROR_CODE = 500;
    /**
     * 大模型/第三方工具调用失败
     */
    public static final int THIRD_PARTY_ERROR_CODE = 502;

    // ====================== 常规JSON响应静态构造方法 ======================
    /**
     * 成功响应（无数据）
     */
    public static Result success() {
        return new Result(SUCCESS_CODE, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static Result success(Object data) {
        return new Result(SUCCESS_CODE, "操作成功", data);
    }

    /**
     * 成功响应（自定义提示语 + 数据）
     */
    public static Result success(String msg, Object data) {
        return new Result(SUCCESS_CODE, msg, data);
    }

    /**
     * 参数错误响应
     */
    public static Result paramError() {
        return new Result(PARAM_ERROR_CODE, "参数错误", null);
    }

    /**
     * 参数错误响应（自定义提示语）
     */
    public static Result paramError(String msg) {
        return new Result(PARAM_ERROR_CODE, msg, null);
    }

    /**
     * 未授权响应
     */
    public static Result unauthorized() {
        return new Result(UNAUTHORIZED_CODE, "未授权/登录过期", null);
    }

    /**
     * 未授权响应（自定义提示语）
     */
    public static Result unauthorized(String msg) {
        return new Result(UNAUTHORIZED_CODE, msg, null);
    }

    /**
     * 无权限响应
     */
    public static Result forbidden() {
        return new Result(FORBIDDEN_CODE, "无权限", null);
    }

    /**
     * 服务器异常响应
     */
    public static Result serverError() {
        return new Result(SERVER_ERROR_CODE, "服务器异常", null);
    }

    /**
     * 服务器异常响应（自定义提示语）
     */
    public static Result serverError(String msg) {
        return new Result(SERVER_ERROR_CODE, msg, null);
    }

    /**
     * 第三方工具调用失败响应
     */
    public static Result thirdPartyError() {
        return new Result(THIRD_PARTY_ERROR_CODE, "大模型/第三方工具调用失败", null);
    }

    /**
     * 第三方工具调用失败响应（自定义提示语）
     */
    public static Result thirdPartyError(String msg) {
        return new Result(THIRD_PARTY_ERROR_CODE, msg, null);
    }

    /**
     * 自定义响应（适用于特殊状态码场景）
     */
    public static Result custom(int code, String msg, Object data) {
        return new Result(code, msg, data);
    }

    public static Result fail(int code, String msg) {
        return new Result(code, msg, null);
    }

    // ====================== SSE流式响应工具方法 ======================
    /**
     * 发送SSE流式响应（JSON格式）
     * @param emitter SSE发射器
     * @param code 状态码
     * @param msg 提示信息
     * @param data 数据体
     */
    public static void sendSseResponse(SseEmitter emitter, int code, String msg, Object data) {
        try {
            // 构建流式响应的JSON数据（与常规响应格式一致）
            Map<String, Object> sseData = new HashMap<>(3);
            sseData.put("code", code);
            sseData.put("msg", msg);
            sseData.put("data", data);

            // 转换为JSON字符串，符合流式响应格式要求
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonStr = objectMapper.writeValueAsString(sseData);

            // 发送SSE消息（指定Content-Type为application/json）
            emitter.send(SseEmitter.event()
                    .data(jsonStr, MediaType.APPLICATION_JSON));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        } catch (IOException e) {
            throw new RuntimeException("SSE消息发送失败", e);
        }
    }

    /**
     * 发送SSE成功响应
     */
    public static void sendSseSuccess(SseEmitter emitter, Object data) {
        sendSseResponse(emitter, SUCCESS_CODE, "操作成功", data);
    }

    /**
     * 发送SSE异常响应
     */
    public static void sendSseError(SseEmitter emitter, int code, String msg) {
        sendSseResponse(emitter, code, msg, null);
    }

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
