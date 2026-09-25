package com.sky.service;

import com.sky.dto.RiderDTO;
import com.sky.entity.Rider;
import com.sky.result.PageResult;

import java.util.List;

/**
 * 骑手服务接口
 */
public interface RiderService {

    /**
     * 新增骑手
     */
    void save(RiderDTO riderDTO);

    /**
     * 修改骑手信息
     */
    void update(RiderDTO riderDTO);

    /**
     * 根据id查询骑手
     */
    Rider getById(Long id);

    /**
     * 分页查询骑手列表（按状态筛选）
     */
    PageResult pageQuery(Integer status, int page, int pageSize);

    /**
     * 更新骑手状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 更新骑手位置
     */
    void updateLocation(Long riderId, Double lng, Double lat);

    /**
     * 获取所有空闲骑手
     */
    List<Rider> getAvailableRiders();
}
