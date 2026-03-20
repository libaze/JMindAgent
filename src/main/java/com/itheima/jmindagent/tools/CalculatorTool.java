package com.itheima.jmindagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 计算器工具
 * 提供基础数学运算功能，支持加减乘除、幂运算等
 */
@Slf4j
@Component
public class CalculatorTool implements CustomTool {

    @Override
    public String getToolName() {
        return "calculator";
    }

    @Override
    public String getToolDescription() {
        return "执行数学计算，支持加减乘除、取余、幂运算等基础运算";
    }

    @Override
    public String getToolAlias() {
        return "计算器";
    }

    /**
     * 加法运算
     */
    @Tool(description = "计算两个数的和")
    public double add(
            @ToolParam(description = "第一个加数") double a,
            @ToolParam(description = "第二个加数") double b
    ) {
        log.info("计算加法：{} + {}", a, b);
        return a + b;
    }

    /**
     * 减法运算
     */
    @Tool(description = "计算两个数的差")
    public double subtract(
            @ToolParam(description = "被减数") double a,
            @ToolParam(description = "减数") double b
    ) {
        log.info("计算减法：{} - {}", a, b);
        return a - b;
    }

    /**
     * 乘法运算
     */
    @Tool(description = "计算两个数的积")
    public double multiply(
            @ToolParam(description = "第一个因数") double a,
            @ToolParam(description = "第二个因数") double b
    ) {
        log.info("计算乘法：{} × {}", a, b);
        return a * b;
    }

    /**
     * 除法运算
     */
    @Tool(description = "计算两个数的商")
    public double divide(
            @ToolParam(description = "被除数") double a,
            @ToolParam(description = "除数，不能为 0") double b
    ) {
        log.info("计算除法：{} ÷ {}", a, b);
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为 0");
        }
        return a / b;
    }

    /**
     * 幂运算
     */
    @Tool(description = "计算底数的指数次幂")
    public double power(
            @ToolParam(description = "底数") double base,
            @ToolParam(description = "指数") double exponent
    ) {
        log.info("计算幂：{}^{}", base, exponent);
        return Math.pow(base, exponent);
    }

    /**
     * 平方根运算
     */
    @Tool(description = "计算一个数的平方根")
    public double sqrt(
            @ToolParam(description = "要开平方的数，必须非负") double number
    ) {
        log.info("计算平方根：√{}", number);
        if (number < 0) {
            throw new IllegalArgumentException("负数不能开平方");
        }
        return Math.sqrt(number);
    }

    /**
     * 取余运算
     */
    @Tool(description = "计算两个数的余数")
    public double modulo(
            @ToolParam(description = "被除数") double a,
            @ToolParam(description = "除数") double b
    ) {
        log.info("计算取余：{} % {}", a, b);
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为 0");
        }
        return a % b;
    }
}
