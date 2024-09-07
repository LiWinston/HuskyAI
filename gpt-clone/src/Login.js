import React, { useState, useRef } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './login.css';

function Login() {
    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false); // 控制密码显示/隐藏
    const [errorMessage, setErrorMessage] = useState('');
    const [suggestions, setSuggestions] = useState([]); // 存储替代用户名
    const [showSuggestionPopup, setShowSuggestionPopup] = useState(false); // 控制气泡提示的显示
    const [checkDone, setCheckDone] = useState(false); // 确保只检查一次用户名
    const [isCheckingUsername, setIsCheckingUsername] = useState(false); // 用户名检查状态
    const navigate = useNavigate();

    const handleUsernameBlur = async () => {
        function isBlank(username) {
            return /^\s*$/.test(username);
        }

        if (!isLogin) {
            if(username === '' || username === null || isBlank(username)) {
                return;
            }
            setIsCheckingUsername(true); // 开始检查，显示加载图标
            try {
                const response = await axios.get(`${window.API_BASE_URL}/user/register/checkUsername`, { params: { username } });
                const result = response.data;
                if (result.code === 0) {
                    setSuggestions(result.data); // 设置替代用户名
                    setShowSuggestionPopup(true); // 显示气泡提示
                }
                setCheckDone(true); // if you want to check username only once after blur, uncomment this line and use "if (!isLogin && !checkDone) {"
            } catch (error) {
                console.error('Username check failed', error);
            } finally {
                setIsCheckingUsername(false); // 检查完成，隐藏加载图标
            }
        }
    };
    const [animationTarget, setAnimationTarget] = useState(null); // 存储当前点击的替代用户名
    const usernameInputRef = useRef(null); // 引用用户名输入框
    const handleSuggestionClick = (suggestion, index) => {
        const usernameRect = usernameInputRef.current.getBoundingClientRect();
        const suggestionElement = document.getElementsByClassName('suggestion-item')[index];
        const suggestionRect = suggestionElement.getBoundingClientRect();

        // 计算飞行动画的相对位移
        const flyToLeft = usernameRect.left - suggestionRect.left;
        const flyToTop = usernameRect.top - suggestionRect.top;

        // 动态设置飞行动画的终点
        suggestionElement.style.setProperty('--fly-to-left', `${flyToLeft}px`);
        suggestionElement.style.setProperty('--fly-to-top', `${flyToTop}px`);

        setAnimationTarget(index);
        setTimeout(() => {
            setUsername(suggestion);
            setShowSuggestionPopup(false);
            setCheckDone(false);
            setAnimationTarget(null);
        }, 250);
    };


    const handleLogin = async () => {
        try {
            const response = await axios.post(`${window.API_BASE_URL}/user/login`, { username, password });
            const result = response.data;

            if (result.code === 1) {
                const uuid = result.data;
                const token = result.msg;
                localStorage.setItem('token', token);
                localStorage.setItem('userUUID', uuid);
                navigate('/chat');
            } else {
                setErrorMessage(result.msg || 'Login failed. Please try again.');
            }
        } catch (error) {
            setErrorMessage('Login failed due to network error. Please try again.');
        }
    };

    const handleRegister = async () => {
        try {
            const response = await axios.post(`${window.API_BASE_URL}/user/register`, { username, password });
            const result = response.data;

            if (result.code === 1) {
                alert('Registration successful. Please log in.');
                setIsLogin(true);
            } else {
                setErrorMessage(result.msg || 'Registration failed. Please try again.');
            }
        } catch (error) {
            setErrorMessage('Registration failed due to network error. Please try again.');
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (isLogin) {
            handleLogin();
        } else {
            handleRegister();
        }
    };

    return (
        <div className="auth-container">
            <h2>{isLogin ? 'Login' : 'Register'}</h2>
            <form onSubmit={handleSubmit}>
                <div className="username-container">
                    <input
                        ref={usernameInputRef} // 绑定输入框的引用
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        onBlur={handleUsernameBlur} // 用户名输入完成时检测
                        placeholder="Username"
                        required
                    />
                    {isCheckingUsername && (
                        <div className="loading-spinner">⏳</div> // 显示加载中的图标
                    )}
                    {showSuggestionPopup && (
                        <div className="suggestion-popup">
                            <span className="close-button"
                                  onClick={() => setShowSuggestionPopup(false)}>&times;
                            </span>
                            <p>Username already exists. Try:</p>
                            {suggestions.map((suggestion, index) => (
                                <div
                                    key={index}
                                    className={`suggestion-item ${animationTarget === index ? 'clicked' : ''}`}
                                    onClick={() => handleSuggestionClick(suggestion, index)}
                                >
                                    {suggestion}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
                <div className="password-container">
                    <input
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Password"
                        required
                    />
                    <button
                        type="button"
                        className="toggle-password"
                        onClick={() => setShowPassword(!showPassword)}
                    >
                        {showPassword ? '🙈' : '👁️'}
                    </button>
                </div>
                <button type="submit" className="auth-button">{isLogin ? 'Login' : 'Register'}</button>
            </form>
            <button onClick={() => setIsLogin(!isLogin)} className="auth-button">
                {isLogin ? 'Switch to Register' : 'Switch to Login'}
            </button>
            {errorMessage && <p className="error-message">{errorMessage}</p>}
        </div>
    );
}

export default Login;
