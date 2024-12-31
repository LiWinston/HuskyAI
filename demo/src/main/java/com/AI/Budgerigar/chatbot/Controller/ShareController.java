package com.AI.Budgerigar.chatbot.Controller;

import com.AI.Budgerigar.chatbot.Entity.Message;
import com.AI.Budgerigar.chatbot.Nosql.ShareRecord;
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

    // Generate sharing link with expiration time
    @PostMapping
    public Result<String> generateShareLink(@RequestBody Map<String, Object> requestData) {
        try {
            String uuid = (String) requestData.get("uuid");
            String conversationId = (String) requestData.get("conversationId");
            List<Integer> messageIndexes = (List<Integer>) requestData.get("messageIndexes");
            Integer expirationHours = (Integer) requestData.get("expirationHours");

            if (expirationHours == null || expirationHours <= 0) {
                expirationHours = 24; // Default to 24 hours if not specified or invalid
            }

            String shareCode = shareService.generateShareLink(uuid, conversationId, messageIndexes, expirationHours);
            return Result.success(shareCode);
        }
        catch (Exception e) {
            log.error("Failed to generate share link: ", e);
            return Result.error("Failed to generate share link.");
        }
    }

    // Get details of the shared conversation
    @GetMapping("/{shareCode}")
    public Result<List<Message>> getSharedConversation(HttpServletRequest request, @PathVariable String shareCode) {
        log.info("Request Method: {}", request.getMethod());
        try {
            List<Message> sharedMessages = shareService.getSharedConversation(shareCode);
            if (sharedMessages.isEmpty()) {
                return Result.error("Share not found or has expired.");
            }
            return Result.success(sharedMessages);
        }
        catch (Exception e) {
            log.error("Failed to retrieve shared conversation: ", e);
            return Result.error("Failed to retrieve shared conversation.");
        }
    }

    // Get all shares for a user
    @GetMapping("/user/{uuid}")
    public Result<List<ShareRecord>> getUserShares(@PathVariable String uuid) {
        try {
            List<ShareRecord> shares = shareService.getUserShares(uuid);
            return Result.success(shares);
        }
        catch (Exception e) {
            log.error("Failed to retrieve user shares: ", e);
            return Result.error("Failed to retrieve user shares.");
        }
    }

    // Update share expiration time
    @PutMapping("/{shareCode}/expiration")
    public Result<String> updateShareExpiration(@PathVariable String shareCode, @RequestBody Map<String, Object> requestData) {
        try {
            Integer newExpirationHours = (Integer) requestData.get("expirationHours");
            if (newExpirationHours == null || newExpirationHours <= 0) {
                return Result.error("Invalid expiration hours.");
            }

            shareService.updateShareExpiration(shareCode, newExpirationHours);
            return Result.success("Share expiration updated successfully.");
        }
        catch (Exception e) {
            log.error("Failed to update share expiration: ", e);
            return Result.error("Failed to update share expiration.");
        }
    }

    // Delete a shared conversation
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

    // Delete all shares for a conversation
    @DeleteMapping("/conversation/{conversationId}")
    public Result<String> deleteSharesByConversation(@PathVariable String conversationId) {
        try {
            shareService.deleteSharesByConversation(conversationId);
            return Result.success("All shares for the conversation deleted successfully.");
        }
        catch (Exception e) {
            log.error("Failed to delete conversation shares: ", e);
            return Result.error("Failed to delete conversation shares.");
        }
    }
}
