package com.itzixi.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.itzixi.bean.ChatEntity;
import com.itzixi.bean.ChatResponseEntity;
import com.itzixi.bean.SearchResult;
import com.itzixi.enums.SSEMsgType;
import com.itzixi.service.ChatService;
import com.itzixi.service.SesrXngService;
import com.itzixi.utils.SSEServer;
import jakarta.annotation.Resource;
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


    @Resource
    private SesrXngService sesrXngService;

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

    /**
     * 基于searxng的实时联网搜索
     *
     * @param chatEntity
     */
    @Override
    public void doInternetSearch(ChatEntity chatEntity) {


        String userId = chatEntity.getCurrentUserName();
        String question = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        List<SearchResult> searchResults = sesrXngService.search(question);

        String finalPrompt = bulidSesrXngPromt(question, searchResults);

        // 组装提示词
        Prompt prompt = new Prompt(finalPrompt);

        System.out.println(prompt.toString());

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


    private static final String sesrXngPROMPT = """
                                              你是一个互联网搜索大师，请基于以下互联网返回的结果作为上下文，根据你的理解结合用户的提问综合后，生成并且输出专业的回答：
                                              【上下文】
                                              {context}
                                              
                                              【问题】
                                              {question}
                                              
                                              【输出】
                                              如果没有查到，请回复：不知道。
                                              如果查到，请回复具体的内容。
                                              """;
    private String bulidSesrXngPromt(String question, List<SearchResult> searchResults) {


        StringBuilder context = new StringBuilder();

        searchResults.forEach(searchResult -> {
            context.append(String.format("<context>\n[来源] %s \n [摘要] %s \n </context>\n",
                    searchResult.getUrl(),
                    searchResult.getContent()));
        });

        return sesrXngPROMPT.replace("{context}", context)
                .replace("{question}", question);
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
