package com.easychat.aspect;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.enums.ResponseCodeEnum;
import com.easychat.exception.BusinessException;
import com.easychat.redis.RedisUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component("globalOperationAspect")
public class GlobalOperationAspect {

    @Resource
    private RedisUtils redisUtils;

    private static final Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);

    @Before("@annotation(com.easychat.annotation.GlobalInterceptor)")//拦截所有添加了@GlobalInterceptor注解的方法 并在方法执行前进行拦截
    public void interceptorDo(JoinPoint joinPoint){
        try {
            //从当前被调用的方法上拿到@GlobalInterceptor中的信息
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            GlobalInterceptor globalInterceptor = method.getAnnotation(GlobalInterceptor.class);
            if(null == globalInterceptor){
                return;
            }
            //若注解要求检查则进行检查
            if(globalInterceptor.checkLogin()||globalInterceptor.checkAdmin()){
                checkLogin(globalInterceptor.checkAdmin());
            }
        } catch (BusinessException e) {
            logger.error("全局拦截异常",e);
            throw e;
        } catch (Exception e) {
            logger.error("全局拦截异常",e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);//日志记录具体异常 而外部则返回封装后的自定义异常
        }
    }

    //验证逻辑
    private void checkLogin(boolean checkAdmin){
        ServletRequestAttributes request = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest httpServletRequest = request.getRequest();
        String token = httpServletRequest.getHeader("token");
        if (token == null){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token);
        if(tokenUserInfoDto == null){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if (checkAdmin && !tokenUserInfoDto.getAdmin()){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }
}
