package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author zhangpj
 * @date 2026/2/13
 */
@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户
     *
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入用户数据
     *
     * @param user
     */
    void insert(User user);

    /**
     * 根据id查询用户
     *
     * @param userId
     * @return
     */
    User getByid(Long userId);


    Integer countByMap(Map map);

    /**
     * 按日分组统计新增用户数，用于报表导出
     * @param begin
     * @param end
     * @return Map&lt;LocalDate, Integer&gt; 日期 -> 新增用户数
     */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS newUsers FROM user WHERE create_time >= #{begin} AND create_time <= #{end} GROUP BY DATE(create_time)")
    Map<String, Integer> getDailyNewUsers(LocalDateTime begin, LocalDateTime end);
}
