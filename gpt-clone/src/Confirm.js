import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios"; // 如果你使用React Router
const LOCAL_URLS = ['http://localhost:8090/health'];
const REMOTE_URL = '/health';
export default function Confirm() {
    const [message, setMessage] = useState("Confirming...");
    // eslint-disable-next-line no-unused-vars
    const [username, setUsername] = useState("");
    const { token } = useParams(); // 获取路由中的 token 参数
    const navigate = useNavigate(); // 用于页面跳转
    // eslint-disable-next-line no-unused-vars
    const [error, setError] = useState(null);

    useEffect(() => {
        const detectEnvironment = async () => {
            let isLocalServiceAvailable = false;
            for (const url of LOCAL_URLS) {
                try {
                    await axios.get(url);
                    window.API_BASE_URL = url.replace('/health', '');
                    isLocalServiceAvailable = true;
                    return;  // 成功连接到本地服务，提前退出函数
                } catch (error) {
                    console.log(`Failed to connect to local service: ${url}`);
                }
            }

            if (!isLocalServiceAvailable) {
                try {
                    await axios.get(REMOTE_URL);
                    window.API_BASE_URL = REMOTE_URL.replace('/health', '/api');
                } catch (error) {
                    setError('Failed to connect to any service.');
                    return;  // 没有连接成功，提前返回
                }
            }
        };

        // 调用环境检测函数
        detectEnvironment().then(() => {
            if (!window.API_BASE_URL) {
                setMessage("No available service.");
                return;
            }

            // 发起 GET 请求到确认控制器
            axios.get(`${window.API_BASE_URL}/user/register/confirm/${token}`)
                .then((response) => {
                    const data = response.data;

                    if (data.code === 1) {
                        setMessage(`Admin registration confirmed! Welcome, ${data.data}.`);
                        setUsername(data.data); // 成功后获取用户名
                        // 等待 3 秒后跳转到登录页面，并粘贴用户名
                        setTimeout(() => {
                            navigate("/login", { state: { username: data.data } });
                        }, 3000);
                    } else {
                        setMessage(data.msg || "Confirmation failed. code: " + data.code);
                    }
                })
                .catch((error) => {
                    setMessage(error.message || "Confirmation failed.");
                });
        });
    }, [token, navigate]);

    return (
        <div style={styles.container}>
            <h1>{message}</h1>
        </div>
    );
}

// 简单的 CSS 样式
const styles = {
    container: {
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        height: "100vh",
        textAlign: "center",
    },
};
