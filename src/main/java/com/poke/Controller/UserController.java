package com.poke.Controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.poke.PoJo.User;
import com.poke.Util.R;
import com.poke.Util.SendMessageUtil;
import com.poke.Util.ValidateCodeUtils;
import com.poke.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/sendMsg")
    public R<String> sendMeg(@RequestBody User user, HttpSession session){
        String phone = user.getPhone();
        if(!StringUtils.isEmpty(phone)){
            String s = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("手机验证码为={}",s);
//            SendMessageUtil.send("123",s,phone);
            redisTemplate.opsForValue().set(phone,s,30, TimeUnit.MINUTES);
            return R.success("发送成功");
        }
        return R.error("发送失败");
    }

    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        log.info(map.toString());

        //获取手机号
        String phone = map.get("phone").toString();

        //获取验证码
        String code = map.get("code").toString();

        //从Session中获取保存的验证码
        Object codeInRedis = redisTemplate.opsForValue().get(phone);

        //进行验证码的比对（页面提交的验证码和Session中保存的验证码比对）
        if(codeInRedis != null && codeInRedis.equals(code)){
            //如果能够比对成功，说明登录成功

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);
            if(user == null){
                //判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            //登录成功,删除验证码
            redisTemplate.delete(phone);
            session.setAttribute("user",user.getId());
            return R.success(user);
        }
        return R.error("登录失败");
    }
}
