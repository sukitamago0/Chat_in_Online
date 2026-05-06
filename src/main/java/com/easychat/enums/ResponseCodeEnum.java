package com.easychat.enums;

public enum ResponseCodeEnum{
    CODE_200(200,"request success"),
    CODE_404(404,"404 not found"),
    CODE_600(600,"request params error"),
    CODE_601(601,"info already exist"),
    CODE_500(500,"serve return error"),
    CODE_901(901,"logging run out of time"),
    CODE_902(902,"您不是对方的好友"),
    CODE_903(903,"您已不再群聊");

    private Integer code;

    private String msg;

    ResponseCodeEnum(Integer code,String msg){
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode(){
        return code;
    }

    public String getMsg(){
        return msg;
    }

}
