import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './login.css';

function Login() {
    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const navigate = useNavigate();

    const handleLogin = async () => {
        try {
            // 移除前端哈希加密，直接发送明文密码，确保 HTTPS 加密
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
            // 移除前端哈希加密，直接发送明文密码，确保 HTTPS 加密
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
                <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Username"
                    required
                />
                <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Password"
                    required
                />
                <button type="submit">{isLogin ? 'Login' : 'Register'}</button>
            </form>
            <button onClick={() => setIsLogin(!isLogin)}>
                {isLogin ? 'Switch to Register' : 'Switch to Login'}
            </button>
            {errorMessage && <p className="error-message">{errorMessage}</p>}
        </div>
    );
}

export default Login;
