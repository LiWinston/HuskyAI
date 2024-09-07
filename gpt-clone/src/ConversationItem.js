import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { FaEllipsisV } from 'react-icons/fa';
import './Chat.css';

const ConversationItem = ({
                              conversation,
                              loadConversation,
                              fetchConversations,
                              selectedConversation, // 当前选中的对话
                              setSelectedConversation,
                              setMessages,
                              setNotification
                          }) => {
    const [showOptions, setShowOptions] = useState(false);

    // 监听全局点击事件以关闭菜单
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (!e.target.closest('.options-menu') && !e.target.closest('.options-icon')) {
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
            await axios.delete(`${window.API_BASE_URL}/chat/${uuid}/${conversation.id}`);
            fetchConversations();
            setSelectedConversation(null);
            setMessages([]);
            setNotification("Conversation deleted successfully");
            setTimeout(() => setNotification(null), 2000);
        } catch (error) {
            console.error('Failed to delete conversation', error);
        }
    };

    const toggleOptionsMenu = (e) => {
        e.stopPropagation(); // 防止事件传播到父元素
        setShowOptions(prev => !prev);
    };

    return (
        <div
            className={`conversation-item ${selectedConversation === conversation.id ? 'selected' : ''}`}
            onClick={() => {
                setSelectedConversation(conversation.id);
                loadConversation(conversation.id);
            }}  // 将 onClick 绑定到整个对话项
        >
            <div className="conversation-content">
                {conversation.title}
            </div>
            <FaEllipsisV className="options-icon" onClick={toggleOptionsMenu}/> {/* 阻止点击事件传播 */}
            <div className={`options-menu ${showOptions ? 'show' : ''}`} onClick={(e) => e.stopPropagation()}>
                <ul>
                    <li onClick={handleDelete}>Delete</li>
                    <li>Share</li>
                </ul>
            </div>
        </div>
    );
};

export default ConversationItem;
