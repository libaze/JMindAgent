package com.itheima.jmindagent.service;

/**
 * 邮件服务接口
 */
public interface IEmailService {

    /**
     * 发送验证码邮件
     *
     * @param to      收件人邮箱
     * @param code    验证码
     * @param purpose 验证码用途
     */
    void sendVerificationCode(String to, String code, String purpose);

    /**
     * 生成 6 位随机验证码
     *
     * @return 6 位数字验证码
     */
    String generateVerificationCode();

    /**
     * 验证验证码是否有效
     *
     * @param email   邮箱地址
     * @param code    验证码
     * @param purpose 验证码用途
     * @return true-有效，false-无效
     */
    boolean verifyCode(String email, String code, String purpose);
}
