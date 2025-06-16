package com.github.paicoding.forum.service.notify.service.impl;

import com.github.paicoding.forum.service.notify.service.SseEmitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class SseEmitterServiceImpl implements SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterServiceImpl.class);

    // ConcurrentHashMap 用于线程安全地存储用户ID到SseEmitter的映射
    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    /**
     * 建立并注册一个新的 SSE 连接
     *
     * @param userId 用户ID
     * @param timeout 超时时间，单位毫秒。-1 表示永不超时
     * @return SseEmitter 实例
     */
    @Override
    public SseEmitter connect(Long userId, Long timeout) {
        // 创建 SseEmitter 实例
        // timeout 参数决定了连接的超时时间。如果设置为 -1，则表示永不超时。
        // SseEmitter 默认的超时时间是 30 秒。
        SseEmitter sseEmitter = new SseEmitter(timeout);

        // 注册到 Map 中
        sseEmitters.put(userId, sseEmitter);
        log.info("SSE连接建立成功，用户ID: {}", userId);

        // 设置连接完成时的回调，从 Map 中移除
        sseEmitter.onCompletion(() -> {
            sseEmitters.remove(userId);
            log.info("SSE连接完成，用户ID: {}", userId);
        });

        // 设置连接超时时的回调，从 Map 中移除，并打印错误日志
        sseEmitter.onTimeout(() -> {
            sseEmitters.remove(userId);
            log.warn("SSE连接超时，用户ID: {}", userId);
        });

        // 设置连接发生错误时的回调，从 Map 中移除，并打印错误日志
        sseEmitter.onError((Throwable throwable) -> {
            sseEmitters.remove(userId);
            log.error("SSE连接错误，用户ID: {}，错误信息: {}", userId, throwable.getMessage());
        });

        // 首次连接成功后，可以发送一个初始消息，例如当前未读消息数量
        // 这里只是一个示例，你可以根据实际需求调整
//          send(userId, "initial_data", "连接成功，请等待消息。");

        return sseEmitter;
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId 用户ID
     * @param eventName 事件名称（可选，用于前端区分不同类型的事件）
     * @param data 要发送的数据对象
     */
    @Override
    public void send(Long userId, String eventName, Object data) {
        SseEmitter sseEmitter = sseEmitters.get(userId);
        if (sseEmitter != null) {
            try {
                SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                        .data(data); // 推送的数据
                if (eventName != null && !eventName.isEmpty()) {
                    eventBuilder.name(eventName); // 事件名称
                }
                // 如果前端需要ID，可以设置ID
                // .id(String.valueOf(System.currentTimeMillis()));

                sseEmitter.send(eventBuilder);
                log.info("SSE消息发送成功,用户ID: {}, 事件名: {}, 数据: {}", userId, eventName, data);
            } catch (IOException e) {
                log.error("SSE消息发送失败,用户ID: {}，错误信息: {}", userId, e.getMessage());
                // 发送失败通常意味着连接已断开，可以考虑移除
                sseEmitters.remove(userId);
            }
        } else {
            log.warn("未找到用户ID为 {} 的SSE连接,消息未发送。", userId);
        }
    }

    /**
     * 关闭指定用户的 SSE 连接
     *
     * @param userId 用户ID
     */
    @Override
    public void closeConnection(Long userId) {
        SseEmitter sseEmitter = sseEmitters.get(userId);
        if (sseEmitter != null) {
            sseEmitter.complete(); // 关闭连接
            sseEmitters.remove(userId);
            log.info("SSE连接已手动关闭,用户ID: {}", userId);
        }
    }

    /**
     * 获取当前活跃的 SSE 连接数量
     *
     * @return 活跃连接数
     */
    @Override
    public int getActiveConnectionCount() {
        return sseEmitters.size();
    }

    /**
     * 执行指定操作，并捕获任何可能发生的IOException，如果发生则移除SseEmitter。
     * 这是一个辅助方法，用于简化发送消息时的错误处理。
     *
     * @param userId 用户ID
     * @param consumer 对 SseEmitter 进行操作的 Consumer
     */
    private void doSend(Long userId, Consumer<SseEmitter> consumer) {
        SseEmitter sseEmitter = sseEmitters.get(userId);
        if (sseEmitter == null) {
            return;
        }
        try {
            consumer.accept(sseEmitter);
        } catch (Exception e) { // 使用 Exception 捕获更广泛的异常，包括 IOException
            log.error("SSE发送操作失败，用户ID: {}，错误信息: {}", userId, e.getMessage());
            sseEmitters.remove(userId);
        }
    }
} 