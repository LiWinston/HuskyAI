import React, { useState, useEffect } from 'react';
import { FaTimes, FaEdit, FaTrash, FaCopy, FaExternalLinkAlt, FaSync } from 'react-icons/fa';
import axiosInstance from '../api/axiosConfig';
import './ShareManageModal.css';
import DatePicker from 'react-datepicker';
import "react-datepicker/dist/react-datepicker.css";
import { toast } from 'react-toastify';

const ShareManageModal = ({ onClose, isZH, conversations }) => {
    const [shares, setShares] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editingShare, setEditingShare] = useState(null);
    const [selectedDate, setSelectedDate] = useState(null);
    const [editMode, setEditMode] = useState('preset');
    const [loadingTitles, setLoadingTitles] = useState({});  // 记录正在加载标题的状态

    const presetOptions = [
        { value: 24, label: isZH ? '1天' : '1 day' },
        { value: 72, label: isZH ? '3天' : '3 days' },
        { value: 168, label: isZH ? '7天' : '7 days' },
        { value: 720, label: isZH ? '30天' : '30 days' }
    ];

    // 加载分享记录
    useEffect(() => {
        fetchShares();
    }, [isZH]);

    const fetchShares = async () => {
        try {
            const uuid = localStorage.getItem('userUUID');
            const response = await axiosInstance.get(`/chat/share/user/${uuid}`);
            if (response.data.code === 1) {
                setShares(response.data.data);
            } else {
                setError(response.data.msg);
            }
        } catch (error) {
            setError(isZH ? '加载分享记录失败' : 'Failed to load share records');
        } finally {
            setLoading(false);
        }
    };

    // 更新过期时间
    const handleUpdateExpiration = async (shareCode, newHours) => {
        try {
            const response = await axiosInstance.put(`/chat/share/${shareCode}/expiration`, {
                expirationHours: newHours
            });
            if (response.data.code === 1) {
                await fetchShares();
                setEditingShare(null);
                setSelectedDate(null);
                toast.success(isZH ? '更新成功' : 'Update successful');
            }
        } catch (error) {
            console.error('Failed to update expiration:', error);
            toast.error(isZH ? '更新失败' : 'Update failed');
        }
    };

    // 处理自定义日期更新
    const handleCustomDateUpdate = async (shareCode) => {
        if (!selectedDate) return;
        
        const now = new Date();
        const diffHours = Math.ceil((selectedDate - now) / (1000 * 60 * 60));
        
        if (diffHours <= 0) {
            toast.error(isZH ? '请选择未来的时间' : 'Please select a future time');
            return;
        }
        
        await handleUpdateExpiration(shareCode, diffHours);
    };

    // 删除分享
    const handleDelete = async (shareCode) => {
        try {
            const response = await axiosInstance.delete(`/chat/share/${shareCode}`);
            if (response.data.code === 1) {
                setShares(shares.filter(share => share.shareCode !== shareCode));
                toast.success(isZH ? '删除成功' : 'Delete successful');
            }
        } catch (error) {
            console.error('Failed to delete share:', error);
            toast.error(isZH ? '删除失败' : 'Delete failed');
        }
    };

    // 复制分享链接
    const handleCopyLink = (shareCode) => {
        const shareLink = `${window.location.origin}/chat/share/${shareCode}`;
        navigator.clipboard.writeText(shareLink);
        toast.success(isZH ? '链接已复制' : 'Link copied');
    };

    // 在新标签页打开分享链接
    const handleOpenShare = (shareCode) => {
        const shareLink = `${window.location.origin}/chat/share/${shareCode}`;
        window.open(shareLink, '_blank');
    };

    // 格式化时间
    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleString(isZH ? 'zh-CN' : 'en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    // 获取对话标题
    const getConversationTitle = (conversationId) => {
        // 先从已加载的对话列表中查找
        const conversation = conversations.find(c => c.id === conversationId);
        if (conversation) {
            return conversation.title || (isZH ? '未命名对话' : 'Untitled Conversation');
        }
        // 如果未找到，则返回null
        return null;
    };

    // 加载单个对话标题
    const loadConversationTitle = async (conversationId) => {
        setLoadingTitles(prev => ({ ...prev, [conversationId]: true }));
        try {
            const response = await axiosInstance.get(`/chat/conversation/${conversationId}/title`);
            if (response.data.code === 1) {
                // 更新shares中对应记录的标题
                setShares(prevShares => prevShares.map(share => 
                    share.conversationId === conversationId 
                        ? { ...share, loadedTitle: response.data.data } 
                        : share
                ));
            }
        } catch (error) {
            console.error('Failed to load conversation title:', error);
            toast.error(isZH ? '加载标题失败' : 'Failed to load title');
        } finally {
            setLoadingTitles(prev => ({ ...prev, [conversationId]: false }));
        }
    };

    return (
        <>
            <div className="modal-backdrop" onClick={onClose} />
            <div className="share-manage-modal">
                <div className="share-manage-modal-header">
                    <h2>{isZH ? '分享管理' : 'Share Management'}</h2>
                    <button className="close-button" onClick={onClose}>
                        <FaTimes />
                    </button>
                </div>

                <div className="share-manage-modal-content">
                    {loading ? (
                        <div className="loading-spinner">{isZH ? '加载中...' : 'Loading...'}</div>
                    ) : error ? (
                        <div className="error-message">{error}</div>
                    ) : shares.length === 0 ? (
                        <div className="no-shares">{isZH ? '暂无分享记录' : 'No shares yet'}</div>
                    ) : (
                        <div className="shares-list">
                            {shares.map((share) => {
                                const title = share.loadedTitle || getConversationTitle(share.conversationId);
                                return (
                                    <div key={share.shareCode} className="share-item">
                                        <div className="share-info">
                                            <div className="share-title">
                                                {title ? (
                                                    title
                                                ) : (
                                                    <button 
                                                        className="load-title-button"
                                                        onClick={() => loadConversationTitle(share.conversationId)}
                                                        disabled={loadingTitles[share.conversationId]}
                                                    >
                                                        {loadingTitles[share.conversationId] ? (
                                                            <FaSync className="loading-icon" />
                                                        ) : (
                                                            isZH ? '加载对话标题' : 'Load Title'
                                                        )}
                                                    </button>
                                                )}
                                                <span className="message-count">
                                                    ({share.messageIndexes?.length || 0} {isZH ? '条消息' : 'messages'})
                                                </span>
                                            </div>
                                            <div className="share-code" onClick={() => handleOpenShare(share.shareCode)}>
                                                <span>{isZH ? '分享码：' : 'Share Code: '}</span>
                                                <code>{share.shareCode}</code>
                                                <FaExternalLinkAlt className="external-link-icon" />
                                            </div>
                                            <div className="share-link">
                                                <span>{isZH ? '链接：' : 'Link: '}</span>
                                                <code>{`${window.location.origin}/chat/share/${share.shareCode}`}</code>
                                                <button 
                                                    className="copy-link-button"
                                                    onClick={() => handleCopyLink(share.shareCode)}
                                                    title={isZH ? "复制链接" : "Copy link"}
                                                >
                                                    <FaCopy />
                                                </button>
                                            </div>
                                            <div className="share-dates">
                                                <div>{isZH ? '创建时间：' : 'Created: '}{formatDate(share.createdAt)}</div>
                                                <div>{isZH ? '过期时间：' : 'Expires: '}{formatDate(share.expireAt)}</div>
                                            </div>
                                        </div>
                                        
                                        {editingShare === share.shareCode ? (
                                            <div className="edit-expiration">
                                                <div className="edit-mode-selector">
                                                    <label>
                                                        <input
                                                            type="radio"
                                                            value="preset"
                                                            checked={editMode === 'preset'}
                                                            onChange={(e) => setEditMode(e.target.value)}
                                                        />
                                                        {isZH ? '预设选项' : 'Preset Options'}
                                                    </label>
                                                    <label>
                                                        <input
                                                            type="radio"
                                                            value="custom"
                                                            checked={editMode === 'custom'}
                                                            onChange={(e) => setEditMode(e.target.value)}
                                                        />
                                                        {isZH ? '自定义时间' : 'Custom Date'}
                                                    </label>
                                                </div>
                                                
                                                {editMode === 'preset' ? (
                                                    <div className="preset-options">
                                                        {presetOptions.map(option => (
                                                            <button
                                                                key={option.value}
                                                                onClick={() => handleUpdateExpiration(share.shareCode, option.value)}
                                                                className="preset-option-button"
                                                            >
                                                                {option.label}
                                                            </button>
                                                        ))}
                                                    </div>
                                                ) : (
                                                    <div className="custom-date-picker">
                                                        <DatePicker
                                                            selected={selectedDate}
                                                            onChange={date => setSelectedDate(date)}
                                                            showTimeSelect
                                                            timeFormat="HH:mm"
                                                            timeIntervals={15}
                                                            dateFormat="yyyy-MM-dd HH:mm"
                                                            minDate={new Date()}
                                                            placeholderText={isZH ? "选择截止时间" : "Select expiration time"}
                                                        />
                                                        <button
                                                            className="confirm-date-button"
                                                            onClick={() => handleCustomDateUpdate(share.shareCode)}
                                                            disabled={!selectedDate}
                                                        >
                                                            {isZH ? '确认' : 'Confirm'}
                                                        </button>
                                                    </div>
                                                )}
                                                
                                                <button
                                                    className="cancel-edit-button"
                                                    onClick={() => {
                                                        setEditingShare(null);
                                                        setSelectedDate(null);
                                                    }}
                                                >
                                                    {isZH ? '取消' : 'Cancel'}
                                                </button>
                                            </div>
                                        ) : (
                                            <div className="share-actions">
                                                <button 
                                                    onClick={() => setEditingShare(share.shareCode)}
                                                    title={isZH ? "编辑过期时间" : "Edit expiration"}
                                                >
                                                    <FaEdit />
                                                </button>
                                                <button 
                                                    onClick={() => handleDelete(share.shareCode)}
                                                    title={isZH ? "删除" : "Delete"}
                                                    className="delete-button"
                                                >
                                                    <FaTrash />
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            </div>
        </>
    );
};

export default ShareManageModal; 