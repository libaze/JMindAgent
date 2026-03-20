package com.itheima.jmindagent.context;

/**
 * 用户上下文管理器
 * 基于 ThreadLocal 实现用户信息的线程隔离存储
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置用户上下文
     * @param context 用户上下文
     */
    public static void set(UserContext context) {
        THREAD_LOCAL.set(context);
    }

    /**
     * 获取当前线程的用户上下文
     * @return 用户上下文，如果不存在则返回 null
     */
    public static UserContext get() {
        return THREAD_LOCAL.get();
    }

    /**
     * 获取当前用户ID
     * @return 用户ID，如果不存在则返回 null
     */
    public static Long getUserId() {
        UserContext context = THREAD_LOCAL.get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户名
     * @return 用户名，如果不存在则返回 null
     */
    public static String getUsername() {
        UserContext context = THREAD_LOCAL.get();
        return context != null ? context.getUsername() : null;
    }

    /**
     * 清除当前线程的用户上下文
     * 必须在请求结束后调用，防止内存泄漏
     */
    public static void clear() {
        THREAD_LOCAL.remove();
    }
}
