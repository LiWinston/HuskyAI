import React, {useEffect, useState} from 'react';
import axios from 'axios';
import {ChevronDown, ChevronUp} from 'lucide-react';

import './um.css';
import {showSweetAlertWithRetVal} from "../Component/sweetAlertUtil";
import Swal from "sweetalert2";

function UserManagement() {
    const [users, setUsers] = useState([]);
    const [expandedUser, setExpandedUser] = useState(null); // Stores the user ID of the currently opened conversation
    const [userModelAccess, setUserModelAccess] = useState({}); // store user model access permissions
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
            setExpandedUser(null); // if currently expanded collapse when clicked
        } else {
            setExpandedUser(userId); // Expand the conversation history of the selected user
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
            accessRestriction[restrictionKey] = value;  // update specified field
            updatedAccess[index] = {
                ...updatedAccess[index],
                accessRestriction  // PutTheUpdatedAccessRestrictionBack
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

            await axios.put(`${window.API_BASE_URL}/admin/user/modelAccess/${userId}`, updatedModelAccess)
                .then(res => {
                    if (res.data.code) {
                        // Success: allow user to choose whether to refresh
                        Swal.fire({
                            icon: 'success',
                            title: 'Success',
                            text: res.data.msg,
                            showCancelButton: true,
                            confirmButtonText: 'Yes, refresh!',
                            cancelButtonText: 'No, stay here',
                        }).then((result) => {
                            if (result.isConfirmed) {
                                fetchUserModelAccess(); // Refresh on user's confirmation
                            }
                        });
                    } else {
                        // Failure: force refresh, no option to skip
                        Swal.fire({
                            icon: 'error',
                            title: 'Error',
                            text: res.data.msg,
                            confirmButtonText: 'Refresh Now',
                            allowOutsideClick: false, // Prevent closing by clicking outside
                            allowEscapeKey: false // Prevent closing with Esc key
                        }).then(() => {
                            fetchUserModelAccess(); // Always refresh after error
                        });
                    }
                });

        } catch (error) {
            console.error('Error saving model access:', error);
            // In case of a request failure or any error, ensure user knows something went wrong
            Swal.fire({
                icon: 'error',
                title: 'Request Failed',
                text: 'An unexpected error occurred. Please try again later.',
                confirmButtonText: 'Refresh Now',
                allowOutsideClick: false,
                allowEscapeKey: false
            }).then(() => {
                fetchUserModelAccess(); // Ensure refresh after error
            });
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
                                                        {/* 新增 key-value 输入框 */}
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
        </div>
    );
}

export default UserManagement;