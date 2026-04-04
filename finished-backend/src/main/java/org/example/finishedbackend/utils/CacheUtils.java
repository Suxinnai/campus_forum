package org.example.finishedbackend.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheUtils {
    @Resource
    StringRedisTemplate template;

    public <T> void saveListToCache(String key, List<T> data, long expire) {
        try {
            template.opsForValue().set(key, JSONArray.from(data).toJSONString(), expire, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败 (key={}), 降级跳过: {}", key, e.getMessage());
        }
    }

    public <T> void saveListToCache(String key, T data, long expire) {
        try {
            template.opsForValue().set(key, JSONArray.from(data).toJSONString(), expire, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败 (key={}), 降级跳过: {}", key, e.getMessage());
        }
    }

    public <T> List<T> getListFromCache(String key, Class<T> itemType) {
        try {
            String s = template.opsForValue().get(key);
            if (s == null) return null;
            return JSONArray.parseArray(s, itemType).stream().toList();
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败 (key={}), 降级跳过: {}", key, e.getMessage());
            return null;
        }
    }

    public <T> T getFromCache(String key, Class<T> itemType) {
        try {
            String s = template.opsForValue().get(key);
            if (s == null) return null;
            return JSONObject.parseObject(s).to(itemType);
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败 (key={}), 降级跳过: {}", key, e.getMessage());
            return null;
        }
    }

    public void deleteCache(String key) {
        try {
            template.delete(key);
        } catch (Exception e) {
            log.warn("Redis 缓存删除失败 (key={}), 降级跳过: {}", key, e.getMessage());
        }
    }

    public void deleteCachePattern(String key) {
        try {
            Set<String> keys = Optional.ofNullable(template.keys(key)).orElse(Collections.emptySet());
            template.delete(keys);
        } catch (Exception e) {
            log.warn("Redis 缓存批量删除失败 (pattern={}), 降级跳过: {}", key, e.getMessage());
        }
    }
}
