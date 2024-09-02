import React from 'react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import VscDarkPlus from 'react-syntax-highlighter/dist/cjs/styles/prism/vsc-dark-plus';
import DOMPurify from 'dompurify';

function MessageList({ messages }) {
    return (
        <div className="chat-window">
            {messages.map((msg, index) => (
                <div key={index} className={`message-container ${msg.sender}`}>
                    <div className={`message ${msg.sender}`}>
                        <ReactMarkdown
                            children={DOMPurify.sanitize(msg.text)}
                            components={{
                                code({ node, inline, className, children, ...props }) {
                                    const match = /language-(\w+)/.exec(className || '');
                                    return !inline ? (
                                        <SyntaxHighlighter
                                            style={VscDarkPlus}
                                            language={match ? match[1] : 'plaintext'}
                                            PreTag="div"
                                            children={String(children).replace(/\n$/, '')}
                                            {...props}
                                        />
                                    ) : (
                                        <code className={className} {...props}>
                                            {children}
                                        </code>
                                    );
                                },
                            }}
                        />
                    </div>
                    <div className={`timestamp ${msg.sender}-timestamp`}>
                        {new Date(msg.timestamp).toLocaleString(navigator.language, {
                            year: 'numeric',
                            month: navigator.language.startsWith('zh') ? 'long' : 'short',
                            day: 'numeric',
                            hour: 'numeric',
                            minute: 'numeric',
                            second: 'numeric',
                        })}
                    </div>
                </div>
            ))}
        </div>
    );
}

export default MessageList;
