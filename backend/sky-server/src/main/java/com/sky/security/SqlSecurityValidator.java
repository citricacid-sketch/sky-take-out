package com.sky.security;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 安全校验器
 * 确保 AI 生成的 SQL 只执行安全的只读查询
 */
public class SqlSecurityValidator {

    // 允许的表名白名单（仅经营分析必需的表）
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(Arrays.asList(
            "orders", "order_detail", "dish", "setmeal", "category"
    ));

    // 表 → 允许查询的字段白名单（敏感字段被排除）
    private static final Map<String, Set<String>> ALLOWED_COLUMNS;
    static {
        Map<String, Set<String>> m = new HashMap<>();
        m.put("orders", new HashSet<>(Arrays.asList(
                "id", "number", "status", "order_time", "checkout_time", "pay_method",
                "pay_status", "amount", "remark", "estimated_delivery_time", "delivery_time",
                "pack_amount", "tableware_number", "address_book_id", "user_id"
        )));
        m.put("order_detail", new HashSet<>(Arrays.asList(
                "id", "name", "order_id", "dish_id", "setmeal_id", "dish_flavor",
                "number", "amount"
        )));
        m.put("dish", new HashSet<>(Arrays.asList(
                "id", "name", "category_id", "price", "description", "status",
                "create_time", "update_time"
        )));
        m.put("setmeal", new HashSet<>(Arrays.asList(
                "id", "category_id", "name", "price", "status", "description",
                "create_time", "update_time"
        )));
        m.put("category", new HashSet<>(Arrays.asList(
                "id", "type", "name", "sort", "status"
        )));
        ALLOWED_COLUMNS = Collections.unmodifiableMap(m);
    }

    // 禁止的危险操作关键字
    private static final String[] FORBIDDEN_KEYWORDS = {
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "TRUNCATE", "CREATE",
            "REPLACE", "MERGE", "GRANT", "REVOKE", "EXEC", "EXECUTE", "CALL",
            "INTO", "LOAD_FILE", "OUTFILE", "DUMPFILE", "BENCHMARK", "SLEEP"
    };

    // 必须以 SELECT 开头
    private static final Pattern SELECT_START = Pattern.compile("^\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);

    // 多语句检测（分号后跟非空内容）
    private static final Pattern MULTI_STATEMENT = Pattern.compile(";\\s*\\S");

    // SELECT * 检测
    private static final Pattern SELECT_STAR = Pattern.compile("SELECT\\s+\\*", Pattern.CASE_INSENSITIVE);

    /**
     * 校验 SQL 是否安全
     * @param sql 待校验的 SQL
     * @throws IllegalArgumentException 如果不安全
     */
    public static void validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 语句不能为空");
        }

        String trimmed = sql.trim();
        String upperSql = trimmed.toUpperCase();

        // 1. 必须以 SELECT 开头
        if (!SELECT_START.matcher(trimmed).find()) {
            throw new IllegalArgumentException("只允许执行 SELECT 查询语句");
        }

        // 2. 禁止多语句（分号后跟非空内容）
        if (MULTI_STATEMENT.matcher(trimmed).find()) {
            throw new IllegalArgumentException("禁止执行多语句查询");
        }

        // 3. 禁止 SELECT *
        if (SELECT_STAR.matcher(trimmed).find()) {
            throw new IllegalArgumentException("禁止使用 SELECT *，必须明确指定字段");
        }

        // 4. 禁止危险关键字
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (containsWord(upperSql, keyword)) {
                throw new IllegalArgumentException("SQL 包含禁止的关键字: " + keyword);
            }
        }

        // 5. 检查表访问权限
        validateTableAccess(upperSql);

        // 6. 检查字段访问权限
        validateColumnAccess(upperSql);
    }

    /**
     * 检查是否包含指定单词（单词边界匹配）
     */
    private static boolean containsWord(String text, String word) {
        Pattern p = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
        return p.matcher(text).find();
    }

    /**
     * 校验表访问权限
     */
    private static void validateTableAccess(String upperSql) {
        // 提取 FROM 和 JOIN 后的表名
        Pattern tablePattern = Pattern.compile(
                "(?:FROM|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_]*)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = tablePattern.matcher(upperSql);
        while (m.find()) {
            String table = m.group(1).toLowerCase();
            if (!ALLOWED_TABLES.contains(table)) {
                throw new IllegalArgumentException(
                        "SQL 访问了未授权的表: " + table + "，只允许: " + ALLOWED_TABLES
                );
            }
        }
    }

    /**
     * 校验字段访问权限
     */
    private static void validateColumnAccess(String upperSql) {
        // 简单但有效的检查：提取 SELECT 和 FROM 之间的字段列表
        Pattern selectFieldsPattern = Pattern.compile(
                "SELECT\\s+(.*?)\\s+FROM",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher m = selectFieldsPattern.matcher(upperSql);
        if (m.find()) {
            String fieldsPart = m.group(1);
            // 分割字段（处理逗号分隔）
            String[] fields = fieldsPart.split(",");
            for (String field : fields) {
                String trimmed = field.trim();
                // 跳过聚合函数、表达式、子查询
                if (trimmed.contains("(") || trimmed.contains("SELECT")) continue;
                // 提取字段名（处理别名）
                String col = trimmed.replaceAll("(?i).*?AS\\s+", "").trim();
                String[] parts = col.split("\\.");
                String colName = parts[parts.length - 1].replaceAll("`", "").trim();
                if (colName.isEmpty() || colName.equals("*")) continue;
                // 检查字段是否敏感
                if (isSensitiveColumn(colName)) {
                    throw new IllegalArgumentException("SQL 访问了敏感字段: " + colName);
                }
            }
        }
    }

    /**
     * 检查字段是否敏感
     */
    private static boolean isSensitiveColumn(String colName) {
        String lower = colName.toLowerCase();
        return lower.contains("password") || lower.contains("phone") ||
                lower.contains("address") || lower.contains("id_number") ||
                lower.contains("salt") || lower.contains("token") ||
                lower.contains("secret") || lower.contains("credential");
    }

    /**
     * 获取允许的表名
     */
    public static Set<String> getAllowedTables() {
        return Collections.unmodifiableSet(ALLOWED_TABLES);
    }

    /**
     * 获取指定表允许的字段
     */
    public static Set<String> getAllowedColumns(String tableName) {
        return ALLOWED_COLUMNS.getOrDefault(tableName.toLowerCase(), Collections.emptySet());
    }
}
