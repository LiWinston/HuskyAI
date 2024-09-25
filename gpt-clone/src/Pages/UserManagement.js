// UserManagement.js
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import ConversationHistory from './ConversationHistory';  // 引入ConversationHistory组件

function UserManagement() {
    const [users, setUsers] = useState([]);
    const [expandedUser, setExpandedUser] = useState(null); // 存储当前展开对话的用户ID

    // 获取用户状态
    const fetchUsers = async () => {
        try {
            const response = await axios.get('/admin/users');
            setUsers(response.data.users);
        } catch (error) {
            console.error("Error fetching users:", error);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    // 更新用户信息
    const updateUser = async (userId, updatedData) => {
        try {
            await axios.post(`/admin/users/${userId}`, updatedData);
            fetchUsers();  // 刷新用户列表
        } catch (error) {
            console.error("Error updating user:", error);
        }
    };

    // 切换展开对话历史的用户
    const toggleConversationHistory = (userId) => {
        if (expandedUser === userId) {
            setExpandedUser(null); // 如果当前已展开，点击时折叠
        } else {
            setExpandedUser(userId); // 展开选中用户的对话历史
        }
    };

    return (
        <div>
            <h3>User Management</h3>
            <ul>
                {users.map(user => (
                    <li key={user.uuid}>
                        <div onClick={() => toggleConversationHistory(user.uuid)}>
                            {user.username} ({user.email}) - {user.role}
                        </div>
                        <button onClick={() => updateUser(user.uuid, { role: 'ADMIN' })}>Promote to Admin</button>
                        {expandedUser === user.uuid && (  // 如果是当前展开的用户，则显示对话历史
                            <ConversationHistory userId={user.uuid} />  // 显示对话历史组件
                        )}
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default UserManagement;
