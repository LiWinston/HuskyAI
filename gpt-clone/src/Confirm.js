import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom"; // 如果你使用React Router

export default function Confirm() {
    const [message, setMessage] = useState("Confirming...");
    const [username, setUsername] = useState("");
    const { token } = useParams(); // 获取路径中的 token 参数
    const navigate = useNavigate(); // 用于页面跳转

    useEffect(() => {
        // 发起 GET 请求到确认控制器
        fetch(`/user/register/confirm/${token}`)
            .then((response) => response.json())
            .then((data) => {
                if (data.code === 1) {
                    setMessage(`Admin registration confirmed! Welcome, ${data.data}.`);
                    setUsername(data.data); // 成功后获取用户名
                    // 等待 3 秒后跳转到登录页面，并粘贴用户名
                    setTimeout(() => {
                        navigate("/login", { state: { username: data.data } });
                    }, 3000);
                } else {
                    setMessage(data.message || "Confirmation failed.");
                }
            })
            .catch((error) => {
                setMessage("Error confirming registration. Please try again.");
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
