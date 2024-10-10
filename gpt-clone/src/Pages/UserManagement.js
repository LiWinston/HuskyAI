import React, {useEffect, useState} from 'react';
import axios from 'axios';
import {ChevronDown, ChevronUp} from 'lucide-react';

import './um.css';

function UserManagement() {
    const [users, setUsers] = useState([]);
    const [expandedUser, setExpandedUser] = useState(null); // 存储当前展开对话的用户ID
    const [userModelAccess, setUserModelAccess] = useState({}); // 存储用户模型访问权限
    const [expandedUsers, setExpandedUsers] = useState({});
    // 获取用户状态
    const fetchUsers = async () => {
        try {
            const response = await axios.get(window.API_BASE_URL + '/admin/user');
            setUsers(response.data.data);
        } catch (error) {
            console.error('Error fetching users:', error);
        }
    };

    const fetchUserModelAccess = async () => {
        try {
            const response = await axios.get(window.API_BASE_URL + '/admin/user/modelAccess');
            const modelAccessData = response.data.data.reduce((acc, config) => {
                acc[config.userId] = config.allowedModels;
                return acc;
            }, {});
            setUserModelAccess(modelAccessData);
        } catch (error) {
            console.error('Error fetching user model access:', error);
        }
    };

    useEffect(() => {
        fetchUsers();
        fetchUserModelAccess();
    }, []);

    const updateUser = async (userId, updatedData) => {
        try {
            await axios.post(`${window.API_BASE_URL}/admin/users/${userId}`, updatedData);
            fetchUsers();
        } catch (error) {
            console.error('Error updating user:', error);
        }
    };

    //Switch the user who expanded the conversation history
    const toggleConversationHistory = (userId) => {
        if (expandedUser === userId) {
            setExpandedUser(null); // 如果当前已展开，点击时折叠
        } else {
            setExpandedUser(userId); // 展开选中用户的对话历史
        }
    };

// Handle model access changes and only update local state
    const handleModelAccessChange = (userId, index, key, value) => {
        setUserModelAccess(prevAccess => {
            const updatedAccess = [...(prevAccess[userId] || [])];
            updatedAccess[index] = {
                ...updatedAccess[index],
                [key]: value
            };
            return {
                ...prevAccess,
                [userId]: updatedAccess
            };
        });
    };

// 处理 AccessRestriction 的更改
    const handleAccessRestrictionChange = (userId, index, restrictionKey, value) => {
        setUserModelAccess(prevAccess => {
            const updatedAccess = [...(prevAccess[userId] || [])];
            const accessRestriction = updatedAccess[index].accessRestriction || {};
            accessRestriction[restrictionKey] = value;  // 更新指定字段
            updatedAccess[index] = {
                ...updatedAccess[index],
                accessRestriction  // 将更新后的 accessRestriction 放回去
            };
            return {
                ...prevAccess,
                [userId]: updatedAccess
            };
        });
    };

    const formatDateForInput = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        return date.toISOString().slice(0, 16); // 只保留到分钟
    };


    const saveModelAccess = async (userId) => {
        try {
            const updatedModelAccess = userModelAccess[userId].map(modelAccess => ({
                url: modelAccess.url,
                model: modelAccess.model,
                accessLevel: modelAccess.accessLevel,
                accessRestriction: modelAccess.accessRestriction || {
                    startTime: null,
                    endTime: null,
                    timeRestricted: false,
                    maxDailyAccess: 0
                },
                priority: modelAccess.priority,
                additionalAttributes: modelAccess.additionalAttributes || {}
            }));

            await axios.put(`${window.API_BASE_URL}/admin/user/modelAccess/${userId}`, updatedModelAccess);
            console.log('Model access saved for user:', userId);
        } catch (error) {
            console.error('Error saving model access:', error);
        }
    };



    const toggleUserExpansion = (userId) => {
        setExpandedUsers(prev => ({...prev, [userId]: !prev[userId]}));
    };


    return (
        <div className="user-management">
            <h3>User Management</h3>
            <ul className="user-list">
                {users.map(user => (
                    <li key={user.uuid} className="user-item">
                        <div className="user-summary" onClick={() => toggleUserExpansion(user.uuid)}>
                            <span>{user.username} ({user.role})</span>
                            <span className="user-uuid">UUID: {user.uuid}</span>
                            {expandedUsers[user.uuid] ? <ChevronUp /> : <ChevronDown />}
                        </div>
                        {expandedUsers[user.uuid] && (
                            <div className="user-details">
                                <button onClick={() => updateUser(user.uuid, {role: 'ADMIN'})}>
                                    Promote to Admin
                                </button>
                                <div className="model-access-info">
                                    <h4>Allowed Models:</h4>
                                    <table>
                                        <thead>
                                        <tr>
                                            <th>Model</th>
                                            <th>URL</th>
                                            <th>Access Level</th>
                                            <th>Access Restriction</th>
                                            <th>Priority</th>
                                            <th>Additional Attributes</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {userModelAccess[user.uuid]?.map((modelAccess, index) => (
                                            <tr key={index}>
                                                <td>{modelAccess.model}</td>
                                                <td>
                                                    <input
                                                        type="text"
                                                        value={modelAccess.url}
                                                        onChange={(e) => handleModelAccessChange(user.uuid, index, 'url', e.target.value)}
                                                    />
                                                </td>
                                                <td>
                                                    <select
                                                        value={modelAccess.accessLevel}
                                                        onChange={(e) => handleModelAccessChange(user.uuid, index, 'accessLevel', e.target.value)}
                                                    >
                                                        <option value="null">No Restriction</option>
                                                        <option value="NOSTREAM">No Stream</option>
                                                        <option value="LIMITCONTEXT">Limit Context</option>
                                                    </select>
                                                </td>
                                                <td>
                                                    <label>Start Time:
                                                        <input
                                                            type="datetime-local"
                                                            value={formatDateForInput(modelAccess.accessRestriction?.startTime)}
                                                            onChange={(e) => handleAccessRestrictionChange(user.uuid, index, 'startTime', e.target.value)}
                                                        />
                                                    </label>
                                                    <label>End Time:
                                                        <input
                                                            type="datetime-local"
                                                            value={formatDateForInput(modelAccess.accessRestriction?.endTime)}
                                                            onChange={(e) => handleAccessRestrictionChange(user.uuid, index, 'endTime', e.target.value)}
                                                        />
                                                    </label>
                                                    <label>Time Restricted:
                                                        <input
                                                            type="checkbox"
                                                            checked={modelAccess.accessRestriction?.timeRestricted || false}
                                                            onChange={(e) => handleAccessRestrictionChange(user.uuid, index, 'timeRestricted', e.target.checked)}
                                                        />
                                                    </label>
                                                    <label>Max Daily Access:
                                                        <input
                                                            type="number"
                                                            value={modelAccess.accessRestriction?.maxDailyAccess || 0}
                                                            onChange={(e) => handleAccessRestrictionChange(user.uuid, index, 'maxDailyAccess', parseInt(e.target.value))}
                                                        />
                                                    </label>
                                                </td>
                                                <td>
                                                    <input
                                                        type="number"
                                                        value={modelAccess.priority}
                                                        onChange={(e) => handleModelAccessChange(user.uuid, index, 'priority', parseInt(e.target.value))}
                                                    />
                                                </td>
                                                <td>
                                                    <textarea
                                                        value={JSON.stringify(modelAccess.additionalAttributes)}
                                                        onChange={(e) => handleModelAccessChange(user.uuid, index, 'additionalAttributes', JSON.parse(e.target.value))}
                                                    />
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                    <button onClick={() => saveModelAccess(user.uuid)}>Save Model Access</button>
                                </div>
                            </div>
                        )}
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default UserManagement;