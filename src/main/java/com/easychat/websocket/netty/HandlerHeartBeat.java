package com.easychat.websocket.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//ChannelDuplexHandler:让Handler既能处理“读”（收到消息）也能处理“写”（发消息）
public class HandlerHeartBeat extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(HandlerHeartBeat.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {//判断这个事件是不是“超时事件”
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                Channel channel = ctx.channel();
                Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
                String userId = attribute.get();
                logger.info("用户 {} 心跳超时",userId);
                ctx.close();//该函数会导致链接关闭 触发回调HandlerWebSocket.channelInactive
            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush("heart");//给客户端发一个字符串 "heart"让客户端知道“我还活着”
            }
        }
    }
}