package com.AI.Budgerigar.chatbot.AOP;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Services.GenerateTittle;
import com.AI.Budgerigar.chatbot.mapper.ConversationMapper;
import com.AI.Budgerigar.chatbot.result.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static com.AI.Budgerigar.chatbot.Constant.ApplicationConstant.CONVERSATION_SUMMARY_GENRATED;

@Aspect
@Component
public class ChatGenerateSummaryAspect {

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    // 修改切入点，匹配所有实现 ChatService 接口的类的 chat 方法
    @Pointcut("execution(public * com.AI.Budgerigar.chatbot.Services.ChatService.chat(..))")
    public void chatMethod() {
    }

    @Pointcut("execution(public * com.AI.Budgerigar.chatbot.Services.StreamChatService.chatFlux(..))")
    public void chatFluxMethod() {
    }

    private static final Logger log = LoggerFactory.getLogger(ChatGenerateSummaryAspect.class);

    private static final Random RANDOM = new Random();

    @Autowired
    ConversationMapper conversationMapper;

    @Autowired
    private GenerateTittle generateTittle;

    @Around("chatMethod()")
    public Object aroundChat(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String conversationId = args[1].toString();
        if (!shouldGenerateTitle(conversationId))
            return joinPoint.proceed();

        // String prompt = args[0].toString();

        log.info("ChatGenerateSummaryAspect.aroundChat()");
        // Simulate a check after the first Redis.addMessage call
        // Asynchronously generate and set the conversation title
        CompletableFuture<Result<String>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return generateTittle.generateAndSetConversationTitle(conversationId);
            }
            catch (Exception e) {
                log.error("Error calling generateAndSetConversationTitle: ", e);
                return Result.error("Error generating title");
            }
        });

        Object result;
        // Proceed with the chat method, but capture the result
        result = joinPoint.proceed();
        // Wait for the title generation to complete
        Result<String> titleResult = future.get();

        Result<String> originalResult = (Result<String>) result;
        // Modify the result to include the generated title
        if (titleResult.getCode() == 1)
            return Result.success(originalResult.getData(),
                    originalResult.getMsg() + CONVERSATION_SUMMARY_GENRATED + titleResult.getData());
        else
            return Result.success(originalResult.getData(),
                    originalResult.getMsg() + CONVERSATION_SUMMARY_GENRATED + titleResult.getMsg());
//        return result;
    }

    @Around("chatFluxMethod()")
    public Object aroundChatFlux(ProceedingJoinPoint joinPoint) throws Throwable {
        // 提前获取参数
        Object[] args = joinPoint.getArgs();
        String conversationId = args[1].toString();

        log.info("ChatGenerateSummaryAspect.aroundChatFlux()");

        // 异步触发标题生成任务
        CompletableFuture<Result<String>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return generateTittle.generateAndSetConversationTitle(conversationId);
            }
            catch (Exception e) {
                log.error("Error generating title during chatFlux: ", e);
                return Result.error("Error generating title");
            }
        });

        // 执行目标方法 (即流式chat)
        Flux<Result<String>> resultFlux = (Flux<Result<String>>) joinPoint.proceed();

        // 监听流的每一条消息，并在最后一条（即 finishReason != null）时合并标题
        return resultFlux.concatMap(result -> {
            // 检查 finishReason，判断是否为流的最后一条消息
            if (result.getMsg() != null && result.getData().isBlank()) {
                // 当是最后一条消息时，等待标题生成结果，并将其与原有的消息合并
                return Mono.fromFuture(future).map(titleResult -> {
                    if (titleResult.getCode() == 1) {
                        // 将生成的标题与原始返回的 finishReason 消息合并
                        String combinedMessage = result.getMsg() + " " + CONVERSATION_SUMMARY_GENRATED
                                + titleResult.getData();
                        return Result.success(result.getData(), combinedMessage);
                    }
                    else {
                        return result; // 如果标题生成失败，直接返回原始结果
                    }
                });
            }
            else {
                // 如果不是最后一条消息，直接返回
                return Mono.just(result);
            }
        });
    }

    // Method to determine if title generation should occur
    private boolean shouldGenerateTitle(String conversationId) {
        return true;
//        if (RANDOM.nextDouble() > 1) {
//            // new conversation must generate title
//            if (chatMessagesRedisDAO.getMessageCount(conversationId) <= 3) {
//                return true;
//            }
//            log.info("Not generating title.");
//            return false;
//        }
//        else {
//            log.info("Generating title.");
//            return true;
//        }


        // // 通过随机数实现60%的概率控制
        // double probability = Math.random(); // 生成一个0到1之间的随机数
        // if (probability > 1) {
        // log.info("Not generating title. Probability: {}", probability);
        // return false;
        // }
        // // 仅有60%的概率继续判断
        //
        // var now = LocalDateTime.now();
        // Conversation conversation = conversationMapper.selectById(conversationId);
        //
        // // 如果查询失败或生成时间为null，直接返回false
        // if (conversation == null || conversation.getCreatedAt() == null) {
        // return false;
        // }
        // log.info("conversation.getCreatedAt():{}", conversation.getCreatedAt());
        //
        // // 计算上次生成标题时间与当前时间的差距
        // var lastGenerated = conversation.getCreatedAt();
        // if (Duration.between(lastGenerated, now).toMinutes() <= 1) {
        // return true;
        // }
        //
        // // 如果总结等于conversationId，直接返回true
        // if (Objects.equals(conversationMapper.getSummaryByCid(conversationId),
        // conversationId)) {
        // return true;
        // }
        //
        // probability = Math.random();
        // return probability < 1;// 若非新会话，最终概率为0.48

    }

}
