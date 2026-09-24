package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.properties.LlmProperties;
import com.sky.service.AIAssistantService;
import com.sky.service.DataQueryService;
import com.sky.utils.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 数据分析助手服务实现
 */
@Service
@Slf4j
public class AIAssistantServiceImpl implements AIAssistantService {

    /**
     * 数据库表结构描述（用于拼 prompt）
     */
    private static final String TABLE_SCHEMA = "以下是苍穹外卖系统的 MySQL 数据库表结构：\n\n" +
            "1. orders（订单表）: id, number(订单号), status(1待付款 2待接单 3已接单 4派送中 5已完成 6已取消), " +
            "user_id, address_book_id, order_time(下单时间), checkout_time(结账时间), pay_method(支付方式), " +
            "pay_status(支付状态 0未支付 1已支付), amount(金额), remark, user_name, phone, address, consignee, " +
            "cancel_reason, rejection_reason, cancel_time, estimated_delivery_time, delivery_time, pack_amount, tableware_number\n\n" +
            "2. order_detail（订单明细表）: id, name(商品名称), order_id, dish_id, setmeal_id, dishFlavor(口味), number(数量), amount, image\n\n" +
            "3. dish（菜品表）: id, name, category_id, price, image, description, status(0停售 1起售), create_time, update_time, create_user, update_user\n\n" +
            "4. setmeal（套餐表）: id, category_id, name, price, status(0停售 1起售), description, image, create_time, update_time, create_user, update_user\n\n" +
            "5. category（分类表）: id, type(1菜品分类 2套餐分类), name, sort, status, create_time, update_time, create_user, update_user\n\n" +
            "6. user（用户表）: id, openid, name, phone, sex, id_number, avatar, create_time\n\n" +
            "7. employee（员工表）: id, username, name, password, phone, sex, id_number, status(0禁用 1正常), create_time, update_time, create_user, update_user\n\n" +
            "注意：order_time 和 create_time 等时间字段是 datetime 类型，可以使用 DATE() 函数提取日期，使用 DATE_SUB(CURDATE(), INTERVAL 7 DAY) 等方式表示时间范围。";

    /**
     * System prompt：生成 SQL
     */
    private static final String SQL_SYSTEM_PROMPT = "你是苍穹外卖数据分析助手。你的职责是将用户的自然语言问题转换为 MySQL 查询 SQL。\n\n"
            + TABLE_SCHEMA + "\n\n" +
            "规则：\n" +
            "1. 只生成 SELECT 查询语句\n" +
            "2. 只使用上面列出的表名\n" +
            "3. 只返回纯 SQL 语句，不要包含任何解释、注释或 markdown 标记\n" +
            "4. 如果需要时间范围，默认使用合理的时间区间（如最近7天、本月等）\n" +
            "5. 金额字段使用 amount，时间字段使用 order_time\n" +
            "6. 状态字段：订单状态 5=已完成，6=已取消，1=待付款，2=待接单\n" +
            "7. 如果需要排序，默认按相关数值降序排列\n" +
            "8. 对于可能返回大量结果的查询，使用 LIMIT 限制返回行数（默认 LIMIT 100）";

    /**
     * System prompt：生成自然语言总结
     */
    private static final String SUMMARY_SYSTEM_PROMPT = "你是苍穹外卖数据分析助手。根据用户的问题和查询结果，生成简洁、专业的中文分析总结。\n\n" +
            "规则：\n" +
            "1. 用简洁的中文回答\n" +
            "2. 突出关键数据和趋势\n" +
            "3. 如果数据为空，说明没有找到相关数据\n" +
            "4. 不要重复 SQL 语句\n" +
            "5. 可以给出简要的业务建议";

    @Autowired
    private DataQueryService dataQueryService;

    @Autowired
    private LlmClient llmClient;

    @Override
    public String ask(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "请输入您的问题。";
        }

        try {
            // 第一步：让 LLM 生成 SQL
            String sql = generateSql(question);
            log.info("LLM 生成的 SQL: {}", sql);

            // 第二步：执行 SQL 查询
            Map<String, Object> queryResult = dataQueryService.executeQuery(sql);

            // 第三步：让 LLM 生成自然语言总结
            String summary = generateSummary(question, sql, queryResult);
            return summary;

        } catch (IllegalArgumentException e) {
            log.warn("查询参数错误: {}", e.getMessage());
            return "查询失败: " + e.getMessage();
        } catch (RuntimeException e) {
            log.error("AI 助手调用失败", e);
            return "抱歉，AI 助手暂时无法处理您的请求，请稍后再试或换个方式提问。错误信息: " + e.getMessage();
        } catch (Exception e) {
            log.error("AI 助手未知错误", e);
            return "抱歉，处理您的请求时发生了未知错误，请稍后再试。";
        }
    }

    /**
     * 调用 LLM 生成 SQL
     *
     * @param question 用户问题
     * @return 纯 SQL 语句
     */
    private String generateSql(String question) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", SQL_SYSTEM_PROMPT);
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", question);
        messages.add(userMessage);

        String response = llmClient.chat(messages);
        return extractSql(response);
    }

    /**
     * 从 LLM 响应中提取纯 SQL
     *
     * @param response LLM 返回内容
     * @return 纯 SQL 语句
     */
    private String extractSql(String response) {
        if (response == null) {
            throw new RuntimeException("LLM 返回空内容");
        }

        String sql = response.trim();

        // 去除 ```sql ... ``` 或 ``` ... ``` 标记
        Pattern codeBlockPattern = Pattern.compile("```(?:sql)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher matcher = codeBlockPattern.matcher(sql);
        if (matcher.find()) {
            sql = matcher.group(1).trim();
        }

        // 去除末尾分号（MyBatis 执行不需要）
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }

        return sql;
    }

    /**
     * 调用 LLM 生成自然语言总结
     *
     * @param question     用户问题
     * @param sql          执行的 SQL
     * @param queryResult  查询结果
     * @return 自然语言总结
     */
    private String generateSummary(String question, String sql, Map<String, Object> queryResult) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", SUMMARY_SYSTEM_PROMPT);
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        StringBuilder content = new StringBuilder();
        content.append("用户问题: ").append(question).append("\n\n");
        content.append("执行的 SQL: ").append(sql).append("\n\n");
        content.append("查询结果: \n");
        content.append(JSON.toJSONString(queryResult));

        userMessage.put("content", content.toString());
        messages.add(userMessage);

        return llmClient.chat(messages);
    }
}
