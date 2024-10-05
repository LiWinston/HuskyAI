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
        let styleSheet;
        let animationName;

        if (titleRef.current) {
            const titleElement = titleRef.current;
            const scrollContent = titleElement.querySelector('.scroll-content');

            if (scrollContent) {
                const lineHeight = parseFloat(getComputedStyle(scrollContent).lineHeight);
                const containerHeight = titleElement.clientHeight;
                const scrollHeight = scrollContent.scrollHeight;

                // 计算超出的行数
                const totalLines = scrollHeight / lineHeight;
                const visibleLines = containerHeight / lineHeight;
                const extraLines = totalLines - visibleLines;

                // 是否超出三行
                setIsLongTitle(extraLines > 0);

                if (extraLines > 0) {
                    // 定义最小和最大速度（秒/行）
                    const minSpeedPerLine = 0.6; // 最快速度
                    const maxSpeedPerLine = 1.6; // 最慢速度

                    // 计算速度因子，使速度随行数线性变化
                    const cappedExtraLines = Math.min(extraLines, 10); // 上限为10行
                    const speedPerLine = maxSpeedPerLine - ((maxSpeedPerLine - minSpeedPerLine) * (cappedExtraLines / 10));

                    // 计算滚动持续时间
                    let duration = extraLines * speedPerLine;

                    // 确保最小持续时间
                    duration = Math.max(duration, 2.1); // 至少2.1秒

                    // 计算关键帧的百分比
                    const initialPauseTime = 0.1; // 初始停顿0.1秒
                    const endPauseTime = 0.1; // 结束停顿0.1秒
                    const scrollTime = duration - initialPauseTime - endPauseTime;

                    // 计算百分比
                    const initialPausePercent = (initialPauseTime / duration) * 100;
                    const endPausePercent = (endPauseTime / duration) * 100;
                    const scrollPercent = ((scrollTime / 2) / duration) * 100; // 下滚和上滚各占一半时间

                    const scrollDownStart = initialPausePercent;
                    const scrollDownEnd = scrollDownStart + scrollPercent;
                    const scrollUpStart = scrollDownEnd;
                    const scrollUpEnd = scrollUpStart + scrollPercent;
                    const finalPauseStart = scrollUpEnd;

                    // 生成唯一的动画名称
                    animationName = `smoothScroll_${Math.random().toString(36).substr(2, 9)}`;

                    // 生成关键帧CSS代码
                    const keyframes = `
                    @keyframes ${animationName} {
                        0% {
                            transform: translateY(0);
                        }
                        ${scrollDownStart}% {
                            transform: translateY(0);
                        }
                        ${scrollDownEnd}% {
                            transform: translateY(calc(-${scrollHeight - containerHeight}px));
                        }
                        ${scrollUpStart}% {
                            transform: translateY(calc(-${scrollHeight - containerHeight}px));
                        }
                        ${scrollUpEnd}% {
                            transform: translateY(0);
                        }
                        100% {
                            transform: translateY(0);
                        }
                    }
                `;

                    // 创建<style>元素并注入CSS代码
                    styleSheet = document.createElement('style');
                    styleSheet.type = 'text/css';
                    styleSheet.innerHTML = keyframes;
                    document.head.appendChild(styleSheet);

                    // 添加鼠标事件监听器
                    const handleMouseEnter = () => {
                        scrollContent.style.animation = `${animationName} ${duration}s linear infinite`;
                    };

                    const handleMouseLeave = () => {
                        scrollContent.style.animation = '';
                    };

                    titleElement.addEventListener('mouseenter', handleMouseEnter);
                    titleElement.addEventListener('mouseleave', handleMouseLeave);

                    // 在清理函数中移除事件监听器
                    return () => {
                        if (styleSheet && styleSheet.parentNode) {
                            styleSheet.parentNode.removeChild(styleSheet);
                        }
                        titleElement.removeEventListener('mouseenter', handleMouseEnter);
                        titleElement.removeEventListener('mouseleave', handleMouseLeave);
                        scrollContent.style.animation = '';
                    };
                }
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
