import axios from 'axios';

export const fetchConversations = async () => {
    try {
        const response = await axios.get(`${window.API_BASE_URL}/chat`, {
            params: {uuid: localStorage.getItem('userUUID')}
        });
        return response.data.data.map(conv => ({
            id: conv.conversationId, title: conv.firstMessage
        }));
    } catch (error) {
        console.error('Error fetching conversations:', error);
        return [];
    }
};

export const sendMessageToServer = async (input, conversationId) => {
    try {
        const response = await axios.post(`${window.API_BASE_URL}/chat`, {
            prompt: input, conversationId: conversationId,
        });
        return {sender: 'bot', text: response.data.data, timestamp: new Date()};
    } catch (error) {
        console.error('Error sending message:', error);
        return {sender: 'bot', text: 'Error sending message', timestamp: new Date()};
    }
};

export const fetchMessagesForConversation = async (conversationId) => {
    try {
        const response = await axios.get(`${window.API_BASE_URL}/chat/${conversationId}`);
        return response.data.data.map(msg => ({
            sender: msg.role, text: msg.content, timestamp: msg.timestamp,
        }));
    } catch (error) {
        console.error('Error loading conversation:', error);
        return [];
    }
};


