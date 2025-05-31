package server.models;

import java.io.Serializable;

public class Message implements Serializable {
    public enum Type {
        REGISTER,        // 注册
        EDIT_REQUEST,    // 请求编辑权限
        UPDATE_USERS,    // 更新用户列表
        INITIAL_STATE,   // 初始状态
        EDIT_GRANTED,    // 授予编辑权限
        EDIT_REJECTED,   // 拒绝编辑权限
        EDIT_ACTION,     // 编辑动作
        EDIT_COMPLETE,   // 编辑完成
        UPDATE_DRAFT,    // 更新草图
        EXIT             // 退出
    }

    private Type type;
    private Object data;
    private String username;

    public Message(Type type, Object data, String username) {
        this.type = type;
        this.data = data;
        this.username = username;
    }

    public Type getCommand() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public String getUsername() {
        return username;
    }
}