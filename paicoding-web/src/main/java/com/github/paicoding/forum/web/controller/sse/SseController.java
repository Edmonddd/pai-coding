package com.github.paicoding.forum.web.controller.sse;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.service.notify.service.SseEmitterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
public class SseController {

    @Autowired
    private SseEmitterService sseEmitterService;

    /**
     * 前端连接 SSE 的端点
     *
     * @return SseEmitter
     */
    @CrossOrigin(origins = "http://localhost:5174", allowCredentials = "true")
    @GetMapping("/notifications")
    public SseEmitter connect(HttpServletRequest request) {
        // 使用 ReqInfoContext 获取当前登录用户ID
        Long userId = null;
        if (ReqInfoContext.getReqInfo() != null && ReqInfoContext.getReqInfo().getUserId() != null) {
            userId = ReqInfoContext.getReqInfo().getUserId();
        }

        if (userId == null) {
            // 如果用户未登录，或者无法获取用户ID，可以返回一个错误或抛出异常
            // 或者根据业务需求进行处理，例如返回一个空的 SseEmitter 或抛出 UnauthorizedException
            // 为了简化示例，这里直接返回 null，实际应用中应更严谨处理
            throw new RuntimeException("用户未登录或无法获取用户ID");
        }

        // 连接超时时间设置为 1 小时 (3600 * 1000 毫秒)。-1 表示永不超时。
        // 在生产环境中，可以根据服务器资源和业务需求调整超时时间，
        // 客户端也应该有重连机制。
        long timeout = 3600 * 1000L;


        return sseEmitterService.connect(userId, timeout);
    }

    @GetMapping("/send")
    public void send() {
        Long userId = null;
        if (ReqInfoContext.getReqInfo() != null && ReqInfoContext.getReqInfo().getUserId() != null) {
            userId = ReqInfoContext.getReqInfo().getUserId();
        }

        sseEmitterService.send(userId, "test", "10");
    }
}