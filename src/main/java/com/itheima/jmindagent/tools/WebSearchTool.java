package com.itheima.jmindagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网络搜索工具
 * 提供网络搜索功能，支持关键词搜索和信息检索
 */
@Slf4j
@Component
public class WebSearchTool implements CustomTool {

    @Override
    public String getToolName() {
        return "web_search";
    }

    @Override
    public String getToolDescription() {
        return "执行网络搜索，根据关键词查找相关信息并返回搜索结果";
    }

    @Override
    public String getToolAlias() {
        return "联网搜索工具";
    }


    /**
     * 执行网络搜索
     */
    @Tool(description = "根据关键词进行网络搜索，返回相关的搜索结果")
    public String searchWeb(
            @ToolParam(description = "搜索关键词或查询内容") String query,
            @ToolParam(description = "返回结果数量，默认 5 条", required = false) Integer numResults
    ) {
        if (numResults == null) {
            numResults = 5;
        }

        log.info("执行网络搜索：关键词={}, 结果数量={}", query, numResults);

        try {
            List<SearchResult> results = performSearch(query, numResults);
            return formatSearchResults(results);
        } catch (Exception e) {
            log.error("搜索失败", e);
            return "{\"error\": \"搜索失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 执行搜索（模拟实现）
     */
    private List<SearchResult> performSearch(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();

        // 模拟搜索结果
        results.add(new SearchResult(
            "关于\"" + query + "\"的权威介绍",
            "这是来自百度百科的搜索结果，提供了关于\"" + query + "\"的详细介绍和基本信息...",
            "https://baike.baidu.com/item/" + query.replace(" ", "-"),
            "百度百科"
        ));

        results.add(new SearchResult(
            query + " - 最新资讯",
            "这是最新的新闻报道，介绍了\"" + query + "\"的相关动态和发展趋势...",
            "https://news.example.com/search?q=" + query.replace(" ", "+"),
            "新闻网"
        ));

        results.add(new SearchResult(
            "深入解析：" + query,
            "这是一篇深度分析文章，从多个角度探讨了\"" + query + "\"的影响和意义...",
            "https://article.example.com/analysis/" + query.replace(" ", "-"),
            "知乎专栏"
        ));

        if (maxResults >= 4) {
            results.add(new SearchResult(
                query + " - 维基百科",
                "维基百科上关于\"" + query + "\"的条目，提供了全面的知识和参考资料...",
                "https://wikipedia.org/wiki/" + query.replace(" ", "_"),
                "维基百科"
            ));
        }

        if (maxResults >= 5) {
            results.add(new SearchResult(
                "如何理解" + query + "?",
                "这是一个问答讨论帖，网友们分享了对\"" + query + "\"的理解和经验...",
                "https://qa.example.com/question/" + query.replace(" ", "-"),
                "问答社区"
            ));
        }

        return results.subList(0, Math.min(maxResults, results.size()));
    }

    /**
     * 格式化搜索结果
     */
    private String formatSearchResults(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"query\":\"").append(results.isEmpty() ? "" : "search").append("\",\"results\":[");

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"title\":\"").append(result.title).append("\",");
            sb.append("\"snippet\":\"").append(result.snippet).append("\",");
            sb.append("\"url\":\"").append(result.url).append("\",");
            sb.append("\"source\":\"").append(result.source).append("\"");
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * 搜索结果内部类
     */
    private static class SearchResult {
        String title;
        String snippet;
        String url;
        String source;

        SearchResult(String title, String snippet, String url, String source) {
            this.title = title;
            this.snippet = snippet;
            this.url = url;
            this.source = source;
        }
    }
}
