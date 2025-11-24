package com.itzixi.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.itzixi.bean.ChatEntity;
import com.itzixi.bean.ChatResponseEntity;
import com.itzixi.enums.SSEMsgType;
import com.itzixi.service.ChatService;
import com.itzixi.utils.SSEServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
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

    @Override
    public void doChatRagSearch(ChatEntity chatEntity, List<Document> list) {
        String botMsgId = chatEntity.getBotMsgId();
        String question = chatEntity.getMessage();
        String userId = chatEntity.getCurrentUserName();


        String context = null;
        if (CollectionUtils.isNotEmpty(list)) {
            context = list.stream().map(Document::getText).collect(Collectors.joining("\n"));
        }

        Prompt prompt = new Prompt(ragPROMPT.replace("{context}", context).replace("{question}", question));

        Flux<String> contentFlux = chatClient.prompt(prompt).stream().content();
        List<String> fluxList = contentFlux.toStream().map(content -> {
            log.info("content: {}", content);
            // 处理消息，例如发送到SSE
            SSEServer.sendMsg(userId, content, SSEMsgType.ADD);
            return content;
        }).collect(Collectors.toList());
        String fullContent = fluxList.stream().collect(Collectors.joining());
        ChatResponseEntity chatResponseEntity = new ChatResponseEntity(fullContent, botMsgId);
        SSEServer.sendMsg(userId, JSONUtil.toJsonStr(chatResponseEntity), SSEMsgType.FINISH);


    }


    // Dify 智能体引擎构建平台

    private static final String ragPROMPT = """
                                              基于上下文的知识库内容回答问题：
                                              【上下文】
                                              {context}
                                              
                                              【问题】
                                              {question}
                                              
                                              【输出】
                                              如果没有查到，请回复：不知道。
                                              如果查到，请回复具体的内容。不相关的近似内容不必提到。
                                              """;

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
