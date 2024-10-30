package com.AI.Budgerigar.chatbot.API.Controller;

import com.AI.Budgerigar.chatbot.API.Svc.SummarizeSvc;
import com.AI.Budgerigar.chatbot.result.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/openapi/contentAIP")
@Slf4j
public class ContentAIPController {

    @Resource
    private SummarizeSvc summarizeSvc;

    //TODO: 限流
    @PostMapping("/summarize")
    public Result<String> summarize(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        String type = body.get("type");
        if(type == null || type.isEmpty()){
            return Result.error("type is required");
        }
        switch (type){
            case "EMAILSUBJECT", "EMAIL", "EMAIL_SUBJECT", "email", "email_subject" :
                return summarizeSvc.summarizeEmail(text);
            default:
                return Result.error("type is not supported");
        }
    }
}
