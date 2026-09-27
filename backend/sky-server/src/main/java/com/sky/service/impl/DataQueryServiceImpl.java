package com.sky.service.impl;

import com.sky.mapper.OrderMapper;
import com.sky.security.SqlSecurityValidator;
import com.sky.service.DataQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * 数据查询服务实现（安全只读查询）
 * 集成 SQL 安全校验器 + 查询超时 + 结果行数限制
 */
@Service
@Slf4j
public class DataQueryServiceImpl implements DataQueryService {

    @Autowired
    private OrderMapper orderMapper;

    @Value("${ai.query.timeout-ms:8000}")
    private int queryTimeoutMs;

    @Value("${ai.query.max-rows:100}")
    private int maxRows;

    // 用于执行超时查询的线程池
    private final ExecutorService queryExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ai-query-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    @Override
    public Map<String, Object> executeQuery(String sql) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 安全校验（代码层强制）
            SqlSecurityValidator.validate(sql);

            log.info("执行安全查询: {}", sql);

            // 2. 带超时执行查询
            Future<List<Map<String, Object>>> future = queryExecutor.submit(
                    () -> orderMapper.executeQuery(sql)
            );

            List<Map<String, Object>> resultList;
            try {
                resultList = future.get(queryTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new RuntimeException("查询超时（超过 " + queryTimeoutMs + "ms），请简化查询条件");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("查询被中断");
            } catch (ExecutionException e) {
                throw new RuntimeException("查询执行失败: " + e.getCause().getMessage(), e.getCause());
            }

            // 3. 结果行数限制
            if (resultList.size() > maxRows) {
                resultList = resultList.subList(0, maxRows);
                log.warn("查询结果超过最大行数限制 {}, 已截断", maxRows);
            }

            // 4. 构造返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("columns", resultList.isEmpty() ? new ArrayList<>() : new ArrayList<>(resultList.get(0).keySet()));
            result.put("rows", resultList);
            result.put("totalRows", resultList.size());

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("查询完成，返回 {} 行，耗时 {} ms", resultList.size(), elapsed);

            return result;

        } catch (IllegalArgumentException e) {
            log.warn("SQL 校验失败: {}", e.getMessage());
            throw new IllegalArgumentException("查询被安全规则拒绝: " + e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询执行失败, sql: {}", sql, e);
            throw new RuntimeException("查询执行失败，请检查查询语句或稍后重试");
        }
    }
}
