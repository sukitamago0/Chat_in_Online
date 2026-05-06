package com.easychat.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalInterceptor {

    //此处是注解的参数定义 在调用注解时不传入参数则默认为default值

    //校验登录
    boolean checkLogin() default true;


    //校验管理员身份
    boolean checkAdmin() default false;
}
