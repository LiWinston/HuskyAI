import React, {useEffect, useRef, useState} from 'react';
import axios from 'axios';
import ReactMarkdown from 'react-markdown';
import {Prism as SyntaxHighlighter} from 'react-syntax-highlighter';
import {AnimatePresence, motion} from 'framer-motion';
import './Chat.css';
import {
    vscDarkPlus,
    dracula,
    tomorrow,
    materialDark,
    oneDark,
    okaidia,
    solarizedlight,
    nord,
    atomDark,
    duotoneDark,
    duotoneLight,
    materialLight,
    nightOwl,
    shadesOfPurple,
    synthwave84,
    vs,
    xonokai,
    coldarkDark,
    coldarkCold,
    gruvboxDark,
    gruvboxLight,
    materialOceanic,
    oneLight,
    twilight,
    darcula,
    a11yDark,
    base16AteliersulphurpoolLight,
    hopscotch,
    lucario,
} from 'react-syntax-highlighter/dist/esm/styles/prism';
import {FaPlus, FaSignOutAlt, FaAdjust, FaTimes, FaEllipsisV} from 'react-icons/fa';
import ConversationItem from './ConversationItem';
import './Component/Toggle.css';
import {showSweetAlertWithRetVal} from './Component/sweetAlertUtil';
import remarkGfm from 'remark-gfm';
import CodeThemePreview from './Component/CodeThemePreview';

import {MathJax, MathJaxContext} from 'better-react-mathjax';
import { useNavigate } from 'react-router-dom';
import Lottie from 'lottie-react';
import loadingAnimation from './assets/loading.json';
import CenterNotice from './Component/CenterNotice';

const CONVERSATION_SUMMARY_GENERATED = '#CVSG##CVSG##CVSG#';

// 在文件顶部定义主题映射
const themeStyles = {
    // 浅色主题
    vs: { name: 'Visual Studio', type: 'light', style: vs },
    oneLight: { name: 'One Light', type: 'light', style: oneLight },
    materialLight: { name: 'Material Light', type: 'light', style: materialLight },
    tomorrow: { name: 'Tomorrow', type: 'light', style: tomorrow },
    solarizedlight: { name: 'Solarized Light', type: 'light', style: solarizedlight },
    duotoneLight: { name: 'Duotone Light', type: 'light', style: duotoneLight },
    coldarkCold: { name: 'Coldark Cold', type: 'light', style: coldarkCold },
    gruvboxLight: { name: 'Gruvbox Light', type: 'light', style: gruvboxLight },
    base16AteliersulphurpoolLight: { name: 'Base16 Ateliersulphurpool', type: 'light', style: base16AteliersulphurpoolLight },

    // 深色主题
    vscDarkPlus: { name: 'VS Code Dark+', type: 'dark', style: vscDarkPlus },
    dracula: { name: 'Dracula', type: 'dark', style: dracula },
    darcula: { name: 'Darcula', type: 'dark', style: darcula },
    materialDark: { name: 'Material Dark', type: 'dark', style: materialDark },
    materialOceanic: { name: 'Material Oceanic', type: 'dark', style: materialOceanic },
    oneDark: { name: 'One Dark', type: 'dark', style: oneDark },
    okaidia: { name: 'Okaidia', type: 'dark', style: okaidia },
    nord: { name: 'Nord', type: 'dark', style: nord },
    atomDark: { name: 'Atom Dark', type: 'dark', style: atomDark },
    duotoneDark: { name: 'Duotone Dark', type: 'dark', style: duotoneDark },
    nightOwl: { name: 'Night Owl', type: 'dark', style: nightOwl },
    shadesOfPurple: { name: 'Shades of Purple', type: 'dark', style: shadesOfPurple },
    synthwave84: { name: 'Synthwave 84', type: 'dark', style: synthwave84 },
    xonokai: { name: 'Xonokai', type: 'dark', style: xonokai },
    coldarkDark: { name: 'Coldark Dark', type: 'dark', style: coldarkDark },
    gruvboxDark: { name: 'Gruvbox Dark', type: 'dark', style: gruvboxDark },
    twilight: { name: 'Twilight', type: 'dark', style: twilight },
    a11yDark: { name: 'A11y Dark', type: 'dark', style: a11yDark },
    hopscotch: { name: 'Hopscotch', type: 'dark', style: hopscotch },
    lucario: { name: 'Lucario', type: 'dark', style: lucario },
};

// 添加代码块边框样式定义
const codeBlockBorders = {
    none: { name: '无边框', style: 'none' },
    default: { name: '默认灰框', style: 'default' },
    glow: { name: '蓝色光晕', style: 'glow' },
    double: { name: '双层实线', style: 'double' }
};

// 将主题选择组件提取出来
const ThemeSelector = React.memo(({ currentTheme, onThemeChange, isZH }) => {
    return (
        <div className="theme-options">
            <div 
                className={`theme-option ${currentTheme === 'light' ? 'selected' : ''}`}
                onClick={() => onThemeChange('light')}
            >
                <span>☀️ {isZH ? "浅色模式" : "Light Mode"}</span>
            </div>
            <div 
                className={`theme-option ${currentTheme === 'dark' ? 'selected' : ''}`}
                onClick={() => onThemeChange('dark')}
            >
                <span>🌙 {isZH ? "深色模式" : "Dark Mode"}</span>
            </div>
            <div 
                className={`theme-option ${currentTheme === 'auto' ? 'selected' : ''}`}
                onClick={() => onThemeChange('auto')}
            >
                <span>🌓 {isZH ? "自动模式" : "Auto Mode"}</span>
                {currentTheme === 'auto' && (
                    <span className="auto-mode-status">
                        ({localStorage.getItem('actualTheme') === 'dark' ? isZH ? "当前：深色" : "Current: Dark" : isZH ? "当前：浅色" : "Current: Light"})
                    </span>
                )}
            </div>
        </div>
    );
});

// 将代码主题选择组件提取出来
const CodeThemeSelector = React.memo(({ codeTheme, onCodeThemeChange, themeStyles, isZH }) => {
    return (
        <div className="code-theme-options">
            <div className="theme-list">
                <div className="theme-group">
                    <div className="theme-group-title">{isZH ? "浅色主题" : "Light Themes"}</div>
                    {Object.entries(themeStyles)
                        .filter(([_, theme]) => theme.type === 'light')
                        .map(([key, theme]) => (
                            <div 
                                key={key}
                                className={`theme-option ${codeTheme === key ? 'selected' : ''}`}
                                onClick={() => onCodeThemeChange(key)}
                            >
                                {theme.name}
                            </div>
                        ))
                    }
                </div>
                <div className="theme-group">
                    <div className="theme-group-title">{isZH ? "深色主题" : "Dark Themes"}</div>
                    {Object.entries(themeStyles)
                        .filter(([_, theme]) => theme.type === 'dark')
                        .map(([key, theme]) => (
                            <div 
                                key={key}
                                className={`theme-option ${codeTheme === key ? 'selected' : ''}`}
                                onClick={() => onCodeThemeChange(key)}
                            >
                                {theme.name}
                            </div>
                        ))
                    }
                </div>
            </div>
            <div className="theme-preview">
                <CodeThemePreview 
                    theme={themeStyles[codeTheme]} 
                    isZH={isZH}
                />
            </div>
        </div>
    );
});

// 将 ThemeModal 组件提到外部
const ThemeModal = React.memo(({ 
    onClose, 
    menuText, 
    currentTheme,
    toggleTheme,
    codeTheme,
    changeCodeTheme,
    themeStyles,
    isZH,
    codeBorderStyle,
    changeCodeBorderStyle 
}) => (
    <>
        <div 
            className="modal-backdrop" 
            onClick={onClose} 
        />
        <div className="theme-modal">
            <div className="theme-modal-header">
                <h2>{menuText.themeSettings}</h2>
                <button 
                    className="close-button"
                    onClick={onClose}
                    aria-label={menuText.close}
                >
                    <FaTimes />
                </button>
            </div>
            
            <div className="theme-modal-content">
                <div className="theme-section">
                    <h3>{menuText.interfaceTheme}</h3>
                    <ThemeSelector 
                        currentTheme={currentTheme}
                        onThemeChange={toggleTheme}
                        isZH={isZH}
                    />
                </div>
                
                <div className="theme-section">
                    <h3>{menuText.codeTheme}</h3>
                    <CodeThemeSelector 
                        codeTheme={codeTheme}
                        onCodeThemeChange={changeCodeTheme}
                        themeStyles={themeStyles}
                        isZH={isZH}
                    />
                </div>

                <div className="theme-section">
                    <h3>{isZH ? "代码块边框样式" : "Code Block Border Style"}</h3>
                    <div className="border-style-options">
                        {Object.entries(codeBlockBorders).map(([key, border]) => (
                            <div
                                key={key}
                                className={`border-style-option ${codeBorderStyle === key ? 'selected' : ''}`}
                                onClick={() => changeCodeBorderStyle(key)}
                            >
                                <div className={`preview-block border-${key}`}>
                                    {isZH ? "预览效果" : "Preview"}
                                </div>
                                <span>{isZH ? border.name : border.name}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    </>
));

function Chat() {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    // eslint-disable-next-line no-unused-vars
    const [rows, setRows] = useState(1); // New row status.
    const [loading, setLoading] = useState(false);
    const [conversations, setConversations] = useState([]); // Ensure initial state is an array
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [selectedModel, setSelectedModel] = useState(''); // The currently selected model.
    const [models, setModels] = useState([]); // List of models
    const [showModelOptions, setShowModelOptions] = useState(false); //
    const [useStream, setUseStream] = useState(false);
    // Automatically update to localStorage when selectedConversation changes.
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
    const [isShareMode, setIsShareMode] = useState(false);
    const [selectedMessages, setSelectedMessages] = useState([]);
    const [shareMessages, setShareMessages] = useState([]);
    const [sharedCid, setSharedCid] = useState(null);
    const [streamingMessage, setStreamingMessage] = useState(null);
    const navigate = useNavigate();
    const [isLoadingMessages, setIsLoadingMessages] = useState(false);
    const [centerNotice, setCenterNotice] = useState({ visible: false, message: '' });
    const [codeBorderStyle, setCodeBorderStyle] = useState('default');

    // 添加语言检测
    const userLang = navigator.language || navigator.userLanguage;
    const isZH = userLang.startsWith('zh');

    // 定义多语言文本
    const i18n = {
        zh: {
            conversations: "对话",
            newChat: "新对话",
            enterShareMode: "进入分享模式，请选择要分享的消息",
            exitShareMode: "已退出分享模式",
            loadingMessages: "加载对话内容...",
            selectedMessages: "已选择 {count} 条消息",
            share: "分享",
            cancel: "取消",
            copySuccess: "分享链接已复制到剪贴板",
            shareFailed: "分享失败，请重试",
            loadFailed: "获取消息失败，请重试",
            shareInstructions: "进入分享模式, 点击对话内容区域选择要分享的消息",
            shareExited: "已退出分享模式，所有选择已清除",
        },
        en: {
            conversations: "Chats",
            newChat: "New Chat",
            enterShareMode: "Share mode, please select messages",
            exitShareMode: "Share mode exited",
            loadingMessages: "Loading messages...",
            selectedMessages: "{count} messages selected",
            share: "Share",
            cancel: "Cancel",
            copySuccess: "Share link copied to clipboard",
            shareFailed: "Failed to share, please try again",
            loadFailed: "Failed to load messages, please try again",
            shareInstructions: "Click on messages to select them for sharing",
            shareExited: "Share mode exited, all selections cleared",
        },
        // 可以继续添加其他语言...
    };
    
    // 获取当前语言的文本,默认使用英文
    const getText = (key) => {
        const lang = userLang.startsWith('zh') ? 'zh' : 'en';
        return i18n[lang][key];
    };

    const handleInputChange = (event) => {
        const text = event.target.value;
        setInput(text);
        adjustTextareaHeight();
    };

    const adjustTextareaHeight = () => {
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            const scrollHeight = textareaRef.current.scrollHeight; // Get scroll height
            const maxHeight = parseInt(getComputedStyle(textareaRef.current).maxHeight, 10);

            // If the height reaches the maximum limit, allow scrolling.
            if (scrollHeight > maxHeight) {
                textareaRef.current.style.height = `${maxHeight}px`;
                textareaRef.current.style.overflowY = 'auto'; // Allow scrolling
            } else {
                textareaRef.current.style.height = `${scrollHeight}px`;
                textareaRef.current.style.overflowY = 'hidden'; // Forbid scrolling
            }

            setTextareaHeight(`${textareaRef.current.style.height}px`);
        }
    };


    useEffect(() => {
        adjustTextareaHeight();
    }, [input]);

    const fetchModels = async () => {
        document.body.style.cursor = 'wait'; // 设置鼠标为等待状态
        try {
            const response = await axios.post('/api/chat/models'
                , {
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
        } finally {
            document.body.style.cursor = 'default'; // 恢复默认鼠标样式
        }
    };

    useEffect(() => {
        const fetchAndLoadConversation = async () => {
            try {
                const userUUID = localStorage.getItem('userUUID');
                setNotification('Fetching conversations...'); // Set notification
                console.log('Fetching conversations for user:', userUUID); // Print user UUID
                await fetchConversations(); // Wait for conversations to be fetched
                setNotification(null); // Clear notification
            } catch (e) {
                console.error('Error loading conversation:', e);
            }
        };

        window.API_BASE_URL = localStorage.getItem('API_BASE_URL');

        fetchModels().then(r => console.log(r)).catch(e => console.error('Error after fetchModels:', e));

        fetchAndLoadConversation().then(() => console.log('Conversations fetched')).catch(e => console.error('Error after fetchAndLoadConversation:', e));
    }, []);

    const modelSelectorRef = useRef(null);
    // Click elsewhere on the page to hide model options.
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (modelSelectorRef.current &&
                !modelSelectorRef.current.contains(event.target)) {
                setShowModelOptions(false);
            }
        };

        // Bind global click event.
        document.addEventListener('mousedown', handleClickOutside);

        return () => {
            // Remove global click event listener.
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    // Modify the useEffect for initializing the loading dialog.
    useEffect(() => {
        const loadInitialConversation = async () => {
            if (conversations.length > 0) {
                const storedConversationId = localStorage.getItem(
                    'selectedConversation');
                const conversationToLoad = conversations.find(
                    conv => conv.id === storedConversationId) || conversations[0];
                await loadConversation(conversationToLoad.id, false); // 初始加载不显示动画
            } else {
                setSelectedConversation(null);
                setMessages([]);
            }
        };

        loadInitialConversation();
    }, [conversations]);

    // 在现有的状态中添加新的加载状态
    const [isLoadingConversations, setIsLoadingConversations] = useState(false);
    const [isLoadingConversation, setIsLoadingConversation] = useState(false);

    // 修改 fetchConversations 函数
    const fetchConversations = async () => {
        setIsLoadingConversations(true);
        try {
            const response = await axios.get(`/api/chat`, {
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
        } finally {
            setTimeout(() => {
                setIsLoadingConversations(false);
            }, 500); // 添加一个小延迟，使动画更流畅
        }
    };

    // 修改 loadConversation 函数
    const loadConversation = async (conversationId, showLoading = false) => {
        if (showLoading) {
            setIsLoadingConversation(true);
        }
        try {
            const uuid = localStorage.getItem('userUUID');
            const response = await axios.get(`/api/chat/${uuid}/${conversationId}`);

            const loadedMessages = response.data.data.length
                ? response.data.data.map(msg => ({
                    sender: msg.role,
                    text: msg.content,
                    timestamp: msg.timestamp,
                }))
                : [{
                    sender: 'system',
                    text: 'No messages yet in this conversation. Start the conversation now!',
                    timestamp: new Date(),
                }];

            setMessages(loadedMessages);
            setSelectedConversation(conversationId);
            localStorage.setItem('selectedConversation', conversationId);
        } catch (error) {
            console.error('Error loading conversation:', error);
            setMessages([{
                sender: 'system',
                text: 'Failed to load conversation. Please try again later.',
                timestamp: new Date(),
            }]);
        } finally {
            if (showLoading) {
                setTimeout(() => {
                    setIsLoadingConversation(false);
                }, 500); // 添加一个小延迟，使动画更流畅
            }
        }
    };

    // Add a new function to create a new conversation.
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
            await loadConversation(newConversationId, false); // 新建对话不显示动画
        } catch (error) {
            console.error('Failed to create new conversation', error);
        }
    };

    const handleShareStart = async (conversationId) => {
        try {
            setIsLoadingMessages(true);
            // 先加载对话内容
            const response = await axios.get(`/api/chat/${localStorage.getItem('userUUID')}/${conversationId}`);
            
            // 设置选中的对话
            setSelectedConversation(conversationId);
            
            // 使用动画过渡更新消息
            const chatWindow = chatWindowRef.current;
            if (chatWindow) {
                // 添加淡出动画
                chatWindow.style.opacity = '0';
                chatWindow.style.transition = 'opacity 0.3s ease';
                
                // 等待淡出动画完成
                await new Promise(resolve => setTimeout(resolve, 300));
                
                // 更新消息内容
                setMessages(response.data.data.map(msg => ({
                    sender: msg.role,
                    text: msg.content,
                    timestamp: msg.timestamp,
                })));
                
                // 设置分享相关状态
                setShareMessages(response.data.data);
                setSharedCid(conversationId);
                setIsShareMode(true);
                setSelectedMessages([]);
                
                // 添加淡入动画
                setTimeout(() => {
                    chatWindow.style.opacity = '1';
                }, 50);
                
                setCenterNotice({
                    visible: true,
                    message: getText('shareInstructions')
                });
            }
        } catch (error) {
            console.error('Error fetching messages for share:', error);
            setNotification(getText('loadFailed'));
        } finally {
            setIsLoadingMessages(false);
        }
    };

    const handleShareCancel = () => {
        setIsShareMode(false);
        setSelectedMessages([]);
        setShareMessages([]);
        setSharedCid(null);
        setCenterNotice({
            visible: true,
            message: getText('shareExited'),
            duration: 1500
        });
    };

    const handleMessageClick = (index) => {
        if (!isShareMode) return;
        
        setSelectedMessages(prev => {
            const isSelected = prev.includes(index);
            if (isSelected) {
                return prev.filter(i => i !== index);
            } else {
                return [...prev, index];
            }
        });
    };

    const handleShareConfirm = async () => {
        if (selectedMessages.length === 0) return;

        try {
            const selectedMsgs = selectedMessages.map(index => shareMessages[index]);
            const response = await axios.post('/api/chat/share', {
                uuid: localStorage.getItem('userUUID'),
                conversationId: sharedCid,
                messageIndexes: selectedMessages,
            });

            if (response.data.code === 1) {
                const shareLink = `${window.location.origin}/chat/share/${response.data.data}`;
                showSweetAlertWithRetVal(`Share link generated: ${shareLink}`, {
                    title: 'Share Link',
                    icon: 'success',
                    confirmButtonText: 'Copy Link',
                    confirmButtonColor: '#3085d6',
                }).then((result) => {
                    if (result.isConfirmed) {
                        navigator.clipboard.writeText(shareLink);
                        setNotification(getText('copySuccess'));
                        setTimeout(() => setNotification(null), 2000);
                    }
                });
                handleShareCancel();
            }
        } catch (error) {
            console.error('Error sharing messages:', error);
            setNotification(getText('shareFailed'));
            setTimeout(() => setNotification(null), 2000);
        }
    };

    // Reposition the input pointer to the input box.
    useEffect(() => {
        if (!loading && textareaRef.current) {
            textareaRef.current.focus();
        }
    }, [loading]);

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
                await loadConversation(cid, false); // 发送消息时创建新对话不显示动画
            }

            const conversationIndex = conversations.findIndex(conv => conv.id === selectedConversation);
            if (conversationIndex >= 0) {
                const selectedConv = conversations[conversationIndex];
                const timeGroup = getTimeGroup(selectedConv.timestampLast);
                if (timeGroup !== "Today") {
                    // If the conversation is not "today," rearrange it to the front.
                    const updatedConversations = [...conversations];
                    updatedConversations.splice(conversationIndex, 1);
                    updatedConversations.unshift({ ...selectedConv, timestampLast: timestamp });
                    setConversations(updatedConversations);
                }
            }

            if (useStream) {
                setStreamingMessage(
                    {sender: 'assistant', text: '', timestamp: timestamp});

                const response = await fetch(`/api/chat/stream`, {
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
                let accumulatedText = '';

                while (true) {
                    const {done, value} = await reader.read();
                    if (done) break;

                    accumulatedData += decoder.decode(value, {stream: true});

                    // Separate JSON objects by newline character.
                    while (accumulatedData.includes('\n')) {
                        const newlineIndex = accumulatedData.indexOf('\n');
                        const jsonChunk = accumulatedData.slice(0, newlineIndex);
                        accumulatedData = accumulatedData.slice(newlineIndex + 1);

                        try {
                            const parsedChunk = JSON.parse(jsonChunk);
                            // console.log('Parsed chunk:', parsedChunk);
                            if (parsedChunk.data) {
                                accumulatedText += parsedChunk.data;
                                setStreamingMessage(prevMessage => ({
                                    ...prevMessage, text: accumulatedText, timestamp: timestamp,
                                }));
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

                // Handle any remaining data.
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
                const response = await axios.post('/api/chat', {
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

            // Basic latency time.
            const baseDelay = 3;
            const maxDelay = 55;

            // Adjust the delay according to the number of remaining characters.
            // The fewer characters, the longer the delay, but not more than 200ms.
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

    const [userScrolled, setUserScrolled] = useState(false);

    // Automatic scrolling logic.
    useEffect(() => {
        if (chatWindowRef.current && !userScrolled) {
            chatWindowRef.current.scrollTo({
                top: chatWindowRef.current.scrollHeight,
                behavior: streamingMessage ? 'instant' : 'smooth',
            });
        }
    }, [messages, streamingMessage, userScrolled]);

    // Listen to user scroll.
    useEffect(() => {
        const handleScroll = () => {
            if (!chatWindowRef.current) return;

            const {scrollTop, scrollHeight, clientHeight} = chatWindowRef.current;

            // If the user scrolls up (not at the bottom), it is considered that the user scrolled manually.
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

        // Get the local time offset and apply it to conversationDate.
        const localConversationDate = new Date(conversationDate.getTime() - conversationDate.getTimezoneOffset() * 60000);

        // Set the time of now to 00:00:00 of the day.
        const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());

        const oneDay = 24 * 60 * 60 * 1000;
        const oneWeek = 7 * oneDay;
        const oneMonth = 30 * oneDay;

        // Detect user language.
        const userLang = navigator.language || navigator.userLanguage;

        // Group time labels according to locale settings.
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

        // Determine whether it is Chinese or English and set the corresponding label.
        const languageLabels = userLang.startsWith('zh') ? labels.zh : labels.en;

        // Time grouping logic.
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

    const handleLogout = () => {
        // 清除所有登录相关的本地存储
        localStorage.removeItem('token');
        localStorage.removeItem('userUUID');
        localStorage.removeItem('selectedConversation');
        localStorage.removeItem('conversations');

        // 跳转到登录页面
        navigate('/');
    };

    const [showThemeModal, setShowThemeModal] = useState(false);
    const [currentTheme, setCurrentTheme] = useState('light');
    const [codeTheme, setCodeTheme] = useState('synthwave84');
    
    // 菜单文本的双语配置
    const menuText = {
        themeSettings: isZH ? "主题设置" : "Theme Settings",
        interfaceTheme: isZH ? "界面主题" : "Interface Theme",
        lightMode: isZH ? "浅色模式" : "Light Mode",
        darkMode: isZH ? "深色模式" : "Dark Mode",
        codeTheme: isZH ? "代码主题" : "Code Theme",
        close: isZH ? "关闭" : "Close",
        autoMode: isZH ? "自动模式" : "Auto Mode",
        currentDark: isZH ? "当前：深色" : "Current: Dark",
        currentLight: isZH ? "当前：浅色" : "Current: Light",
    };

    // 代码主题选项名称映射（保持英文，因为专有名词）
    const themeNames = {
        vscDarkPlus: 'VS Code Dark+',
        dracula: 'Dracula',
        tomorrow: 'Tomorrow',
        materialDark: 'Material Dark',
        oneDark: 'One Dark'
    };

    // 处理 ESC 键关闭弹窗
    useEffect(() => {
        const handleEsc = (event) => {
            if (event.key === 'Escape') {
                setShowThemeModal(false);
            }
        };
        
        if (showThemeModal) {
            document.addEventListener('keydown', handleEsc);
        }
        
        return () => {
            document.removeEventListener('keydown', handleEsc);
        };
    }, [showThemeModal]);

    // 加载保存的主题设置
    useEffect(() => {
        const loadSavedThemes = () => {
            const savedTheme = localStorage.getItem('theme') || 'light';
            const savedCodeTheme = localStorage.getItem('codeTheme') || 'synthwave84';
            const savedBorderStyle = localStorage.getItem('codeBorderStyle') || 'default';
            
            setCurrentTheme(savedTheme);
            setCodeTheme(savedCodeTheme);
            setCodeBorderStyle(savedBorderStyle);
            document.documentElement.setAttribute('data-theme', savedTheme);
            document.documentElement.setAttribute('data-code-border', savedBorderStyle);
        };

        loadSavedThemes();
    }, []);

    // 切换主题
    const toggleTheme = (theme) => {
        setCurrentTheme(theme);
        localStorage.setItem('theme', theme);
        document.documentElement.setAttribute('data-theme', theme);
    };

    // 切换代码主题
    const changeCodeTheme = (theme) => {
        setCodeTheme(theme);
        localStorage.setItem('codeTheme', theme);
    };

    // 切换代码块边框样式
    const changeCodeBorderStyle = (style) => {
        setCodeBorderStyle(style);
        localStorage.setItem('codeBorderStyle', style);
        document.documentElement.setAttribute('data-code-border', style);
    };

    useEffect(() => {
        if (!isShareMode) return;

        const handleClickOutside = (e) => {
            const chatWindow = chatWindowRef.current;
            if (!chatWindow) return;

            if (!e.target.closest('.message-container') && 
                !e.target.closest('.share-controls')) {
                handleShareCancel();
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [isShareMode]);

    return (
        <div className="chat-interface">
            <button className="theme-button" onClick={() => setShowThemeModal(true)}>
                <FaAdjust />
            </button>
            <button className="logout-button" onClick={handleLogout}>
                <FaSignOutAlt />
            </button>
            {showThemeModal && (
                <ThemeModal 
                    onClose={() => setShowThemeModal(false)}
                    menuText={menuText}
                    currentTheme={currentTheme}
                    toggleTheme={toggleTheme}
                    codeTheme={codeTheme}
                    changeCodeTheme={changeCodeTheme}
                    themeStyles={themeStyles}
                    isZH={isZH}
                    codeBorderStyle={codeBorderStyle}
                    changeCodeBorderStyle={changeCodeBorderStyle}
                />
            )}
            <div className="conversation-list">
                <div className="conversation-header">
                    <h3>{getText('conversations')}</h3>
                    <button className="new-conversation-btn" onClick={createNewConversation}>
                        <FaPlus size={12} />
                        <span>{getText('newChat')}</span>
                    </button>
                </div>

                {isLoadingConversations ? (
                    <div className="loading-overlay">
                        <div className="loading-container">
                            <Lottie 
                                animationData={loadingAnimation}
                                loop={true}
                                style={{ width: 80, height: 80 }}
                            />
                            <div className="loading-text">
                                {isZH ? "加载对话列表..." : "Loading conversations..."}
                            </div>
                        </div>
                    </div>
                ) : (
                    Array.isArray(conversations) &&
                    groupConversationsByTimeGroup(conversations).map((item, index) => {
                        if (item.type === 'time-group-divider') {
                            return (
                                <div key={index} className="time-group-divider">
                                    {item.group} {/* Display group labels like "Today," "Yesterday." */}
                                </div>
                            );
                        }

                        // Render dialogue entry.
                        return (
                            <ConversationItem
                                key={item.id}
                                conversation={item}
                                conversations={conversations}
                                messages={messages}
                                loadConversation={loadConversation}
                                fetchConversations={fetchConversations}
                                selectedConversation={selectedConversation}
                                setSelectedConversation={setSelectedConversation}
                                setMessages={setMessages}
                                setNotification={setNotification}
                                handleShareStart={handleShareStart}
                            />
                        );
                    })
                )}
            </div>

            <div className="chat-container">
                {isLoadingConversation ? (
                    <div className="loading-overlay">
                        <div className="loading-container">
                            <Lottie 
                                animationData={loadingAnimation}
                                loop={true}
                                style={{ width: 80, height: 80 }}
                            />
                            <div className="loading-text">
                                {isZH ? "加载对话内容..." : "Loading messages..."}
                            </div>
                        </div>
                    </div>
                ) : (
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
                                <ErrorBoundary key={index}>
                                    <MessageComponent 
                                        key={index}
                                        msg={msg}
                                        messages={messages}
                                        index={index}
                                        isStreaming={streamingMessage === index}
                                        codeTheme={codeTheme}
                                        isShareMode={isShareMode}
                                        selectedMessages={selectedMessages}
                                        handleMessageClick={handleMessageClick}
                                    />
                                </ErrorBoundary>
                            ))}
                        </AnimatePresence>
                        {streamingMessage && (
                            <MessageComponent 
                                msg={streamingMessage} 
                                messages={messages}
                                index={messages.length}
                                isStreaming={true}
                                codeTheme={codeTheme}
                            />
                        )}

                    </div>
                )}

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
                        // rows={rows}
                        style={{height: textareaHeight}}
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
            {/*  */}
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

            {isShareMode && (
                <div className="share-controls">
                    <div className="share-controls-content">
                        <span className="selected-count">
                            {getText('selectedMessages').replace('{count}', selectedMessages.length)}
                        </span>
                        <div className="share-buttons">
                            <button 
                                className="share-submit"
                                onClick={handleShareConfirm}
                                disabled={selectedMessages.length === 0}
                            >
                                {getText('share')}
                            </button>
                            <button 
                                className="share-cancel"
                                onClick={handleShareCancel}
                            >
                                {getText('cancel')}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {isLoadingMessages && (
                <div className="global-loading-overlay">
                    <div className="loading-container">
                        <Lottie 
                            animationData={loadingAnimation}
                            loop={true}
                            style={{ width: 120, height: 120 }}
                        />
                        <div className="loading-text">{getText('loadingMessages')}</div>
                    </div>
                </div>
            )}

            <CenterNotice 
                message={centerNotice.message}
                isVisible={centerNotice.visible}
                onClose={() => setCenterNotice({ visible: false, message: '' })}
                duration={5000}  // 5秒
            />
        </div>);
}

// 预处理文本，将"\(...)"和"\[...]"转换为"$$...$$"的式
function preprocessText(text) {
    return text
    .replace(/\\\((.*?)\\\)/g, '$$$$ $1 $$$$')
    .replace(/\\\[([\s\S]*?)\\\]/g, '$$$$ $1 $$$$');
}
class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError(error) {
        // 更新 state 使下一次渲染能够显示降级后的 UI
        return { hasError: true };
    }

    componentDidCatch(error, errorInfo) {
        console.error("Caught an error:", error, errorInfo);
    }

    render() {
        if (this.state.hasError) {
            // 可以自定义错误提示页面
            return <h1>Something went wrong.</h1>;
        }
        return this.props.children;
    }
}

function formatMessageTime(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    
    // 检测用户语言
    const userLang = navigator.language || navigator.userLanguage;
    const isZH = userLang.startsWith('zh');
    
    // 双语文本配置
    const texts = {
        justNow: isZH ? "刚刚" : "just now",
        minutesAgo: isZH ? "分钟前" : " mins ago",
        today: isZH ? "今天" : "Today",
        yesterday: isZH ? "昨天" : "Yesterday",
        dayBeforeYesterday: isZH ? "前天" : "2 days ago",
        morning: isZH ? "上午" : "AM",
        afternoon: isZH ? "下午" : "PM",
        weekdays: {
            zh: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'],
            en: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
        }
    };

    // 获取时间和时段
    const hour = date.getHours();
    const minute = date.getMinutes();
    const isPM = hour >= 12;
    const hour12 = hour % 12 || 12;
    
    // 格式化具体时间
    const timeStr = isZH 
        ? `${texts[isPM ? 'afternoon' : 'morning']}${hour12}:${minute.toString().padStart(2, '0')}`
        : `${hour12}:${minute.toString().padStart(2, '0')} ${isPM ? 'PM' : 'AM'}`;

    // 1分钟内
    if (seconds < 60) {
        return texts.justNow;
    }
    
    // 59分钟内
    if (minutes < 60) {
        return `${minutes}${texts.minutesAgo}`;
    }

    // 判断日期
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const dayBeforeYesterday = new Date(today);
    dayBeforeYesterday.setDate(dayBeforeYesterday.getDate() - 2);

    const isToday = date >= today;
    const isYesterday = date >= yesterday && date < today;
    const isDayBeforeYesterday = date >= dayBeforeYesterday && date < yesterday;

    // 三天内
    if (isToday) {
        return `${texts.today} ${timeStr}`;
    }
    if (isYesterday) {
        return `${texts.yesterday} ${timeStr}`;
    }
    if (isDayBeforeYesterday) {
        return `${texts.dayBeforeYesterday} ${timeStr}`;
    }

    // 计算是否在本周内
    const startOfWeek = new Date(today);
    startOfWeek.setDate(today.getDate() - today.getDay());
    const isThisWeek = date >= startOfWeek;
    
    // 本周内
    if (isThisWeek) {
        const weekday = texts.weekdays[isZH ? 'zh' : 'en'][date.getDay()];
        return `${weekday} ${timeStr}`;
    }

    // 判断是否在本年内
    const isThisYear = date.getFullYear() === now.getFullYear();
    
    // 本年内
    if (isThisYear) {
        if (isZH) {
            return `${date.getMonth() + 1}月${date.getDate()}日 ${timeStr}`;
        } else {
            return date.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric'
            }) + ` ${timeStr}`;
        }
    }
    
    // 超出本年
    if (isZH) {
        return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${timeStr}`;
    } else {
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }) + ` ${timeStr}`;
    }
}

function MessageComponent({msg, messages, index, isStreaming = false, codeTheme, isShareMode = false, selectedMessages = [], handleMessageClick}) {
    const mathJaxRef = useRef(null);

    const mathJaxConfig = {
        loader: { load: ["input/tex", "output/chtml"] }, // specify output
        tex: { packages: { "[+]": ["color"] } },
        startup: {
            typeset: false,  // Prevent automatic typesetting
        }
    };

    useEffect(() => {
        // 清理函数
        return () => {
            if (mathJaxRef.current) {
                try {
                    const jaxElements = mathJaxRef.current.getElementsByClassName('MathJax');
                    while (jaxElements.length > 0 && jaxElements[0].parentNode === mathJaxRef.current) {
                        jaxElements[0].remove();
                    }

                    // 移除MathJax脚本缓存
                    const scriptElements = mathJaxRef.current.getElementsByTagName('script');
                    while (scriptElements.length > 0 && scriptElements[0].parentNode === mathJaxRef.current) {
                        scriptElements[0].remove();
                    }
                } catch (error) {
                    console.error('MathJax cleanup error:', error);
                }
            }
        };
    }, [msg.text]);

    if (!msg || typeof msg !== 'object') {
        console.error('Invalid message object:', msg);
        return null;
    }

    const processedText = preprocessText(msg.text || '');

    const messageDate = msg.timestamp ? new Date(msg.timestamp) : new Date();
    const currentDate = new Date();
    const isRecent = (currentDate - messageDate) < (24 * 60 * 60 * 1000);
    const isFirstRecentMessage = isRecent && (index === 0 ||
        (messages[index - 1] && new Date(messages[index - 1].timestamp) <
            (currentDate - 24 * 60 * 60 * 1000)));

    return (<React.Fragment>
        <MathJaxContext config={mathJaxConfig}>
        {isFirstRecentMessage && (<div className="day-divider">
            <div className="divider-line"></div>
            <div className="divider-text">Messages within the last day</div>
            <div className="divider-line"></div>
        </div>)}
        <motion.div
            className={`message-container ${msg.sender} ${isRecent
                ? 'recent-message'
                : ''} ${isStreaming ? 'streaming' : ''} ${isShareMode ? 'share-mode' : ''} ${selectedMessages.includes(index) ? 'selected' : ''}`}
            initial={{opacity: 0, y: 20}}
            animate={{opacity: 1, y: 0}}
            exit={{opacity: 0, y: -20}}
            transition={{duration: 0.3}}
            onClick={() => isShareMode && handleMessageClick && handleMessageClick(index)}
        >
            <div className={`message ${msg.sender}`}>
                <div className="markdown-table-container" ref={mathJaxRef}>
                    <MathJax dynamic>
                        <ReactMarkdown
                            children={processedText || ''}
                            remarkPlugins={[remarkGfm]}
                            components={{
                                code({node, inline, className, children, ...props}) {
                                    const match = /language-(\w+)/.exec(className || '');
                                    const codeContent = String(children).replace(/\n$/, '');

                                    // 判断是否应该内联显示
                                    const shouldInline = inline ||
                                        (codeContent.length < 50 && !codeContent.includes('\n'));

                                    return shouldInline ? (
                                        <code
                                            className={`inline-code ${className || ''}`}
                                            {...props}
                                        >
                                            {children}
                                        </code>
                                    ) : (
                                        <SyntaxHighlighter
                                            style={themeStyles[codeTheme].style}
                                            language={match ? match[1] : 'plaintext'}
                                            PreTag="div"
                                            children={codeContent}
                                            {...props}
                                        />
                                    );
                                },
                            }}
                            />
                        </MathJax>
                    </div>
                </div>
                <div className={`timestamp ${msg.sender}-timestamp`}>
                    {formatMessageTime(msg.timestamp)}
                </div>
            </motion.div>
        </MathJaxContext>
    </React.Fragment>);
}

export default Chat;
