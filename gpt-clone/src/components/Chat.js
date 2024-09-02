import React, { useState, useEffect } from 'react';
import '../Chat.css';
import { fetchConversations, sendMessageToServer, fetchMessagesForConversation } from '../services/chatService';
import ConversationList from './ConversationList';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import NotificationBanner from './NotificationBanner';
import useChat from '../hooks/useChat';

function Chat() {
    const { conversations, selectedConversation, setSelectedConversation, messages, setMessages, notification, setNotification } = useChat();

    useEffect(() => {
        fetchConversations().then(convs => setMessages(convs));
    }, []);

    const handleSendMessage = async (input) => {
        const newMessage = await sendMessageToServer(input, selectedConversation);
        setMessages(prevMessages => [...prevMessages, newMessage]);
        if (!selectedConversation) {
            setSelectedConversation(newMessage.conversationId);
            fetchConversations();
        }
    };

    const handleSelectConversation = async (conversationId) => {
        const loadedMessages = await fetchMessagesForConversation(conversationId);
        setMessages(loadedMessages);
        setSelectedConversation(conversationId);
    };

    return (
        <div className="chat-interface">
            <ConversationList
                conversations={conversations}
                onSelectConversation={handleSelectConversation}
                selectedConversation={selectedConversation}
            />
            <div className="chat-container">
                <MessageList messages={messages} />
                <MessageInput onSendMessage={handleSendMessage} />
            </div>
            {notification && <NotificationBanner message={notification} />}
        </div>
    );
}

export default Chat;
