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
            console.log('Loading conversation:', conversationId);
            const response = await axios.get(`${window.API_BASE_URL}/chat/${conversationId}`);

            // 直接使用返回的消息对象
            const loadedMessages = response.data.data.map(msg => ({
                sender: msg.role, text: msg.content, timestamp: msg.timestamp
            }));

            setMessages(loadedMessages);
            setSelectedConversation(conversationId);
        } catch (error) {
            console.error('Error loading conversation:', error);
        }
    };

    const sendMessage = async () => {
        if (input.trim() === '') return;

        const timestamp = new Date().toLocaleTimeString();
        const newMessage = {sender: 'user', text: input, timestamp};

        setMessages(prevMessages => [...prevMessages, newMessage]);
        setInput('');
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            setTextareaHeight('auto');
        }
        setLoading(true);

        try {
            const response = await axios.post(`${window.API_BASE_URL}/chat`, {
                prompt: input, conversationId: selectedConversation
            });
            const sanitizedResponse = DOMPurify.sanitize(response.data.data);
            const botMessage = {sender: 'bot', text: sanitizedResponse, timestamp: new Date().toLocaleTimeString()};

            setMessages(prevMessages => [...prevMessages, botMessage]);
            setLoading(false);

            if (response.data.msg) {
                const msg = response.data.msg;
                if (msg.includes(CONVERSATION_SUMMARY_GENERATED)) {
                    const newTitle = msg.split(CONVERSATION_SUMMARY_GENERATED)[1];
                    // 更新对话标题
                    setConversations(prevConversations =>
                        prevConversations.map(conv =>
                            conv.id === selectedConversation ? {...conv, title: newTitle} : conv
                        )
                    );
                    setNotification(response.data.msg.split(CONVERSATION_SUMMARY_GENERATED)[0] + ', Conversation summary generated, ' + newTitle);
                    setTimeout(() => setNotification(null), 3500);
                }else{
                    setNotification(response.data.msg); // 设置通知消息
                    setTimeout(() => setNotification(null), 2000);
                }
            }

            if (!selectedConversation) {
                setSelectedConversation(response.data.data.conversationId);
                fetchConversations();
            }
        } catch (error) {
            console.error('Error sending message:', error);
            setLoading(false);
        }
    };

    useEffect(() => {
        if (chatWindowRef.current) {
            chatWindowRef.current.scrollTo({
                top: chatWindowRef.current.scrollHeight, behavior: 'smooth'
            });
        }
    }, [messages]);

    return (
        <div className="chat-interface">
            <div className="conversation-list">
                <h3>Conversations</h3>
                {Array.isArray(conversations) && conversations.map((conv) => (<div
                        key={conv.id}
                        className={`conversation-item ${selectedConversation === conv.id ? 'selected' : ''}`}
                        onClick={() => loadConversation(conv.id)}
                    >
                        {conv.title}
                    </div>))}
            </div>
            <div className="chat-container">
                <div className="chat-window" ref={chatWindowRef}>
                    <AnimatePresence>
                        {messages.map((msg, index) => (<motion.div
                                key={index}
                                className={`message-container ${msg.sender}`}
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
                                                // const detectedLanguage = detectLanguage(String(children));
                                                return !inline ? (<SyntaxHighlighter
                                                        style={VscDarkPlus}
                                                        language={match ? match[1] : 'plaintext'}
                                                        PreTag="div"
                                                        children={String(children).replace(/\n$/, '')}
                                                        {...props}
                                                    />) : (<code className={className} {...props}>
                                                        {children}
                                                    </code>);
                                            }
                                        }}
                                    />
                                </div>
                                <div className={`timestamp ${msg.sender}-timestamp`}>{msg.timestamp}</div>
                            </motion.div>))}
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
                        }}                        placeholder="Type your message..."
                        disabled={loading}
                        // rows={rows} // 根据输入内容动态调整行数
                        style={{ height: textareaHeight }} // 动态调整高度
                    />
                    <button onClick={sendMessage} disabled={loading}>
                        {loading ? 'Sending...' : 'Send'}
                    </button>
                </div>
            </div>
            {/* 浮动弹幕通知 */}
            <AnimatePresence>
                {notification && (
                    <motion.div
                        className="notification-banner"
                        initial={{ opacity: 0, y: 50 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: 50 }}
                        transition={{ duration: 0.5 }}
                    >{notification}
                    </motion.div>
                )}
            </AnimatePresence>

        </div>);
}

function detectLanguage(code) {
    // 移除注释，以避免干扰检测
    // code = code.replace(/\/\/.*|\/\*[\s\S]*?\*\//g, '');

    const languageRules = [{
        name: 'javascript',
        rules: [{pattern: /\b(const|let|var)\s+\w+\s*=/, score: 2}, {
            pattern: /function\s+\w+\s*\(.*\)\s*{/,
            score: 2
        }, {pattern: /=>\s*{/, score: 2}, {
            pattern: /console\.(log|error|warn|info)\(/,
            score: 2
        }, {pattern: /document\.getElementById\(/, score: 3}, {
            pattern: /\}\s*else\s*{/,
            score: 1
        }, {pattern: /new Promise\(/, score: 3}, {pattern: /async\s+function/, score: 3}, {
            pattern: /\bawait\b/,
            score: 3
        }, {
            pattern: /import\s+.*\s+from\s+['"]/,
            score: 3
        }, {
            pattern: /export\s+(default\s+)?(function|class|const|let|var)/,
            score: 3
        }, {pattern: /\.[0-9]+e[+-]?[0-9]+/, score: 1}]
    }, {
        name: 'python',
        rules: [{pattern: /def\s+\w+\s*\(.*\):\s*$/, score: 2}, {
            pattern: /class\s+\w+(\(\w+\))?:\s*$/,
            score: 2
        }, {pattern: /import\s+\w+(\s+as\s+\w+)?/, score: 2}, {
            pattern: /from\s+\w+\s+import\s+/,
            score: 2
        }, {pattern: /print\s*\(/, score: 1}, {
            pattern: /if\s+__name__\s*==\s*('|")__main__\1:/,
            score: 3
        }, {pattern: /^\s*@\w+/, score: 2}, {pattern: /\bfor\s+\w+\s+in\s+/, score: 2}, {
            pattern: /\bwith\s+.*\s+as\s+/,
            score: 3
        }, {pattern: /\blambda\s+.*:/, score: 3}, {pattern: /:\s*$/, score: 1}, {pattern: /\s{4}/, score: 1}]
    }, {
        name: 'java',
        rules: [{
            pattern: /public\s+(static\s+)?(final\s+)?(class|interface|enum)\s+\w+/,
            score: 3
        }, {pattern: /public\s+static\s+void\s+main\s*\(String\s*\[\]\s*args\)/, score: 5}, {
            pattern: /@Override/,
            score: 2
        }, {pattern: /System\.out\.println\(/, score: 2}, {
            pattern: /import\s+java\./,
            score: 2
        }, {pattern: /new\s+\w+(\s*<.*>)?\s*\(/, score: 1}, {
            pattern: /\b(public|private|protected)\s+/,
            score: 1
        }, {pattern: /\b(final|static)\s+/, score: 1}, {
            pattern: /\b(try|catch|finally)\s*{/,
            score: 2
        }, {pattern: /\bthrows\s+\w+/, score: 2}, {
            pattern: /\bextends\s+\w+/,
            score: 2
        }, {pattern: /\bimplements\s+\w+/, score: 2}]
    }, {
        name: 'cpp',
        rules: [{pattern: /#include\s*<[\w.]+>/, score: 3}, {
            pattern: /std::\w+/,
            score: 2
        }, {pattern: /int\s+main\s*\(\s*(int|void)/, score: 4}, {
            pattern: /\b(class|struct)\s+\w+/,
            score: 2
        }, {pattern: /\b(public|private|protected):/, score: 2}, {
            pattern: /\b(const|virtual|friend|typename)\b/,
            score: 2
        }, {pattern: /\b(new|delete)\b/, score: 2}, {
            pattern: /\b(template|namespace)\b/,
            score: 3
        }, {pattern: /\bcout\s*<</, score: 2}, {pattern: /\bcin\s*>>/, score: 2}, {pattern: /::\s*\w+/, score: 2}]
    }, {
        name: 'csharp',
        rules: [{pattern: /using\s+System;/, score: 3}, {pattern: /namespace\s+\w+/, score: 2}, {
            pattern: /class\s+\w+/,
            score: 2
        }, {pattern: /Console\.WriteLine\(/, score: 2}, {
            pattern: /public\s+(static\s+)?void\s+Main\s*\(/,
            score: 4
        }, {
            pattern: /\b(public|private|protected|internal)\s+/,
            score: 1
        }, {pattern: /\b(var|string|int|bool)\s+\w+\s*=/, score: 2}, {
            pattern: /\busing\s*\(/,
            score: 2
        }, {pattern: /\basync\s+Task/, score: 3}, {
            pattern: /\bawait\s+/,
            score: 3
        }, {pattern: /\b(List|Dictionary)<.*>/, score: 2}]
    }, {
        name: 'php',
        rules: [{pattern: /<\?php/, score: 5}, {pattern: /\$\w+/, score: 2}, {
            pattern: /echo\s+/,
            score: 2
        }, {pattern: /function\s+\w+\s*\(/, score: 2}, {
            pattern: /\b(public|private|protected)\s+function/,
            score: 3
        }, {pattern: /\bclass\s+\w+/, score: 2}, {pattern: /\bnamespace\s+\w+/, score: 3}, {
            pattern: /\buse\s+\w+/,
            score: 2
        }, {pattern: /\b(include|require)(_once)?\s*\(/, score: 2}, {
            pattern: /\b(foreach|as)\b/,
            score: 2
        }, {pattern: /\b->\w+/, score: 2}]
    }, {
        name: 'ruby',
        rules: [{pattern: /def\s+\w+/, score: 2}, {pattern: /class\s+\w+/, score: 2}, {
            pattern: /module\s+\w+/,
            score: 2
        }, {pattern: /\battr_(reader|writer|accessor)\b/, score: 3}, {
            pattern: /\bputs\b/,
            score: 1
        }, {pattern: /\b(if|unless|while|until)\b/, score: 1}, {
            pattern: /\bdo\s*\|.*\|/,
            score: 2
        }, {pattern: /\bend\b/, score: 1}, {
            pattern: /\b(require|include)\b/,
            score: 2
        }, {pattern: /\b(true|false|nil)\b/, score: 1}, {pattern: /:\w+/, score: 2}]
    }, {
        name: 'go',
        rules: [{pattern: /package\s+main/, score: 3}, {
            pattern: /func\s+main\(\)/,
            score: 4
        }, {pattern: /import\s+\([\s\S]*?\)/, score: 3}, {
            pattern: /func\s+\(\w+\s+\*?\w+\)\s+\w+/,
            score: 3
        }, {pattern: /fmt\.(Print|Println|Printf)\(/, score: 2}, {
            pattern: /\bvar\s+\w+\s+\w+/,
            score: 2
        }, {pattern: /\b:=\b/, score: 2}, {pattern: /\bgo\s+func\b/, score: 3}, {
            pattern: /\bchan\b/,
            score: 2
        }, {pattern: /\bdefer\b/, score: 2}, {pattern: /\binterface\{\}/, score: 2}]
    }];

    const scores = {};
    languageRules.forEach(lang => {
        scores[lang.name] = 0;
        lang.rules.forEach(rule => {
            const matches = (code.match(rule.pattern) || []).length;
            scores[lang.name] += matches * rule.score;
        });
    });

    // 根据得分确定最可能的语言
    const detectedLanguage = Object.keys(scores).reduce((a, b) => scores[a] > scores[b] ? a : b);
    const confidence = scores[detectedLanguage] / Object.values(scores).reduce((a, b) => a + b, 0);

    return detectedLanguage;  // 只返回检测到的语言名称，不返回置信度
}

export default Chat;