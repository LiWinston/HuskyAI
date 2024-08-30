import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { okaidia } from 'react-syntax-highlighter/dist/esm/styles/prism';
import './Chat.css';
import DOMPurify from 'dompurify';

function Chat() {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const chatWindowRef = useRef(null);

    const sendMessage = async () => {
        if (input.trim() === '') return;

        const timestamp = new Date().toLocaleTimeString();

        setMessages([...messages, { sender: 'user', text: input, timestamp }]);
        setLoading(true);

        try {
            const response = await axios.post(`${window.API_BASE_URL}/chat`, { prompt: input }, { headers: { 'Content-Type': 'application/json' } });
            const sanitizedResponse = DOMPurify.sanitize(response.data);

            setMessages([...messages, { sender: 'user', text: input, timestamp }, { sender: 'bot', text: sanitizedResponse, timestamp }]);
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

            setMessages([...messages, { sender: 'user', text: input, timestamp }, { sender: 'bot', text: errorMessage, timestamp }]);
            setError(null);
        }

        setInput('');
        setLoading(false);
    };

    useEffect(() => {
        if (chatWindowRef.current) {
            chatWindowRef.current.scrollTo({
                top: chatWindowRef.current.scrollHeight,
                behavior: 'smooth'
            });
        }
    }, [messages]);

    return (
        <div className="chat-container">
            <div className="chat-window" ref={chatWindowRef}>
                {messages.map((msg, index) => (
                    <div key={index} className={`message ${msg.sender}`}>
                        <ReactMarkdown
                            children={DOMPurify.sanitize(msg.text)}
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
                        <div className={`timestamp ${msg.sender}-timestamp`}>{msg.timestamp}</div>
                    </div>
                ))}
            </div>
            <div className="input-container">
                <textarea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && sendMessage()}
                    placeholder="Type your message..."
                    disabled={loading}
                    className={loading ? 'input-disabled' : ''}
                    rows="1"
                    style={{ resize: 'none', overflow: 'auto', maxHeight: '120px' }} // maximum height set to 6 lines
                />
                <button onClick={sendMessage} disabled={loading}>
                    {loading ? 'Sending...' : 'Send'}
                </button>
            </div>
        </div>
    );
}

export default Chat;
