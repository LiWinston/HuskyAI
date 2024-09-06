package com.AI.Budgerigar.chatbot.AOP;

import com.AI.Budgerigar.chatbot.result.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static com.AI.Budgerigar.chatbot.Constant.ApplicationConstant.CONVERSATION_SUMMARY_GENRATED;

@Aspect
@Component
public class ChatGenerateSummaryAspect {

    private static final Logger log = LoggerFactory.getLogger(ChatGenerateSummaryAspect.class);

    private static final Random RANDOM = new Random();

    @Around("execution(* com.AI.Budgerigar.chatbot.Services.impl.BaiduChatServiceImpl.chat(..))")
    public Object aroundChat(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;
        Object[] args = joinPoint.getArgs();

        log.info("ChatGenerateSummaryAspect.aroundChat()");

        // Proceed with the chat method, but capture the result
        result = joinPoint.proceed();

        // Simulate a check after the first Redis.addMessage call
        if (shouldGenerateTitle()) {
            // Asynchronously generate and set the conversation title
            CompletableFuture<Result<String>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Use reflection to find and call the generateAndSetConversationTitle
                    // method
                    Method method = joinPoint.getTarget()
                        .getClass()
                        .getMethod("generateAndSetConversationTitle", String.class);
                    return (Result<String>) method.invoke(joinPoint.getTarget(), args[1].toString());
                }
                catch (Exception e) {
                    log.error("Error calling generateAndSetConversationTitle: ", e);
                    return Result.error("Error generating title");
                }
            });

            // Wait for the title generation to complete
            Result<String> titleResult = future.get();

            if (titleResult.getCode() == 1) {
                // Modify the result to include the generated title
                Result<String> originalResult = (Result<String>) result;
                return Result.success(originalResult.getData(),
                        originalResult.getMsg() + CONVERSATION_SUMMARY_GENRATED + titleResult.getData());
            }
        }

        return result;
    }

    // Method to determine if title generation should occur
    private boolean shouldGenerateTitle() {
        // Example: 20% probability
        return RANDOM.nextInt(100) < 100;
    }

}
