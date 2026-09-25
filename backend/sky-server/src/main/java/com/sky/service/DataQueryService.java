package com.sky.service;

import java.util.Map;

/**
 * 数据查询服务（安全只读查询）
 */
public interface DataQueryService {

    /**
     * 执行安全的只读查询
     *
     * @param sql SQL 语句（必须是 SELECT 开头）
     * @return 查询结果（列名 -> 值 的 Map 列表）
     */
    Map<String, Object> executeQuery(String sql);
}
