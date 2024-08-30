import React from 'react';
import './login.css';
import logo from './logo.svg'; // 确保你有一个 logo 文件

function Login() {
    // Function to handle Okta login
    const handleOktaLogin = () => {
        window.location.href = `${window.API_BASE_URL}/login/okta`;
    };

    // Function to handle Google login
    const handleGoogleLogin = () => {
        window.location.href = `${window.API_BASE_URL}/login/google`;
    };

    return (
        <div className="login-container">
            <img src={logo} alt="Logo" className="login-logo" />
            <h2>Login to your account</h2>
            <div className="login-buttons">
                <button onClick={handleOktaLogin} className="okta-login-button">
                    Login with OktaVerify
                </button>
                <button onClick={handleGoogleLogin} className="google-login-button">
                    Login with Google
                </button>
            </div>
        </div>
    );
}

export default Login;
