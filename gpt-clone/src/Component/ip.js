// 使用 fetch 实现 IP 检测模块
async function detectIP() {
    try {
        const response = await fetch('https://api.qjqq.cn/api/Local');
        const data = await response.json();

        if (data.msg === "success") {
            return data.data;
        } else {
            console.error("查询失败:", data.msg);
        }
    } catch (error) {
        console.error("请求出错:", error);
    }
}

export default detectIP;
