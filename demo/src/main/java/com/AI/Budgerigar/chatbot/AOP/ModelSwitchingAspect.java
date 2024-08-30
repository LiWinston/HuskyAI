package com.AI.Budgerigar.chatbot.AOP;

import com.AI.Budgerigar.chatbot.Cache.ChatMessagesRedisDAO;
import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.DTO.ErrorResponse;
import com.AI.Budgerigar.chatbot.Services.ChatService;
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
        log.info("Chat method intercepted");
        int maxRetries = baiduConfig.getModelList().size();
        Throwable lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                log.info("Using model: " + baiduConfig.getCurrentModel());
                return joinPoint.proceed(); // 尝试执行原始方法
            } catch (Exception e) {
                log.error("Error using model {}: {}", baiduConfig.getCurrentModel(), e.getMessage());
                lastException = e;
                baiduConfig.switchToNextModel(); // 切换到下一个模型
                ChatService impl = (ChatService) joinPoint.getTarget();
                String conversationId = impl.getConversationId();
                chatMessagesRedisDAO.maintainMessageHistory(conversationId);
            }
        }

        // 如果所有尝试都失败，抛出最后一个异常
        if (lastException != null) return new ErrorResponse(lastException);
        else {
            return "None of the models worked";
        }
    }
}
