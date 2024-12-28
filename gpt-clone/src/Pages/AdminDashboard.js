import React from 'react';
import ModelManagement from './ModelManagement';
import UserManagement from "./UserManagement";
import ConversationHistory from "./ConversationHistory";
import './AdminDashboard.css';

function AdminDashboard() {
    return (
        <div className="admin-dashboard">
            <header className="dashboard-header">
                <h2>Admin Dashboard</h2>
            </header>
            <main className="dashboard-main">
                <section className="dashboard-section">
                    <div className="section-header">
                        <h3>Model Management</h3>
                    </div>
                    <div className="section-content">
                        <ModelManagement />
                    </div>
                </section>
                <section className="dashboard-section">
                    <div className="section-header">
                        <h3>User Management</h3>
                    </div>
                    <div className="section-content">
                        <UserManagement />
                    </div>
                </section>
                <section className="dashboard-section">
                    <div className="section-header">
                        <h3>Conversation History</h3>
                    </div>
                    <div className="section-content">
                        <ConversationHistory />
                    </div>
                </section>
            </main>
        </div>
    );
}

export default AdminDashboard;