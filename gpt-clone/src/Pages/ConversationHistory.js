// ConversationHistory.js
import React, { useEffect, useState } from 'react';
import axios from 'axios';

function ConversationHistory({ userId }) {
    const [conversations, setConversations] = useState([]);  // 存储对话片段
    const [messages, setMessages] = useState([]);            // 存储搜索到的消息
    const [keyword, setKeyword] = useState('');              // 存储关键词
    const [hotKeywords, setHotKeywords] = useState([]);      // 存储热搜词
    const [searchResult, setSearchResult] = useState([]);    // 存储关键词搜索结果
    const [selectedConversation, setSelectedConversation] = useState(null);

    // 获取热搜关键词
    useEffect(() => {
        const fetchHotKeywords = async () => {
            try {
                const response = await axios.get(window.API_BASE_URL + `/admin/chat-conversations/top-keywords?n=10`);  // 获取前10个热词
                setHotKeywords(response.data);
            } catch (error) {
                console.error('Error fetching hot keywords:', error);
            }
        };
        fetchHotKeywords();
    }, []);

    // 搜索对话片段
    const searchConversations = async () => {
        try {
            const response = await axios.get(window.API_BASE_URL + `/admin/chat-conversations/search?keyword=${keyword}`);
            setSearchResult(response.data);
        } catch (error) {
            console.error('Error searching conversations:', error);
        }
    };

    // 点击热词，进行搜索
    const handleHotKeywordClick = (hotKeyword) => {
        setKeyword(hotKeyword);
        searchConversations();  // 执行搜索
    };

    return (
        <div>
            <h4>Conversation History</h4>

            {/* 搜索框 */}
            <div>
                <input
                    type="text"
                    placeholder="Enter keyword to search"
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                />
                <button onClick={searchConversations}>Search</button>
            </div>

            {/* 热搜关键词展示 */}
            <div>
                <h5>Hot Keywords</h5>
                <ul>
                    {hotKeywords.map((keyword, idx) => (
                        <li key={idx} onClick={() => handleHotKeywordClick(keyword)}>
                            {keyword}
                        </li>
                    ))}
                </ul>
            </div>

            {/* 搜索结果展示 */}
            <div>
                {searchResult.length > 0 ? (
                    <div>
                        <h5>Search Results</h5>
                        <ul>
                            {searchResult.map((msg, idx) => (
                                <li key={idx}>
                                    <strong>{msg.role}</strong>: {msg.content} <em>{msg.timestamp}</em>
                                </li>
                            ))}
                        </ul>
                    </div>
                ) : (
                    <p>No results found for "{keyword}".</p>
                )}
            </div>
        </div>
    );
}

export default ConversationHistory;
