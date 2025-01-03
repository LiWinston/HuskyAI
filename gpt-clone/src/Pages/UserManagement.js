import React, {useEffect, useState} from 'react';
import axios from 'axios';
import {ChevronDown, ChevronUp} from 'lucide-react';

import './um.css';
import {showSweetAlertWithRetVal} from "../Component/sweetAlertUtil";
import Swal from "sweetalert2";
import axiosInstance from '../api/axiosConfig';

function UserManagement() {
    const [users, setUsers] = useState([]);
    const [expandedUser, setExpandedUser] = useState(null); // Stores the user ID of the currently opened conversation
    const [userModelAccess, setUserModelAccess] = useState({}); // store user model access permissions
    const [expandedUsers, setExpandedUsers] = useState({});
    // Get user status.
    const [promotingUser, setPromotingUser] = useState(null);
    const [editingUser, setEditingUser] = useState(null);
    const [adminInfo, setAdminInfo] = useState({
        email: '',
        adminLevel: 0,
        role: 'USER'
    });

    const fetchUsers = async () => {
        try {
            const response = await axiosInstance.get('/admin/user');
            setUsers(response.data.data || []);
        } catch (error) {
            console.error('Error fetching users:', error);
            console.error('Error details:', {
                status: error.response?.status,
                data: error.response?.data,
                headers: error.response?.headers
            }); // 详细错误日志
            
            let errorMessage = '获取用户列表失败: ';
            if (error.response?.status === 403) {
                errorMessage += '没有访问权限';
            } else {
                errorMessage += error.response?.data?.msg || error.message || '未知错误';
            }
            await showSweetAlertWithRetVal(errorMessage, {icon: 'error', title: '错误'});
            setUsers([]); // 设置空数组避免渲染错误
        }
    };

    const fetchUserModelAccess = async () => {
        try {
            const response = await axiosInstance.get('/admin/user/modelAccess');
            
            // 添加空值检查
            if (!response.data || !response.data.data) {
                setUserModelAccess({});
                return;
            }
            
            const modelAccessData = response.data.data.reduce((acc, config) => {
                if (config && config.userId) {
                    acc[config.userId] = config.allowedModels || [];
                }
                return acc;
            }, {});
            setUserModelAccess(modelAccessData);
        } catch (error) {
            console.error('Error fetching user model access:', error);
            let errorMessage = '获取模型权限失败: ';
            if (error.response?.status === 403) {
                errorMessage += '没有访问权限';
            } else {
                errorMessage += error.response?.data?.message || error.message || '未知错误';
            }
            await showSweetAlertWithRetVal(errorMessage, {icon: 'error', title: '错误'});
            // 设置空对象避免渲染错误
            setUserModelAccess({});
        }
    };

    useEffect(() => {
        fetchUsers();
        fetchUserModelAccess();
    }, []);

    const updateUser = async (userId, updatedData) => {
        try {
            const response = await axiosInstance.post(`/admin/user/${userId}`, updatedData);
            await fetchUsers();
        } catch (error) {
            console.error('Error updating user:', error);
            let errorMessage = '更新用户信息失败: ';
            if (error.response?.status === 403) {
                errorMessage += '没有访问权限';
            } else {
                errorMessage += error.response?.data?.message || error.message || '未知错误';
            }
            await showSweetAlertWithRetVal(errorMessage, {icon: 'error', title: '错误'});
        }
    };

    //Switch the user who expanded the conversation history
    const toggleConversationHistory = (userId) => {
        if (expandedUser === userId) {
            setExpandedUser(null); // If currently expanded, click to collapse.
        } else {
            setExpandedUser(userId); // Expand the selected user's chat history.
        }
    };

// Handle model access changes and only update local state
    const handleModelAccessChange = (userId, index, key, value) => {
        setUserModelAccess(prevAccess => {
            const userAccess = prevAccess[userId] || [];
            const updatedAccess = [...userAccess];
            if (!updatedAccess[index]) {
                updatedAccess[index] = {};
            }
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

// Handling changes to AccessRestriction.
    const handleAccessRestrictionChange = (userId, index, restrictionKey, value) => {
        setUserModelAccess(prevAccess => {
            const userAccess = prevAccess[userId] || [];
            const updatedAccess = [...userAccess];
            if (!updatedAccess[index]) {
                updatedAccess[index] = {};
            }
            const accessRestriction = updatedAccess[index].accessRestriction || {};
            accessRestriction[restrictionKey] = value;
            updatedAccess[index] = {
                ...updatedAccess[index],
                accessRestriction
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
        return date.toISOString().slice(0, 16); // keep only until minutes
    };

    // 统一的错误处理函数
    const handleApiError = async (error, defaultMessage) => {
        console.error(defaultMessage, error);
        let errorMessage = defaultMessage + ': ';
        
        if (error.response) {
            switch (error.response.status) {
                case 403:
                    errorMessage += '您没有足够的权限执行此操作';
                    break;
                case 400:
                    if (error.response.data && error.response.data.msg) {
                        if (error.response.data.msg.includes('insufficient admin level')) {
                            errorMessage += '无法操作更高级别的管理员';
                        } else {
                            errorMessage += error.response.data.msg;
                        }
                    }
                    break;
                default:
                    errorMessage += error.response.data?.msg || '未知错误';
            }
        } else {
            errorMessage += error.message || '未知错误';
        }
        
        await showSweetAlertWithRetVal(errorMessage, {icon: 'error', title: '错误'});
        return false; // 返回 false 表示操作失败
    };

    const handlePromoteSubmit = async () => {
        try {
            const response = await axiosInstance.post(`/admin/user/${promotingUser.uuid}`, {
                role: 'ADMIN',
                email: adminInfo.email,
                adminLevel: adminInfo.adminLevel,
                verified: true
            });

            // 检查响应状态
            if (response.data.code === 0) {
                throw new Error(response.data.msg);
            }
            
            setPromotingUser(null);
            setAdminInfo({email: '', adminLevel: 0});
            await fetchUsers();
            await showSweetAlertWithRetVal('用户已成功升级为管理员', {icon: 'success', title: '成功'});
        } catch (error) {
            await handleApiError(error, '提升用户权限失败');
            // 保持对话框打开，让用户可以修改输入
            return false;
        }
    };

    const handleDemoteClick = async (userId) => {
        try {
            const response = await axiosInstance.post(`/admin/user/${userId}`, {
                role: 'USER'
            });

            // 检查响应状态
            if (response.data.code === 0) {
                throw new Error(response.data.msg);
            }

            await fetchUsers();
            await showSweetAlertWithRetVal('用户已成功降级为普通用户', {icon: 'success', title: '成功'});
        } catch (error) {
            await handleApiError(error, '降级用户失败');
        }
    };

    const handleAdminInfoSubmit = async () => {
        try {
            const isPromoting = editingUser.role === 'USER' && adminInfo.role === 'ADMIN';
            const isDemoting = editingUser.role === 'ADMIN' && adminInfo.role === 'USER';
            
            const response = await axiosInstance.post(`/admin/user/${editingUser.uuid}`, {
                role: adminInfo.role,
                email: adminInfo.email,
                adminLevel: adminInfo.adminLevel
            });

            // 检查响应状态
            if (response.data.code === 0) {
                throw new Error(response.data.msg);
            }
            
            setEditingUser(null);
            setAdminInfo({email: '', adminLevel: 0, role: 'USER'});
            await fetchUsers();
            
            let message = isPromoting ? '用户已成功升级为管理员' :
                         isDemoting ? '管理员已成功降级为普通用户' :
                         '管理员信息更新成功';
            
            await showSweetAlertWithRetVal(message, {icon: 'success', title: '成功'});
        } catch (error) {
            await handleApiError(error, '更新管理员信息失败');
        }
    };

    const saveModelAccess = async (userId) => {
        try {
            const modelAccess = userModelAccess[userId] || [];
            const response = await axiosInstance.put(`/admin/user/modelAccess/${userId}`, modelAccess);

            // 检查响应状态
            if (response.data.code === 0) {
                throw new Error(response.data.msg);
            }

            await showSweetAlertWithRetVal('模型权限更新成功', {icon: 'success', title: '成功'});
        } catch (error) {
            await handleApiError(error, '保存模型权限失败');
        }
    };

    const handleNewAttributeChange = (userId, index, key, value) => {
        setUserModelAccess(prevAccess => {
            const updatedAccess = [...(prevAccess[userId] || [])];
            const currentModel = updatedAccess[index] || {};
            currentModel.newAttribute = {
                ...currentModel.newAttribute,
                [key]: value
            };
            updatedAccess[index] = currentModel;

            return {
                ...prevAccess,
                [userId]: updatedAccess
            };
        });
    };

    const handleAdditionalAttributesChange = (userId, index, key, value) => {
        setUserModelAccess(prevAccess => {
            const updatedAccess = [...(prevAccess[userId] || [])];
            const currentModel = updatedAccess[index] || {};
            const additionalAttributes = currentModel.additionalAttributes || {};
            additionalAttributes[key] = value;//Update the value of the specified key
            updatedAccess[index] = {
                ...currentModel,
                additionalAttributes: additionalAttributes
            };

            return {
                ...prevAccess,
                [userId]: updatedAccess
            };
        });
    };

    const addNewAttribute = (userId, index) => {
        const currentModel = userModelAccess[userId][index];
        const { key, value } = currentModel.newAttribute || {};

        if (key && value) {
            handleAdditionalAttributesChange(userId, index, key, value);
            handleNewAttributeChange(userId, index, 'key', '');
            handleNewAttributeChange(userId, index, 'value', '');
        } else {
            alert('Both key and value are required.');
        }
    };

    const handleDeleteAttribute = (userId, index, key) => {
        setUserModelAccess(prevAccess => {
            const updatedAccess = [...(prevAccess[userId] || [])];
            const currentModel = updatedAccess[index] || {};
            const additionalAttributes = currentModel.additionalAttributes || {};
            delete additionalAttributes[key];

            updatedAccess[index] = {
                ...currentModel,
                additionalAttributes: additionalAttributes
            };

            return {
                ...prevAccess,
                [userId]: updatedAccess
            };
        });
    };


    const toggleUserExpansion = (userId) => {
        setExpandedUsers(prev => ({...prev, [userId]: !prev[userId]}));
    };

    const handlePromoteClick = (user) => {
        setPromotingUser(user);
        setAdminInfo({
            email: '',
            adminLevel: 0,
            role: 'ADMIN'
        });
    };

    const handleEditAdminInfo = (user) => {
        setEditingUser(user);
        // 如果是管理员,使用现有信息
        if (user.role === 'ADMIN') {
            setAdminInfo({
                email: user.email || '',
                adminLevel: user.adminLevel || 0,
                role: 'ADMIN'
            });
        } else {
            // 如果是普通用户,设置默认值
            setAdminInfo({
                email: '',
                adminLevel: 0,
                role: 'USER'
            });
        }
    };

    return (
        <div className="user-management">
            <h3>User Management</h3>
            <ul className="user-list">
                {users.map(user => (
                    <li key={user.uuid} className="user-item">
                        <div className="user-summary" onClick={() => toggleUserExpansion(user.uuid)}>
                            <span>
                                {user.username} ({user.role === 'ADMIN' ? `Admin Level ${user.adminLevel}` : 'User'})
                            </span>
                            <span className="user-uuid">UUID: {user.uuid}</span>
                            {expandedUsers[user.uuid] ? <ChevronUp /> : <ChevronDown />}
                        </div>
                        {expandedUsers[user.uuid] && (
                            <div className="user-details">
                                <button onClick={() => handleEditAdminInfo(user)}>
                                    Update Admin Info
                                </button>
                                {user.role === 'USER' ? (
                                    <button onClick={() => handlePromoteClick(user)}>
                                        Promote to Admin
                                    </button>
                                ) : (
                                    <button onClick={() => handleDemoteClick(user.uuid)}>
                                        Demote to User
                                    </button>
                                )}
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
                                                    <div>
                                                        {Object.keys(modelAccess.additionalAttributes || {}).map((attrKey, i) => (
                                                            <div key={i} className="key-value-pair">
                                                                <span>{attrKey}:</span>
                                                                <input
                                                                    type="text"
                                                                    value={modelAccess.additionalAttributes[attrKey]}
                                                                    onChange={(e) => handleAdditionalAttributesChange(user.uuid, index, attrKey, e.target.value)}
                                                                />
                                                                <button
                                                                    onClick={() => handleDeleteAttribute(user.uuid, index, attrKey)}>Delete
                                                                </button>
                                                            </div>
                                                        ))}
                                                        <div className="new-key-value">
                                                            <input
                                                                type="text"
                                                                placeholder="New Key"
                                                                value={modelAccess.newAttribute?.key || ''}
                                                                onChange={(e) => handleNewAttributeChange(user.uuid, index, 'key', e.target.value)}
                                                            />
                                                            <input
                                                                type="text"
                                                                placeholder="New Value"
                                                                value={modelAccess.newAttribute?.value || ''}
                                                                onChange={(e) => handleNewAttributeChange(user.uuid, index, 'value', e.target.value)}
                                                            />
                                                            <button
                                                                onClick={() => addNewAttribute(user.uuid, index)}>Add
                                                            </button>
                                                        </div>
                                                    </div>
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

            {/* 管理员信息编辑对话框 */}
            {editingUser && (
                <div className="modal">
                    <div className="modal-content">
                        <h4>Update Admin Info - {editingUser.username}</h4>
                        <div className="form-group">
                            <label>Role:</label>
                            <select
                                value={adminInfo.role}
                                onChange={(e) => setAdminInfo({...adminInfo, role: e.target.value})}
                            >
                                <option value="USER">User</option>
                                <option value="ADMIN">Admin</option>
                            </select>
                        </div>
                        {(adminInfo.role === 'ADMIN') && (
                            <>
                                <div className="form-group">
                                    <label>Email:</label>
                                    <input
                                        type="email"
                                        value={adminInfo.email}
                                        onChange={(e) => setAdminInfo({...adminInfo, email: e.target.value})}
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Admin Level:</label>
                                    <select
                                        value={adminInfo.adminLevel}
                                        onChange={(e) => setAdminInfo({...adminInfo, adminLevel: parseInt(e.target.value)})}
                                    >
                                        <option value={0}>Level 0 (Basic)</option>
                                        <option value={1}>Level 1</option>
                                        <option value={2}>Level 2</option>
                                        <option value={3}>Level 3</option>
                                    </select>
                                </div>
                            </>
                        )}
                        <div className="modal-buttons">
                            <button onClick={handleAdminInfoSubmit}>Save</button>
                            <button onClick={() => setEditingUser(null)}>Cancel</button>
                        </div>
                    </div>
                </div>
            )}

            {/* 管理员提升对话框 */}
            {promotingUser && (
                <div className="modal">
                    <div className="modal-content">
                        <h4>Promote {promotingUser.username} to Admin</h4>
                        <div className="form-group">
                            <label>Email:</label>
                            <input
                                type="email"
                                value={adminInfo.email}
                                onChange={(e) => setAdminInfo({...adminInfo, email: e.target.value})}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label>Admin Level:</label>
                            <select
                                value={adminInfo.adminLevel}
                                onChange={(e) => setAdminInfo({...adminInfo, adminLevel: parseInt(e.target.value)})}
                            >
                                <option value={0}>Level 0 (Basic)</option>
                                <option value={1}>Level 1</option>
                                <option value={2}>Level 2</option>
                                <option value={3}>Level 3</option>
                            </select>
                        </div>
                        <div className="modal-buttons">
                            <button onClick={handlePromoteSubmit}>Confirm</button>
                            <button onClick={() => setPromotingUser(null)}>Cancel</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

// 加样式
const modalStyles = `
.modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}

.modal-content {
    background-color: white;
    padding: 20px;
    border-radius: 8px;
    width: 400px;
}

.form-group {
    margin-bottom: 15px;
}

.form-group label {
    display: block;
    margin-bottom: 5px;
}

.form-group input,
.form-group select {
    width: 100%;
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 4px;
}

.modal-buttons {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 20px;
}

.modal-buttons button {
    padding: 8px 16px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
}

.modal-buttons button:first-child {
    background-color: #4CAF50;
    color: white;
}

.modal-buttons button:last-child {
    background-color: #f44336;
    color: white;
}
`;

// 将样式添加到文档中
const styleSheet = document.createElement("style");
styleSheet.innerText = modalStyles;
document.head.appendChild(styleSheet);

export default UserManagement;
