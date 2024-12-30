package com.AI.Budgerigar.chatbot.Context;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PageContext {
    private static final ThreadLocal<Integer> currentPage = new ThreadLocal<>();
    
    public static void setCurrentPage(Integer page) {
        currentPage.set(page);
        log.debug("设置当前页面: {}", page);
    }
    
    public static Integer getCurrentPage() {
        return currentPage.get();
    }
    
    public static void clear() {
        currentPage.remove();
    }
} 