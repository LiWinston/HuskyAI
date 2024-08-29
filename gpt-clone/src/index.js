import React, { useState, useEffect } from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import axios from 'axios';
import logo from './logo.svg'; // 确保导入你的logo文件

const root = ReactDOM.createRoot(document.getElementById('root'));

// 设置默认API URL
const LOCAL_URLS = ['http://localhost:8080/chat', 'http://localhost:8090/chat'];
const REMOTE_URL = 'https://lmsgpt.onrender.com/chat';

// 加载时显示的 Loading 组件
function LoadingScreen() {
    return (
        <div className="loading-screen">
            <img src={logo} alt="Loading..." className="loading-logo" />
            <p>Loading, please wait...</p>
        </div>
    );
}

async function detectEnvironment() {
    let isLocalServiceAvailable = false;

    for (const url of LOCAL_URLS) {
        try {
            // 尝试向本地服务发送请求以检测其可用性
            await axios.post(url, { prompt: 'ping' });
            // 如果请求成功，将基础URL设置为本地URL
            window.API_BASE_URL = url.replace('/chat', '');
            isLocalServiceAvailable = true;
            break;
        } catch (error) {
            // 捕获错误并继续尝试下一个URL
        }
    }

    // 如果所有本地服务都不可用，设置为远程URL
    if (!isLocalServiceAvailable) {
        window.API_BASE_URL = REMOTE_URL.replace('/chat', '');
    }

    // 渲染主应用
    root.render(
        <React.StrictMode>
            <App />
        </React.StrictMode>
    );
}

// 初始渲染 LoadingScreen
root.render(
    <React.StrictMode>
        <LoadingScreen />
    </React.StrictMode>
);

// 调用检测环境函数
detectEnvironment();

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
