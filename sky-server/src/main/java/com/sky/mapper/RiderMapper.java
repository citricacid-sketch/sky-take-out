package com.sky.mapper;

import com.sky.entity.Rider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 骑手 Mapper
 */
@Mapper
public interface RiderMapper {

    /**
     * 新增骑手
     */
    void insert(Rider rider);

    /**
     * 修改骑手信息
     */
    void update(Rider rider);

    /**
     * 根据id查询骑手
     */
    Rider getById(Long id);

    /**
     * 按状态筛选骑手列表
     */
    List<Rider> list(Rider rider);

    /**
     * 查询所有空闲骑手（status=1）
     */
    List<Rider> getAvailableRiders();

    /**
     * 更新骑手当前位置
     */
    void updateLocation(@Param("id") Long id, @Param("lng") Double lng, @Param("lat") Double lat);
}
