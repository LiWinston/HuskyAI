// ConversationHistory.js
import React, {useEffect, useState} from 'react';
import axiosInstance from '../api/axiosConfig';

function ConversationHistory({userId}) {
    const [conversations, setConversations] = useState([]);
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [messages, setMessages] = useState([]);

    useEffect(() => {
        const fetchConversations = async () => {
            try {
                const response = await axiosInstance.get(`/chat/${userId}`);
                setConversations(response.data);
            } catch (error) {
                console.error('Error fetching conversations:', error);
            }
        };

        if (userId) {
            fetchConversations();
        }
    }, [userId]);

    const loadConversation = async (conversationId) => {
        try {
            const response = await axiosInstance.get(`/chat/${userId}/${conversationId}`);
            setMessages(response.data);
            setSelectedConversation(conversationId);
        } catch (error) {
            console.error('Error loading conversation:', error);
        }
    };

    const fetchMessages = async (conversationId) => {
        try {
            const response = await axiosInstance.get(`/chat/${userId}/${conversationId}`);
            setMessages(response.data);
        } catch (error) {
            console.error('Error fetching messages:', error);
        }
    };

    return (
        <div>
            <h4>Conversations</h4>
            <ul>
                {conversations.map(conv => (
                    <li key={conv.id} onClick={() => loadConversation(conv.id)}>
                        {conv.title}
                    </li>
                ))}
            </ul>
            {selectedConversation && (
                <div>
                    <h4>Messages</h4>
                    <ul>
                        {messages.map((msg, idx) => (
                            <li key={idx}>
                                <strong>{msg.role}</strong>: {msg.content}
                                <em>{msg.timestamp}</em>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
}

export default ConversationHistory;
