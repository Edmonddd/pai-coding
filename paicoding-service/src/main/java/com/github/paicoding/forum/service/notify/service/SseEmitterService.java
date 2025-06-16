package com.github.paicoding.forum.service.notify.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterService {

    SseEmitter connect(Long userId, Long timeout);

    void send(Long userId, String eventName, Object data);

    void closeConnection(Long userId);

    /**
     * 获取当前活跃的 SSE 连接数量
     *
     * @return 活跃连接数
     */
    int getActiveConnectionCount();
}
