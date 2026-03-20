package com.itheima.jmindagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件处理工具
 * 提供文件读写、目录操作等文件系统功能
 */
@Slf4j
@Component
public class FileTool implements CustomTool {

    @Override
    public String getToolName() {
        return "file_operations";
    }

    @Override
    public String getToolDescription() {
        return "执行文件操作，包括读取文件、写入文件、列出目录内容等";
    }

    @Override
    public String getToolAlias() {
        return "文件管理器";
    }

    /**
     * 读取文件内容
     */
    @Tool(description = "读取指定文件的完整内容")
    public String readFile(
            @ToolParam(description = "要读取的文件路径") String filePath
    ) {
        log.info("读取文件：{}", filePath);

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + filePath;
            }

            String content = Files.readString(path);
            log.info("文件读取成功，长度：{}字符", content.length());
            return content;

        } catch (IOException e) {
            log.error("读取文件失败", e);
            return "错误：读取文件失败 - " + e.getMessage();
        }
    }

    /**
     * 写入文件内容
     */
    @Tool(description = "向指定文件写入内容")
    public String writeFile(
            @ToolParam(description = "要写入的文件路径") String filePath,
            @ToolParam(description = "要写入的内容") String content
    ) {
        log.info("写入文件：{}", filePath);

        try {
            Path path = Paths.get(filePath);
            Path parent = path.getParent();

            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, content);
            log.info("文件写入成功");
            return "成功：文件已写入 - " + filePath;

        } catch (IOException e) {
            log.error("写入文件失败", e);
            return "错误：写入文件失败 - " + e.getMessage();
        }
    }

    /**
     * 列出目录内容
     */
    @Tool(description = "列出指定目录下的所有文件和子目录")
    public String listDirectory(
            @ToolParam(description = "要列出的目录路径") String directoryPath
    ) {
        log.info("列出目录：{}", directoryPath);

        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                return "错误：目录不存在 - " + directoryPath;
            }

            if (!Files.isDirectory(path)) {
                return "错误：路径不是目录 - " + directoryPath;
            }

            List<String> entries = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    String type = Files.isDirectory(entry) ? "[DIR] " : "[FILE]";
                    entries.add(type + entry.getFileName().toString());
                }
            }

            log.info("目录列出成功，共{}个条目", entries.size());
            return String.join("\n", entries);

        } catch (IOException e) {
            log.error("列出目录失败", e);
            return "错误：列出目录失败 - " + e.getMessage();
        }
    }

    /**
     * 检查文件是否存在
     */
    @Tool(description = "检查指定路径的文件或目录是否存在")
    public boolean checkExists(
            @ToolParam(description = "要检查的文件或目录路径") String path
    ) {
        boolean exists = Files.exists(Paths.get(path));
        log.info("检查路径{}是否存在：{}", path, exists);
        return exists;
    }

    /**
     * 获取文件信息
     */
    @Tool(description = "获取文件的基本信息，包括大小、创建时间等")
    public String getFileInfo(
            @ToolParam(description = "要查询的文件路径") String filePath
    ) {
        log.info("获取文件信息：{}", filePath);

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + filePath;
            }

            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"path\":\"").append(filePath).append("\",");
            sb.append("\"isDirectory\":").append(attrs.isDirectory()).append(",");
            sb.append("\"size\":").append(attrs.size()).append(",");
            sb.append("\"creationTime\":\"").append(attrs.creationTime()).append("\",");
            sb.append("\"lastModifiedTime\":\"").append(attrs.lastModifiedTime()).append("\",");
            sb.append("\"lastAccessTime\":\"").append(attrs.lastAccessTime()).append("\",");
            sb.append("\"isRegularFile\":").append(attrs.isRegularFile()).append(",");
            sb.append("\"isSymbolicLink\":").append(attrs.isSymbolicLink()).append(",");
            sb.append("\"fileKey\":\"").append(attrs.fileKey() != null ? attrs.fileKey().toString() : "").append("\"");
            sb.append("}");

            return sb.toString();

        } catch (IOException e) {
            log.error("获取文件信息失败", e);
            return "错误：获取文件信息失败 - " + e.getMessage();
        }
    }
}
