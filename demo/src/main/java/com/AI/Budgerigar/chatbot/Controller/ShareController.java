package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.AIUtil.Message;
import com.AI.Budgerigar.chatbot.Services.ShareService;
import com.AI.Budgerigar.chatbot.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat/share")
@Slf4j
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @RequestMapping(value = "/*", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptionsRequest() {
        return ResponseEntity.ok().build();
    }

    // 生成分享链接
    @PostMapping
    public Result<String> generateShareLink(@RequestBody Map<String, Object> requestData) {
        try {
            String uuid = (String) requestData.get("uuid");
            String conversationId = (String) requestData.get("conversationId");
            List<Integer> messageIndexes = (List<Integer>) requestData.get("messageIndexes");

            // 调用服务生成分享链接
            String shareCode = shareService.generateShareLink(uuid, conversationId, messageIndexes);

            // 返回生成的分享链接
            return Result.success(shareCode);
        }
        catch (Exception e) {
            log.error("Failed to generate share link: ", e);
            return Result.error("Failed to generate share link.");
        }
    }

    // 获取分享的对话详情
    @GetMapping("/{shareCode}")
    public Result<List<Message>> getSharedConversation(HttpServletRequest request, @PathVariable String shareCode) {
        log.info("Request Method: {}", request.getMethod());
        try {
            List<Message> sharedMessages = shareService.getSharedConversation(shareCode);
            return Result.success(sharedMessages);
        }
        catch (Exception e) {
            log.error("Failed to retrieve shared conversation: ", e);
            return Result.error("Failed to retrieve shared conversation.");
        }
    }

    // 删除分享记录
    @DeleteMapping("/{shareCode}")
    public Result<String> deleteShare(@PathVariable String shareCode) {
        try {
            shareService.deleteShare(shareCode);
            return Result.success("Share deleted successfully.");
        }
        catch (Exception e) {
            log.error("Failed to delete share: ", e);
            return Result.error("Failed to delete share.");
        }
    }

}
