import React, { useState } from 'react';
import axios from 'axios';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { okaidia } from 'react-syntax-highlighter/dist/esm/styles/prism';
import './Chat.css';

function Chat() {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    const sendMessage = async () => {
        if (input.trim() === '') return;

        setMessages([...messages, { sender: 'user', text: input }]);
        setLoading(true); // Set loading state to true

        try {
            const response = await axios.get('http://localhost:8080/chat', {
                params: { prompt: input }
            });

            setMessages([...messages, { sender: 'user', text: input }, { sender: 'bot', text: response.data }]);
            setError(null);
        } catch (error) {
            let errorMessage = 'An unexpected error occurred';
            if (error.response) {
                if (error.response.data && typeof error.response.data === 'object') {
                    errorMessage = error.response.data.error + ': ' + error.response.data.message + ' (' + error.response.data.status + ')' || 'No error message in response';
                } else if (typeof error.response.data === 'string') {
                    errorMessage = error.response.data;
                } else {
                    errorMessage = 'Error response received but no message available';
                }
            } else if (error.message) {
                errorMessage = error.message + ' (no response received)';
            }

            setMessages([...messages, { sender: 'user', text: input }, { sender: 'bot', text: errorMessage }]);
            setError(null);
        }

        setInput('');
        setLoading(false); // Reset loading state to false
    };

    return (
        <div className="chat-container">
            <div className="chat-window">
                {messages.map((msg, index) => (
                    <div key={index} className={`message ${msg.sender}`}>
                        <ReactMarkdown
                            children={msg.text}
                            components={{
                                code({ node, inline, className, children, ...props }) {
                                    const match = /language-(\w+)/.exec(className || '');
                                    return !inline && match ? (
                                        <SyntaxHighlighter
                                            style={okaidia}
                                            language={match[1]}
                                            PreTag="div"
                                            children={String(children).replace(/\n$/, '')}
                                            {...props}
                                        />
                                    ) : (
                                        <code className={className} {...props}>
                                            {children}
                                        </code>
                                    );
                                }
                            }}
                        />
                    </div>
                ))}
            </div>
            <div className="input-container">
                <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
                    placeholder="Type your message..."
                    disabled={loading} // Disable input when loading
                    className={loading ? 'input-disabled' : ''} // Apply class when loading
                />
                <button onClick={sendMessage} disabled={loading}>
                    {loading ? 'Sending...' : 'Send'} {/* Show loading text when loading */}
                </button>
            </div>
        </div>
    );
}

export default Chat;
