import React from 'react';

function ConversationList({ conversations, onSelectConversation, selectedConversation }) {
    return (
        <div className="conversation-list">
            <h3>Conversations</h3>
            {conversations.map((conv) => (
                <div
                    key={conv.id}
                    className={`conversation-item ${selectedConversation === conv.id ? 'selected' : ''}`}
                    onClick={() => onSelectConversation(conv.id)}
                >
                    {conv.title}
                </div>
            ))}
        </div>
    );
}

export default ConversationList;
