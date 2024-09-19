package com.AI.Budgerigar.chatbot.AOP;

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

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static com.AI.Budgerigar.chatbot.Constant.ApplicationConstant.CONVERSATION_SUMMARY_GENRATED;

@Aspect
@Component
public class ChatGenerateSummaryAspect {

    // 修改切入点，匹配所有实现 ChatService 接口的类的 chat 方法
    @Pointcut("execution(public * com.AI.Budgerigar.chatbot.Services.ChatService.chat(..))")
    public void chatMethod() {
    }

    private static final Logger log = LoggerFactory.getLogger(ChatGenerateSummaryAspect.class);

    private static final Random RANDOM = new Random();

    @Autowired
    ConversationMapper conversationMapper;

    @Autowired
    private GenerateTittle generateTittle;

    @Around("chatMethod()")
    public Object aroundChat(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;
        Object[] args = joinPoint.getArgs();
        // String prompt = args[0].toString();
        String conversationId = args[1].toString();
        log.info("ChatGenerateSummaryAspect.aroundChat()");

        // Proceed with the chat method, but capture the result
        result = joinPoint.proceed();

        // Simulate a check after the first Redis.addMessage call
        if (shouldGenerateTitle(conversationId)) {
            // Asynchronously generate and set the conversation title
            CompletableFuture<Result<String>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Use reflection to find and call the generateAndSetConversationTitle
                    // method
                    // Method method = joinPoint.getTarget()
                    // .getClass()
                    // .getMethod("generateAndSetConversationTitle", String.class);
                    // return (Result<String>) method.invoke(joinPoint.getTarget(),
                    // args[1].toString());
                    return generateTittle.generateAndSetConversationTitle(conversationId);
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
    private boolean shouldGenerateTitle(String conversationId) {
        return RANDOM.nextDouble() < 0.75;
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
