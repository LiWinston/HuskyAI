import React, {useEffect, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import axios from 'axios';
import './Confirm.css';

const LOCAL_URLS = ['http://localhost:8090/health'];
const REMOTE_URL = '/health';

// 语言包
const translations = {
    zh: {
        title: '账号确认',
        confirming: '正在确认中...',
        noService: '无可用服务',
        confirmSuccess: '注册确认成功！',
        welcome: '欢迎您，',
        confirmFailed: '确认失败',
        linkExpired: '链接已过期',
        errorCode: '错误代码：'
    },
    en: {
        title: 'Account Confirmation',
        confirming: 'Confirming...',
        noService: 'No available service',
        confirmSuccess: 'Registration confirmed!',
        welcome: 'Welcome, ',
        confirmFailed: 'Confirmation failed',
        linkExpired: 'Link expired',
        errorCode: 'Error code: '
    }
};

export default function Confirm() {
    const [message, setMessage] = useState('');
    const [username, setUsername] = useState('');
    const [status, setStatus] = useState('pending');
    const [lang, setLang] = useState('zh');
    const {token} = useParams();
    const navigate = useNavigate();

    // 检测浏览器语言
    useEffect(() => {
        const browserLang = navigator.language.toLowerCase();
        setLang(browserLang.startsWith('zh') ? 'zh' : 'en');
    }, []);

    useEffect(() => {
        setMessage(translations[lang].confirming);
        
        const detectEnvironment = async () => {
            let isLocalServiceAvailable = false;
            for (const url of LOCAL_URLS) {
                try {
                    await axios.get(url);
                    window.API_BASE_URL = url.replace('/health', '');
                    isLocalServiceAvailable = true;
                    return;
                } catch (error) {
                    console.log(`Failed to connect to local service: ${url}`);
                }
            }

            if (!isLocalServiceAvailable) {
                try {
                    await axios.get(REMOTE_URL);
                    window.API_BASE_URL = REMOTE_URL.replace('/health', '/api');
                } catch (error) {
                    setStatus('error');
                    setMessage(translations[lang].noService);
                }
            }
        };

        detectEnvironment().then(() => {
            if (!window.API_BASE_URL) {
                setMessage(translations[lang].noService);
                setStatus('error');
                return;
            }

            axios.get(`/api/user/register/confirm/${token}`).then((response) => {
                const data = response.data;

                if (data.code === 1) {
                    setMessage(translations[lang].confirmSuccess);
                    setUsername(data.data);
                    setStatus('success');
                    setTimeout(() => {
                        navigate('/login', {state: {username: data.data}});
                    }, 3000);
                } else {
                    setMessage(translations[lang].linkExpired);
                    setStatus('error');
                }
            }).catch((error) => {
                setMessage(translations[lang].linkExpired);
                setStatus('error');
            });
        });
    }, [token, navigate, lang]);

    return (
        <div className="confirm-container">
            <div className={`confirm-card ${status}`}>
                <h1 className="confirm-title">
                    {translations[lang].title}
                </h1>
                <div className="confirm-content">
                    <p className="confirm-message">{message}</p>
                    {status === 'success' && username && (
                        <p className="confirm-username">
                            {translations[lang].welcome}{username}
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}
