import React, {useEffect, useRef, useState} from 'react';
import axios from 'axios';
import {useParams} from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import {Prism as SyntaxHighlighter} from 'react-syntax-highlighter';
import VscDarkPlus
  from 'react-syntax-highlighter/dist/esm/styles/prism/vsc-dark-plus';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import './SharePage.css';
import {FaCopy, FaDownload} from 'react-icons/fa'; // 添加下载图标

// 使用开源头像链接
const userAvatarUrl = 'https://i.imgur.com/jQhQZKk.png';  // 示例头像：用户
const assistantAvatarUrl = 'https://i.imgur.com/LdP5UQi.png';  // 示例头像：机器人

const LOCAL_URLS = ['http://localhost:8090/health'];
const REMOTE_URL = '/health';

function SharePage() {
  const {shareCode} = useParams();
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const chatWindowRef = useRef(null);
  const [copyNotification, setCopyNotification] = useState(false);

  const detectEnvironment = async () => {
    let isLocalServiceAvailable = false;
    for (const url of LOCAL_URLS) {
      try {
        await axios.get(url);
        window.API_BASE_URL = url.replace('/health', '');
        isLocalServiceAvailable = true;
        return;
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
      }
    }
  };

  useEffect(() => {
    const detectEnvironmentAndFetchMessages = async () => {
      if (!window.API_BASE_URL) {
        await detectEnvironment();
      }
      if (window.API_BASE_URL) {
        try {
          const response = await axios.get(
              `${window.API_BASE_URL}/chat/share/${shareCode}`);
          setMessages(response.data.data || []);
          setLoading(false);
        } catch (error) {
          setError('Failed to load shared conversation.');
          setLoading(false);
        }
      }
    };

    detectEnvironmentAndFetchMessages();
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

  const handleExportAsImage = () => {
    const container = chatWindowRef.current; // 直接使用 chatWindowRef

    html2canvas(container, {
      scale: 1.5,
      backgroundColor: null, // 设置背景颜色为透明
      logging: true,
    }).then((canvas) => {
      const link = document.createElement('a');
      link.download = 'conversation.png';
      link.href = canvas.toDataURL('image/png');
      link.click();
    });
  };

  const handleExportAsPDF = () => {
    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
    });

    const container = chatWindowRef.current; // 直接使用 chatWindowRef

    html2canvas(container, {scale: 1, backgroundColor: null}).then((canvas) => {
      const imgData = canvas.toDataURL('image/png');
      const imgWidth = 210;
      const pageHeight = 297;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;

      let heightLeft = imgHeight;
      let position = 0;

      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;

      while (heightLeft >= 0) {
        position = heightLeft - imgHeight;
        pdf.addPage();
        pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
        heightLeft -= pageHeight;
      }

      pdf.save('conversation.pdf');
    });
  };

  if (loading) {
    return <p>Loading...</p>;
  }

  if (error) {
    return <p>{error}</p>;
  }

  return (
      <div className="share-page">
        <div className="share-page__username-header">
          <h2>Shared by: {shareCode}</h2>
          <div className="share-page__export-buttons">
            <button className="share-page__button" onClick={handleExportAsImage}
                    title="Export as Image">
              <FaDownload/> Image
            </button>
            <button className="share-page__button" onClick={handleExportAsPDF}
                    title="Export as PDF">
              <FaDownload/> PDF
            </button>
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
                    <div
                        className="share-page__message-content"
                        onMouseEnter={() => {
                          if (!msg.content.includes('```')) {
                            const copyIcon = chatWindowRef.current.querySelectorAll(
                                '.copy-icon')[index];
                            copyIcon.style.display = 'block';
                          }
                        }}
                        onMouseLeave={() => {
                          if (!msg.content.includes('```')) {
                            const copyIcon = chatWindowRef.current.querySelectorAll(
                                '.copy-icon')[index];
                            copyIcon.style.display = 'none';
                          }
                        }}
                    >
                      <ReactMarkdown
                          // children={DOMPurify.sanitize(msg.content)}
                          children={msg.content}
                          components={{
                            code({
                                   node,
                                   inline,
                                   className,
                                   children,
                                   ...props
                                 }) {
                              const match = /language-(\w+)/.exec(
                                  className || '');
                              return !inline ? (
                                  <div
                                      className="code-block"
                                      onMouseEnter={() => {
                                        const copyIcon = chatWindowRef.current.querySelectorAll(
                                            '.copy-icon')[index];
                                        copyIcon.style.display = 'block';
                                      }}
                                      onMouseLeave={() => {
                                        const copyIcon = chatWindowRef.current.querySelectorAll(
                                            '.copy-icon')[index];
                                        copyIcon.style.display = 'none';
                                      }}
                                  >
                                    <SyntaxHighlighter
                                        style={VscDarkPlus}
                                        language={match
                                            ? match[1]
                                            : 'plaintext'}
                                        PreTag="div"
                                        {...props}
                                    >
                                      {String(children).trim()}
                                    </SyntaxHighlighter>
                                    <FaCopy className="copy-icon"
                                            onClick={() => handleCopyText(
                                                String(children).trim())}/>
                                  </div>
                              ) : (
                                  <code className={className} {...props}>
                                    {children}
                                  </code>
                              );
                            },
                          }}
                      />
                      {!msg.content.includes('```') && (
                          <FaCopy className="copy-icon"
                                  onClick={() => handleCopyText(msg.content)}/>
                      )}
                    </div>
                    <p className="share-page__timestamp">{new Date(
                        msg.timestamp).toLocaleString()}</p>
                  </div>
              ))
          ) : (
              <p>No messages to display.</p>
          )}
        </div>
        {copyNotification &&
            <div className="copy-notification">Copied to clipboard!</div>}
      </div>
  );
}

export default SharePage;
