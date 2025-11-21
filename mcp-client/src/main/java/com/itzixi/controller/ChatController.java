package com.itzixi.controller;

import com.itzixi.bean.ChatEntity;
import com.itzixi.service.ChatService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.net.http.HttpResponse;


/**
 * 聊天控制器
 */
@RestController
@RequestMapping("chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    /**
     * 聊天接口
     *
     * @param message 聊天消息
     * @return 聊天回复
     */
    @GetMapping("/message")
    public String getMessage(String message) {
        return "你说：" + message;
    }

    @GetMapping("chat")
    public String chat(String msg) {
        return chatService.chatTest(msg);
    }

    @GetMapping("chat/streamStr")
    public Flux<String> chatStreamStr(String msg, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatService.streamStr(msg);
    }

    @PostMapping("doChat")
    public void doChat(@RequestBody ChatEntity chatEntity) {
        chatService.doChat(chatEntity);
    }



}
