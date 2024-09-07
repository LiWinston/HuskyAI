import React, { useState } from 'react';
import axios from 'axios';
import { FaEllipsisV } from 'react-icons/fa';
import './Chat.css';

const ConversationItem = ({
                              conversation,
                              loadConversation,
                              fetchConversations,
                              selectedConversation, // 新增的 prop，用于检测当前选中的对话
                              setSelectedConversation,
                              setMessages,
                              setNotification
                          }) => {
    const [showOptions, setShowOptions] = useState(false);

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
        e.stopPropagation(); // 防止点击事件传播到父元素
        setShowOptions(prev => !prev);
    };

    return (
        <div
            className={`conversation-item ${selectedConversation === conversation.id ? 'selected' : ''}`}  // 根据是否选中设置样式
            onClick={() => setSelectedConversation(conversation.id)}  // 点击选中对话并更新选中状态
        >
            <div className="conversation-content" onClick={() => loadConversation(conversation.id)}>
                {conversation.title}
            </div>
            <FaEllipsisV className="options-icon" onClick={toggleOptionsMenu} />
            {showOptions && (
                <div className="options-menu" onClick={(e) => e.stopPropagation()}>
                    <ul>
                        <li onClick={handleDelete}>Delete</li>
                        <li>Share</li>
                    </ul>
                </div>
            )}
        </div>
    );
};

export default ConversationItem;
