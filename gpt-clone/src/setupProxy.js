const { createProxyMiddleware } = require('http-proxy-middleware');
const fetch = require('node-fetch');

console.log('Loading proxy configuration...');

module.exports = function(app) {
    // 用于记录本地服务是否可用的状态
    let isLocalServerAvailable = false;

    // 创建远程代理中间件
    const remoteProxy = createProxyMiddleware({
        target: 'https://huskyAI.bitsleep.cn',
        changeOrigin: true,
        logLevel: 'debug',
        onProxyReq: (proxyReq, req, res) => {
            console.log('使用远程服务:', req.url);
        }
    });

    // 创建本地代理中间件
    const localProxy = createProxyMiddleware({
        target: 'http://localhost:8090',
        changeOrigin: true,
        pathRewrite: {'^/api': ''},
        logLevel: 'debug',
        onProxyReq: (proxyReq, req, res) => {
            console.log('使用本地服务:', req.url);
        }
    });

    // 定期检查本地服务
    const checkLocalService = async () => {
        try {
            // 修改：增加超时设置和错误处理
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 3000); // 3秒超时
            
            const response = await fetch('http://localhost:8090/health', {
                signal: controller.signal
            });
            clearTimeout(timeoutId);
            
            const text = await response.text();
            console.log('本地服务响应:', text); // 添加调试日志
            
            // 验证是否是目标服务
            if (text.includes('HuskyAI_Backend')) {
                if (!isLocalServerAvailable) {
                    console.log('检测到本地服务可用，切换到本地服务');
                    isLocalServerAvailable = true;
                }
            } else {
                console.log('本地服务响应不匹配，使用远程服务');
                isLocalServerAvailable = false;
            }
        } catch (error) {
            console.log('检查本地服务出错:', error.message); // 添加错误日志
            if (isLocalServerAvailable) {
                console.log('本地服务不可用，切换到远程服务');
                isLocalServerAvailable = false;
            }
        }
    };

    // 立即检查一次
    checkLocalService();
    
    // 每5秒检查一次本地服务状态
    setInterval(checkLocalService, 5000);

    // 代理请求处理
    app.use('/api', (req, res, next) => {
        console.log(`收到请求: ${req.method} ${req.url}`);
        if (isLocalServerAvailable) {
            localProxy(req, res, next);
        } else {
            remoteProxy(req, res, next);
        }
    });
};