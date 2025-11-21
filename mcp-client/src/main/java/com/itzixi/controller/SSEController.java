package com.itzixi.controller;

import com.itzixi.enums.SSEMsgType;
import com.itzixi.service.ChatService;
import com.itzixi.utils.SSEServer;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;


/**
 * 聊天控制器
 */
@RestController
@RequestMapping("sse")
public class SSEController {


    /**
     * 前端发送连接请求，连接SSE服务
     * @param userId
     * @return
     */
    @GetMapping(value = "/connect", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter connect(String userId){
        return SSEServer.connect(userId);
    }

    @GetMapping("/sendMessage")
    public String getMessage(@RequestParam String userId,@RequestParam String message) {
        SSEServer.sendMsg(userId, message, SSEMsgType.MESSAGE);
        return "OK";
    }

    @GetMapping("/sendMessageAdd")
    public String getMessageAdd(@RequestParam String userId,@RequestParam String message) {
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            SSEServer.sendMsg(userId, message, SSEMsgType.ADD);
        }
        return "OK";
    }
}
