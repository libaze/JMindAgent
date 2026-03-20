package com.itheima.jmindagent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * 页面导航控制器
 */
@Controller
public class PageController {

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public ModelAndView loginPage() {
        return new ModelAndView("login");
    }

    /**
     * 聊天页面（主页）
     */
    @GetMapping("/chat")
    public ModelAndView chatPage() {
        return new ModelAndView("chat");
    }

    /**
     * 知识库页面
     */
    @GetMapping("/knowledge")
    public ModelAndView knowledgePage() {
        return new ModelAndView("knowledge");
    }

    /**
     * 智能体页面
     */
    @GetMapping("/agent")
    public ModelAndView agentPage() {
        return new ModelAndView("agent");
    }

    /**
     * 重定向到聊天页面（首页）
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/chat";
    }
}
