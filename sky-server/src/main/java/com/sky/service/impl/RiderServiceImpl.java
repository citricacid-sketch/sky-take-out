package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.RiderDTO;
import com.sky.entity.Rider;
import com.sky.mapper.RiderMapper;
import com.sky.result.PageResult;
import com.sky.service.RiderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 骑手服务实现
 */
@Service
@Slf4j
public class RiderServiceImpl implements RiderService {

    @Autowired
    private RiderMapper riderMapper;

    @Override
    public void save(RiderDTO riderDTO) {
        Rider rider = new Rider();
        BeanUtils.copyProperties(riderDTO, rider);
        // 默认状态为离线
        if (rider.getStatus() == null) {
            rider.setStatus(0);
        }
        rider.setCreateTime(LocalDateTime.now());
        rider.setUpdateTime(LocalDateTime.now());
        riderMapper.insert(rider);
    }

    @Override
    public void update(RiderDTO riderDTO) {
        Rider rider = new Rider();
        BeanUtils.copyProperties(riderDTO, rider);
        rider.setUpdateTime(LocalDateTime.now());
        riderMapper.update(rider);
    }

    @Override
    public Rider getById(Long id) {
        return riderMapper.getById(id);
    }

    @Override
    public PageResult pageQuery(Integer status, int page, int pageSize) {
        PageHelper.startPage(page, pageSize);
        Rider rider = new Rider();
        rider.setStatus(status);
        List<Rider> list = riderMapper.list(rider);
        Page<Rider> riderPage = (Page<Rider>) list;
        return new PageResult(riderPage.getTotal(), riderPage.getResult());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Rider rider = Rider.builder()
                .id(id)
                .status(status)
                .updateTime(LocalDateTime.now())
                .build();
        riderMapper.update(rider);
    }

    @Override
    public void updateLocation(Long riderId, Double lng, Double lat) {
        riderMapper.updateLocation(riderId, lng, lat);
    }

    @Override
    public List<Rider> getAvailableRiders() {
        return riderMapper.getAvailableRiders();
    }
}
