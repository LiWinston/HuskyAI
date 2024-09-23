package com.AI.Budgerigar.chatbot.AOP;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Services.GenerateTittle;
import com.AI.Budgerigar.chatbot.Services.impl.OpenAIChatServiceImpl;
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

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.AI.Budgerigar.chatbot.Constant.ApplicationConstant.CONVERSATION_SUMMARY_GENRATED;

@Aspect
@Component
public class ChatGenerateSummaryAspect {

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Pointcut("execution(public * com.AI.Budgerigar.chatbot.Services.impl.preChatBehaviour.getHistoryPreChat(..))")
    public void getHistoryPreChatMethod() {
    }

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

    // 用于存储每个线程中异步任务的状态
    private final ConcurrentHashMap<Thread, CompletableFuture<Result<String>>> futureMap = new ConcurrentHashMap<>();

    public static final Result<String> DO_NOT_GEN = Result.error("DO_NOT_GEN");

    private static final CompletableFuture<Result<String>> NO_TITLE_GEN_Future = CompletableFuture
        .completedFuture(DO_NOT_GEN);

    // 监控 getHistoryPreChat 方法的执行
    @Around("getHistoryPreChatMethod()")
    public Object aroundGetHistoryPreChat(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("开切：aroundGetHistoryPreChat");

        List<String[]> result = (List<String[]>) joinPoint.proceed(); // 执行
                                                                      // getHistoryPreChat
                                                                      // 方法
        // 提前准备好标题生成的异步任务
        Object[] args = joinPoint.getArgs();
        Object caller = args[0];
        String conversationId = args[2].toString(); // 假设 conversationId 是第二个参数

        log.info("getHistoryPreChat completed for conversationId: {}", conversationId);

        if (shouldGenerateTitle(conversationId)) {
            // 如果需要生成标题，启动异步任务并存储到 futureMap 中
            CompletableFuture<Result<String>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    if (caller instanceof OpenAIChatServiceImpl openAIChatService) {
                        String openAIUrl = openAIChatService.getOpenAIUrl();
                        String model = openAIChatService.getModel();
                        return generateTittle.generateAndSetConversationTitle(conversationId, openAIUrl, model);
                    }
                    return generateTittle.generateAndSetConversationTitle(conversationId);
                }
                catch (Exception e) {
                    log.error("Error generating title asynchronously: ", e);
                    throw new RuntimeException(e);
                }
            });
            futureMap.put(Thread.currentThread(), future); // 存储异步任务到 futureMap
        }
        else {
            // 如果不需要生成标题，存储 null 到 futureMap
            futureMap.put(Thread.currentThread(), NO_TITLE_GEN_Future);
        }

        return result;
    }

    // chat 方法的切面逻辑
    @Around("chatMethod()")
    public Object aroundChat(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("开切：aroundChat");
        // 执行目标方法，即 chat 方法
        Object result = joinPoint.proceed();

        // 获取异步任务，并等待其完成（如果存在）
        CompletableFuture<Result<String>> future = futureMap.remove(Thread.currentThread()); // 从Map中获取并移除
        if (future != null && future.get() != DO_NOT_GEN) {
            try {
                // 等待异步标题生成完成
                Result<String> titleResult = future.get(); // 等待任务完成

                Result<String> originalResult = (Result<String>) result;
                // 合并标题结果到原有的返回值
                if (titleResult.getCode() == 1) {
                    return Result.success(originalResult.getData(),
                            originalResult.getMsg() + CONVERSATION_SUMMARY_GENRATED + titleResult.getData());
                }
                else {
                    return Result.success(originalResult.getData(),
                            originalResult.getMsg() + CONVERSATION_SUMMARY_GENRATED + titleResult.getMsg());
                }
            }
            catch (Exception e) {
                log.error("Error waiting for title generation in chat: ", e); // 捕获等待过程中发生的异常
                // 如果在生成标题的过程中出现异常，返回原始结果
                return result;
            }
        }
        // 如果 future 是 null，表示不生成标题，直接返回原始结果
        return result;
    }

    @Around("chatFluxMethod()")
    public Object aroundChatFlux(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("开切：aroundChatFlux");
        // 执行目标方法 (即流式chat)
        Flux<Result<String>> resultFlux = (Flux<Result<String>>) joinPoint.proceed();
        CompletableFuture<Result<String>> future = futureMap.remove(Thread.currentThread()); // 从Map中获取并移除
        AtomicBoolean titleAppended = new AtomicBoolean(false);

        // 监听流的每一条消息，并在最后一条（即 finishReason != null）时合并标题
        return resultFlux.concatMap(result -> {
            if (future != null && future.isDone() && !titleAppended.get()) {
                // 如果 future 已经完成，尽早合并标题
                return Mono.fromFuture(future).map(titleResult -> {
                    titleAppended.set(true);
                    if (titleResult.getCode() == 1) {
                        // 合并生成的标题
                        String combinedMessage = result.getMsg() + " " + CONVERSATION_SUMMARY_GENRATED
                                + titleResult.getData();
                        return Result.success(result.getData(), combinedMessage);
                    }
                    else if (titleResult != DO_NOT_GEN) {
                        String combinedMessage = result.getMsg() + " " + CONVERSATION_SUMMARY_GENRATED
                                + titleResult.getMsg();
                        return Result.success(result.getData(), combinedMessage);
                    }
                    return result;
                }).onErrorResume(e -> {
                    log.error("Error waiting for title generation in chatFlux: ", e);
                    return Mono.just(result);
                });
            }
            // 检查 finishReason，判断是否为流的最后一条消息
            if (result.getMsg() != null && result.getData().isBlank() && !titleAppended.get()) {
                if (future != null) {
                    // 当是最后一条消息时，等待标题生成结果，并将其与原有的消息合并
                    return Mono.fromFuture(future).map(titleResult -> {

                        titleAppended.set(true);
                        if (titleResult.getCode() == 1) {
                            // 将生成的标题与原始返回的 finishReason 消息合并
                            String combinedMessage = result.getMsg() + " " + CONVERSATION_SUMMARY_GENRATED
                                    + titleResult.getData();
                            return Result.success(result.getData(), combinedMessage);
                        }
                        else {
                            String combinedMessage = result.getMsg() + " " + CONVERSATION_SUMMARY_GENRATED
                                    + titleResult.getMsg();
                            return Result.success(result.getData(), combinedMessage);
                        }
                    }).onErrorResume(e -> {
                        // 如果在生成标题的过程中出现异常，返回原始结果
                        log.error("Error waiting for title generation in chatFlux: ", e);
                        return Mono.just(result);
                    });
                }
                return Mono.just(result); // 如果不需要生成标题，直接返回原始结果
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
        // if (RANDOM.nextDouble() > 1) {
        // // new conversation must generate title
        // if (chatMessagesRedisDAO.getMessageCount(conversationId) <= 3) {
        // return true;
        // }
        // log.info("Not generating title.");
        // return false;
        // }
        // else {
        // log.info("Generating title.");
        // return true;
        // }

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
