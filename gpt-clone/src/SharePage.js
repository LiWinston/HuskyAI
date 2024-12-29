import React, {useEffect, useRef, useState} from 'react';
import axios from 'axios';
import {useParams} from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {Prism as SyntaxHighlighter} from 'react-syntax-highlighter';
import VscDarkPlus from 'react-syntax-highlighter/dist/esm/styles/prism/vsc-dark-plus';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import './SharePage.css';
import {FaCopy, FaDownload, FaHome} from 'react-icons/fa';
import Lottie from 'lottie-react';
import loadingAnimation from './assets/loading.json';
import {MathJax, MathJaxContext} from 'better-react-mathjax';

// 使用开源头像链接
const userAvatarUrl = 'https://img.icons8.com/?size=100&id=23265&format=png&color=000000';  // 示例头像：用户
const assistantAvatarUrl = 'https://img.icons8.com/?size=100&id=37410&format=png&color=000000';  // 示例头像：机器人

// 预处理文本，将"\(...)"和"\[...]"转换为"$$...$$"的格式
function preprocessText(text) {
    return text
    .replace(/\\\((.*?)\\\)/g, '$$$$ $1 $$$$')
    .replace(/\\\[([\s\S]*?)\\\]/g, '$$$$ $1 $$$$');
}

function SharePage() {
    const {shareCode} = useParams();
    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const chatWindowRef = useRef(null);
    const [copyNotification, setCopyNotification] = useState(false);

    useEffect(() => {
        const fetchMessages = async () => {
            try {
                const response = await axios.get(`/api/chat/share/${shareCode}`);
                // 确保消息按时间顺序排序
                const sortedMessages = (response.data.data || []).sort((a, b) =>
                    new Date(a.timestamp) - new Date(b.timestamp)
                );
                setMessages(sortedMessages);
                setLoading(false);
            } catch (error) {
                setError('Failed to load shared conversation.');
                setLoading(false);
            }
        };

        fetchMessages();
    }, [shareCode]);

    useEffect(() => {
        if (chatWindowRef.current && messages.length > 0) {
            const firstMessage = chatWindowRef.current.querySelector(
                '.share-page__message-container');
            if (firstMessage) {
                firstMessage.scrollIntoView({behavior: 'smooth', block: 'center'});
            }
        }
    }, [messages]);

    const handleCopyText = (text) => {
        navigator.clipboard.writeText(text).then(() => {
            setCopyNotification(true);
            setTimeout(() => setCopyNotification(false), 1500);  // 定时消失的气泡
        });
    };

    const handleExportAsImage = async () => {
        const container = chatWindowRef.current;

        try {
            // 保存原始样式
            const originalStyle = container.style.cssText;
            const originalWidth = container.offsetWidth;
            const originalHeight = container.offsetHeight;

            // 临时调整样式以获得更好的截图效果
            container.style.width = '1200px';
            container.style.height = 'auto';  // 确保高度自适应
            container.style.padding = '40px';
            container.style.background = '#ffffff';
            container.style.position = 'relative';
            container.style.overflow = 'visible';

            // 等待所有图片加载完成
            const images = container.getElementsByTagName('img');
            await Promise.all(Array.from(images).map(img => {
                if (img.complete) return Promise.resolve();
                return new Promise(resolve => {
                    img.onload = resolve;
                    img.onerror = resolve;
                });
            }));

            // 使用 html2canvas 截图
            const canvas = await html2canvas(container, {
                scale: 2,  // 提高清晰度
                useCORS: true,
                backgroundColor: '#ffffff',
                logging: false,
                windowWidth: 1200,
                windowHeight: container.scrollHeight,
                height: container.scrollHeight,
                width: 1200,
                onclone: (clonedDoc) => {
                    // 处理克隆的 DOM，确保样式正确应用
                    const clonedContainer = clonedDoc.querySelector('.share-page__chat-window');
                    if (clonedContainer) {
                        clonedContainer.style.transform = 'none';
                        clonedContainer.style.width = '1200px';
                        clonedContainer.style.height = 'auto';
                    }
                }
            });

            // 恢复原始样式
            container.style.cssText = originalStyle;
            container.style.width = originalWidth + 'px';
            container.style.height = originalHeight + 'px';

            // 创建下载链接
            const image = canvas.toDataURL('image/png', 1.0);
            const link = document.createElement('a');
            link.download = `chat-${new Date().toISOString().slice(0,10)}.png`;
            link.href = image;
            link.click();
        } catch (error) {
            console.error('Export failed:', error);
            // TODO: 显示错误通知
        }
    };

    const handleExportAsPDF = async () => {
        const container = chatWindowRef.current;

        try {
            // 保存原始样式
            const originalStyle = container.style.cssText;

            // 设置临时样式
            container.style.width = '800px';
            container.style.padding = '40px';
            container.style.background = '#ffffff';

            // 等待图片加载
            await Promise.all(
                Array.from(container.getElementsByTagName('img'))
                    .map(img => img.complete ? Promise.resolve() : new Promise(resolve => {
                        img.onload = resolve;
                        img.onerror = resolve;
                    }))
            );

            // 创建 PDF
            const pdf = new jsPDF({
                orientation: 'portrait',
                unit: 'pt',
                format: 'a4',
                compress: true
            });

            // 获取页面尺寸（以点为单位）
            const pageWidth = pdf.internal.pageSize.getWidth();
            const pageHeight = pdf.internal.pageSize.getHeight();
            const margin = 40;

            // 计算可用内容区域
            const contentWidth = pageWidth - (2 * margin);
            const contentHeight = pageHeight - (2 * margin);

            // 生成完整内容的画布
            const canvas = await html2canvas(container, {
                scale: 2,
                useCORS: true,
                backgroundColor: '#ffffff',
                logging: false,
                width: 800,
                windowWidth: 800
            });

            // 计算缩放后的尺寸
            const imgWidth = contentWidth;
            const imgHeight = (canvas.height * contentWidth) / canvas.width;

            // 计算总页数
            const pageCount = Math.ceil(imgHeight / contentHeight);

            // 逐页添加内容
            for (let page = 0; page < pageCount; page++) {
                if (page > 0) {
                    pdf.addPage();
                }

                // 计算当前页的裁剪区域
                const yStart = page * contentHeight * (canvas.height / imgHeight);
                const yHeight = Math.min(
                    canvas.height - yStart,
                    contentHeight * (canvas.height / imgHeight)
                );

                // 创建临时画布进行裁剪
                const tempCanvas = document.createElement('canvas');
                tempCanvas.width = canvas.width;
                tempCanvas.height = yHeight;

                const ctx = tempCanvas.getContext('2d');
                ctx.drawImage(
                    canvas,
                    0, yStart,
                    canvas.width, yHeight,
                    0, 0,
                    canvas.width, yHeight
                );

                // 将裁剪后的内容添加到 PDF
                const imgData = tempCanvas.toDataURL('image/jpeg', 1.0);

                // 计算当前页图片高度
                const currentPageImgHeight = (yHeight * contentWidth) / canvas.width;

                pdf.addImage(
                    imgData,
                    'JPEG',
                    margin,
                    margin,
                    contentWidth,
                    currentPageImgHeight
                );
            }

            // 恢复原始样式
            container.style.cssText = originalStyle;

            // 保存 PDF
            pdf.save(`chat-${new Date().toISOString().slice(0,10)}.pdf`);
        } catch (error) {
            console.error('Export failed:', error);
            // TODO: 添加错误提示
        }
    };

    const MessageContent = ({ content, onCopy }) => {
        const mathJaxRef = useRef(null);

        const mathJaxConfig = {
            loader: { load: ["input/tex", "output/chtml"] },
            tex: { packages: { "[+]": ["color"] } },
            startup: {
                typeset: false,  // 防止自动排版
            }
        };

        useEffect(() => {
            // 清理函数
            return () => {
                if (mathJaxRef.current) {
                    try {
                        const jaxElements = mathJaxRef.current.getElementsByClassName('MathJax');
                        while (jaxElements.length > 0 && jaxElements[0].parentNode === mathJaxRef.current) {
                            jaxElements[0].remove();
                        }

                        // 移除MathJax脚本缓存
                        const scriptElements = mathJaxRef.current.getElementsByTagName('script');
                        while (scriptElements.length > 0 && scriptElements[0].parentNode === mathJaxRef.current) {
                            scriptElements[0].remove();
                        }
                    } catch (error) {
                        console.error('MathJax cleanup error:', error);
                    }
                }
            };
        }, [content]);

        const processedText = preprocessText(content || '');

        return (
            <div className="share-page__message-content">
                <div className="markdown-table-container" ref={mathJaxRef}>
                    <MathJax dynamic>
                        <ReactMarkdown
                            children={processedText}
                            remarkPlugins={[remarkGfm]}
                            components={{
                                code({node, inline, className, children, ...props}) {
                                    const match = /language-(\w+)/.exec(className || '');
                                    const codeContent = String(children).replace(/\n$/, '');

                                    // 判断是否应该内联显示
                                    const shouldInline = inline ||
                                        (codeContent.length < 50 && !codeContent.includes('\n'));

                                    return shouldInline ? (
                                        <code
                                            className={`inline-code ${className || ''}`}
                                            {...props}
                                        >
                                            {children}
                                        </code>
                                    ) : (
                                        <div className="code-block">
                                            <SyntaxHighlighter
                                                style={VscDarkPlus}
                                                language={match ? match[1] : 'text'}
                                                PreTag="div"
                                                {...props}
                                            >
                                                {codeContent}
                                            </SyntaxHighlighter>
                                            <button
                                                className="copy-button"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    onCopy(codeContent);
                                                }}
                                            >
                                                <FaCopy />
                                            </button>
                                        </div>
                                    );
                                },
                                table({node, ...props}) {
                                    return (
                                        <div className="table-container">
                                            <table {...props} />
                                        </div>
                                    );
                                }
                            }}
                        />
                    </MathJax>
                </div>
                <button
                    className="copy-button"
                    onClick={(e) => {
                        e.stopPropagation();
                        onCopy(content);
                    }}
                >
                    <FaCopy />
                </button>
            </div>
        );
    };

    // 添加时间格式化函数
    const formatTimestamp = (timestamp) => {
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;

        // 如果是今天的消息
        if (date.toDateString() === now.toDateString()) {
            return date.toLocaleTimeString('en-US', {
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            });
        }

        // 如果是昨天的消息
        const yesterday = new Date(now);
        yesterday.setDate(yesterday.getDate() - 1);
        if (date.toDateString() === yesterday.toDateString()) {
            return 'Yesterday ' + date.toLocaleTimeString('en-US', {
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            });
        }

        // 如果是今年的消息
        if (date.getFullYear() === now.getFullYear()) {
            return date.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            });
        }

        // 其他情况显示完整日期
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });
    };

    if (loading) {
        return (
            <div className="share-page__loading">
                <Lottie
                    animationData={loadingAnimation}
                    loop={true}
                    style={{ width: 120, height: 120 }}
                />
                <div>Loading shared conversation...</div>
            </div>
        );
    }

    if (error) {
        return <p>{error}</p>;
    }

    return (
        <MathJaxContext>
            <div className="share-page">
                <div className="share-page__header">
                    <div className="share-page__title">
                        Share ID: <span className="share-page__title-code">{shareCode}</span>
                    </div>
                    <div className="share-page__actions">
                        <button
                            className="share-page__button"
                            onClick={handleExportAsImage}
                            title="Export as Image"
                        >
                            <FaDownload/> Image
                        </button>
                        <button
                            className="share-page__button"
                            onClick={handleExportAsPDF}
                            title="Export as PDF"
                        >
                            <FaDownload/> PDF
                        </button>
                        <a
                            href="https://huskyAI.bitsleep.cn"
                            className="share-page__home-link"
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            <FaHome /> Home
                        </a>
                    </div>
                </div>
                <div className="share-page__chat-window" ref={chatWindowRef}>
                    {messages.length > 0 ? (
                        messages.map((msg, index) => (
                            <div
                                key={index}
                                className={`share-page__message-container ${
                                    msg.role === 'user'
                                        ? 'share-page__message--user'
                                        : 'share-page__message--assistant'
                                }`}
                            >
                                <div className="share-page__avatar">
                                    {msg.role === 'user' ? (
                                        <img src={userAvatarUrl} alt="User"/>
                                    ) : (
                                        <img src={assistantAvatarUrl} alt="Assistant"/>
                                    )}
                                </div>
                                <MessageContent content={msg.content} onCopy={handleCopyText} />
                                <p className="share-page__timestamp">
                                    {formatTimestamp(msg.timestamp)}
                                </p>
                            </div>
                        ))
                    ) : (
                        <p>No messages to display.</p>
                    )}
                </div>
                {copyNotification &&
                    <div className="copy-notification">Copied to clipboard!</div>}
            </div>
        </MathJaxContext>
    );
}

export default SharePage;
