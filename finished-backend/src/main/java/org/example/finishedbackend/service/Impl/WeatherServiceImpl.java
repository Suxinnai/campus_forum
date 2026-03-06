package org.example.finishedbackend.service.Impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.finishedbackend.entity.VO.response.WeatherVO;
import org.example.finishedbackend.service.WeatherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    @Resource
    RestTemplate rest;

    @Value("${spring.weather.key}")
    String key;

    @Resource
    StringRedisTemplate template;

    @Override
    public WeatherVO fetchWeather(double longitude, double latitude) {
        try {
            return fetchFromCache(longitude, latitude);
        } catch (Exception e) {
            log.error("获取天气信息失败: ", e);
            return null;
        }
    }

    private WeatherVO fetchFromCache(double longitude, double latitude) {
        String geoUrl = "https://geoapi.qweather.com/v2/city/lookup?location=" + longitude + "," + latitude + "&key="
                + key;
        log.info("请求和风天气 GeoAPI: {}", geoUrl);
        byte[] geoData = rest.getForObject(geoUrl, byte[].class);
        JSONObject geo = parseJson(geoData);
        if (geo == null) {
            log.warn("GeoAPI 返回数据解析失败, 原始数据: {}", geoData != null ? new String(geoData) : "null");
            return null;
        }
        log.info("GeoAPI 返回: code={}", geo.getString("code"));
        if (!"200".equals(geo.getString("code"))) {
            log.warn("GeoAPI 返回非 200 状态: {}", geo.toJSONString());
            return null;
        }
        JSONObject location = geo.getJSONArray("location").getJSONObject(0);
        Integer id = location.getInteger("id");
        String cacheKey = "weather:" + id;
        String cache = template.opsForValue().get(cacheKey);
        if (cache != null) {
            JSONObject jsonObject = JSONObject.parseObject(cache);
            return new WeatherVO(jsonObject.getJSONObject("location"), jsonObject.getJSONObject("now"),
                    jsonObject.getJSONArray("hourly"));
        }
        WeatherVO vo = fetchFromAPI(id, location);
        if (vo == null)
            return null;
        template.opsForValue().set(cacheKey, JSONObject.from(vo).toJSONString(), 1, TimeUnit.HOURS);
        return vo;
    }

    private WeatherVO fetchFromAPI(int id, JSONObject location) {
        WeatherVO vo = new WeatherVO();
        vo.setLocation(location);

        String nowUrl = "https://devapi.qweather.com/v7/weather/now?location=" + id + "&key=" + key;
        log.info("请求和风天气 NowAPI: {}", nowUrl);
        JSONObject now = parseJson(rest.getForObject(nowUrl, byte[].class));
        if (now == null || !"200".equals(now.getString("code"))) {
            log.warn("NowAPI 返回失败: {}", now != null ? now.toJSONString() : "null");
            return null;
        }
        vo.setNow(now.getJSONObject("now"));

        String hourlyUrl = "https://devapi.qweather.com/v7/weather/24h?location=" + id + "&key=" + key;
        log.info("请求和风天气 HourlyAPI: {}", hourlyUrl);
        JSONObject hourly = parseJson(rest.getForObject(hourlyUrl, byte[].class));
        if (hourly == null || !"200".equals(hourly.getString("code"))) {
            log.warn("HourlyAPI 返回失败: {}", hourly != null ? hourly.toJSONString() : "null");
            return null;
        }
        vo.setHourly(new JSONArray(hourly.getJSONArray("hourly").stream().limit(5).toList()));
        return vo;
    }

    private JSONObject parseJson(byte[] data) {
        if (data == null || data.length == 0)
            return null;
        try {
            return JSONObject.parseObject(new String(data));
        } catch (Exception e) {
            log.error("JSON 解析失败: ", e);
            return null;
        }
    }
}
