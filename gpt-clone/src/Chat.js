// src/Chat.js
import React, { useState } from 'react';
import axios from 'axios';
import './Chat.css';

function Chat() {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [error, setError] = useState(null);

    const sendMessage = async () => {
        if (input.trim() === '') return;

        // Add user message to chat history
        setMessages([...messages, { sender: 'user', text: input }]);

        try {
            // Send GET request to backend with prompt parameter
            const response = await axios.get('http://localhost:8080/chat', {
                params: { prompt: input }
            });

            // Add bot response to chat history
            setMessages([...messages, { sender: 'user', text: input }, { sender: 'bot', text: response.data }]);

            // Clear any previous errors
            setError(null);
        } catch (error) {
            console.error('Error response:', error.response);
            console.error('Error message:', error.message);

            // Extract detailed error message
            let errorMessage = 'An unexpected error occurred';
            if (error.response) {
                if (error.response.data && typeof error.response.data === 'object') {
                    // Extract error message from ErrorResponse DTO
                    errorMessage = error.response.data.error + ': ' + error.response.data.message + ' (' + error.response.data.status + ')'  || 'No error message in response';
                } else if (typeof error.response.data === 'string') {
                    // Handle plain text error responses
                    errorMessage = error.response.data;
                } else {
                    errorMessage = 'Error response received but no message available';
                }
            } else if (error.message) {
                errorMessage = error.message + ' (no response received)';
            }

            // Add error message to chat history
            setMessages([
                ...messages,
                { sender: 'user', text: input },
                { sender: 'bot', text: errorMessage }
            ]);

            // Set error state to prevent persistent error display
            setError(null);
        }

        setInput('');
    };

    return (
        <div className="chat-container">
            <div className="chat-window">
                {messages.map((msg, index) => (
                    <div key={index} className={`message ${msg.sender}`}>
                        {msg.text}
                    </div>
                ))}
            </div>
            <div className="input-container">
                <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
                    placeholder="Type your message..."
                />
                <button onClick={sendMessage}>Send</button>
            </div>
        </div>
    );
}

export default Chat;
