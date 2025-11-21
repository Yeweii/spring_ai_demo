package com.itzixi.utils;

import com.itzixi.enums.SSEMsgType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * SSE服务器
 */
@Slf4j
public class SSEServer {

    // 存放所有用户的SSE连接
    private static final Map<String, SseEmitter> sseClients = new ConcurrentHashMap<>();


    public static SseEmitter connect(String userId) {
        // 设置超时时间，默认30秒，0L表示永不超时。
        SseEmitter sseEmitter = new SseEmitter(0L);

        // 注册回调方法
        sseEmitter.onTimeout(timeoutCallback(userId));
        sseEmitter.onCompletion(completionCallback(userId));
        sseEmitter.onError(errorCallback(userId));
        sseClients.put(userId, sseEmitter);

        log.info("SSE连接创建成功，连接的用户ID为：{}", userId);
        return sseEmitter;
    }

    private static Consumer<Throwable> errorCallback(String userId) {
        return (Throwable throwable) -> {
            log.error("SSE连接错误...", throwable);
            // 移除用户连接
            remove(userId);
        };
    }

    private static Runnable completionCallback(String userId) {
        return () -> {
            log.info("SSE连接完成...");
            remove(userId);
        };
    }


    private static Runnable timeoutCallback(String userId) {
        return () -> {
            log.info("SSE超时...");
            // 移除用户连接
            sseClients.remove(userId);
        };
    }

    public static void remove(String userId) {
        // 删除用户
        sseClients.remove(userId);
        log.info("SSE连接被移除，移除的用户ID为：{}", userId);
    }

    /**
     * 发送信息
     * @param userId
     * @param message
     * @param sseMsgType
     */
    public static void sendMsg(String userId, String message, SSEMsgType sseMsgType) {

        if (CollectionUtils.isEmpty(sseClients)) {
            return;
        }

        if (sseClients.containsKey(userId)) {
            SseEmitter sseEmitter = sseClients.get(userId);
            sendEmitterMessage(userId, message, sseMsgType, sseEmitter);
        }

    }

    public static void sendEmitterMessage(String userId, String message, SSEMsgType sseMsgType, SseEmitter sseEmitter) {
        try {
            sseEmitter.send(SseEmitter.event()
                    .id(userId)
                    .name(sseMsgType.type)
                    .data(message));

        } catch (Exception e) {
            log.error("SSE发送消息失败...", e);
            // 移除用户连接
            remove(userId);
        }
    }
}
