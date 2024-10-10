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

    // Modify the pointcut to match the chat method of all classes implementing the
    // ChatService interface.
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

    // To store the state of asynchronous tasks in each thread.
    private final ConcurrentHashMap<Thread, CompletableFuture<Result<String>>> futureMap = new ConcurrentHashMap<>();

    public static final Result<String> DO_NOT_GEN = Result.error("DO_NOT_GEN");

    private static final CompletableFuture<Result<String>> NO_TITLE_GEN_Future = CompletableFuture
        .completedFuture(DO_NOT_GEN);

    // Monitor "getHistoryPreChat" method.
    @Around("getHistoryPreChatMethod()")
    public Object aroundGetHistoryPreChat(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Start：aroundGetHistoryPreChat");

        List<String[]> result = (List<String[]>) joinPoint.proceed(); // execute
                                                                      // getHistoryPreChat
                                                                      // method
        // Prepare the asynchronous task (title generation) in advance.
        Object[] args = joinPoint.getArgs();
        Object caller = args[0];
        String conversationId = args[2].toString(); // Pretend conversationId is the
                                                    // second parameter.

        log.info("getHistoryPreChat completed for conversationId: {}", conversationId);

        if (shouldGenerateTitle(conversationId)) {
            // If a title needs to be generated, start an asynchronous task and store it
            // in "futureMap".
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
            futureMap.put(Thread.currentThread(), future); // Store the asynchronous task
                                                           // in "futureMap".
        }
        else {
            // If a title does not need to be generated, store null in "futureMap".
            futureMap.put(Thread.currentThread(), NO_TITLE_GEN_Future);
        }

        return result;
    }

    // chat method logic
    @Around("chatMethod()")
    public Object aroundChat(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Start：aroundChat");
        // Execute the target method (chat).
        Object result = joinPoint.proceed();

        // Retrieve the asynchronous task and wait for it to complete (if it exists).
        CompletableFuture<Result<String>> future = futureMap.remove(Thread.currentThread()); // Retrieve
                                                                                             // and
                                                                                             // remove
                                                                                             // from
                                                                                             // the
                                                                                             // Map.
        if (future != null && future.get() != DO_NOT_GEN) {
            try {
                // Waiting for asynchronous title generation to complete.
                Result<String> titleResult = future.get(); // Wait for the asynchronous
                                                           // task to complete.

                Result<String> originalResult = (Result<String>) result;
                // Merge the title results into the original return value.
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
                log.error("Error waiting for title generation in chat: ", e); // Exceptions
                                                                              // occurred
                                                                              // during
                                                                              // the
                                                                              // capture
                                                                              // waiting
                                                                              // process.
                // If an exception occurs during the title generation process, return the
                // original result.
                return result;
            }
        }
        // If "future" is null, it means no title is generated and the original result is
        // returned directly.
        return result;
    }

    @Around("chatFluxMethod()")
    public Object aroundChatFlux(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Start：aroundChatFlux");
        // Execute the target method (chatFlux).
        Flux<Result<String>> resultFlux = (Flux<Result<String>>) joinPoint.proceed();
        CompletableFuture<Result<String>> future = futureMap.remove(Thread.currentThread()); // Retrieve
                                                                                             // and
                                                                                             // remove
                                                                                             // from
                                                                                             // the
                                                                                             // Map.
        AtomicBoolean titleAppended = new AtomicBoolean(false);

        // Listen to each message in the stream and merge the titles at the last one
        // (i.e., when finishReason isn't equal to null).
        return resultFlux.concatMap(result -> {
            if (future != null && future.isDone() && !titleAppended.get()) {
                // If "future" is done, merge the title as soon as possible.
                return Mono.fromFuture(future).map(titleResult -> {
                    titleAppended.set(true);
                    if (titleResult.getCode() == 1) {
                        // Merged generated title.
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
            // Check "finishReason" to determine if it is the last message of the stream.
            if (result.getMsg() != null && result.getData().isBlank() && !titleAppended.get()) {
                if (future != null) {
                    // When it's the last message, wait for the title generation result
                    // and merge it with the original message.
                    return Mono.fromFuture(future).map(titleResult -> {

                        titleAppended.set(true);
                        if (titleResult.getCode() == 1) {
                            // Merge the generated title with the original "finishReason"
                            // message returned.
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
                        // If an exception occurs during the title generation process,
                        // return the original result.
                        log.error("Error waiting for title generation in chatFlux: ", e);
                        return Mono.just(result);
                    });
                }
                log.info("No need to generate a title，returning original result directly.");
            }
            return Mono.just(result); // If no title needs to be generated, return the
                                      // original result directly.
        });
    }

    // Method to determine if title generation should occur
    private boolean shouldGenerateTitle(String conversationId) {
        // return true;
        if (RANDOM.nextDouble() > 0.88) {
            // new conversation must generate title
            if (chatMessagesRedisDAO.getMessageCount(conversationId) <= 3) {
                return true;
            }
            log.info("Not generating title.");
            return false;
        }
        else {
            log.info("Generating title.");
            return true;
        }
    }

}
