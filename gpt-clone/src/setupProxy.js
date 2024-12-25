const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
    // 用于记录本地服务是否可用的状态
    let isLocalServerAvailable = true;

    // 创建远程代理中间件
    const remoteProxy = createProxyMiddleware({
        target: 'https://lmsgpt.bitsleep.cn',
        changeOrigin: true,
        pathRewrite: {'^/api': '/api'},
        logLevel: 'debug',
        onProxyReq: (proxyReq, req, res) => {
            console.log('远程代理请求:', {
                originalUrl: req.url,
                targetUrl: proxyReq.path,
                method: req.method
            });
        }
    });

    // 创建本地代理中间件
    const localProxy = createProxyMiddleware({
        target: 'http://localhost:8090',
        changeOrigin: true,
        pathRewrite: {'^/api': ''},
        logLevel: 'debug',
        onProxyReq: (proxyReq, req, res) => {
            // 如果已知本地服务不可用，直接使用远程代理
            if (!isLocalServerAvailable) {
                return remoteProxy(req, res);
            }
            console.log('本地代理请求:', {
                originalUrl: req.url,
                targetUrl: proxyReq.path,
                method: req.method
            });
        },
        onError: (err, req, res) => {
            // 本地服务器连接失败时，标记状态并静默切换到远程服务器
            if (isLocalServerAvailable) {
                console.log('本地服务器不可用，切换到远程服务器');
                isLocalServerAvailable = false;
            }
            remoteProxy(req, res);
        }
    });

    // 使用代理中间件
    app.use('/api', (req, res, next) => {
        if (isLocalServerAvailable) {
            localProxy(req, res, next);
        } else {
            remoteProxy(req, res, next);
        }
    });
};