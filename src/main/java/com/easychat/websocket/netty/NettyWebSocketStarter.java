package com.easychat.websocket.netty;

import com.easychat.config.AppConfig;
import com.easychat.utils.StringTools;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

//传统SpringBoot + HttpServlet是一个请求一个线程，1000 人在线就要 1000 个线程 → 服务器直接卡死。
//Netty 用极少的线程（boss 1个 + work 几个）就能服务成千上万的长连接用户。
@Component
public class NettyWebSocketStarter implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketStarter.class.getName());

    //监听端口，接受新连接的accept线程：
    //这行代码创建了1个线程 这个线程只做一件事：
    //死循环调用selector.select()，只监听 OP_ACCEPT 事件（也就是有人来敲门）
    //1 个线程完全够 accept 几万甚至几十万连接/秒
    //当有人敲门（TCP 三次握手完成），这个线程就把新连接（SocketChannel）扔给 workerGroup 的某个线程，然后自己继续回去听门。
    private static EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    // 处理IO请求的线程
    private static EventLoopGroup workGroup = new NioEventLoopGroup();

    @Resource
    private HandlerWebSocket handlerWebSocket;

    @Resource
    private AppConfig appConfig;

    @PreDestroy
    public void close() {
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }

    //spring启动类中 通过线程调用run函数
    @Override
    public void run() {
        try {
            //启动服务器
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup);

            serverBootstrap.channel(NioServerSocketChannel.class)//指定服务端通道类型nio模式：将收到的io事件交由pipeline与channelhandler处理
                    .handler(new LoggingHandler(LogLevel.DEBUG))//为handler添加日志处理器
                    //以上可以视为标准模板
                    //每个客户端链接都创建一条新pipeline
                    .childHandler(new ChannelInitializer() {
                        //handler作用于boosGroup 通常是放置全局日志或统计
                        //.childHandler(new ChannelInitializer() { ... })作用域每一个新来的客户端 决定了每个用户连接后 业务具体的处理逻辑
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            //pipeline是一个双向链表 读事件从前往后 写事件从后往前传播
                            //[1] HttpServerCodec
                            //[2] HttpObjectAggregator
                            //[3] IdleStateHandler
                            //[4] HandlerHeartBeat
                            //[5] WebSocketServerProtocolHandler
                            //[6] HandlerWebSocket

                            //固定写法 使支持http协议
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new HttpServerCodec());

                            // 对http包分块聚合，聚合成为FullHttpRequest/FullHttpResponse
                            // 保证接收http的完整性
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));//支持的最大数据长度 http分片拼凑最大长度

                            //心跳检测 超时则报IdleStateEvent 直到遇到userEventTriggered（在自定义处理器HandlerHeartBeat中重写了）
                            pipeline.addLast(new IdleStateHandler(6, 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new HandlerHeartBeat());//超时处理器

                            //升级http为ws
                            pipeline.addLast(new WebSocketServerProtocolHandler(
                                    "/ws",
                                    null,
                                    true,
                                    64*1024,
                                    true,
                                    true,
                                    10000L));//自动处理http->ws 成功后会发出事件：HandshakeComplete 被handlerWebSocket.userEventTriggered中自定义的具体方法监听到
                            pipeline.addLast(handlerWebSocket);
                        }
                    });
            Integer wsPort = appConfig.getWsPort();
            String wsPortStr = System.getProperty("ws.port");
            if(!StringTools.isEmpty(wsPortStr)){//多端口启动 防止端口冲突
                wsPort = Integer.parseInt(wsPortStr);
            }
            ChannelFuture channelFuture = serverBootstrap.bind(appConfig.getWsPort()).sync();//端口绑定
            logger.info("netty启动成功 端口：{}", appConfig.getWsPort());
            channelFuture.channel().closeFuture().sync();//阻塞主线程，直到服务器 Channel 被关闭后才继续执行，避免 main 方法提前退出，让 Netty 服务一直运行
        } catch (Exception e) {
            logger.error("启动netty失败");
        }finally {
            //释放资源
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}


