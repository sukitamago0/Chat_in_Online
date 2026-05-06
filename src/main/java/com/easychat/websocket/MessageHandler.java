package com.easychat.websocket;

import com.easychat.entity.dto.MessageSendDto;
import com.easychat.utils.JsonUtils;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component("messageHandler")
public class MessageHandler {//netty消息中转

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String MESSAGE_TOPIC = "message.topic";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @PostConstruct
    public void lisMessage() {//PostConstruct Bean全部注册完后会自动执行该方法
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC); //注册一个频道名为MESSAGE_TOPIC的长时监听器
        rTopic.addListener(MessageSendDto.class, (MessageSendDto, sendDto) -> {//在任意机器向redis频道发消息则向所有订阅了该频道的服务器触发以下回调函数
            logger.info("收到订阅消息:{}", JsonUtils.convertObj2Json(sendDto));
             channelContextUtils.sendMessage(sendDto);//redis收到消息后的回调函数 可视为服务器发给用户
        });
    }


    public void sendMessage(MessageSendDto sendDto){
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.publish(sendDto);//发消息给redis
    }

}
