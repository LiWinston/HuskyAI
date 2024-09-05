package com.AI.Budgerigar.chatbot.mapper;

import com.AI.Budgerigar.chatbot.model.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMapper {

    Boolean checkConversationExistsByUuid(@Param("uuid") String uuid, @Param("conversationId") String conversationId);

    int createConversationForUuid(@Param("uuid") String uuid, @Param("conversationId") String conversationId);

    String getSummaryByCid(@Param("uuid") String uuid);

    void setMessageForShort(@Param("cid") String cid, @Param("summary") String summary);

    List<Conversation> getConversationsByUserUuid(@Param("conversationId") String conversationId);

}
