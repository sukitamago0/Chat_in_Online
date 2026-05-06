package com.easychat.websocket.netty;

import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.redis.RedisComponent;
import com.easychat.utils.StringTools;
import com.easychat.websocket.ChannelContextUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

//真正的websocket业务驱动器

@Component
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    //<TextWebSocketFrame>表示channelRead()只关心TextWebSocketFrame类型的帧数据 其他抛弃

    private static final Logger logger = LoggerFactory.getLogger(HandlerWebSocket.class);

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 通道就绪后 调用，一般用户来做初始化
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("有新的连接加入......");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //HandlerHeartBeat类中的ctx.close();会触发该方法
        logger.info("有连接断开......");
        channelContextUtils.removeContext(ctx.channel());
    }

    @Override
    //收到客户端发送的 WebSocket 文本消息触发（客户端->服务端）
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        Channel channel = ctx.channel();
        Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attribute.get();
//        logger.info("收到userId:{}的消息:{}", userId, textWebSocketFrame.text());
        redisComponent.saveHeartBeat(userId);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            //监听WebSocketServerProtocolHandler.HandshakeComplete
            WebSocketServerProtocolHandler.HandshakeComplete complete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;

            String url = complete.requestUri();
            String token = getToken(url);
            if(token == null){
                ctx.channel().close();//为什么不能ctx.close()？
                return;
            }
            TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(token);
            if(null==tokenUserInfoDto){
                ctx.channel().close();
                return;
            }
            channelContextUtils.addContext(tokenUserInfoDto.getUserId(),ctx.channel());
        }
    }

    private String getToken(String url) {
        if (StringTools.isEmpty(url) || url.indexOf("?") == -1) {
            return null;
        }
        String[] queryParams = url.split("\\?");
        if (queryParams.length != 2) {
            return null;
        }
        String[] params = queryParams[1].split("=");
        if (params.length != 2) {
            return null;
        }
        return params[1];
    }
}