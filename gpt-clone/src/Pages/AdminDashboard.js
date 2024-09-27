import React from 'react';
import ModelManagement from './ModelManagement';
import './admin.css';

function AdminDashboard() {
    return (
        <div>
            <h2>Admin Dashboard</h2>
            <div>
                <h3>Model Management</h3>
                <ModelManagement/> {/* 模型管理 */}
            </div>
            {/*<div>*/}
            {/*    <h3>User Management</h3>*/}
            {/*    <UserManagement />  /!* 用户管理 *!/*/}
            {/*</div>*/}
            {/*<div>*/}
            {/*    <h3>Conversation History</h3>*/}
            {/*    /!* 可以将 conversation 用户ID 作为 prop 传递进去 *!/*/}
            {/*    <ConversationHistory userId={null} />  /!* 对话历史 *!/*/}
            {/*</div>*/}
        </div>
    );
}

export default AdminDashboard;
