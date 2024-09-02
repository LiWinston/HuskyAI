import { useState } from 'react';

function useChat() {
    const [conversations, setConversations] = useState([]);
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [messages, setMessages] = useState([]);
    const [notification, setNotification] = useState(null);

    return {
        conversations,
        setConversations,
        selectedConversation,
        setSelectedConversation,
        messages,
        setMessages,
        notification,
        setNotification,
    };
}

export default useChat;
