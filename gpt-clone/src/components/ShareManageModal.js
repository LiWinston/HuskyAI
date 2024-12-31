import React, { useState, useEffect } from 'react';
import { FaTimes, FaEdit, FaTrash, FaCopy } from 'react-icons/fa';
import axiosInstance from '../api/axiosConfig';
import './ShareManageModal.css';

const ShareManageModal = ({ onClose, isZH }) => {
    const [shares, setShares] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // 加载分享记录
    useEffect(() => {
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

        fetchShares();
    }, [isZH]);

    // 更新过期时间
    const handleUpdateExpiration = async (shareCode, newHours) => {
        try {
            const response = await axiosInstance.put(`/chat/share/${shareCode}/expiration`, {
                expirationHours: newHours
            });
            if (response.data.code === 1) {
                // 重新加载分享记录
                const uuid = localStorage.getItem('userUUID');
                const sharesResponse = await axiosInstance.get(`/chat/share/user/${uuid}`);
                if (sharesResponse.data.code === 1) {
                    setShares(sharesResponse.data.data);
                }
            }
        } catch (error) {
            console.error('Failed to update expiration:', error);
        }
    };

    // 删除分享
    const handleDelete = async (shareCode) => {
        try {
            const response = await axiosInstance.delete(`/chat/share/${shareCode}`);
            if (response.data.code === 1) {
                setShares(shares.filter(share => share.shareCode !== shareCode));
            }
        } catch (error) {
            console.error('Failed to delete share:', error);
        }
    };

    // 复制分享链接
    const handleCopyLink = (shareCode) => {
        const shareLink = `${window.location.origin}/chat/share/${shareCode}`;
        navigator.clipboard.writeText(shareLink);
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
                            {shares.map((share) => (
                                <div key={share.shareCode} className="share-item">
                                    <div className="share-info">
                                        <div className="share-code">
                                            {isZH ? '分享码：' : 'Share Code: '}{share.shareCode}
                                        </div>
                                        <div className="share-dates">
                                            <div>{isZH ? '创建时间：' : 'Created: '}{formatDate(share.createdAt)}</div>
                                            <div>{isZH ? '过期时间：' : 'Expires: '}{formatDate(share.expireAt)}</div>
                                        </div>
                                    </div>
                                    <div className="share-actions">
                                        <button 
                                            onClick={() => handleUpdateExpiration(share.shareCode, 24)}
                                            title={isZH ? "延长24小时" : "Extend 24 hours"}
                                        >
                                            <FaEdit />
                                        </button>
                                        <button 
                                            onClick={() => handleCopyLink(share.shareCode)}
                                            title={isZH ? "复制链接" : "Copy link"}
                                        >
                                            <FaCopy />
                                        </button>
                                        <button 
                                            onClick={() => handleDelete(share.shareCode)}
                                            title={isZH ? "删除" : "Delete"}
                                            className="delete-button"
                                        >
                                            <FaTrash />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </>
    );
};

export default ShareManageModal; 