import React, {useEffect, useRef, useState} from 'react';
import axios from 'axios';
import ReactMarkdown from 'react-markdown';
import {Prism as SyntaxHighlighter} from 'react-syntax-highlighter';
import {AnimatePresence, motion} from 'framer-motion';
import './Chat.css';
import {vscDarkPlus} from 'react-syntax-highlighter/dist/esm/styles/prism';
import {FaPlus} from 'react-icons/fa';
import ConversationItem from './ConversationItem'; // 引入加号图标
import './Component/Toggle.css';
import {showSweetAlertWithRetVal} from './Component/sweetAlertUtil';

const CONVERSATION_SUMMARY_GENERATED = '#CVSG##CVSG##CVSG#';

function Chat() {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    // eslint-disable-next-line no-unused-vars
    const [rows, setRows] = useState(1); // 新增行数状态
    const [loading, setLoading] = useState(false);
    const [conversations, setConversations] = useState([]); // Ensure initial state is an array
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [selectedModel, setSelectedModel] = useState(''); // 当前选中的模型
    const [models, setModels] = useState([]); // List of models
    const [showModelOptions, setShowModelOptions] = useState(false); //
    const [useStream, setUseStream] = useState(false);
    // 当 selectedConversation 改变时，自动更新到 localStorage
    useEffect(() => {
        if (selectedConversation !== null) {
            localStorage.setItem('selectedConversation', selectedConversation);
        } else {
            localStorage.removeItem('selectedConversation');
        }
    }, [selectedConversation]);
    const [notification, setNotification] = useState(null);
    const chatWindowRef = useRef(null);
    const [textareaHeight, setTextareaHeight] = useState('auto');
    const textareaRef = useRef(null);
    // eslint-disable-next-line no-unused-vars
    const [animatingTitle, setAnimatingTitle] = useState(null);
    const [showShareModal, setShowShareModal] = useState(false); // 控制分享弹窗
    const [selectedMessages, setSelectedMessages] = useState([]); // 选择分享的消息
    const [shareMessages, setShareMessages] = useState([]); // 存储分享的消息
    const [sharedCid, setSharedCid] = useState(null);
    const [streamingMessage, setStreamingMessage] = useState(null);

    const handleInputChange = (event) => {
        const text = event.target.value;
        setInput(text);
        adjustTextareaHeight();
    };

    const adjustTextareaHeight = () => {
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto'; // 重置高度
            const scrollHeight = textareaRef.current.scrollHeight; // 获取实际内容高度
            const maxHeight = parseInt(getComputedStyle(textareaRef.current).maxHeight, 10);

            // 如果高度达到最大限度，允许滚动
            if (scrollHeight > maxHeight) {
                textareaRef.current.style.height = `${maxHeight}px`;
                textareaRef.current.style.overflowY = 'auto'; // 允许滚动
            } else {
                textareaRef.current.style.height = `${scrollHeight}px`;
                textareaRef.current.style.overflowY = 'hidden'; // 禁止滚动
            }

            setTextareaHeight(`${textareaRef.current.style.height}px`);
        }
    };


    useEffect(() => {
        adjustTextareaHeight();
    }, [input]);

    const fetchModels = async () => {
        try {
            const response = await axios.post(`${window.API_BASE_URL}/chat/models`, {
                    uuid: localStorage.getItem('userUUID'),
                },
            );
            if (response.data.code === 1) {
                setModels(response.data.data); // Set models from response
                if ((selectedModel === '' && response.data.data.length > 0)
                    || !response.data.data.includes(selectedModel)
                ) {
                    setSelectedModel(response.data.data[0]);
                }
            } else if (response.data.code === 0 && response.data.data) {
                setModels(response.data.data);
                if ((selectedModel === '' && response.data.data.length > 0)
                    || !response.data.data.includes(selectedModel)
                ) {
                    setSelectedModel(response.data.data[0]);
                }
                setNotification('Fail to update models, list remains the same');
                setTimeout(() => setNotification(null), 1000);
            } else {
                console.error('Error fetching models:', response.data.msg);
            }
        } catch (error) {
            console.error('Failed to fetch models:', error);
        }
    };

    useEffect(() => {
        const fetchAndLoadConversation = async () => {
            try {
                const userUUID = localStorage.getItem('userUUID');
                setNotification('Fetching conversations...'); // 设置通知
                console.log('Fetching conversations for user:', userUUID); // 打印用户UUID

                console.log(`${window.API_BASE_URL}/chat`, {uuid: userUUID});
                await fetchConversations(); // 等待对话列表获取完成
                setNotification(null); // 清除通知
            } catch (e) {
                console.error('Error loading conversation:', e);
            }
        };

        window.API_BASE_URL = localStorage.getItem('API_BASE_URL');

        fetchModels().then(r => console.log(r)).catch(e => console.error('Error after fetchModels:', e));

        fetchAndLoadConversation().then(() => console.log('Conversations fetched')).catch(e => console.error('Error after fetchAndLoadConversation:', e));
    }, []);

    const modelSelectorRef = useRef(null);  // 用于引用整个模型选择器的 ref
    // 点击页面其他地方隐藏模型选项的逻辑
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (modelSelectorRef.current &&
                !modelSelectorRef.current.contains(event.target)) {
                setShowModelOptions(false);
            }
        };

        // 绑定全局点击事件
        document.addEventListener('mousedown', handleClickOutside);

        return () => {
            // 清除全局点击事件监听器
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    // 修改初始化加载对话的 useEffect
    useEffect(() => {
        const loadInitialConversation = async () => {
            if (conversations.length > 0) {
                const storedConversationId = localStorage.getItem(
                    'selectedConversation');
                const conversationToLoad = conversations.find(
                    conv => conv.id === storedConversationId) || conversations[0];
                await loadConversation(conversationToLoad.id);
            } else {
                setSelectedConversation(null);
                setMessages([]);
            }
        };

        loadInitialConversation();
    }, [conversations]);

    const fetchConversations = async () => {
        try {
            const response = await axios.get(`${window.API_BASE_URL}/chat`, {
                params: {uuid: localStorage.getItem('userUUID')},
            });
            const conversationsData = response.data.data.map(conv => ({
                id: conv.conversationId,
                title: conv.firstMessage,
                timestampCreat: conv.createdAt,
                timestampLast: conv.lastMessageAt,
            }));

            setConversations(conversationsData);

            return conversationsData;
        } catch (error) {
            console.error('Error fetching conversations:', error);
            throw error;
        }
    };

    const loadConversation = async (conversationId) => {
        try {
            const uuid = localStorage.getItem('userUUID');
            const response = await axios.get(
                `${window.API_BASE_URL}/chat/${uuid}/${conversationId}`);

            const loadedMessages = response.data.data.length
                ? response.data.data.map(msg => ({
                    sender: msg.role,
                    text: msg.content,
                    timestamp: msg.timestamp,
                }))
                : [
                    {
                        sender: 'system',
                        text: 'No messages yet in this conversation. Start the conversation now!',
                        timestamp: new Date(),
                    }];

            setMessages(loadedMessages);
            setSelectedConversation(conversationId);
            localStorage.setItem('selectedConversation', conversationId);
        } catch (error) {
            console.error('Error loading conversation:', error);
            setMessages([
                {
                    sender: 'system',
                    text: 'Failed to load conversation. Please try again later.',
                    timestamp: new Date(),
                }]);
        }
    };

    // 新增函数用于创建新对话
    const createNewConversation = async () => {
        try {
            const uuid = localStorage.getItem('userUUID');
            const newConversationId = new Date().getTime().toString();
            const newConversation = {
                id: newConversationId,
                title: 'New Conversation',
                timestampCreat: new Date(),
                timestampLast: new Date(),
            };
            setConversations(
                prevConversations => [newConversation, ...prevConversations]);
            await loadConversation(newConversationId);
            // await axios.get(
            //     `${window.API_BASE_URL}/chat/${uuid}/${newConversationId}`);
        } catch (error) {
            console.error('Failed to create new conversation', error);
        }
    };

    const openShareModal = async (conversationId) => {
        try {
            // const uuid = localStorage.getItem('userUUID');
            // const response = await axios.get(`${window.API_BASE_URL}/chat/${uuid}/${conversationId}`);
            // setShareMessages(response.data.data);  // 设置分享消息
            setShowShareModal(true);  // 显示分享 modal
        } catch (error) {
            console.error('Error syncing conversation history', error);
        }
    };

    const handleSelectMessage = (messageId) => {
        setSelectedMessages(prev => {
            if (prev.includes(messageId)) {
                return prev.filter(id => id !== messageId); // 取消选择
            } else {
                return [...prev, messageId];  // 添加选择
            }
        });
    };

    const handleShareConfirm = async () => {
        try {
            const uuid = localStorage.getItem('userUUID');
            const response = await axios.post(`${window.API_BASE_URL}/chat/share`, {
                uuid, conversationId: sharedCid, messageIndexes: selectedMessages,
            });
            const shareLink = window.location.origin + '/chat/share/' +
                response.data.data;
            setShowShareModal(false);  // 关闭分享弹窗
            // alert(`Share link generated: ${shareLink}`);  // 显示分享链接
            showSweetAlertWithRetVal(`Share link generated: ${shareLink}`, {
                title: 'Share Link',
                icon: 'success',
                confirmButtonText: 'Copy Link',
                confirmButtonColor: '#3085d6',
            }).then((result) => {
                if (result.isConfirmed) {
                    navigator.clipboard.writeText(shareLink);
                    setNotification('Link copied to clipboard');
                    setTimeout(() => setNotification(null), 2000);
                }
            });

        } catch (error) {
            console.error('Error sharing conversation', error);
        }
    };

    // 将输入指针重新定位到输入框
    useEffect(() => {
        if (!loading && textareaRef.current) {
            textareaRef.current.focus(); // 只有在非loading状态时聚焦
        }
    }, [loading]); // 监测 loading 状态

    const sendMessage = async () => {
        if (input.trim() === '') return;

        const timestamp = new Date();
        const newMessage = {sender: 'user', text: input, timestamp};

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
                cid = new Date().getTime().toString();
                setSelectedConversation(cid);
                await axios.get(`${window.API_BASE_URL}/chat/${localStorage.getItem('userUUID')}/${cid}`);
            }

            const conversationIndex = conversations.findIndex(conv => conv.id === selectedConversation);
            if (conversationIndex >= 0) {
                const selectedConv = conversations[conversationIndex];
                const timeGroup = getTimeGroup(selectedConv.timestampLast);
                if (timeGroup !== "Today") {
                    // 如果对话不在“今天”内，重排到最前
                    const updatedConversations = [...conversations];
                    updatedConversations.splice(conversationIndex, 1); // 从原位置移除
                    updatedConversations.unshift({ ...selectedConv, timestampLast: timestamp }); // 插入到最前面
                    setConversations(updatedConversations); // 更新对话列表
                }
            }

            if (useStream) {
                setStreamingMessage(
                    {sender: 'assistant', text: '', timestamp: timestamp});

                const response = await fetch(`${window.API_BASE_URL}/chat/stream`, {
                    method: 'POST', headers: {
                        'Content-Type': 'application/json',
                    }, body: JSON.stringify({
                        prompt: input,
                        conversationId: selectedConversation,
                        model: selectedModel,
                        stream: true,
                    }),
                });

                if (!response.body || response.ok === false) {
                    console.error('Error streaming response:', response);
                    setLoading(false);
                    return;
                }

                const reader = response.body.getReader();
                const decoder = new TextDecoder('utf-8');
                let accumulatedData = '';
                let accumulatedText = ''; // 手动累积文本

                while (true) {
                    const {done, value} = await reader.read();
                    if (done) break;

                    accumulatedData += decoder.decode(value, {stream: true});

                    // 按换行符分隔 JSON 对象
                    while (accumulatedData.includes('\n')) {
                        const newlineIndex = accumulatedData.indexOf('\n');
                        const jsonChunk = accumulatedData.slice(0, newlineIndex);
                        accumulatedData = accumulatedData.slice(newlineIndex + 1);

                        try {
                            const parsedChunk = JSON.parse(jsonChunk);  // 解析JSON
                            // console.log('Parsed chunk:', parsedChunk);
                            if (parsedChunk.data) {
                                accumulatedText += parsedChunk.data;
                                setStreamingMessage(prevMessage => ({
                                    ...prevMessage, text: accumulatedText, timestamp: timestamp,
                                }));
                                // 每次更新消息到 messages 列表
                                // console.log('Streaming message:', streamingMessage);
                                // setMessages(prevMessages => [...prevMessages, { sender: 'assistant', text: parsedChunk.data, timestamp: new Date() }]);
                            }
                            if (parsedChunk.msg) {
                                parseMsgAndPotentialNotify(parsedChunk.msg);
                                break;
                            }
                        } catch (error) {
                            console.error('Error parsing chunk:', error);
                        }
                    }
                }

                // 处理可能剩余的数据
                if (accumulatedData) {
                    try {
                        const parsedChunk = JSON.parse(accumulatedData);
                        console.log('Parsed final chunk:', parsedChunk);
                        if (parsedChunk.data) {
                            setStreamingMessage(prevMessage => ({
                                ...prevMessage,
                                text: prevMessage.text + parsedChunk.data,
                                timestamp: new Date(),
                            }));
                        }
                    } catch (error) {
                        console.error('Error parsing final chunk:' + accumulatedData,
                            error);
                    }
                }

                const finalStreamText = accumulatedText;
                setMessages(prevMessages => [
                    ...prevMessages, {
                        sender: 'assistant', text: finalStreamText, timestamp: new Date(),
                    }]);
                setLoading(false);
                setStreamingMessage(null);
            } else {
                // Non-streaming handling remains the same
                const response = await axios.post(`${window.API_BASE_URL}/chat`, {
                    prompt: input,
                    conversationId: selectedConversation,
                    model: selectedModel,
                    stream: false,
                }, {
                    timeout: 60000,
                });

                const assistantMessage = {
                    sender: 'assistant',
                    text: response.data.data,
                    timestamp: new Date(),
                };
                setMessages(prevMessages => [...prevMessages, assistantMessage]);
                setLoading(false);

                if (response.data.msg) {
                    const msg = response.data.msg;
                    parseMsgAndPotentialNotify(msg);
                }
            }
        } catch (error) {
            console.error('Error sending message:', error);
            setLoading(false);
        }
    };

    const parseMsgAndPotentialNotify = (msg, to1 = 3500, to2 = 2000) => {
        if (msg.includes(CONVERSATION_SUMMARY_GENERATED)) {
            const [beforeSummary, newTitle] = msg.split(
                CONVERSATION_SUMMARY_GENERATED);
            const notificationMsg = beforeSummary.trim()
                ? beforeSummary + ', Conversation summary generated, ' + newTitle
                : 'Conversation summary generated, ' + newTitle;
            animateTitleUpdate(selectedConversation, newTitle);
            setNotification(notificationMsg);
            setTimeout(() => setNotification(null), to1);
        } else {
            setNotification(msg);
            setTimeout(() => setNotification(null), to2);
        }
    };

    const animateTitleUpdate = (conversationId, newTitle) => {
        setAnimatingTitle({
            id: conversationId,
            targetTitle: newTitle,
            currentTitle: '',
            index: 0,
        });
    };

    useEffect(() => {
        if (animatingTitle) {
            const totalLength = animatingTitle.targetTitle.length;
            const remainingChars = totalLength - animatingTitle.index;

            // 基础延迟时间
            const baseDelay = 3;
            const maxDelay = 55;

            // 根据剩余字符数调整延迟
            // 字符越少，延迟越长，但不超过200ms
            const adjustedDelay = Math.min(baseDelay + (1 / remainingChars) * 1000,
                maxDelay);

            const timer = setTimeout(() => {
                if (animatingTitle.index < totalLength) {
                    setAnimatingTitle(prev => ({
                        ...prev,
                        currentTitle: prev.currentTitle + prev.targetTitle[prev.index],
                        index: prev.index + 1,
                    }));
                } else {
                    setConversations(prevConversations =>
                        prevConversations.map(conv =>
                            conv.id === animatingTitle.id
                                ? {...conv, title: animatingTitle.targetTitle}
                                : conv,
                        ),
                    );
                    setAnimatingTitle(null);
                }
            }, adjustedDelay);

            return () => clearTimeout(timer);
        }
    }, [animatingTitle]);

    const [userScrolled, setUserScrolled] = useState(false); // 用于判断用户是否手动滚动

    // 自动滚动逻辑
    useEffect(() => {
        if (chatWindowRef.current && !userScrolled) { // 只有在用户没有手动上滚时，才强制滚动
            chatWindowRef.current.scrollTo({
                top: chatWindowRef.current.scrollHeight,
                behavior: streamingMessage ? 'instant' : 'smooth', // 根据是否流式判断滚动行为
            });
        }
    }, [messages, streamingMessage, userScrolled]);

    // 监听用户滚动
    useEffect(() => {
        const handleScroll = () => {
            if (!chatWindowRef.current) return;

            const {scrollTop, scrollHeight, clientHeight} = chatWindowRef.current;

            // 如果用户滚动到了上方（不在底部），则认为用户手动滚动了
            if (scrollTop + clientHeight < scrollHeight - 10) {
                setUserScrolled(true);
            } else {
                setUserScrolled(false);
            }
        };

        const chatWindow = chatWindowRef.current;
        if (chatWindow) {
            chatWindow.addEventListener('scroll', handleScroll);
        }

        return () => {
            if (chatWindow) {
                chatWindow.removeEventListener('scroll', handleScroll);
            }
        };
    }, []);

    const getTimeGroup = (timestamp) => {
        const now = new Date();
        const conversationDate = new Date(timestamp);

        // 获取本地时间偏移量，并应用到 conversationDate
        const localConversationDate = new Date(conversationDate.getTime() - conversationDate.getTimezoneOffset() * 60000);

        // 设置 now 的时间为当天的 00:00:00
        const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());

        const oneDay = 24 * 60 * 60 * 1000;
        const oneWeek = 7 * oneDay;
        const oneMonth = 30 * oneDay;  // 近似一个月

        // 检测用户语言
        const userLang = navigator.language || navigator.userLanguage;

        // 根据语言环境设置时间分组标签
        const labels = {
            en: {
                today: "Today",
                yesterday: "Yesterday",
                withinWeek: "Within 7 Days",
                withinMonth: "Within a Month",
                earlier: "Earlier"
            },
            zh: {
                today: "今天",
                yesterday: "昨天",
                withinWeek: "一周内",
                withinMonth: "一月内",
                earlier: "更早"
            }
        };

        // 判断是中文还是英文，设置对应的标签
        const languageLabels = userLang.startsWith('zh') ? labels.zh : labels.en;

        // 时间分组逻辑
        if (localConversationDate >= todayStart) {
            return languageLabels.today;
        } else if (localConversationDate >= new Date(todayStart - oneDay)) {
            return languageLabels.yesterday;
        } else if (localConversationDate >= new Date(todayStart - oneWeek)) {
            return languageLabels.withinWeek;
        } else if (localConversationDate >= new Date(todayStart - oneMonth)) {
            return languageLabels.withinMonth;
        } else {
            return languageLabels.earlier;
        }
    };

// Function to group conversations by custom time groups
    const groupConversationsByTimeGroup = (conversations) => {
        const groupedConversations = [];
        let currentTimeGroup = '';

        conversations.forEach((conv) => {
            const timeGroup = getTimeGroup(conv.timestampLast);

            // Check if the conversation is from a new time group
            if (timeGroup !== currentTimeGroup) {
                groupedConversations.push({
                    type: 'time-group-divider',
                    group: timeGroup,
                });
                currentTimeGroup = timeGroup;
            }

            // Push the actual conversation
            groupedConversations.push({
                type: 'conversation-item',
                ...conv,
            });
        });

        return groupedConversations;
    };

    return (
        <div className="chat-interface">
            <div className="conversation-list">
                {/* 头部 */}
                <div className="conversation-header">
                    <h3>Conversations</h3>
                    <button className="new-conversation-btn" onClick={createNewConversation}>
                        <FaPlus/>
                    </button>
                </div>

                {/* 按时间分组的对话 */}
                {Array.isArray(conversations) &&
                    groupConversationsByTimeGroup(conversations).map((item, index) => {
                        // 渲染时间分隔符
                        if (item.type === 'time-group-divider') {
                            return (
                                <div key={index} className="time-group-divider">
                                    {item.group} {/* 显示分组标签，如"今天"、"昨天" */}
                                </div>
                            );
                        }

                        // 渲染对话条目
                        return (
                            <ConversationItem
                                key={item.id} // 每个对话的唯一 ID
                                conversation={{
                                    ...item,
                                    title: animatingTitle && animatingTitle.id === item.id
                                        ? animatingTitle.currentTitle
                                        : item.title,
                                }} // 传递对话数据
                                conversations={conversations} // 传递对话列表数据
                                messages={messages} // 传递消息数据
                                loadConversation={loadConversation} // 传递加载对话的函数
                                fetchConversations={fetchConversations} // 传递刷新对话列表的函数
                                selectedConversation={selectedConversation} // 传递当前选中的对话 ID
                                setSelectedConversation={setSelectedConversation} // 传递设置对话状态的函数
                                setMessages={setMessages} // 传递设置消息状态的函数
                                setNotification={setNotification} // 传递设置通知状态的函数
                                openShareModal={openShareModal} // 传递打开分享弹窗的函数
                                setShareMessages={setShareMessages} // 传递设置分享消息状态的函数
                                setSharedCid={setSharedCid} // 传递设置分享对话 ID 的函数
                            />
                        );
                    })}
            </div>

            {/* 分享弹窗 */}
            {showShareModal && (<div className="share-modal">
                <h3>Select messages to share</h3>
                <div className="message-list">
                    {shareMessages && shareMessages.length > 0 ? (shareMessages.map(
                        (msg, index) => (<div key={index}>
                            <input
                                type="checkbox"
                                checked={selectedMessages.includes(index)}
                                onChange={() => handleSelectMessage(index)}
                            />
                            <span>{msg.content || msg.text}</span>
                            {/* Fix for different message formats */
                                // msg.content for GetResponse
                                // msg.text for current messages in frontend
                            }
                        </div>))) : (<p>No messages available to share.</p>)}
                </div>
                <button onClick={handleShareConfirm}>Share</button>
                <button onClick={() => setShowShareModal(false)}>Cancel</button>
            </div>)}

            <div className="chat-container">
                <div className="chat-window" ref={chatWindowRef}
                     style={{height: 'calc(100vh - 100px)', overflowY: 'scroll'}}>
                    {/*<h3>Chat</h3>*/}
                    <div className="model-selector" ref={modelSelectorRef}>
                        <button className="floating-model-btn" onClick={() => {
                            if (showModelOptions) {
                                setShowModelOptions(false);
                            } else {
                                fetchModels().then(r => {
                                    setShowModelOptions(true);
                                });
                            }
                        }}>
                            Current Model: {selectedModel}
                        </button>

                        <div className={`model-options ${showModelOptions ? 'show' : ''}`}>
                            {models.length > 0 ? (models.map(
                                (model) => (<button key={model} onClick={() => {
                                    setSelectedModel(model);
                                    setShowModelOptions(false);
                                }}>
                                    {model}
                                </button>))) : (<p>No models available</p>)}
                        </div>
                    </div>


                    <AnimatePresence>
                        {messages.map((msg, index) => (
                            <MessageComponent key={index} msg={msg} messages={messages}
                                              index={index}/>))}
                    </AnimatePresence>
                    {streamingMessage && (
                        <MessageComponent msg={streamingMessage} messages={messages}
                                          index={messages.length}
                                          isStreaming={true}/>)}

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
                    <div className="inputButtonContainer">

                        <label className="toggle-switch tooltip">
                            <input
                                type="checkbox"
                                checked={useStream}
                                onChange={() => setUseStream(!useStream)}
                            />
                            <span className="slider"></span>
                            <span className="tooltip-text">{useStream
                                ? 'Stream on'
                                : 'Stream off'}</span>
                        </label>


                        <button className=".chat-container sendButton" onClick={sendMessage} disabled={loading}>
                            {loading ? (
                                <div className="thinking-animation">
                                    <span></span>
                                    <span></span>
                                    <span></span>
                                </div>
                            ) : 'Send'}
                        </button>

                    </div>
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

function MessageComponent({msg, messages, index, isStreaming = false}) {
    // 添加防御性检查
    if (!msg || typeof msg !== 'object') {
        console.error('Invalid message object:', msg);
        return null;
    }

    const messageDate = msg.timestamp ? new Date(msg.timestamp) : new Date();
    const currentDate = new Date();
    const isRecent = (currentDate - messageDate) < (24 * 60 * 60 * 1000);
    const isFirstRecentMessage = isRecent && (index === 0 ||
        (messages[index - 1] && new Date(messages[index - 1].timestamp) <
            (currentDate - 24 * 60 * 60 * 1000)));

    return (<React.Fragment>
        {isFirstRecentMessage && (<div className="day-divider">
            <div className="divider-line"></div>
            <div className="divider-text">Messages within the last day</div>
            <div className="divider-line"></div>
        </div>)}
        <motion.div
            className={`message-container ${msg.sender} ${isRecent
                ? 'recent-message'
                : ''} ${isStreaming ? 'streaming' : ''}`}
            initial={{opacity: 0, y: 20}}
            animate={{opacity: 1, y: 0}}
            exit={{opacity: 0, y: -20}}
            transition={{duration: 0.3}}
        >
            <div className={`message ${msg.sender}`}>
                <ReactMarkdown
                    children={msg.text || ''}
                    components={{
                        code({node, inline, className, children, ...props}) {
                            const match = /language-(\w+)/.exec(className || '');
                            return !inline ? (<SyntaxHighlighter
                                style={vscDarkPlus}
                                language={match ? match[1] : 'plaintext'}
                                PreTag="div"
                                children={String(children).replace(/\n$/, '')}
                                {...props}
                            />) : (<code className={className} {...props}>
                                {children}
                            </code>);
                        },
                    }}
                />
            </div>
            <div className={`timestamp ${msg.sender}-timestamp`}>
                {messageDate.toLocaleString(navigator.language, {
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
    </React.Fragment>);
}

export default Chat;
