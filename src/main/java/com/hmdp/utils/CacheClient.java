package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


@Slf4j
@Component
public class CacheClient {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public void set(String id, Object value, Long timeout, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(id, JSONUtil.toJsonStr(value), timeout, timeUnit);
    }

public void setWithLogicExpire(String id, Object value, Long timeout, TimeUnit timeUnit) {
   RedisData redisData = new RedisData();
    redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(timeout)));
    redisData.setData(value);
        stringRedisTemplate.opsForValue().set(id, JSONUtil.toJsonStr(redisData));
}

    public <R,ID>R  queryWithThrough(String prefix, ID id, Class<R> r, Function<ID,R> dbFallBack,Long timeout, TimeUnit timeUnit) {
        //查redis
        String stringkey = prefix + id;
        String s = stringRedisTemplate.opsForValue().get(stringkey);

        //有
        if(StrUtil.isNotBlank(s)) {
            R bean = JSONUtil.toBean(s, r);
            //取
            return bean;
        }
        if(s!=null) {
            return null;
        }
//        Shop shop = getById(id);
        R shop= dbFallBack.apply(id);

        if(shop==null){
            stringRedisTemplate.opsForValue().set(stringkey,"",2L+ RandomUtil.randomLong(5),TimeUnit.MINUTES);
            return null;
        }

        String jsonStr = JSONUtil.toJsonStr(shop);

        this.stringRedisTemplate.opsForValue().set(stringkey,jsonStr,timeout,timeUnit);
        return shop;
    }

    //缓存击穿
    public <R,ID> R queryWithMutex(String prefix, ID id, Class<R> r, Function<ID,R> dbFallBack,Long timeout, TimeUnit timeUnit){
        //查redis
        String key = prefix + id;
        String s = stringRedisTemplate.opsForValue().get(key);
//        saveShop2Redis(id);
        //有
        if(StrUtil.isBlank(s)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(s, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        LocalDateTime expireTime = redisData.getExpireTime();
        String lockKey = "lock:shop:" + id;
        R shop = JSONUtil.toBean(data,r);
        if(expireTime==null||expireTime.isAfter(LocalDateTime.now())){
            //未过期
            return shop;
        }
        boolean b = tryLock(lockKey);
        if(b){
            String s1 = stringRedisTemplate.opsForValue().get(key);
            //有
            if(StrUtil.isNotBlank(s1)) {
                RedisData redisData1 = JSONUtil.toBean(s1, RedisData.class);
                LocalDateTime expireTime1 = redisData1.getExpireTime();
                JSONObject data1 = (JSONObject) redisData1.getData();
                R shop1 = JSONUtil.toBean(data1, r);
                if(expireTime==null||expireTime.isAfter(LocalDateTime.now())){
                    //未过期
                    return shop1;
                }
            }
            executorService.submit(()->{
                try {
                    R apply = dbFallBack.apply(id);
                    this.stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(apply),timeout,timeUnit);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }finally {
                            unLock(lockKey);
                        }
                    }

            );

        }

        return shop;
    }
    private boolean tryLock(String shopId){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(shopId, "1", 50, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(flag);
    }
    private void unLock(String shopId){
        stringRedisTemplate.delete(shopId);
    }




}
