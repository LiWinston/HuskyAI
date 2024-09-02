package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
import com.AI.Budgerigar.chatbot.Services.ChatService;
import com.AI.Budgerigar.chatbot.Services.ChatSyncService;
import com.AI.Budgerigar.chatbot.Services.userService;
import com.AI.Budgerigar.chatbot.model.Conversation;
import com.AI.Budgerigar.chatbot.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    @Qualifier("baidu")
    private ChatService chatService;

    @Autowired
    private userService userService;

    @Autowired
    private ChatSyncService chatSyncService;

    @Autowired
    private ExecutorService executorService;// 线程池 thread pool

    // 获取DB中所有对话清单，以备用户选取.每个对话清单包含对话ID和对话节选
    // get all conversation list from DB for user to choose.
    // Each conversation list contains conversation ID and conversation excerpt
    @GetMapping()
    public Result<?> getConversationList(@RequestParam String uuid) {
        try {
            // 使用 userService 查询用户是否存在
            var userExists = userService.checkUserExistsByUuid(uuid);
            if (userExists.getCode() == 0) {
                return Result.error(userExists.getMsg());
            }

            // 获取用户的对话列表及消息节选
            List<Conversation> conversations = userService.getConversations(uuid);
            return Result.success(conversations);

        }
        catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 用get传输ConversationId，表达获取历史记录之义
    // use get to transfer ConversationId, express the meaning of getting history
    // restfully
    @GetMapping("/{conversationId}")
    public Result<?> chat(@PathVariable String conversationId) {
        // 先把当前对话缓存提交到DB: first submit current conversation cache to DB
        // executorService.submit(() ->
        // chatSyncService.updateHistoryFromRedis(chatService.getConversationId()));
        chatSyncService.updateHistoryFromRedis(conversationId);
        // 读取 ConversationId 并设置到 chatService 中: read ConversationId and set to
        // chatService
        // chatService.setConversationId(conversationId);
        chatSyncService.updateHistoryFromRedis(conversationId);
        // get历史传给前端显示: get history to show in front end
        try {
            List<Message> messageList = chatSyncService.getHistory(conversationId);
            return Result.success(messageList);
        }
        catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping()
    public Result<?> chatPost(@RequestBody Map<String, String> body) {
        try {
            // 调用 chatService 的 chat 方法并返回结果 : Use chatService to handle the request
            Result<String> response = chatService.chat(body.get("prompt"), body.get("conversationId"));
            return Result.success(response.getData(), response.getMsg());
        }
        catch (Exception e) {
            // Catch any exception and return an error response
            return Result.error(e.getMessage());
        }
    }

    @RequestMapping(value = "/chat", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptionsRequest() {
        return ResponseEntity.ok().build();
    }

}
