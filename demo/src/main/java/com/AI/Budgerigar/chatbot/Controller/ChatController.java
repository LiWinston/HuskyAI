package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.DTO.ErrorResponse;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.model.Cid;
import com.AI.Budgerigar.chatbot.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    @Qualifier("baidu")
    private ChatService chatService;

    @Autowired
    private userService userService;


    // 获取DB中所有对话清单，以备用户选取
    @GetMapping()
    public Result<?> getConversationList(@RequestParam String uuid) {
        try {
            // 使用 userService 查询用户是否存在
            boolean userExists = userService.checkUserExists(uuid);
            if (!userExists) {
                return Result.error("User not found");
            }

            // 获取用户的对话列表及消息节选
            List<Cid> conversations
                    = userService.getConversations(uuid);
            return Result.success(conversations);

        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    //用get传输ConversationId，后续可扩展
    @GetMapping("/{conversationId}")
    public ResponseEntity<?> chat(@PathVariable String conversationId) {
        // 读取 ConversationId 并设置到 chatService 中
        chatService.setConversationId(conversationId);
        return ResponseEntity.ok("ConversationId set to " + conversationId);
    }



    @PostMapping()
    public ResponseEntity<?> chatPost(@RequestBody Map<String, String> body) {
        try {
            // Use chatService to handle the request
            String response = chatService.chat(body.get("prompt"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Catch any exception and return error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e));
        }
    }

    @RequestMapping(value = "/chat", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptionsRequest() {
        return ResponseEntity.ok().build();
    }
}
