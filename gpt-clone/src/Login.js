import React, {useState, useRef, useEffect} from 'react';
import axios from 'axios';
import {useLocation, useNavigate} from 'react-router-dom';
import './login.css';

function Login() {
    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false); // 控制密码显示/隐藏
    const [errorMessage, setErrorMessage] = useState('');
    const [suggestions, setSuggestions] = useState([]); // 存储替代用户名
    const [showSuggestionPopup, setShowSuggestionPopup] = useState(false); // 控制气泡提示的显示
    // eslint-disable-next-line no-unused-vars
    const [checkDone, setCheckDone] = useState(false); // 确保只检查一次用户名
    const [isCheckingUsername, setIsCheckingUsername] = useState(false); // 用户名检查状态
    const navigate = useNavigate();

    const [isAdmin, setIsAdmin] = useState(false);  // 新增状态: 是否为管理员
    const [adminEmail, setAdminEmail] = useState('');  // 新增状态: 管理员邮箱


    const location = useLocation();// 获取当前的路由信息, 用于获取注册成功后的用户名
    useEffect(() => {
        // 如果从 Confirm 页面传递了用户名，则设置到输入框
        if (location.state && location.state.username) {
            setUsername(location.state.username);
        }
    }, [location]);

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
            // 发送登录请求
            const response = await axios.post(`${window.API_BASE_URL}/user/login`, { username, password });
            const result = response.data;

            if (result.code === 1) {
                // 登录成功，处理登录后的逻辑
                const token = result.token;
                const uuid = result.uuid;
                const role = result.role;
                const confirmedAdmin = result.confirmedAdmin;

                // 保存 token 和 uuid 到 localStorage
                localStorage.setItem('token', token);
                localStorage.setItem('userUUID', uuid);

                // 检查用户角色和管理员验证状态
                if (role === 'admin' && confirmedAdmin) {
                    // 已验证的管理员，跳转到管理员面板
                    navigate('/adminBoard');
                } else if (role === 'admin' && !confirmedAdmin) {
                    // 未确认的管理员，显示独特的提示
                    setErrorMessage(result.msg || 'Admin not yet verified. Please contact support.');
                    navigate('/chat');
                } else {
                    // 普通用户，跳转到聊天页面
                    navigate('/chat');
                }
            } else {
                // 登录失败，显示错误消息
                setErrorMessage(result.msg || 'Login failed. Please try again.');
                navigate('/');
            }
        } catch (error) {
            // 捕获异常，设置错误消息
            setErrorMessage(error.response?.data?.msg || 'An error occurred during login. Please try again.');
        }
    };

    const handleRegister = async () => {
        try {
            const requestData = { username, password, isAdmin, adminEmail };  // 新增管理员邮箱信息
            if (isAdmin) {
                requestData.isAdmin = true;  // 如果是管理员，则增加管理员标识
                requestData.adminEmail = adminEmail;  // 如果是管理员，则增加邮箱信息
            }else{
                requestData.isAdmin = false;  // 如果不是管理员，则增加管理员标识
            }

            const response = await axios.post(`${window.API_BASE_URL}/user/register`, requestData);
            const result = response.data;

            if (result.code === 1) {
                alert(result.msg);
                setIsLogin(true);
            }  else {
                setErrorMessage(result.msg || 'Unknown err, Registration failed. Please try again.');
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
                {!isLogin && (
                    <>
                        <div className="admin-checkbox">
                            <label>
                                As admin
                            </label>
                            <input
                                type="checkbox"
                                checked={isAdmin}
                                onChange={(e) => setIsAdmin(e.target.checked)}
                            />
                        </div>
                        {isAdmin && (
                            <div className="admin-email-container">
                                <input
                                    type="email"
                                    value={adminEmail}
                                    onChange={(e) => setAdminEmail(e.target.value)}
                                    placeholder="Admin Email"
                                    required
                                />
                            </div>
                        )}
                    </>
                )}
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
