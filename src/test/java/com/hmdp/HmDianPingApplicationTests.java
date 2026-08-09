package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Import({CacheClient.class})
class HmDianPingApplicationTests {
@Resource
private CacheClient cacheClient;
@Autowired
private IShopService iShopService;
@Test
public void contextLoads(){
    Shop shop = iShopService.getById(1);
    cacheClient.setWithLogicExpire("cache:shop:"+1,shop,10L,TimeUnit.SECONDS);


}

}
