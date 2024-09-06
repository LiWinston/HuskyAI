import React, { useEffect, useState } from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Route, Routes, useNavigate } from 'react-router-dom';
import Chat from './Chat';
import Login from './Login';
import axios from 'axios';
import './index.css';
import reportWebVitals from './reportWebVitals';

const root = ReactDOM.createRoot(document.getElementById('root'));
const LOCAL_URLS = ['http://localhost:8090/health'];
const REMOTE_URL = '/health';

function detectEnvironment(updateStatus, setError, finishDetection) {
    let isLocalServiceAvailable = false;

    const checkServices = async () => {
        for (const url of LOCAL_URLS) {
            try {
                updateStatus(`Trying to connect to local service: ${url}`);
                await axios.get(url);
                window.API_BASE_URL = url.replace('/health', '');
                isLocalServiceAvailable = true;
                updateStatus(`Connected to local service: ${url}`);
                await new Promise(resolve => setTimeout(resolve, 100));
                finishDetection();
                return;
            } catch (error) {
                updateStatus(`Failed to connect to local service: ${url}`);
            }
        }

        if (!isLocalServiceAvailable) {
            try {
                updateStatus(`Trying to connect to remote server: ${REMOTE_URL}`);
                await axios.get(REMOTE_URL);
                window.API_BASE_URL = REMOTE_URL.replace('/health', '/api');
                updateStatus('Connected to remote server');
                await new Promise(resolve => setTimeout(resolve, 100));
                finishDetection();
            } catch (error) {
                setError('Failed to connect to any service.');
            }
        }
    };

    checkServices();
}

function LoadingContainer() {
    const [statusMessage, setStatusMessage] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [isDetectionComplete, setDetectionComplete] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        detectEnvironment(setStatusMessage, setErrorMessage, () => {
            setDetectionComplete(true);
            navigate('/login');
        });
    }, [navigate]);

    if (errorMessage) {
        return <div>Error: {errorMessage}</div>;
    }

    if (isDetectionComplete) {
        return null;
    }

    return <div>Loading: {statusMessage}</div>;
}

root.render(
    <BrowserRouter>
        <Routes>
            <Route path="/" element={<LoadingContainer />} />
            <Route path="/login" element={<Login />} />
            <Route path="/chat" element={<Chat />} />
        </Routes>
    </BrowserRouter>
);

reportWebVitals();
