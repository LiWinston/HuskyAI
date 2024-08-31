import React, {useEffect, useState} from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import reportWebVitals from './reportWebVitals';
import axios from 'axios';
import logo from './logo.svg';
import {BrowserRouter, Route, Routes, useNavigate} from "react-router-dom";
import Chat from "./Chat"; // Make sure to import your logo file / 确保导入你的logo文件

const root = ReactDOM.createRoot(document.getElementById('root'));

// Set default API URLs / 设置默认API URL
const LOCAL_URLS = ['http://localhost:8090/health'];
const REMOTE_URL = '/health';

// Loading screen component displayed while checking service availability / 检测服务可用性时显示的加载组件
function LoadingScreen({statusMessage}) {
    return (<div className="loading-screen">
            <img src={logo} alt="Loading..." className="loading-logo"/>
            <p>Loading, please wait...</p>
            {statusMessage && <p>{statusMessage}</p>}
        </div>);
}

// Error screen component displayed if a connection error occurs / 如果发生连接错误时显示的错误组件
function ErrorScreen({errorMessage}) {
    return (<div className="error-screen">
            <img src={logo} alt="Error" className="loading-logo"/>
            <p>{errorMessage}</p>
        </div>);
}

// Function to detect the environment by checking the availability of services / 通过检查服务的可用性来检测环境的函数
async function detectEnvironment(updateStatus, setError, finishDetection) {
    let isLocalServiceAvailable = false;

    // Check local services / 检查本地服务
    for (const url of LOCAL_URLS) {
        try {
            updateStatus(`Trying to connect to local service: ${url}`);
            await axios.get(url); // Use GET request to check service availability / 使用GET请求检测服务可用性
            window.API_BASE_URL = url.replace('/health', ''); // Set base URL to the root of the service / 设置基础URL为服务的根路径
            isLocalServiceAvailable = true;
            updateStatus(`Connected to local service: ${url}`);
            await new Promise(resolve => setTimeout(resolve, 300)); // Display status message / 显示状态消息
            break;
        } catch (error) {
            updateStatus(`Failed to connect to local service: ${url}`);
            await new Promise(resolve => setTimeout(resolve, 300)); // Display status message / 显示状态消息
        }
    }

    // If local services are not available, check remote service / 如果本地服务不可用，检测远程服务
    if (!isLocalServiceAvailable) {
        try {
            updateStatus(`Trying to connect to remote server: ${REMOTE_URL}`);
            await axios.get(REMOTE_URL); // Use GET request to check remote service availability / 使用GET请求检测远程服务可用性
            window.API_BASE_URL = REMOTE_URL.replace('/health', '/api');

            updateStatus('Connected to remote server');
            await new Promise(resolve => setTimeout(resolve, 300)); // Display status message / 显示状态消息
        } catch (error) {
            // If remote service is unavailable or blocked by CORS policy / 如果远程服务不可用或被CORS策略阻止
            setError('Failed to connect');
            return; // Stop further processing / 停止进一步处理
        }
    }

    // Complete detection and notify to render the main application / 完成检测并通知渲染主应用
    finishDetection();
}

// Loading container component to manage state / 管理状态的加载容器组件
function LoadingContainer() {
    const [statusMessage, setStatusMessage] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [isDetectionComplete, setDetectionComplete] = useState(false);
    const [showUuidInput, setShowUuidInput] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        detectEnvironment(setStatusMessage, setErrorMessage, () => {
            setDetectionComplete(true);
            setShowUuidInput(true);
        });
    }, []);

    const handleUuidSubmit = async (uuid) => {
        try {
            // Store UUID in localStorage for future use
            localStorage.setItem('userUUID', uuid);

            // Send GET request to /chat with UUID
            const response = await axios.get(`${window.API_BASE_URL}/chat?uuid=${uuid}`);

            // Check if data is received and navigate to chat
            if (response.data && response.data.data) {
                navigate('/chat', {state: response.data.data});
            } else {
                console.error('Unexpected response format:', response.data);
                setErrorMessage('Failed to fetch chat data. Please try again.');
                setTimeout(() => {
                    navigate('/')
                    setShowUuidInput(true);
                    setErrorMessage('');
                }, 500); // 延时执行导航，以确保状态更新生效
            }
        } catch (error) {
            console.error('Error fetching chat data:', error);
            setErrorMessage('Failed to fetch chat data. Please try again.');
            setTimeout(() => {
                navigate('/')
                setShowUuidInput(true);
                setErrorMessage('');
            }, 500); // Delay navigation to ensure state update takes effect / 延迟导航以确保状态更新生效
        }
    };


    if (errorMessage) {
        return <ErrorScreen errorMessage={errorMessage}/>;
    }

    if (showUuidInput) {
        return <UUIDInput onSubmit={handleUuidSubmit}/>;
    }

    if (isDetectionComplete) {
        return <LoadingScreen statusMessage="Environment detection complete. Please enter your UUID."/>;
    }

    return <LoadingScreen statusMessage={statusMessage}/>;
}

// Initial rendering of LoadingContainer, which will manage state / 初始渲染LoadingContainer，它将负责状态管理
root.render(<BrowserRouter>
    <Routes>
        <Route path="/" element={<LoadingContainer/>}/>
        <Route path="/chat" element={<Chat/>}/>
    </Routes>
</BrowserRouter>);

function UUIDInput({onSubmit}) {
    const [uuid, setUuid] = useState('');

    const handleSubmit = (e) => {
        e.preventDefault();
        onSubmit(uuid);
    };

    return (<div className={"uuid-input-parent"}>
            <div className="uuid-input-container">
                <h2>Enter Your UUID</h2>
                <form onSubmit={handleSubmit}>
                    <input
                        type="text"
                        value={uuid}
                        onChange={(e) => setUuid(e.target.value)}
                        placeholder="Enter your UUID"
                        required
                    />
                    <button type="submit">Submit</button>
                </form>
            </div>
        </div>);
}

// Report web vitals (optional) / 报告网页指标（可选）
reportWebVitals();
