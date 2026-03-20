package com.itheima.jmindagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 天气查询工具
 * 提供天气查询功能，支持按城市名称查询天气信息
 */
@Slf4j
@Component
public class WeatherTool implements CustomTool {

    private static final Map<String, String> CITY_WEATHER_CACHE = new HashMap<>();
    private static final Random RANDOM = new Random();

    @Override
    public String getToolName() {
        return "weather_query";
    }

    @Override
    public String getToolDescription() {
        return "查询指定城市的天气信息，包括温度、天气状况、湿度和风力等级";
    }

    @Override
    public String getToolAlias() {
        return "天气查询";
    }

    /**
     * 查询城市天气
     * @param city 城市名称
     * @return 天气信息 JSON 字符串
     */
    @Tool(description = "查询指定城市的当前天气信息")
    public String queryWeather(
            @ToolParam(description = "要查询天气的城市名称，如：北京、上海、广州")
            String city
    ) {
        log.info("查询城市天气：{}", city);

        try {
            String weatherInfo = getWeatherFromCache(city);
            if (weatherInfo == null) {
                weatherInfo = generateWeatherInfo(city);
                CITY_WEATHER_CACHE.put(city.toLowerCase(), weatherInfo);
            }

            log.info("天气查询结果：{}", weatherInfo);
            return weatherInfo;

        } catch (Exception e) {
            log.error("天气查询失败", e);
            return "{\"error\": \"天气查询失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 从缓存中获取天气信息
     */
    private String getWeatherFromCache(String city) {
        return CITY_WEATHER_CACHE.get(city.toLowerCase());
    }

    /**
     * 生成模拟天气信息
     */
    private String generateWeatherInfo(String city) {
        String[] conditions = {"晴", "多云", "阴", "小雨", "中雨", "大雨", "雷阵雨", "小雪", "大雪"};
        String condition = conditions[RANDOM.nextInt(conditions.length)];

        int baseTemp = switch (condition) {
            case "晴" -> RANDOM.nextInt(15) + 20;
            case "多云", "阴" -> RANDOM.nextInt(10) + 15;
            default -> RANDOM.nextInt(8) + 10;
        };

        int humidity = RANDOM.nextInt(40) + 40;
        int windLevel = RANDOM.nextInt(8) + 1;

        return String.format(
            "{\"city\":\"%s\",\"temperature\":%d,\"condition\":\"%s\",\"humidity\":%d,\"windLevel\":%d,\"updateTime\":\"%s\"}",
            city, baseTemp, condition, humidity, windLevel, java.time.LocalDateTime.now()
        );
    }
}
