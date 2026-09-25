package com.sky.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度地图配置类
 */
@Configuration
@ConfigurationProperties(prefix = "sky.baidu")
@Data
public class BaiduMapConfiguration {

    /**
     * 百度地图AK
     */
    private String ak;

    /**
     * 商家门店地址
     */
    private String shopAddress;
}
