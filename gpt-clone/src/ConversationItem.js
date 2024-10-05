import React, {useEffect, useRef, useState} from 'react';
import axios from 'axios';
import {FaEllipsisV} from 'react-icons/fa';
import './Chat.css';

const ConversationItem = ({
                              conversation,
                              conversations,
                              messages,
                              loadConversation,
                              fetchConversations,
                              selectedConversation, // 当前选中的对话
                              setSelectedConversation,
                              setMessages,
                              setNotification,
                              openShareModal,
                              setShareMessages,
                              setSharedCid,
                          }) => {
    const [showOptions, setShowOptions] = useState(false);
    // eslint-disable-next-line no-unused-vars
    const [showShareModal, setShowShareModal] = useState(false);

    const titleRef = useRef(null);
    const [isLongTitle, setIsLongTitle] = useState(false);
    const [scrollDuration, setScrollDuration] = useState('15s');

    useEffect(() => {
        if (titleRef.current) {
            const titleElement = titleRef.current;
            const lineHeight = parseFloat(getComputedStyle(titleElement).lineHeight);
            const containerHeight = titleElement.clientHeight;
            const scrollHeight = titleElement.scrollHeight;

            // 计算超出的行数
            const totalLines = scrollHeight / lineHeight;
            const visibleLines = containerHeight / lineHeight;
            const extraLines = totalLines - visibleLines;

            // 是否超出三行
            setIsLongTitle(extraLines > 0);

            if (extraLines > 0) {
                // 计算滚动时间
                // 基础速度：2秒/行
                const baseSpeed = 1.6;
                // 最快速度：1秒/行
                const maxSpeed = 0.6;

                // 计算理想滚动时间（秒）
                let duration = Math.min(
                    // 基础速度计算
                    extraLines * baseSpeed,
                    // 最大速度计算
                    extraLines * maxSpeed
                );

                // 添加非线性因素：使用对数函数让速度随内容增加而放缓
                // 但仍然保持在最小和最大速度范围内
                duration = Math.min(
                    extraLines * baseSpeed,
                    duration * (1 + Math.log10(extraLines) * 0.2)
                );

                // 确保最小持续时间
                duration = Math.max(duration, 2.1); // 至少4秒

                // 考虑动画中的停顿时间（开始5%和结束5%）
                duration = duration * 1.1;

                // 设置滚动持续时间
                setScrollDuration(`${duration}s`);

                // 将duration设置为CSS变量
                titleElement.style.setProperty('--scroll-duration', `${duration}s`);
            }
        }
    }, [conversation.title]);

    // 监听全局点击事件以关闭菜单
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (!e.target.closest('.options-menu') &&
                !e.target.closest('.options-icon')) {
                setShowOptions(false);
            }
        };
        window.addEventListener('click', handleClickOutside);

        // 清理事件监听器
        return () => {
            window.removeEventListener('click', handleClickOutside);
        };
    }, []);

    const handleDelete = async () => {
        const uuid = localStorage.getItem('userUUID');
        try {
            await axios.delete(
                `${window.API_BASE_URL}/chat/${uuid}/${conversation.id}`);
            // 获取更新后的会话列表
            const updatedConversations = await fetchConversations();

            if (selectedConversation === conversation.id) {
                if (updatedConversations.length > 0) {
                    loadConversation(updatedConversations[0].id);
                } else {
                    setSelectedConversation(null);
                    setMessages([]);
                    localStorage.removeItem('selectedConversation');
                }
            }

            setNotification('Conversation deleted successfully');
            setTimeout(() => setNotification(null), 2000);
        } catch (error) {
            console.error('Failed to delete conversation', error);
        }
    };

    const handleShare = async () => {
        const uuid = localStorage.getItem('userUUID');

        // // 如果当前对话已经被选中且 messages 已经存在，直接使用它
        // if (selectedConversation === conversation.id && messages.length > 0) {
        //     setShareMessages(messages);  // 直接复用当前的消息
        //     setSharedCid(conversation.id);  // 设置分享的对话 ID
        //     openShareModal(conversation.id);  // 打开分享 modal
        //     return;
        // }

        // 否则，发起请求获取历史消息
        try {
            const response = await axios.get(
                `${window.API_BASE_URL}/chat/${uuid}/${conversation.id}`);
            const fetchedMessages = response.data.data;
            setShareMessages(fetchedMessages);  // 更新分享消息
            setSharedCid(conversation.id);  // 设置分享的对话 ID
            openShareModal(conversation.id);  // 打开分享 modal
        } catch (error) {
            console.error('Failed to sync conversation history for sharing', error);
        }
    };

    const toggleOptionsMenu = (e) => {
        e.stopPropagation(); // 防止事件传播到父元素
        setShowOptions(prev => !prev);
    };

    return (<div
        className={`conversation-item ${selectedConversation === conversation.id
            ? 'selected'
            : ''}`}
        onClick={() => {
            setSelectedConversation(conversation.id);
            loadConversation(conversation.id);
        }}  // 将 onClick 绑定到整个对话项
    >
        <div
            ref={titleRef}
            className={`conversation-item-title ${isLongTitle ? 'long-title' : ''}`}
        >
            <div className="scroll-content">
                {conversation.title}
            </div>
        </div>
        <FaEllipsisV className="options-icon"
                     onClick={toggleOptionsMenu}/> {/* 阻止点击事件传播 */}
        <div className={`options-menu ${showOptions ? 'show' : ''}`}
             onClick={(e) => e.stopPropagation()}>
            <ul>
                <li onClick={handleDelete}>Delete</li>
                <li onClick={handleShare}>Share</li>
            </ul>
        </div>
    </div>);
};

export default ConversationItem;
