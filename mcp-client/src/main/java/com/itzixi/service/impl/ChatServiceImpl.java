package com.itzixi.service.impl;

import cn.hutool.json.JSONUtil;
import com.itzixi.bean.ChatEntity;
import com.itzixi.bean.ChatResponseEntity;
import com.itzixi.enums.SSEMsgType;
import com.itzixi.service.ChatService;
import com.itzixi.utils.SSEServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

import static com.itzixi.utils.SSEServer.sendEmitterMessage;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private String systemPrompt =
            """
                你是一个非常聪明的人工智能助手，可以帮我解决很多问题，我为你取一个名字，你的名字叫‘LaGoGo’。
            """;

    private final ChatClient chatClient;
    public ChatServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)

                .build();
    }
    /**
     * 聊天测试方法
     *
     * @param msg 聊天消息
     * @return 聊天回复
     */
    @Override
    public String chatTest(String msg) {
        return chatClient.prompt(msg).call().content();
    }

    @Override
    public void doChat(ChatEntity chatEntity) {
        String userId = chatEntity.getCurrentUserName();
        String prompt = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        Flux<String> contentFlux = chatClient.prompt(prompt).stream().content();

        List<String> list = contentFlux.toStream().map(content -> {
            log.info("content: {}", content);
            // 处理消息，例如发送到SSE
            SSEServer.sendMsg(userId, content, SSEMsgType.ADD);
            return content;
        }).collect(Collectors.toList());

        String fullContent = list.stream().collect(Collectors.joining());

        ChatResponseEntity chatResponseEntity = new ChatResponseEntity(fullContent, botMsgId);

        SSEServer.sendMsg(userId, JSONUtil.toJsonStr(chatResponseEntity), SSEMsgType.FINISH);

    }

    /**
     *
     * @param prompt
     * @return
     */
    @Override
    public Flux<String> streamStr(String prompt) {
        return chatClient.prompt(prompt).stream().content();
    }
}
