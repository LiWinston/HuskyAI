import axios from 'axios';

// 通用的请求头生成函数
export const getCommonHeaders = (additionalHeaders = {}) => {
    const token = localStorage.getItem('token');
    const userUUID = localStorage.getItem('userUUID');
    
    return {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        'X-User-UUID': userUUID || '',
        ...additionalHeaders
    };
};

// 创建axios实例
const axiosInstance = axios.create({
    baseURL: '/api'
});

// 请求拦截器
axiosInstance.interceptors.request.use(
    (config) => {
        config.headers = getCommonHeaders(config.headers);
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// 响应拦截器
axiosInstance.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // 清除本地存储
            localStorage.removeItem('token');
            localStorage.removeItem('userUUID');
            // 重定向到登录页
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default axiosInstance; 