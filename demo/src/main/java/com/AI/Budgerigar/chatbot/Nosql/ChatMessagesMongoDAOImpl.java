package com.AI.Budgerigar.chatbot.Nosql;

import com.AI.Budgerigar.chatbot.Entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class ChatMessagesMongoDAOImpl implements ChatMessagesMongoDAO {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public int getConversationLengthById(String conversationId) {
        try {
            MatchOperation match = Aggregation.match(Criteria.where("_id").is(conversationId));
            ProjectionOperation project = Aggregation.project().and("messages").size().as("messageCount");
            Aggregation aggregation = Aggregation.newAggregation(match, project);

            AggregationResults<Document> result = mongoTemplate.aggregate(aggregation, "chat_conversations",
                    Document.class);
            Document document = result.getUniqueMappedResult();

            if (document != null) {
                return document.getInteger("messageCount", 0);
            }
            else {
                return 0;
            }

        }
        catch (Exception e) {
            log.error("MongoDB_GET_LENGTH : Failed to get conversation length for conversation ID: {}", conversationId,
                    e);
            // Obtain using traditional methods.
            ChatConversationRecord chatConversationRecord = mongoTemplate.findById(conversationId,
                    ChatConversationRecord.class);
            if (chatConversationRecord != null) {
                return chatConversationRecord.getMessages().size();
            }
            else {
                return 0;
            }
        }
    }

    @Override
    public void updateHistoryById(String conversationId, List<Message> newMessages) {
        List<String[]> messageArrays = newMessages.stream().map(this::convertToArray).collect(Collectors.toList());

        try {
            ChatConversationRecord chatConversationRecord = mongoTemplate.findById(conversationId,
                    ChatConversationRecord.class);
            if (chatConversationRecord != null) {
                chatConversationRecord.addStringMessages(messageArrays); // Use a string
                                                                         // array.
                mongoTemplate.save(chatConversationRecord);
            }
            else {
                ChatConversationRecord newChatConversationRecord = new ChatConversationRecord();
                newChatConversationRecord.setMessages(messageArrays);
                newChatConversationRecord.setConversationId(conversationId);
                mongoTemplate.save(newChatConversationRecord);
            }
        }
        catch (Exception e) {
            log.error("Mongo_UPD_Simple : Failed to update (Simple method) for conversation ID: {}", conversationId, e);
        }
    }

    @Override
    public List<Message> getConversationHistory(String conversationId) {
        // Step 1: Attempting to retrieve session records from MongoDB.
        try {
            ChatConversationRecord chatConversationRecord = mongoTemplate.findById(conversationId,
                    ChatConversationRecord.class);

            // Step 2: Check if session records exist.
            if (chatConversationRecord != null) {
                List<String[]> messagesList = chatConversationRecord.getMessages();

                // Step 3: Verify whether the structure and content of the message list
                // are valid.
                if (messagesList != null && !messagesList.isEmpty() && messagesList.get(0) != null) {
                    log.info("MongoDB_Get_History:Found {} history - {}. Returning {} messages.", messagesList.size(),
                            conversationId, messagesList.size());

                    // Step 4: Convert the message list to a list of Message objects and
                    // return.
                    return messagesList.stream().map(this::convertToMessage).collect(Collectors.toList());
                }
                else {
                    // Error: The message structure is invalid.
                    log.error("MongoDB_Get_History:Invalid history - {}. Returning empty list.", conversationId);
                    return new ArrayList<>(); // Return a mutable empty list.
                }
            }
            else {
                // Error: No corresponding session record was found.
                log.error("MongoDB_Get_History:No history - {}. Returning empty list.", conversationId);
                return new ArrayList<>(); // Return a mutable empty list.
            }
        }
        catch (Exception e) {
            // Step 5: Exception Handling - Capture and log error information.
            log.error("Error fetching conversation history for ID: " + conversationId + ", Error: " + e.getMessage(),
                    e);
            return new ArrayList<>(); // Return a mutable empty list.
        }
    }

    // A method to convert String[] to Message objects.
    private Message convertToMessage(String[] messageArray) {
        if (messageArray.length == 3) {
            return new Message(messageArray[0], messageArray[1], messageArray[2]);
        }
        else {
            throw new IllegalArgumentException("Invalid message format. Expected [role, timestamp, content]");
        }
    }

    @Override
    public void replaceHistoryById(String conversationId, List<Message> newMessages) {
        // Convert a list of Message objects to a list of String[].
        List<String[]> messageArrays = newMessages.stream().map(this::convertToArray).collect(Collectors.toList());

        // Find current ChatConversationRecord
        try {
            ChatConversationRecord chatConversationRecord = mongoTemplate.findById(conversationId,
                    ChatConversationRecord.class);

            if (chatConversationRecord != null) {
                // Update the existing message list.
                chatConversationRecord.setMessages(messageArrays);
            }
            else {
                // If not exist，create a new ChatConversationRecord
                chatConversationRecord = new ChatConversationRecord();
                chatConversationRecord.setConversationId(conversationId);
                chatConversationRecord.setMessages(messageArrays);
            }

            // Save updated ChatConversationRecord
            mongoTemplate.save(chatConversationRecord);
            log.info("Replaced history for conversation ID: {} with {} messages.", conversationId, newMessages.size());
        }
        catch (Exception e) {
            log.error("Failed to replace history for conversation ID: {}", conversationId, e);
            throw e;
        }

    }

    @Override
    public Boolean deleteConversationById(String conversationId) {
        try {
            mongoTemplate.remove(new Query(Criteria.where("_id").is(conversationId)), ChatConversationRecord.class);
            log.info("Mongo Deleted conversation with ID: {}", conversationId);
            return true;
        }
        catch (Exception e) {
            log.error("Failed to delete conversation with ID: {}", conversationId, e);
            return false;
        }
    }

    private String[] convertToArray(Message message) {
        return new String[] { message.getRole(), message.getTimestamp(), message.getContent() };
    }

}
