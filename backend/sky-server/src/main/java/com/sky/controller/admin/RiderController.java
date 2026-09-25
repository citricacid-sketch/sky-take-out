package com.sky.controller.admin;

import com.sky.dto.RiderDTO;
import com.sky.entity.Rider;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.RiderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 骑手管理（管理端）
 */
@RestController
@RequestMapping("/admin/rider")
@Slf4j
@Api(tags = "骑手管理")
public class RiderController {

    @Autowired
    private RiderService riderService;

    /**
     * 新增骑手
     */
    @PostMapping
    @ApiOperation("新增骑手")
    public Result save(@RequestBody RiderDTO riderDTO) {
        log.info("新增骑手：{}", riderDTO);
        riderService.save(riderDTO);
        return Result.success();
    }

    /**
     * 修改骑手信息
     */
    @PutMapping
    @ApiOperation("修改骑手信息")
    public Result update(@RequestBody RiderDTO riderDTO) {
        log.info("修改骑手信息：{}", riderDTO);
        riderService.update(riderDTO);
        return Result.success();
    }

    /**
     * 根据id查询骑手详情
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询骑手详情")
    public Result<Rider> getById(@PathVariable Long id) {
        log.info("根据id查询骑手详情：{}", id);
        Rider rider = riderService.getById(id);
        return Result.success(rider);
    }

    /**
     * 分页查询骑手列表（按状态筛选）
     */
    @GetMapping("/page")
    @ApiOperation("分页查询骑手列表")
    public Result<PageResult> page(@RequestParam(required = false) Integer status,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int pageSize) {
        log.info("分页查询骑手列表：status={}, page={}, pageSize={}", status, page, pageSize);
        PageResult pageResult = riderService.pageQuery(status, page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 更新骑手状态
     */
    @PostMapping("/status/{status}")
    @ApiOperation("更新骑手状态")
    public Result updateStatus(@PathVariable Integer status, @RequestParam Long id) {
        log.info("更新骑手状态：id={}, status={}", id, status);
        riderService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 更新骑手位置
     */
    @PostMapping("/location")
    @ApiOperation("更新骑手位置")
    public Result updateLocation(@RequestParam Long riderId,
                                 @RequestParam Double lng,
                                 @RequestParam Double lat) {
        log.info("更新骑手位置：riderId={}, lng={}, lat={}", riderId, lng, lat);
        riderService.updateLocation(riderId, lng, lat);
        return Result.success();
    }
}
