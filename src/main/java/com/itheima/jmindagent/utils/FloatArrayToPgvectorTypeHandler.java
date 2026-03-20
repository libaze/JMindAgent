package com.itheima.jmindagent.utils;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * MyBatis TypeHandler：float[] 数组与 PostgreSQL vector 类型转换
 */
public class FloatArrayToPgvectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        try {
            PGobject pgObject = new PGobject();
            pgObject.setType("vector");
            pgObject.setValue(toVectorString(parameter));
            ps.setObject(i, pgObject);
        } catch (Exception e) {
            throw new SQLException("Failed to convert float[] to PostgreSQL vector", e);
        }
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVectorString(rs.getString(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVectorString(rs.getString(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVectorString(cs.getString(columnIndex));
    }

    /**
     * 将 float[] 转换为 PostgreSQL vector 格式字符串
     * 格式：[0.123456,0.789012,...]
     */
    private String toVectorString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(String.format("%.6f", vector[i]));
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将 PostgreSQL vector 字符串解析为 float[]
     */
    private float[] parseVectorString(String vectorStr) throws SQLException {
        if (vectorStr == null || vectorStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 移除方括号
            String content = vectorStr.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
            }

            if (content.isEmpty()) {
                return new float[0];
            }

            // 分割并转换
            String[] parts = content.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }

            return result;
        } catch (Exception e) {
            throw new SQLException("Failed to parse vector string: " + vectorStr, e);
        }
    }
}
