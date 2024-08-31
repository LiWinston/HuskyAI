package com.AI.Budgerigar.chatbot.mapper;

import com.AI.Budgerigar.chatbot.model.Cid;
import com.AI.Budgerigar.chatbot.model.UserPw;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    UserPw getUserByUuid(@Param("uuid") String uuid);
    List<Cid> getConversationsByUserUuid(@Param("uuid") String uuid);
}