import React from 'react';
import ModelManagement from './ModelManagement';
import UserManagement from "./UserManagement";
import ConversationHistory from "./ConversationHistory";

function AdminDashboard() {
    return (
        <div className="admin-dashboard">
            <header>
                <h2>Admin Dashboard</h2>
            </header>
            <main>
                <section className="dashboard-section">
                    <h3>Model Management</h3>
                    <div className="scrollable-container">
                        <ModelManagement />
                    </div>
                </section>
                <section className="dashboard-section">
                    <h3>User Management</h3>
                    <div className="scrollable-container">
                        <UserManagement />
                    </div>
                </section>
                <div>
                    <h3>Conversation History</h3>
                    {/* You can pass the conversation user ID as prop */}
                    <div className="scrollable-container">
                        <ConversationHistory />
                    </div>
                </div>
            </main>
        </div>
    );
}

export default AdminDashboard;