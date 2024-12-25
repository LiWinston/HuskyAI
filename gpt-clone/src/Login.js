import React, {useEffect, useRef, useState} from 'react';
import axios from 'axios';
import {useLocation, useNavigate} from 'react-router-dom';
import './login.css';
import {showSweetAlert, showSweetAlertWithRetVal, showSweetError} from './Component/sweetAlertUtil';
import Swal from "sweetalert2";
import detectIP from "./Component/ip";

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
    useEffect(() => {
        // If the username is passed from the Confirm page, set it in the input box.
        if (location.state && location.state.username) {
            setUsername(location.state.username);
        }
    }, [location]);

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


    const handleLogin = async () => {
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
            UserIpInfo(username, password);
            // Send login request.
            const response = await axios.post(`/api/user/login`,
                {username, password},
                {headers: {'Content-Type': 'application/json'}});
            const result = response.data;

            if (result.code === 1) {
                // Login successful, processing post-login logic.
                const token = result.token;
                const uuid = result.uuid;
                const role = result.role;
                const confirmedAdmin = result.confirmedAdmin;

                // Save token and uuid to localStorage.
                localStorage.setItem('token', token);
                localStorage.setItem('userUUID', uuid);

                // Check user roles and admin verification status.
                if (role === 'admin' && confirmedAdmin) {
                    Swal.fire({
                        title: 'Admin Login',
                        text: 'Where to go?',
                        icon: 'success',
                        confirmButtonText: 'Chat',
                        showCancelButton: true,
                        cancelButtonText: 'Dashboard',
                        showDenyButton: true,
                        denyButtonText: 'Logout',
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
                    // Unconfirmed administrators, display unique prompts.
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
                    // Regular user, redirect to the chat page.
                    localStorage.removeItem("selectedConversation");
                    localStorage.removeItem("conversations");
                    navigate('/chat');
                }
            } else {
                // Login failed, show error message.
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
        }
    };

    const handleRegister = async () => {
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
                // alert(result.msg);
                showSweetAlert(result.msg, {
                    title: 'Registration Success',
                    icon: 'success',
                    confirmButtonText: 'Go to Login',
                });
                setIsLogin(true);
            } else {
                showSweetError(result.msg || 'Unknown err, Registration failed. Please try again.');
                setErrorMessage('Please try again.');
            }
        } catch (error) {
            showSweetError('Registration failed due to network error. Please try again.');
            setErrorMessage(error);
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
                        ref={usernameInputRef}
                        type="text"
                        value={username}
                        onChange={(e) => {
                            setUsername(e.target.value);
                            setIsUsernameValid(false); // 重置验证状态
                        }}
                        onBlur={handleUsernameBlur}
                        placeholder="Username"
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
