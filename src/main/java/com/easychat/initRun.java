package com.easychat;

import com.easychat.redis.RedisComponent;
import com.easychat.redis.RedisUtils;
import com.easychat.websocket.netty.NettyWebSocketStarter;
import io.lettuce.core.RedisConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;

@Component("initRun")
public class initRun implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(initRun.class);

    @Resource
    private DataSource dataSource;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private NettyWebSocketStarter nettyWebSocketStarter;

    //在spring启动完成后执行的逻辑
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try{
            dataSource.getConnection();
            redisUtils.get("test");
            new Thread(nettyWebSocketStarter).start();
            logger.info("服务启动成功");
        } catch (SQLException e) {
            logger.error("数据库配置错误，请检查数据库配置");
        } catch (RedisConnectionException e) {
            logger.error("Redis配置错误，请检查Redis配置");
        } catch (Exception e) {
            logger.error("服务启动失败", e);
        }
    }
}
