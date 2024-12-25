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
                              selectedConversation, // Currently selected conversation ID
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
            await axios.delete(
                `/api/chat/${uuid}/${conversation.id}`);
            // Get the updated session list.
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



        try {
            const response = await axios.get(
                `/api/chat/${uuid}/${conversation.id}`);
            const fetchedMessages = response.data.data;
            setShareMessages(fetchedMessages);
            setSharedCid(conversation.id);
            openShareModal(conversation.id);
        } catch (error) {
            console.error('Failed to sync conversation history for sharing', error);
        }
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
            loadConversation(conversation.id);
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
