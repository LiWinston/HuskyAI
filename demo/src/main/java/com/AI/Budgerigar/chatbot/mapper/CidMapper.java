package com.AI.Budgerigar.chatbot.mapper;

import com.AI.Budgerigar.chatbot.model.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CidMapper {
    String getSummaryByCid(@Param("uuid") String uuid);
    void setMessageForShort(@Param("cid") String cid, @Param("summary") String summary);
    List<Conversation> getConversationsByUserUuid(@Param("uuid") String uuid);
}
