package com.AI.Budgerigar.chatbot.DTO;

import lombok.Data;

/**
 * 分页参数DTO
 */
@Data
public class PageDTO {
    /** 当前页码 */
    private Integer current = 1;
    /** 每页数量 */
    private Integer size = 20;
} 