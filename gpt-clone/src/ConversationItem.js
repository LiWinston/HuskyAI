import React, {useEffect, useRef, useState} from 'react';
import axios from 'axios';
import {FaEllipsisV} from 'react-icons/fa';
import { toast } from 'react-toastify';
import './Chat.css';

const ConversationItem = ({
                              conversation,
                              conversations,
                              messages,
                              loadConversation,
                              fetchConversations,
                              selectedConversation, // Currently selected conversation ID
                              setSelectedConversation,
                              setMessages,
                              handleShareStart,
                          }) => {
    const [showOptions, setShowOptions] = useState(false);

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

                // Calculate the number of lines that overflow the container
                const totalLines = scrollHeight / lineHeight;
                const visibleLines = containerHeight / lineHeight;
                const extraLines = totalLines - visibleLines;


                setIsLongTitle(extraLines > 0);

                if (extraLines > 0) {
                    // Define the minimum and maximum speed (seconds/line).
                    const minSpeedPerLine = 0.6; // fastest speed
                    const maxSpeedPerLine = 1.6; // slowest speed

                    // Calculate the speed factor to make the speed change linearly with the number of rows.
                    const cappedExtraLines = Math.min(extraLines, 10); // A maximum of 10 lines.
                    const speedPerLine = maxSpeedPerLine - ((maxSpeedPerLine - minSpeedPerLine) * (cappedExtraLines / 10));

                    // Calculate the rolling duration.
                    let duration = extraLines * speedPerLine;

                    // Ensure minimum duration.
                    duration = Math.max(duration, 2.1);

                    // Calculate the percentage of keyframes.
                    const initialPauseTime = 0.1;
                    const endPauseTime = 0.1;
                    const scrollTime = duration - initialPauseTime - endPauseTime;

                    // Calculate the percentage of keyframes.
                    const initialPausePercent = (initialPauseTime / duration) * 100;
                    const endPausePercent = (endPauseTime / duration) * 100;
                    const scrollPercent = ((scrollTime / 2) / duration) * 100;

                    const scrollDownStart = initialPausePercent;
                    const scrollDownEnd = scrollDownStart + scrollPercent;
                    const scrollUpStart = scrollDownEnd;
                    const scrollUpEnd = scrollUpStart + scrollPercent;
                    const finalPauseStart = scrollUpEnd;

                    // Generate a unique animation name
                    animationName = `smoothScroll_${Math.random().toString(36).substr(2, 9)}`;

                    // Generate the CSS keyframes
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

                    // Create a new style sheet and append the keyframes
                    styleSheet = document.createElement('style');
                    styleSheet.type = 'text/css';
                    styleSheet.innerHTML = keyframes;
                    document.head.appendChild(styleSheet);

                    // Apply the animation to the scroll content
                    const handleMouseEnter = () => {
                        scrollContent.style.animation = `${animationName} ${duration}s linear infinite`;
                    };

                    const handleMouseLeave = () => {
                        scrollContent.style.animation = '';
                    };

                    titleElement.addEventListener('mouseenter', handleMouseEnter);
                    titleElement.addEventListener('mouseleave', handleMouseLeave);

                    // Set the duration for the parent component to use
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

    // Listen to global click events to close the menu.
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (!e.target.closest('.options-menu') &&
                !e.target.closest('.options-icon')) {
                setShowOptions(false);
            }
        };
        window.addEventListener('click', handleClickOutside);

        // Clean up event listeners.
        return () => {
            window.removeEventListener('click', handleClickOutside);
        };
    }, []);

    const handleDelete = async () => {
        const uuid = localStorage.getItem('userUUID');
        try {
            // 获取要删除的元素
            const conversationElement = titleRef.current.closest('.conversation-item');
            
            // 获取元素的完整高度（包括margin）
            const elementHeight = conversationElement.offsetHeight + 
                parseInt(window.getComputedStyle(conversationElement).marginTop) +
                parseInt(window.getComputedStyle(conversationElement).marginBottom);

            // 为后续元素设置自定义属性，用于CSS计算位移
            conversationElement.style.setProperty('--element-height', `${elementHeight}px`);
            
            // 添加删除动画类
            conversationElement.classList.add('deleting');

            // 等待动画完成
            await new Promise(resolve => setTimeout(resolve, 300));

            // 设置删除标记
            localStorage.setItem('isDeleting', 'true');

            // 如果删除的是当前选中的对话，先清除 localStorage
            if (selectedConversation === conversation.id) {
                localStorage.removeItem('selectedConversation');
            }

            await axios.delete(`/api/chat/${uuid}/${conversation.id}`);
            
            // 获取更新后的会话列表
            const updatedConversations = await fetchConversations();

            if (selectedConversation === conversation.id) {
                if (updatedConversations.length > 0) {
                    // 加载新的第一个对话，并更新 localStorage
                    const newSelectedId = updatedConversations[0].id;
                    await loadConversation(newSelectedId);
                    localStorage.setItem('selectedConversation', newSelectedId);
                } else {
                    setSelectedConversation(null);
                    setMessages([]);
                }
            }

            // 清除删除标记
            localStorage.removeItem('isDeleting');

            toast.success('Conversation deleted successfully', {
                position: "bottom-center",
                autoClose: 2000,
                hideProgressBar: false,
                closeOnClick: true,
                pauseOnHover: true,
                draggable: true,
                progress: undefined,
                theme: localStorage.getItem('theme') === 'dark' ? 'dark' : 'light'
            });
        } catch (error) {
            console.error('Failed to delete conversation', error);
            // 清除删除标记
            localStorage.removeItem('isDeleting');
            // 如果删除失败，移除动画类
            const conversationElement = titleRef.current.closest('.conversation-item');
            conversationElement.classList.remove('deleting');
            conversationElement.style.removeProperty('--element-height');
            
            toast.error('Failed to delete conversation', {
                position: "bottom-center",
                autoClose: 3000,
                hideProgressBar: false,
                closeOnClick: true,
                pauseOnHover: true,
                draggable: true,
                progress: undefined,
                theme: localStorage.getItem('theme') === 'dark' ? 'dark' : 'light'
            });
        }
    };

    const handleShare = async () => {
        handleShareStart(conversation.id);
        setShowOptions(false);
    };

    const toggleOptionsMenu = (e) => {
        e.stopPropagation(); // Prevent the click event from propagating to the parent element.
        setShowOptions(prev => !prev);
    };

    return (<div
        className={`conversation-item ${selectedConversation === conversation.id
            ? 'selected'
            : ''}`}
        onClick={() => {
            setSelectedConversation(conversation.id);
            loadConversation(conversation.id, true);
        }}
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
                     onClick={toggleOptionsMenu}/> {/*  */}
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
