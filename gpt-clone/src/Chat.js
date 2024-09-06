import React, {useEffect, useRef, useState} from 'react';
import axios from 'axios';
import ReactMarkdown from 'react-markdown';
import {Prism as SyntaxHighlighter} from 'react-syntax-highlighter';
import DOMPurify from 'dompurify';
import {AnimatePresence, motion} from 'framer-motion';
import './Chat.css';
import VscDarkPlus from "react-syntax-highlighter/src/styles/prism/vsc-dark-plus";
import {getWindowFromNode} from "@testing-library/dom/dist/helpers";

const CONVERSATION_SUMMARY_GENERATED = "#CVSG##CVSG##CVSG#";

function Chat() {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [rows, setRows] = useState(1); // 新增行数状态
    const [loading, setLoading] = useState(false);
    const [conversations, setConversations] = useState([]); // Ensure initial state is an array
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [notification, setNotification] = useState(null);
    const chatWindowRef = useRef(null);
    const [textareaHeight, setTextareaHeight] = useState('auto');
    const textareaRef = useRef(null);
    const [displayedTitle, setDisplayedTitle] = useState({});

    const handleInputChange = (event) => {
        const text = event.target.value;
        setInput(text);
        adjustTextareaHeight();
    };

    const adjustTextareaHeight = () => {
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto'; // 重置高度
            let scrollHeight = textareaRef.current.scrollHeight; // 获取实际内容高度

            // 确保高度不会超过定义的最大高度
            const maxHeight = parseInt(getWindowFromNode(textareaRef.current).getComputedStyle(textareaRef.current).maxHeight);
            if (scrollHeight > maxHeight) {
                scrollHeight = maxHeight;
            }

            textareaRef.current.style.height = `${scrollHeight}px`;
            setTextareaHeight(`${scrollHeight}px`);
        }
    };

    useEffect(() => {
        adjustTextareaHeight();
    }, [input]);

    useEffect(() => {
        fetchConversations().then(r => console.log('Conversations fetched:', r));
    }, []);

    const fetchConversations = async () => {
        try {
            const response = await axios.get(`${window.API_BASE_URL}/chat`, {
                params: {uuid: localStorage.getItem('userUUID')}
            });
            setConversations(response.data.data.map(conv => ({
                id: conv.conversationId, title: conv.firstMessage
            })));
        } catch (error) {
            console.error('Error fetching conversations:', error);
        }
    };


    const loadConversation = async (conversationId) => {
        try {
            const uuid = localStorage.getItem('userUUID');
            console.log('Loading conversation:', conversationId);
            const response = await axios.get(`${window.API_BASE_URL}/chat/${uuid}/${conversationId}`);

            // 如果 response.data.data 为空，则处理为空的情况
            const loadedMessages = response.data.data.length ? response.data.data.map(msg => ({
                sender: msg.role,
                text: msg.content,
                timestamp: msg.timestamp
            })) : [];

            setMessages(loadedMessages);
            setSelectedConversation(conversationId);

            // 如果没有消息，添加一条默认提示消息
            if (loadedMessages.length === 0) {
                setMessages([{
                    sender: 'system',
                    text: 'No messages yet in this conversation. Start the conversation now!',
                    timestamp: new Date()
                }]);
            }

        } catch (error) {
            console.error('Error loading conversation:', error);
            // 如果加载出错，仍然显示错误提示
            setMessages([{
                sender: 'system',
                text: 'Failed to load conversation. Please try again later.',
                timestamp: new Date()
            }]);
        }
    };


    const sendMessage = async () => {
        if (input.trim() === '') return;

        const timestamp = new Date();
        const newMessage = { sender: 'user', text: input, timestamp };

        setMessages(prevMessages => [...prevMessages, newMessage]);
        setInput('');
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            setTextareaHeight('auto');
        }
        setLoading(true);

        try {
            let cid = selectedConversation;
            if (!cid) {
                // 如果没有选择对话，则创建一个新对话, generate a new conversation ID
                cid = new Date().getTime().toString();
                setSelectedConversation(cid);
                await axios.get(`${window.API_BASE_URL}/chat/${localStorage.getItem('userUUID')}/${cid}`);
            }
            const response = await axios.post(`${window.API_BASE_URL}/chat/${localStorage.getItem('userUUID')}/${cid}`, {
                prompt: input, conversationId: selectedConversation
            });
            const sanitizedResponse = DOMPurify.sanitize(response.data.data);
            const assistantMessage = { sender: 'assistant', text: sanitizedResponse, timestamp: new Date() };

            setMessages(prevMessages => [...prevMessages, assistantMessage]);
            setLoading(false);

            if (response.data.msg) {
                const msg = response.data.msg;
                if (msg.includes(CONVERSATION_SUMMARY_GENERATED)) {
                    const newTitle = msg.split(CONVERSATION_SUMMARY_GENERATED)[1];
                    // 使用打字机效果更新对话标题
                    animateTitleUpdate(selectedConversation, newTitle);
                    setNotification(response.data.msg.split(CONVERSATION_SUMMARY_GENERATED)[0] + ', Conversation summary generated, ' + newTitle);
                    setTimeout(() => setNotification(null), 3500);
                } else {
                    setNotification(response.data.msg);
                    setTimeout(() => setNotification(null), 2000);
                }
            }

            if (!selectedConversation) {
                // setSelectedConversation(response.data.data.conversationId);
                fetchConversations();
            }
        } catch (error) {
            console.error('Error sending message:', error);
            setLoading(false);
        }
    };

    const animateTitleUpdate = (conversationId, newTitle) => {
        let currentTitle = '';
        setDisplayedTitle(prev => ({ ...prev, [conversationId]: '' }));

        newTitle.split('').forEach((char, index) => {
            setTimeout(() => {
                currentTitle += char;
                setDisplayedTitle(prev => ({ ...prev, [conversationId]: currentTitle }));
            }, index * 100); // 每个字符间隔100毫秒
        });
    };


    useEffect(() => {
        if (chatWindowRef.current) {
            chatWindowRef.current.scrollTo({
                top: chatWindowRef.current.scrollHeight, behavior: 'smooth'
            });
        }
    }, [messages]);

    return (<div className="chat-interface">
        <div className="conversation-list">
            <h3>Conversations</h3>
            {Array.isArray(conversations) && conversations.map((conv) => (
                <div
                    key={conv.id}
                    className={`conversation-item ${selectedConversation === conv.id ? 'selected' : ''}`}
                    onClick={() => loadConversation(conv.id)}
                >
                    {displayedTitle[conv.id] || conv.title}
                </div>
            ))}
        </div>
        <div className="chat-container">
            <div className="chat-window" ref={chatWindowRef}>
                <AnimatePresence>
                    {messages.map((msg, index) => {
                        const messageDate = new Date(msg.timestamp);
                        const currentDate = new Date();
                        const isRecent = (currentDate - messageDate) < (24 * 60 * 60 * 1000); // 判断是否为最近一天的消息

                        // 判断是否是一天内的第一条消息
                        const isFirstRecentMessage = isRecent && (index === 0 || (new Date(messages[index - 1].timestamp) < (currentDate - 24 * 60 * 60 * 1000)));
                        return (
                            <React.Fragment key={index}>
                                {isFirstRecentMessage && (
                                    <div className="day-divider">
                                        <div className="divider-line"></div>
                                        <div className="divider-text">Messages within the last day</div>
                                        <div className="divider-line"></div>
                                    </div>
                                )}
                                <motion.div
                                    className={`message-container ${msg.sender} ${isRecent ? 'recent-message' : ''}`}
                                    initial={{opacity: 0, y: 20}}
                                    animate={{opacity: 1, y: 0}}
                                    exit={{opacity: 0, y: -20}}
                                    transition={{duration: 0.3}}
                                >
                                    <div className={`message ${msg.sender}`}>
                                        <ReactMarkdown
                                            children={DOMPurify.sanitize(msg.text)}
                                            components={{
                                                code({node, inline, className, children, ...props}) {
                                                    const match = /language-(\w+)/.exec(className || '');
                                                    return !inline ? (
                                                        <SyntaxHighlighter
                                                            style={VscDarkPlus}
                                                            language={match ? match[1] : 'plaintext'}
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
                                    <div className={`timestamp ${msg.sender}-timestamp`}>
                                        {new Date(msg.timestamp).toLocaleString(navigator.language, {
                                            year: 'numeric',
                                            month: navigator.language.startsWith('zh') ? 'long' : 'short',
                                            day: 'numeric',
                                            hour: 'numeric',
                                            minute: 'numeric',
                                            second: 'numeric',
                                            dayPeriod: 'short',
                                        })}
                                    </div>
                                </motion.div>
                            </React.Fragment>
                        );
                    })}

                </AnimatePresence>
            </div>

            <div className="input-container">
                    <textarea
                        ref={textareaRef}
                        value={input}
                        onChange={handleInputChange}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && !e.shiftKey) {
                                e.preventDefault();
                                sendMessage();
                            }
                        }} placeholder="Type your message..."
                        disabled={loading}
                        // rows={rows} // 根据输入内容动态调整行数
                        style={{height: textareaHeight}} // 动态调整高度
                    />
                <button onClick={sendMessage} disabled={loading}>
                    {loading ? 'Sending...' : 'Send'}
                </button>
            </div>
        </div>
        {/* 浮动弹幕通知 */}
        <AnimatePresence>
            {notification && (<motion.div
                className="notification-banner"
                initial={{opacity: 0, y: 50}}
                animate={{opacity: 1, y: 0}}
                exit={{opacity: 0, y: 50}}
                transition={{duration: 0.5}}
            >{notification}
            </motion.div>)}
        </AnimatePresence>

    </div>);
}

export default Chat;