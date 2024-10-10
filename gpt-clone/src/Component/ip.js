// Implement an IP detection module using fetch.
async function detectIP() {
    try {
        const response = await fetch('https://api.qjqq.cn/api/Local');
        const data = await response.json();

        if (data.msg === 'success') {
            return data.data;
        } else {
            console.error('Query failed:', data.msg);
        }
    } catch (error) {
        console.error('Request error:', error);
    }
}

export default detectIP;
