package com.sky.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 百度地图API工具类
 */
@Component
@Slf4j
public class BaiduMapUtilFinal {

    @Value("${sky.baidu.ak}")
    private String ak;

    @Value("${sky.baidu.shop-address}")
    private String shopAddress;

    /**
     * 调用百度地图地理编码API，将地址转换为经纬度坐标
     * @param address 地址
     * @return 经纬度坐标
     */
    public JSONObject getCoordinate(String address) {
        try {
            // 对地址进行URL编码
            String encodedAddress = URLEncoder.encode(address, "UTF-8");

            // 构造请求URL
            String url = "https://api.map.baidu.com/geocoding/v3/?address=" + encodedAddress
                    + "&output=json&ak=" + ak;

            log.info("调用百度地图地理编码API，请求地址：{}", url);
            log.info("待编码的原始地址：{}", address);

            // 发送HTTP GET请求
            String result = sendHttpGetRequest(url);

            log.info("百度地图地理编码API返回结果：{}", result);

            // 解析返回结果
            JSONObject jsonObject = JSONObject.parseObject(result);

            // 检查返回状态
            if (jsonObject.getInteger("status") == 0) {
                JSONObject resultObj = jsonObject.getJSONObject("result");
                if (resultObj != null && resultObj.containsKey("location")) {
                    JSONObject location = resultObj.getJSONObject("location");
                    Double lng = location.getDouble("lng");
                    Double lat = location.getDouble("lat");
                    log.info("地址 {} 对应的经纬度为：lng={}, lat={}", address, lng, lat);
                    return location;
                } else {
                    log.error("百度地图地理编码API返回结果中没有location字段，返回结果：{}", result);
                    return null;
                }
            } else {
                log.error("百度地图地理编码API调用失败，状态码：{}，消息：{}",
                        jsonObject.getInteger("status"), jsonObject.getString("message"));
                return null;
            }
        } catch (Exception e) {
            log.error("调用百度地图地理编码API异常", e);
            return null;
        }
    }

    /**
     * 调用百度地图距离计算API，计算两点之间的直线距离
     * @param origin 起点坐标，格式：经度,纬度
     * @param destination 终点坐标，格式：经度,纬度
     * @return 距离，单位：米
     */
    public Integer getDistance(String origin, String destination) {
        try {
            // 构造请求URL，使用Distance API计算直线距离
            // 添加output=json参数，并确保坐标格式正确
            String url = "https://api.map.baidu.com/routematrix/v2/driving?output=json&origins=" + origin
                    + "&destinations=" + destination + "&ak=" + ak;

            log.info("调用百度地图距离计算API，请求地址：{}", url);
            log.info("起点坐标：{}，终点坐标：{}", origin, destination);

            // 发送HTTP GET请求
            String result = sendHttpGetRequest(url);

            log.info("百度地图距离计算API返回结果：{}", result);

            // 解析返回结果
            JSONObject jsonObject = JSONObject.parseObject(result);

            // 检查返回状态
            if (jsonObject.getInteger("status") == 0) {
                JSONObject resultObj = jsonObject.getJSONArray("result").getJSONObject(0);
                JSONObject distanceObj = resultObj.getJSONObject("distance");
                Integer distance = distanceObj.getInteger("value");
                log.info("两点之间的距离为：{} 米", distance);
                return distance;
            } else {
                log.error("百度地图距离计算API调用失败，状态码：{}，消息：{}",
                        jsonObject.getInteger("status"), jsonObject.getString("message"));
                return null;
            }
        } catch (Exception e) {
            log.error("调用百度地图距离计算API异常", e);
            return null;
        }
    }

    /**
     * 发送HTTP GET请求
     * @param url 请求URL
     * @return 响应结果
     */
    private String sendHttpGetRequest(String url) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } finally {
            if (response != null) {
                response.close();
            }
            httpClient.close();
        }
    }

    /**
     * 计算用户地址与商家门店之间的距离
     * @param userAddress 用户地址
     * @return 距离，单位：米
     */
    public Integer calculateDistance(String userAddress) {
        // 获取商家门店的经纬度
        JSONObject shopLocation = getCoordinate(shopAddress);
        if (shopLocation == null) {
            log.error("获取商家门店坐标失败，门店地址：{}", shopAddress);
            return null;
        }

        // 获取用户地址的经纬度
        JSONObject userLocation = getCoordinate(userAddress);
        if (userLocation == null) {
            log.error("获取用户地址坐标失败，用户地址：{}", userAddress);
            return null;
        }

        // 构造起点和终点坐标，保留6位小数
        String origin = String.format("%.6f,%.6f", shopLocation.getDouble("lng"), shopLocation.getDouble("lat"));
        String destination = String.format("%.6f,%.6f", userLocation.getDouble("lng"), userLocation.getDouble("lat"));

        // 如果起点和终点坐标相同，直接返回0距离
        if (origin.equals(destination)) {
            log.info("起点和终点坐标相同，距离为0米");
            return 0;
        }

        // 计算距离
        return getDistance(origin, destination);
    }
}
