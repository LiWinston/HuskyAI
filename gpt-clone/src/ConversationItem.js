import React, { useState } from 'react';
import axios from 'axios';
import { FaEllipsisV } from 'react-icons/fa';  // 选项图标
import './Chat.css';  // 假设样式定义在此文件中

const ConversationItem = ({
                              conversation,
                              loadConversation,
                              fetchConversations,
                              setSelectedConversation,
                              setMessages,
                              setNotification
                          }) => {
    const [showOptions, setShowOptions] = useState(false);

    const handleDelete = async () => {
        const uuid = localStorage.getItem('userUUID');
        try {
            await axios.delete(`${window.API_BASE_URL}/chat/${uuid}/${conversation.id}`);
            fetchConversations(); // 重新获取对话列表

            // 清空当前对话显示，并弹出通知框
            setSelectedConversation(null);
            setMessages([]);
            setNotification("Conversation deleted successfully");

            // 设置通知框显示时间，2秒后自动消失
            setTimeout(() => setNotification(null), 2000);
        } catch (error) {
            console.error('Failed to delete conversation', error);
        }
    };


    return (
        <div
            className="conversation-item"
            onMouseEnter={() => setShowOptions(true)}
            onMouseLeave={() => setShowOptions(false)}
        >
            <div onClick={() => loadConversation(conversation.id)}>
                {conversation.title}
            </div>
            {showOptions && (
                <div className="conversation-options">
                    <FaEllipsisV className="options-icon" />
                    <div className="options-menu">
                        <ul>
                            <li onClick={handleDelete}>Delete</li>
                            <li> Share</li>
                        </ul>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ConversationItem;
