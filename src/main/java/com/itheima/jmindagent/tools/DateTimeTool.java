package com.itheima.jmindagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 时间日期工具
 * 提供时间日期查询、转换和计算功能
 */
@Slf4j
@Component
public class DateTimeTool implements CustomTool {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getToolName() {
        return "datetime_utils";
    }

    @Override
    public String getToolDescription() {
        return "获取当前时间、日期，进行时区转换、日期计算和时间格式化";
    }

    @Override
    public String getToolAlias() {
        return "日期时间工具";
    }

    /**
     * 获取当前日期时间
     */
    @Tool(description = "获取当前的日期和时间")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        String result = now.format(DATETIME_FORMATTER);
        log.info("获取当前日期时间：{}", result);
        return result;
    }

    /**
     * 获取当前日期
     */
    @Tool(description = "获取当前的日期")
    public String getCurrentDate() {
        LocalDate today = LocalDate.now();
        String result = today.format(DATE_FORMATTER);
        log.info("获取当前日期：{}", result);
        return result;
    }

    /**
     * 获取当前时间
     */
    @Tool(description = "获取当前的时间")
    public String getCurrentTime() {
        LocalTime now = LocalTime.now();
        String result = now.format(TIME_FORMATTER);
        log.info("获取当前时间：{}", result);
        return result;
    }

    /**
     * 获取指定时区的当前时间
     */
    @Tool(description = "获取指定时区的当前日期和时间")
    public String getDateTimeByTimezone(
            @ToolParam(description = "时区 ID，如：Asia/Shanghai、America/New_York、Europe/London")
            String timezoneId
    ) {
        try {
            ZoneId zoneId = ZoneId.of(timezoneId);
            ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
            String result = zonedDateTime.format(DATETIME_FORMATTER);
            log.info("获取时区{}的日期时间：{}", timezoneId, result);
            return result;
        } catch (Exception e) {
            log.error("时区无效：{}", timezoneId, e);
            return "错误：无效的时区 ID - " + timezoneId;
        }
    }

    /**
     * 计算两个日期之间的天数差
     */
    @Tool(description = "计算两个日期之间相差的天数")
    public long daysBetweenDates(
            @ToolParam(description = "起始日期，格式：yyyy-MM-dd") String startDate,
            @ToolParam(description = "结束日期，格式：yyyy-MM-dd") String endDate
    ) {
        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
            long days = ChronoUnit.DAYS.between(start, end);
            log.info("计算日期差：{} 到 {}，相差{}天", startDate, endDate, days);
            return days;
        } catch (Exception e) {
            log.error("日期格式错误", e);
            throw new IllegalArgumentException("日期格式应为 yyyy-MM-dd");
        }
    }

    /**
     * 日期加减天数
     */
    @Tool(description = "在指定日期上增加或减少天数")
    public String addDaysToDate(
            @ToolParam(description = "基准日期，格式：yyyy-MM-dd") String date,
            @ToolParam(description = "要增加的天数（负数表示减少）") long days
    ) {
        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate resultDate = localDate.plusDays(days);
            String result = resultDate.format(DATE_FORMATTER);
            log.info("日期{}增加{}天后的结果：{}", date, days, result);
            return result;
        } catch (Exception e) {
            log.error("日期计算错误", e);
            throw new IllegalArgumentException("日期格式应为 yyyy-MM-dd");
        }
    }

    /**
     * 获取指定日期是星期几
     */
    @Tool(description = "获取指定日期是星期几")
    public String getDayOfWeek(
            @ToolParam(description = "日期，格式：yyyy-MM-dd") String date
    ) {
        try {
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            String dayOfWeek = localDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINA);
            log.info("日期{}是星期{}", date, dayOfWeek);
            return dayOfWeek;
        } catch (Exception e) {
            log.error("日期格式错误", e);
            throw new IllegalArgumentException("日期格式应为 yyyy-MM-dd");
        }
    }

    /**
     * 判断是否为闰年
     */
    @Tool(description = "判断指定年份是否为闰年")
    public boolean isLeapYear(
            @ToolParam(description = "要判断的年份") int year
    ) {
        boolean isLeap = Year.of(year).isLeap();
        log.info("年份{}是否为闰年：{}", year, isLeap);
        return isLeap;
    }
}
