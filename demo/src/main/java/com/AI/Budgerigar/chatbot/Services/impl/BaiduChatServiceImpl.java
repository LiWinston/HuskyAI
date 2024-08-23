package com.AI.Budgerigar.chatbot.Services.impl;

import com.AI.Budgerigar.chatbot.Config.BaiduConfig;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.model.chat.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Level;

@Service
public class BaiduChatServiceImpl implements ChatService {

    @Autowired
    private Qianfan qianfan;

    @Autowired
    private BaiduConfig baiduConfig;

    @Override
    public String chat(String input) {
        try {
            ChatResponse response = qianfan.chatCompletion()
                    .model(baiduConfig.getModel())
                    .addMessage("user", input)
                    .execute();

            String result = response.getResult();
            logInfo(result);

            return result;
        } catch (Exception e) {
            // 记录异常信息
            logger.log(Level.SEVERE, "Error occurred in " + BaiduChatServiceImpl.class.getName() + ": " + e.getMessage(), e);
            // 根据需要重新抛出异常或返回一个默认的错误信息
            throw new RuntimeException("Error processing chat request", e);
        }
    }
}
