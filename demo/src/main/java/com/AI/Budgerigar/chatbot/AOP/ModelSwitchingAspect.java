package com.AI.Budgerigar.chatbot.AOP;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ModelSwitchingAspect {

    @Autowired
    private BaiduConfig baiduConfig;

    @Autowired
    private ChatMessagesRedisDAO chatMessagesRedisDAO;

    @Around("execution(* com.AI.Budgerigar.chatbot.Services.impl.BaiduChatServiceImpl.chat(..))")
    public Object aroundChatMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("ModelSwitchingAspect.aroundChatMethod()");
        int maxRetries = baiduConfig.getModelList().size();
        Throwable lastException = null;
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < maxRetries; i++) {
            try {
                log.info("Using model: " + baiduConfig.getCurrentChatBuilder());
                return joinPoint.proceed(); // Attempt to execute the original method.
            }
            catch (Exception e) {
                log.error("Error using model {}: {}", baiduConfig.getCurrentModel(), e.getMessage());
                lastException = e;
                baiduConfig.switchToNextModel(); // Change to the next model.
                ChatService impl = (ChatService) joinPoint.getTarget();
                String conversationId = args[1].toString();
                chatMessagesRedisDAO.maintainMessageHistory(conversationId);
            }
        }

        // If all attempts fail, throw the last exception.
        if (lastException != null)
            return Result.error(lastException.getMessage());
        else {
            return "None of the models worked";
        }
    }

}
