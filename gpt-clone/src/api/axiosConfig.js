import axios from 'axios';

// 创建axios实例
const axiosInstance = axios.create({
    baseURL: '/api'
});

// 请求拦截器
axiosInstance.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        const userUUID = localStorage.getItem('userUUID');
        
        if (token && userUUID) {
            config.headers.Authorization = `Bearer ${token}`;
            config.headers['X-User-UUID'] = userUUID;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
axiosInstance.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 401) {
            // token过期或无效,清除本地存储并跳转到登录页
            localStorage.removeItem('token');
            localStorage.removeItem('userUUID');
            window.location.href = '/';
        }
        return Promise.reject(error);
    }
);

export default axiosInstance; 