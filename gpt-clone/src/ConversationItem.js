import React, {useEffect, useState} from 'react';
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
        <div className="conversation-content">
            {conversation.title}
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
