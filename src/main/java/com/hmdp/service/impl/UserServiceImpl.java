package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.file.CopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
@Resource
private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if(!RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式不对，请重新输入");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set("login:code"+phone,code,2, TimeUnit.MINUTES);
            log.debug("验证码为:{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if(!RegexUtils.isPhoneInvalid(phone)) {
        return Result.fail("手机号格式不对");
        }
        String code = stringRedisTemplate.opsForValue().get("login:code"+phone);
        String loginFormCode = loginForm.getCode();
        if(code==null || !code.equals(loginFormCode)){
            return Result.fail("验证码错误");
        }
        User user = query().eq("phone", phone).one();
        if(user==null){
          user=createUser(phone);
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO,new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((name,value)->value.toString()));
        String token = UUID.randomUUID().toString(true);
        stringRedisTemplate.opsForHash().putAll("login:user"+token,map);
        stringRedisTemplate.expire("login:user"+token,30,TimeUnit.MINUTES);
        return Result.ok(token);


    }

    @Override
    public Result logout(HttpServletRequest request) {
        String name=UserHolder.getUser().getNickName();
        String token = request.getHeader("authorization");
        LocalDateTime now = LocalDateTime.now();
        stringRedisTemplate.delete("login:user" + token);
        UserHolder.removeUser();
        log.info("{}在{}退出",name,now);
        return Result.ok();

    }

    private User createUser(String phone) {

        User user = new User();
         user.setPhone(phone);
         user.setNickName("user_"+RandomUtil.randomString(10));
        save(user);
         return user;
    }
}
