package org.example.finishedbackend.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class FlowUtils {

    @Resource
    StringRedisTemplate template;

    private static final LimitAction defaultAction = overclock -> !overclock;

    public boolean limitOnceCheck(String key, int blockTime) {
        return this.internalCheck(key, 1, blockTime, defaultAction);
    }

    public boolean limitOnceUpgradeCheck(String key, int frequency, int baseTime, int upgradeTime) {
        return this.internalCheck(key, frequency, baseTime, (overclock) -> {
            if (overclock)
                template.opsForValue().set(key, "1", upgradeTime, TimeUnit.SECONDS);
            return false;
        });
    }

    public boolean limitPeriodCheck(String counterKey, String blockKey, int blockTime, int frequency, int period) {
        return this.internalCheck(counterKey, frequency, period, (overclock) -> {
            if (overclock)
                template.opsForValue().set(blockKey, "", blockTime, TimeUnit.SECONDS);
            return !overclock;
        });
    }

    public boolean limitPeriodCounterCheck(String counterKey, int frequency, int period) {
        return this.internalCheck(counterKey, frequency, period, defaultAction);
    }

    private boolean internalCheck(String key, int frequency, int period, LimitAction action) {
        try {
            if (Boolean.TRUE.equals(template.hasKey(key))) {
                Long value = Optional.ofNullable(template.opsForValue().increment(key)).orElse(0L);
                return action.run(value > frequency);
            } else {
                template.opsForValue().set(key, "1", period, TimeUnit.SECONDS);
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis 不可用, 跳过限流检查: {}", e.getMessage());
            return true;
        }
    }

    public boolean check(String key, int blockTime) {
        try {
            if (Boolean.TRUE.equals(template.hasKey(key))) {
                return false;
            } else {
                template.opsForValue().set(key, "", blockTime, TimeUnit.SECONDS);
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis 不可用, 跳过检查: {}", e.getMessage());
            return true;
        }
    }

    private interface LimitAction {
        boolean run(boolean overclock);
    }

}
