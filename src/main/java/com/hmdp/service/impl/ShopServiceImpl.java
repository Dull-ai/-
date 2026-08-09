package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import io.netty.util.internal.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
@Resource
private StringRedisTemplate stringRedisTemplate;
@Resource
private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) throws InterruptedException {
//        Shop shop = queryWithThrough(id);
//        Shop shop = queryWithMutex(id);
//        Shop shop = cacheClient.queryWithThrough("cache:shop:", 0, Shop.class, (id1) -> this.getById(id1), 30L, TimeUnit.SECONDS);
        Shop shop = cacheClient.queryWithMutex("cache:shop:", id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);
        if(shop==null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }
//        private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
//    //缓存击穿
//    public Shop queryWithMutex(Long id) throws InterruptedException {
//        //查redis
//        String s = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
//       saveShop2Redis(id);
//        //有
//        if(StrUtil.isBlank(s)) {
//            return null;
//        }
//        RedisData redisData = JSONUtil.toBean(s, RedisData.class);
//        JSONObject data = (JSONObject) redisData.getData();
//        LocalDateTime expireTime = redisData.getExpireTime();
//        String lockKey = "lock:shop:" + id;
//        Shop shop = JSONUtil.toBean(data, Shop.class);
//        if(expireTime.isAfter(LocalDateTime.now())){
//              //未过期
//              return shop;
//          }
//        boolean b = tryLock(lockKey);
//        if(b){
//            String s1 = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
//            //有
//            if(StrUtil.isNotBlank(s1)) {
//                RedisData redisData1 = JSONUtil.toBean(s1, RedisData.class);
//                LocalDateTime expireTime1 = redisData1.getExpireTime();
//                JSONObject data1 = (JSONObject) redisData1.getData();
//                Shop shop1 = JSONUtil.toBean(data1, Shop.class);
//                if(expireTime1.isAfter(LocalDateTime.now())){
//                    //未过期
//                    return shop1;
//                }
//            }
//            executorService.submit(()->{
//                        try {
//                            this.saveShop2Redis(id);
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }finally {
//                            unLock(lockKey);
//                        }
//                    }
//
//                    );
//
//        }
//
//        return shop;
//    }
//public void saveShop2Redis(Long id) throws InterruptedException {
//    Shop shop = getById(id);
//    Thread.sleep(200);
//    RedisData rd = new RedisData();
//    rd.setExpireTime(LocalDateTime.now().plusSeconds(30));
//    rd.setData(shop);
//    stringRedisTemplate.opsForValue().set("cache:shop:"+id,JSONUtil.toJsonStr(rd));
//}
//    //缓存穿透
//    public Shop queryWithThrough(Long id) {
//        //查redis
//        String s = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
//
//        //有
//        if(StrUtil.isNotBlank(s)) {
//            Shop bean = JSONUtil.toBean(s, Shop.class);
//            //取
//            return bean;
//        }
//        if(s!=null) {
//            return null;
//        }
//        Shop shop = getById(id);
//
//        if(shop==null){
//            stringRedisTemplate.opsForValue().set("cache:shop:" + id,"",2L+ RandomUtil.randomLong(5),TimeUnit.MINUTES);
//            return null;
//        }
//
//        String jsonStr = JSONUtil.toJsonStr(shop);
//
//        stringRedisTemplate.opsForValue().set("cache:shop:" + id, jsonStr,30L+RandomUtil.randomLong(5), TimeUnit.MINUTES);
//        return shop;
//    }
//
//
//
//    private boolean tryLock(String shopId){
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(shopId, "1", 50, TimeUnit.MINUTES);
//        return BooleanUtil.isTrue(flag);
//    }
//        private void unLock(String shopId){
//            stringRedisTemplate.delete(shopId);
//        }
    @Override
    @Transactional
    public Result update(Shop shop) {
        String key = "cache:shop:" + shop.getId();
        if(key==null){
            return Result.fail("店铺不为null");
        }
        //先更新数据库，再删除缓存
        updateById(shop);
        stringRedisTemplate.delete(key);
        return Result.ok();
    }
}
