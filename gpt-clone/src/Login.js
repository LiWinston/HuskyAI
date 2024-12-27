import React, {useEffect, useRef, useState} from 'react';
import axios from 'axios';
import {useLocation, useNavigate} from 'react-router-dom';
import './login.css';
import {showSweetAlert, showSweetAlertWithRetVal, showSweetError} from './Component/sweetAlertUtil';
import Swal from "sweetalert2";
import detectIP from "./Component/ip";
import Lottie from "lottie-react";
import loadingAnimation from "./assets/loading.json";

function Login() {
    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false); // Control password display/hide.
    const [errorMessage, setErrorMessage] = useState('');
    const [suggestions, setSuggestions] = useState([]); // Store alternative username.
    const [showSuggestionPopup, setShowSuggestionPopup] = useState(false); // Control the display of tooltips.
    // eslint-disable-next-line no-unused-vars
    const [checkDone, setCheckDone] = useState(false); // Ensure the username is checked only once.
    const [isCheckingUsername, setIsCheckingUsername] = useState(false); // Check if the username is being checked.
    const navigate = useNavigate();

    const [isAdmin, setIsAdmin] = useState(false);
    const [adminEmail, setAdminEmail] = useState('');

    const [isUsernameValid, setIsUsernameValid] = useState(false);

    const location = useLocation();
    const [isSSORegistering, setIsSSORegistering] = useState(false);

    useEffect(() => {
        // If the username is passed from the Confirm page, set it in the input box.
        if (location.state && location.state.username) {
            setUsername(location.state.username);
        }
    }, [location]);

    useEffect(() => {
        // 检查URL中是否有SSO相关参数
        const urlParams = new URLSearchParams(window.location.search);
        const ssoToken = urlParams.get('sso_token');
        const error = urlParams.get('error');
        
        if (error) {
            showSweetError(decodeURIComponent(error));
        } else if (ssoToken) {
            handleSSOCallback(ssoToken);
        }
    }, []);

    const handleUsernameBlur = async () => {
        function isBlank(username) {
            return /^\s*$/.test(username);
        }

        if (!isLogin) {
            if (username === '' || username === null || isBlank(username)) {
                setIsUsernameValid(false);
                return;
            }
            setIsCheckingUsername(true);
            try {
                const response = await axios.get(`/api/user/register/checkUsername`, {params: {username}});
                const result = response.data;
                if (result.code === 0) {
                    setSuggestions(result.data);
                    setShowSuggestionPopup(true);
                    setIsUsernameValid(false);
                } else {
                    // 用户名可用
                    setIsUsernameValid(true);
                    setShowSuggestionPopup(false);
                }
                setCheckDone(true);
            } catch (error) {
                console.error('Username check failed', error);
                setIsUsernameValid(false);
            } finally {
                setIsCheckingUsername(false);
            }
        }
    };
    const [animationTarget, setAnimationTarget] = useState(null); // Store the alternative username of the current click.
    const usernameInputRef = useRef(null); // Cite the username input box.
    const handleSuggestionClick = (suggestion, index) => {
        const usernameRect = usernameInputRef.current.getBoundingClientRect();
        const suggestionElement = document.getElementsByClassName('suggestion-item')[index];
        const suggestionRect = suggestionElement.getBoundingClientRect();

        // Calculate the relative displacement of the flight animation.
        const flyToLeft = usernameRect.left - suggestionRect.left;
        const flyToTop = usernameRect.top - suggestionRect.top;

        // Dynamically set the endpoint of the flight animation.
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

    const [isLoading, setIsLoading] = useState(false);

    const handleLogin = async () => {
        setIsLoading(true);
        async function UserIpInfo(username, password) {
            const ipInfo = await detectIP();
            console.log(ipInfo);
            const loginDto = {username, password};
            axios.post(`/api/user/login/ip`,
                {
                    loginDTO: loginDto,
                    ipInfoDTO: ipInfo
                })
                .catch((error) => {
                    console.error('IP info failed', error);
                });
        }

        try {
            await UserIpInfo(username, password);
            const response = await axios.post(`/api/user/login`,
                {username, password},
                {headers: {'Content-Type': 'application/json'}});
            const result = response.data;

            if (result.code === 1) {
                const token = result.token;
                const uuid = result.uuid;
                const role = result.role;
                const confirmedAdmin = result.confirmedAdmin;

                localStorage.setItem('token', token);
                localStorage.setItem('userUUID', uuid);

                if (role === 'admin' && confirmedAdmin) {
                    Swal.fire({
                        title: text.loginSuccess.title,
                        text: text.loginSuccess.text,
                        icon: 'success',
                        confirmButtonText: text.loginSuccess.chat,
                        showCancelButton: true,
                        cancelButtonText: text.loginSuccess.dashboard,
                        showDenyButton: true,
                        denyButtonText: text.loginSuccess.logout,
                        showCloseButton: true,
                    }).then((result) => {
                        if (result.isConfirmed) {
                            localStorage.removeItem("selectedConversation");
                            localStorage.removeItem("conversations");
                            navigate('/chat');
                        } else if (result.isDenied) {
                            localStorage.removeItem("token");
                            localStorage.removeItem("userUUID");
                            navigate('/');
                        } else {
                            navigate('/admin');
                        }
                    });
                } else if (role === 'admin' && !confirmedAdmin) {
                    showSweetAlertWithRetVal('Admin not yet verified. Please contact support.', {
                        title: 'Admin Verification',
                        icon: 'warning',
                        confirmButtonText: 'Go to Chat',
                    }).then(() => {
                        setErrorMessage(result.msg || 'Admin not yet verified. Please contact support.');
                        localStorage.removeItem("selectedConversation");
                        localStorage.removeItem("conversations");
                        navigate('/chat');
                    });
                } else {
                    localStorage.removeItem("selectedConversation");
                    localStorage.removeItem("conversations");
                    navigate('/chat');
                }
            } else {
                showSweetAlertWithRetVal(result.msg || 'Login failed. Please try again.', {
                    title: 'Login Failed',
                    icon: 'error',
                    confirmButtonText: 'Try Again',
                }).then(() => {
                    setErrorMessage(result.msg || 'Login failed. Please try again.');
                });
            }
        } catch (error) {
            setErrorMessage(error.response?.data?.msg || 'An error occurred during login. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleRegister = async () => {
        setIsLoading(true);
        try {
            const requestData = {username, password, isAdmin, adminEmail};
            if (isAdmin) {
                requestData.isAdmin = true;
                requestData.adminEmail = adminEmail;
            } else {
                requestData.isAdmin = false;
            }

            const response = await axios.post(`/api/user/register`, requestData);
            const result = response.data;

            if (result.code === 1) {
                showSweetAlert(result.msg, {
                    title: text.registerSuccess.title,
                    icon: 'success',
                    confirmButtonText: text.registerSuccess.button,
                });
                setIsLogin(true);
            } else {
                showSweetError(result.msg || text.errors.registration);
                setErrorMessage('Please try again.');
            }
        } catch (error) {
            showSweetError(text.errors.network);
            setErrorMessage(error);
        } finally {
            setIsLoading(false);
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

    // 检测用户语言
    const userLang = navigator.language || navigator.userLanguage;
    const isZH = userLang.startsWith('zh');

    // 文本的双语配置
    const text = {
        login: isZH ? "登录" : "Login",
        register: isZH ? "注册" : "Register",
        username: isZH ? "用户名" : "Username",
        password: isZH ? "密码" : "Password",
        loginButton: isZH ? "登录" : "Login",
        registerButton: isZH ? "注册" : "Register",
        switchToRegister: isZH ? "切换到注册" : "Switch to Register",
        switchToLogin: isZH ? "切换到登录" : "Switch to Login",
        asAdmin: isZH ? "管理员身份" : "As Admin",
        adminEmail: isZH ? "管理员邮箱" : "Admin Email",
        usernameExists: isZH ? "用户名已存在，建议：" : "Username already exists. Try:",
        loginSuccess: {
            title: isZH ? "管理员登录" : "Admin Login",
            text: isZH ? "去往何处？" : "Where to go?",
            chat: isZH ? "聊天" : "Chat",
            dashboard: isZH ? "控制台" : "Dashboard",
            logout: isZH ? "登出" : "Logout",
        },
        registerSuccess: {
            title: isZH ? "注册成功" : "Registration Success",
            text: isZH ? "注册成功！" : "Registration successful!",
            button: isZH ? "去登录" : "Go to Login",
        },
        errors: {
            network: isZH ? "网络错误，请重试" : "Network error. Please try again.",
            unknown: isZH ? "未知错误，请重试" : "Unknown error. Please try again.",
            registration: isZH ? "注册失败，请重试" : "Registration failed. Please try again.",
        }
    };

    const handleSSOLogin = () => {
        window.location.href = 'https://bitsleep.cn/sso/login';
    };

    const handleSSOCallback = async (token) => {
        try {
            const response = await axios.post('/api/sso/callback', { token });
            const result = response.data;
            
            if (result.code === 1) {
                // 登录成功
                localStorage.setItem('token', result.token);
                localStorage.setItem('userUUID', result.uuid);
                navigate('/chat');
            } else if (result.code === 2) {
                // 需要注册
                setIsSSORegistering(true);
                // 保存SSO信息以供注册使用
                localStorage.setItem('sso_token', token);
            } else {
                showSweetError(result.msg || 'SSO login failed');
            }
        } catch (error) {
            showSweetError(error.response?.data?.msg || 'An error occurred during SSO login');
        }
    };

    return (
        <div className="auth-container">
            <h2>{isLogin ? text.login : text.register}</h2>
            
            {/* SSO登录按钮 */}
            {isLogin && !isSSORegistering && (
                <div className="sso-login">
                    <button onClick={handleSSOLogin} className="btn-sso">
                        {isZH ? "使用 BitSleep 账号一键登录" : "Login with BitSleep"}
                    </button>
                    <div className="divider">
                        {isZH ? "或" : "OR"}
                    </div>
                </div>
            )}
            
            <form onSubmit={handleSubmit}>
                <div className="username-container">
                    <input
                        ref={usernameInputRef}
                        type="text"
                        value={username}
                        onChange={(e) => {
                            setUsername(e.target.value);
                            setIsUsernameValid(false);
                        }}
                        onBlur={handleUsernameBlur}
                        placeholder={text.username}
                        required
                        className={isUsernameValid ? 'valid' : ''}
                    />
                    {isCheckingUsername && (
                        <div className="loading-spinner">⏳</div>
                    )}
                    {!isLogin && isUsernameValid && !isCheckingUsername && (
                        <div className="success-icon">✓</div>
                    )}
                    {showSuggestionPopup && (
                        <div className="suggestion-popup">
                            <span className="close-button"
                                  onClick={() => setShowSuggestionPopup(false)}>&times;</span>
                            <p>{text.usernameExists}</p>
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
                        placeholder={text.password}
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
                            <label>{text.asAdmin}</label>
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
                                    placeholder={text.adminEmail}
                                    required
                                />
                            </div>
                        )}
                    </>
                )}
                <button type="submit" className="auth-button">
                    {isLogin ? text.loginButton : text.registerButton}
                </button>
            </form>
            <button onClick={() => setIsLogin(!isLogin)} className="auth-button">
                {isLogin ? text.switchToRegister : text.switchToLogin}
            </button>
            {errorMessage && <p className="error-message">{errorMessage}</p>}
            
            {isLoading && (
                <div className="global-loading-overlay">
                    <div className="loading-container">
                        <Lottie 
                            animationData={loadingAnimation}
                            loop={true}
                            style={{ width: 80, height: 80 }}
                        />
                        <div className="loading-text">
                            {isZH ? 
                                (isLogin ? "登录中..." : "注册中...") : 
                                (isLogin ? "Logging in..." : "Registering...")}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default Login;
