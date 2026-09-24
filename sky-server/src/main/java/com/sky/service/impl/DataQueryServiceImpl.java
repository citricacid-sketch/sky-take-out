package com.sky.service.impl;

import com.sky.mapper.OrderMapper;
import com.sky.service.DataQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据查询服务实现（安全只读查询）
 */
@Service
@Slf4j
public class DataQueryServiceImpl implements DataQueryService {

    /**
     * 允许的表名白名单
     */
    private static final String[] ALLOWED_TABLES = {
            "orders", "order_detail", "dish", "setmeal", "category", "user", "employee"
    };

    /**
     * 禁止的 SQL 关键字（危险操作）
     */
    private static final String[] FORBIDDEN_KEYWORDS = {
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "TRUNCATE", "CREATE",
            "REPLACE", "MERGE", "GRANT", "REVOKE", "EXEC", "EXECUTE", "UNION"
    };

    /**
     * 必须以 SELECT 开头
     */
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Map<String, Object> executeQuery(String sql) {
        // 1. 安全检查
        validateSql(sql);

        log.info("执行安全查询: {}", sql);

        try {
            // 2. 通过 OrderMapper 的通用查询方法执行
            List<Map<String, Object>> resultList = orderMapper.executeQuery(sql);

            // 3. 构造返回结果
            return new java.util.HashMap<String, Object>() {{
                put("columns", resultList.isEmpty() ? new java.util.ArrayList<>() : resultList.get(0).keySet());
                put("rows", resultList);
                put("totalRows", resultList.size());
            }};
        } catch (Exception e) {
            log.error("查询执行失败, sql: {}", sql, e);
            throw new RuntimeException("查询执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 校验 SQL 安全性
     *
     * @param sql 待校验的 SQL
     */
    private void validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 语句不能为空");
        }

        String upperSql = sql.toUpperCase().trim();

        // 1. 必须以 SELECT 开头
        if (!SELECT_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("只允许执行 SELECT 查询语句");
        }

        // 2. 禁止危险关键字
        for (String keyword : FORBIDDEN_KEYWORDS) {
            // 使用单词边界匹配，避免误判（如 SELECT 中包含 SUBSTR）
            if (upperSql.contains(keyword + " ") || upperSql.contains(keyword + "\t") || upperSql.contains(keyword + "(")) {
                throw new IllegalArgumentException("SQL 包含禁止的关键字: " + keyword);
            }
        }

        // 3. 检查是否只访问白名单表
        // 简单检查：提取 FROM 和 JOIN 后的表名
        if (!isValidTableAccess(upperSql)) {
            throw new IllegalArgumentException("SQL 访问了未授权的表，只允许访问: " + String.join(", ", ALLOWED_TABLES));
        }
    }

    /**
     * 检查 SQL 是否只访问白名单中的表
     *
     * @param upperSql 大写的 SQL
     * @return 是否合法
     */
    private boolean isValidTableAccess(String upperSql) {
        // 提取所有 FROM 和 JOIN 后面的标识符
        // 简化处理：检查 SQL 中出现的所有表名是否都在白名单中
        String[] tokens = upperSql.split("[\\s,()]+");
        boolean nextIsTable = false;

        for (String token : tokens) {
            if ("FROM".equals(token) || "JOIN".equals(token)) {
                nextIsTable = true;
                continue;
            }
            if (nextIsTable) {
                // 去除可能的别名（如 orders o）
                String tableName = token.toLowerCase();
                // 跳过子查询标记
                if ("SELECT".equals(tableName)) {
                    nextIsTable = false;
                    continue;
                }
                if (!isAllowedTable(tableName)) {
                    return false;
                }
                nextIsTable = false;
            }
        }
        return true;
    }

    /**
     * 检查表名是否在白名单中
     *
     * @param tableName 表名
     * @return 是否在白名单中
     */
    private boolean isAllowedTable(String tableName) {
        for (String allowed : ALLOWED_TABLES) {
            if (allowed.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }
}
