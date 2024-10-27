// UserControlCenter.js
import React, { useState, useEffect } from 'react';
import './UserControlCenter.css';
import axios from "axios";
import { motion, AnimatePresence } from 'framer-motion';

const UserControlCenter = ({ username }) => {
    const [isOpen, setIsOpen] = useState(false);
    const [activeMenu, setActiveMenu] = useState('settings');
    const [systemPrompt, setSystemPrompt] = useState('');
    const [shares, setShares] = useState([]);
    const [userSettings, setUserSettings] = useState({
        username: username,
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });

    const menuItems = [
        { id: 'settings', label: 'User Settings' },
        { id: 'prompt', label: 'System Prompt' },
        { id: 'shares', label: 'Share Management' }
    ];

    useEffect(() => {
        if (isOpen) {
            fetchShares();
        }
    }, [isOpen]);

    const fetchShares = async () => {
        try {
            const response = await axios.get(`${window.API_BASE_URL}/chat/shares`, {
                params: { uuid: localStorage.getItem('userUUID') }
            });
            setShares(response.data.data);
        } catch (error) {
            console.error('Error fetching shares:', error);
        }
    };

    const handlePasswordUpdate = async () => {
        // Implement password update logic
    };

    const handleSystemPromptUpdate = async () => {
        // Implement system prompt update logic
    };

    const handleDeleteShare = async (shareCode) => {
        // Implement share deletion logic
    };

    const renderContent = () => {
        switch (activeMenu) {
            case 'settings':
                return (
                    <div className="settings-content">
                        <h3>User Settings</h3>
                        <div className="form-group">
                            <label>Username</label>
                            <input
                                type="text"
                                value={userSettings.username}
                                onChange={(e) => setUserSettings({...userSettings, username: e.target.value})}
                            />
                        </div>
                        <div className="form-group">
                            <label>Current Password</label>
                            <input
                                type="password"
                                value={userSettings.currentPassword}
                                onChange={(e) => setUserSettings({...userSettings, currentPassword: e.target.value})}
                            />
                        </div>
                        {/* Add other password fields */}
                        <button onClick={handlePasswordUpdate}>Update</button>
                    </div>
                );

            case 'prompt':
                return (
                    <div className="prompt-content">
                        <h3>System Prompt Settings</h3>
                        <textarea
                            value={systemPrompt}
                            onChange={(e) => setSystemPrompt(e.target.value)}
                            maxLength={500}
                            placeholder="Enter your system prompt here..."
                        />
                        <div className="char-count">{systemPrompt.length}/500</div>
                        <button onClick={handleSystemPromptUpdate}>Save</button>
                    </div>
                );

            case 'shares':
                return (
                    <div className="shares-content">
                        <h3>Share Management</h3>
                        <div className="shares-list">
                            {shares.map(share => (
                                <div key={share.shareCode} className="share-item">
                                    <div>Share Code: {share.shareCode}</div>
                                    <div>Conversation ID: {share.conversationId}</div>
                                    <div>Message Indexes: {share.messageIndexes.join(', ')}</div>
                                    <button onClick={() => handleDeleteShare(share.shareCode)}>Delete</button>
                                </div>
                            ))}
                        </div>
                    </div>
                );
        }
    };

    return (
        <>
            <div className="user-control-button" onClick={() => setIsOpen(true)}>
                <span>User: {username}</span>
            </div>

            {isOpen && (
                <motion.div
                    className="user-control-modal"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.2 }}
                >
                    <div className="modal-content">
                        <div className="modal-header">
                            <h2>User Control Center</h2>
                            <button onClick={() => setIsOpen(false)}>&times;</button>
                        </div>
                        <div className="modal-body">
                            <div className="sidebar">
                                {menuItems.map(item => (
                                    <div
                                        key={item.id}
                                        className={`menu-item ${activeMenu === item.id ? 'active' : ''}`}
                                        onClick={() => setActiveMenu(item.id)}
                                    >
                                        {item.label}
                                    </div>
                                ))}
                            </div>
                            <div className="content">
                                {renderContent()}
                            </div>
                        </div>
                    </div>
                </motion.div>
            )}
        </>
    );
};

export default UserControlCenter;