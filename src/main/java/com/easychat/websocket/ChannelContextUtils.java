package com.easychat.websocket;

import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.WsInitData;
import com.easychat.entity.po.ChatMessage;
import com.easychat.entity.po.ChatSessionUser;
import com.easychat.entity.po.UserContactApply;
import com.easychat.entity.po.UserInfo;
import com.easychat.entity.query.ChatMessageQuery;
import com.easychat.entity.query.ChatSessionUserQuery;
import com.easychat.entity.query.UserContactApplyQuery;
import com.easychat.entity.query.UserInfoQuery;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.enums.UserContactApplyStatusEnum;
import com.easychat.enums.UserContactTypeEnum;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatSessionUserMapper;
import com.easychat.mapper.UserContactApplyMapper;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.redis.RedisComponent;
import com.easychat.service.ChatSessionUserService;
import com.easychat.utils.JsonUtils;
import com.easychat.utils.StringTools;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChannelContextUtils {
    private final RedisComponent redisComponent;

    private static final ConcurrentHashMap<String, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ChannelGroup> GROUP_CONTEXT_MAP = new ConcurrentHashMap<>();

    @Resource
    private ChatSessionUserMapper<ChatSessionUser,ChatSessionUserQuery> chatSessionUserMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

    @Resource
    private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;

    public ChannelContextUtils(RedisComponent redisComponent) {
        this.redisComponent = redisComponent;
    }

    public void addContext(String userId, Channel channel) {
        String channelId = channel.id().toString();
        AttributeKey attributeKey = null;
        if (AttributeKey.exists(channelId)) {
            attributeKey = AttributeKey.newInstance(channelId);
        } else {
            attributeKey = AttributeKey.valueOf(channelId);
        }
        channel.attr(attributeKey).set(userId);

        //将用户加入群组通信
        List<String> contactIdList = redisComponent.getUserContactList(userId);
        for (String groupId : contactIdList) {
            if (groupId.startsWith(UserContactTypeEnum.GROUP.getPrefix())) {
                add2Group(groupId, channel);
            }
        }

        USER_CONTEXT_MAP.put(userId, channel);
        redisComponent.saveHeartBeat(userId);

        //更新用户最后登录时间
        UserInfo updateInfo = new UserInfo();
        updateInfo.setLastLoginTime(new Date());
        userInfoMapper.updateByUserId(updateInfo, userId);

        //给用户发送消息
        UserInfo userInfo = userInfoMapper.selectByUserId(userId);
        Long sourceLastOffTime = userInfo.getLastOffTime();
        Long lastOffTime = sourceLastOffTime;
        if (sourceLastOffTime == null && System.currentTimeMillis() - Constants.MILLISECONDS_3DAYS_AGO > sourceLastOffTime) {
            lastOffTime = Constants.MILLISECONDS_3DAYS_AGO;
        }

        //查询所有的会话信息，保证设备会话同步
        ChatSessionUserQuery sessionUserQuery = new ChatSessionUserQuery();
        sessionUserQuery.setUserId(userId);
        sessionUserQuery.setOrderBy("last_receive_time desc");
        List<ChatSessionUser> chatSessionUserList = chatSessionUserMapper.selectList(sessionUserQuery);

        WsInitData wsInitData = new WsInitData();
        wsInitData.setChatSessionList(chatSessionUserList);

        //查询聊天消息 发给我的消息 和群聊
        List<String> groupIdList = contactIdList.stream()
                .filter(item -> item.startsWith(UserContactTypeEnum.GROUP.getPrefix()))
                .collect(Collectors.toList());
        groupIdList.add(userId);
        ChatMessageQuery messageQuery = new ChatMessageQuery();
        messageQuery.setContactIdList(groupIdList);
        messageQuery.setLastReceiveTime(lastOffTime);
        List<ChatMessage> chatMessageList = chatMessageMapper.selectList(messageQuery);
        wsInitData.setChatMessageList(chatMessageList);

        /*
         * 3. 查询好友申请
         */
        UserContactApplyQuery applyQuery = new UserContactApplyQuery();
        applyQuery.setReceiveUserId(userId);
        applyQuery.setLastApplyTimestamp(lastOffTime);
        applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
        Integer applyCount = userContactApplyMapper.selectCount(applyQuery);
        wsInitData.setApplyCount(applyCount);
        wsInitData.setApplyCount(applyCount);


        //发送消息
        MessageSendDto messageSendDto = new MessageSendDto();
        messageSendDto.setMessageType(MessageTypeEnum.INIT.getType());
        messageSendDto.setContactId(userId);
        messageSendDto.setExtendData(wsInitData);

        sendMsg(messageSendDto, userId);

    }

    //发送消息
    public static void sendMsg(MessageSendDto messageSendDto, String receiveId) {
        Channel userChannel = USER_CONTEXT_MAP.get(receiveId);
        if(userChannel == null) {
            return;
        }

        //相对于客户端而言，联系人就是发送人，所以要转一下再发送 这里因为数据库影响逻辑很乱
        if(MessageTypeEnum.ADD_FRIEND_SELF.getType().equals(messageSendDto.getMessageType())) {
            UserInfo userInfo = (UserInfo) messageSendDto.getExtendData();
            messageSendDto.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            messageSendDto.setContactId(userInfo.getUserId());
            messageSendDto.setContactName(userInfo.getNickName());
            messageSendDto.setExtendData(null);
        }else{
            messageSendDto.setContactId(messageSendDto.getSendUserId());
            messageSendDto.setContactName(messageSendDto.getSendUserNickName());
        }
        userChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDto)));
    }

    private void add2Group(String groupId, Channel channel) {
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        if (group == null) {
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            GROUP_CONTEXT_MAP.put(groupId, group);
        }
        if (channel == null) {
            return;
        }
        group.add(channel);
    }

    //退出
    public void removeContext(Channel channel) {
        Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attribute.get();
        if (StringTools.isEmpty(userId)) {
            USER_CONTEXT_MAP.remove(userId);
        }
        redisComponent.removeUserHeartBeat(userId);

        //更新用户最后离线时间
        UserInfo userInfo = new UserInfo();
        userInfo.setLastOffTime(System.currentTimeMillis());
        userInfoMapper.updateByUserId(userInfo, userId);
    }

    public void sendMessage(MessageSendDto messageSendDto) {
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(messageSendDto.getContactId());
        switch (contactTypeEnum){
            case USER:
                send2User(messageSendDto);
                break;
            case GROUP:
                send2Group(messageSendDto);
                break;
        }
    }

    private void send2User(MessageSendDto messageSendDto) {
        String contactId = messageSendDto.getContactId();
        if(StringTools.isEmpty(messageSendDto.getContactId())){
            return;
        }
        sendMsg(messageSendDto,contactId);
        //强制下线消息
        if(MessageTypeEnum.FORCE_OFF_LINE.getType().equals(messageSendDto.getMessageType())){
            closeConnect(contactId);
        }
    }

    public void closeConnect(String userId){
        if(StringTools.isEmpty(userId)){
            return;
        }
        redisComponent.cleanUserTokenByUserId(userId);
        Channel channel = USER_CONTEXT_MAP.get(userId);
        if(channel == null) {
            return;
        }
        channel.close();
    }

    private void send2Group(MessageSendDto messageSendDto) {
        if(StringTools.isEmpty(messageSendDto.getContactId())){
            return;
        }
        ChannelGroup channelGroup = GROUP_CONTEXT_MAP.get(messageSendDto.getContactId());
        if(channelGroup == null) {
            return;
        }
        channelGroup.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDto)));
    }

    public void addUser2Group(String userId, String groupId) {
        Channel channel = USER_CONTEXT_MAP.get(userId);
        add2Group(groupId, channel);
    }

}