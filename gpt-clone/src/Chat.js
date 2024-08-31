import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { okaidia } from 'react-syntax-highlighter/dist/esm/styles/prism';
import './Chat.css';
import DOMPurify from 'dompurify';

function Chat() {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const chatWindowRef = useRef(null);

    const sendMessage = async () => {
        if (input.trim() === '') return;

        const timestamp = new Date().toLocaleTimeString();

        setMessages([...messages, { sender: 'user', text: input, timestamp }]);
        setLoading(true);

        try {
            const response = await axios.post(`${window.API_BASE_URL}/chat`, { prompt: input }, { headers: { 'Content-Type': 'application/json' } });
            const sanitizedResponse = DOMPurify.sanitize(response.data);


            setMessages([...messages, { sender: 'user', text: input, timestamp }, { sender: 'bot', text: sanitizedResponse, timestamp }]);
            setError(null);
        } catch (error) {
            let errorMessage = 'An unexpected error occurred';
            if (error.response) {
                if (error.response.data && typeof error.response.data === 'object') {
                    errorMessage = error.response.data.error + ': ' + error.response.data.message + ' (' + error.response.data.status + ')' || 'No error message in response';
                } else if (typeof error.response.data === 'string') {
                    errorMessage = error.response.data;
                } else {
                    errorMessage = 'Error response received but no message available';
                }
            } else if (error.message) {
                errorMessage = error.message + ' (no response received)';
            }

            setMessages([...messages, { sender: 'user', text: input, timestamp }, { sender: 'bot', text: errorMessage, timestamp }]);
            setError(null);
        }

        setInput('');
        setLoading(false);
    };

    useEffect(() => {
        if (chatWindowRef.current) {
            chatWindowRef.current.scrollTo({
                top: chatWindowRef.current.scrollHeight,
                behavior: 'smooth'
            });
        }
    }, [messages]);

    return (
        <div className="chat-container">
            <div className="chat-window" ref={chatWindowRef}>
                {messages.map((msg, index) => (
                    <div key={index} className={`message ${msg.sender}`}>
                        <ReactMarkdown
                            children={DOMPurify.sanitize(msg.text)}
                            components={{
                                code({ node, inline, className, children, ...props }) {
                                    const match = /language-(\w+)/.exec(className || '');
                                    const detectedLanguage = detectLanguage(String(children));
                                    return !inline ? (
                                        <SyntaxHighlighter
                                            style={okaidia}
                                            language={match ? match[1] : detectedLanguage}
                                            PreTag="div"
                                            children={String(children).replace(/\n$/, '')}
                                            {...props}
                                        />
                                    ) : (
                                        <code className={className} {...props}>
                                            {children}
                                        </code>
                                    );
                                }
                            }}
                        />

                        <div className={`timestamp ${msg.sender}-timestamp`}>{msg.timestamp}</div>
                    </div>
                ))}
            </div>
            <div className="input-container">
                <textarea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && sendMessage()}
                    placeholder="Type your message..."
                    disabled={loading}
                    className={loading ? 'input-disabled' : ''}
                    rows="1"
                    style={{ resize: 'none', overflow: 'auto', maxHeight: '120px' }} // maximum height set to 6 lines
                />
                <button onClick={sendMessage} disabled={loading}>
                    {loading ? 'Sending...' : 'Send'}
                </button>
            </div>
        </div>
    );
}

function detectLanguage(code) {
    // 移除注释，以避免干扰检测
    code = code.replace(/\/\/.*|\/\*[\s\S]*?\*\//g, '');

    const languageRules = [
        {
            name: 'javascript',
            rules: [
                { pattern: /\b(const|let|var)\s+\w+\s*=/, score: 2 },
                { pattern: /function\s+\w+\s*\(.*\)\s*{/, score: 2 },
                { pattern: /=>\s*{/, score: 2 },
                { pattern: /console\.(log|error|warn|info)\(/, score: 2 },
                { pattern: /document\.getElementById\(/, score: 3 },
                { pattern: /\}\s*else\s*{/, score: 1 },
                { pattern: /new Promise\(/, score: 3 },
                { pattern: /async\s+function/, score: 3 },
                { pattern: /\bawait\b/, score: 3 },
                { pattern: /import\s+.*\s+from\s+['"]/, score: 3 },
                { pattern: /export\s+(default\s+)?(function|class|const|let|var)/, score: 3 },
                { pattern: /\.[0-9]+e[+-]?[0-9]+/, score: 1 }
            ]
        },
        {
            name: 'python',
            rules: [
                { pattern: /def\s+\w+\s*\(.*\):\s*$/, score: 2 },
                { pattern: /class\s+\w+(\(\w+\))?:\s*$/, score: 2 },
                { pattern: /import\s+\w+(\s+as\s+\w+)?/, score: 2 },
                { pattern: /from\s+\w+\s+import\s+/, score: 2 },
                { pattern: /print\s*\(/, score: 1 },
                { pattern: /if\s+__name__\s*==\s*('|")__main__\1:/, score: 3 },
                { pattern: /^\s*@\w+/, score: 2 },
                { pattern: /\bfor\s+\w+\s+in\s+/, score: 2 },
                { pattern: /\bwith\s+.*\s+as\s+/, score: 3 },
                { pattern: /\blambda\s+.*:/, score: 3 },
                { pattern: /:\s*$/, score: 1 },
                { pattern: /\s{4}/, score: 1 }
            ]
        },
        {
            name: 'java',
            rules: [
                { pattern: /public\s+(static\s+)?(final\s+)?(class|interface|enum)\s+\w+/, score: 3 },
                { pattern: /public\s+static\s+void\s+main\s*\(String\s*\[\]\s*args\)/, score: 5 },
                { pattern: /@Override/, score: 2 },
                { pattern: /System\.out\.println\(/, score: 2 },
                { pattern: /import\s+java\./, score: 2 },
                { pattern: /new\s+\w+(\s*<.*>)?\s*\(/, score: 1 },
                { pattern: /\b(public|private|protected)\s+/, score: 1 },
                { pattern: /\b(final|static)\s+/, score: 1 },
                { pattern: /\b(try|catch|finally)\s*{/, score: 2 },
                { pattern: /\bthrows\s+\w+/, score: 2 },
                { pattern: /\bextends\s+\w+/, score: 2 },
                { pattern: /\bimplements\s+\w+/, score: 2 }
            ]
        },
        {
            name: 'cpp',
            rules: [
                { pattern: /#include\s*<[\w.]+>/, score: 3 },
                { pattern: /std::\w+/, score: 2 },
                { pattern: /int\s+main\s*\(\s*(int|void)/, score: 4 },
                { pattern: /\b(class|struct)\s+\w+/, score: 2 },
                { pattern: /\b(public|private|protected):/, score: 2 },
                { pattern: /\b(const|virtual|friend|typename)\b/, score: 2 },
                { pattern: /\b(new|delete)\b/, score: 2 },
                { pattern: /\b(template|namespace)\b/, score: 3 },
                { pattern: /\bcout\s*<</, score: 2 },
                { pattern: /\bcin\s*>>/, score: 2 },
                { pattern: /::\s*\w+/, score: 2 }
            ]
        },
        {
            name: 'csharp',
            rules: [
                { pattern: /using\s+System;/, score: 3 },
                { pattern: /namespace\s+\w+/, score: 2 },
                { pattern: /class\s+\w+/, score: 2 },
                { pattern: /Console\.WriteLine\(/, score: 2 },
                { pattern: /public\s+(static\s+)?void\s+Main\s*\(/, score: 4 },
                { pattern: /\b(public|private|protected|internal)\s+/, score: 1 },
                { pattern: /\b(var|string|int|bool)\s+\w+\s*=/, score: 2 },
                { pattern: /\busing\s*\(/, score: 2 },
                { pattern: /\basync\s+Task/, score: 3 },
                { pattern: /\bawait\s+/, score: 3 },
                { pattern: /\b(List|Dictionary)<.*>/, score: 2 }
            ]
        },
        {
            name: 'php',
            rules: [
                { pattern: /<\?php/, score: 5 },
                { pattern: /\$\w+/, score: 2 },
                { pattern: /echo\s+/, score: 2 },
                { pattern: /function\s+\w+\s*\(/, score: 2 },
                { pattern: /\b(public|private|protected)\s+function/, score: 3 },
                { pattern: /\bclass\s+\w+/, score: 2 },
                { pattern: /\bnamespace\s+\w+/, score: 3 },
                { pattern: /\buse\s+\w+/, score: 2 },
                { pattern: /\b(include|require)(_once)?\s*\(/, score: 2 },
                { pattern: /\b(foreach|as)\b/, score: 2 },
                { pattern: /\b->\w+/, score: 2 }
            ]
        },
        {
            name: 'ruby',
            rules: [
                { pattern: /def\s+\w+/, score: 2 },
                { pattern: /class\s+\w+/, score: 2 },
                { pattern: /module\s+\w+/, score: 2 },
                { pattern: /\battr_(reader|writer|accessor)\b/, score: 3 },
                { pattern: /\bputs\b/, score: 1 },
                { pattern: /\b(if|unless|while|until)\b/, score: 1 },
                { pattern: /\bdo\s*\|.*\|/, score: 2 },
                { pattern: /\bend\b/, score: 1 },
                { pattern: /\b(require|include)\b/, score: 2 },
                { pattern: /\b(true|false|nil)\b/, score: 1 },
                { pattern: /:\w+/, score: 2 }
            ]
        },
        {
            name: 'go',
            rules: [
                { pattern: /package\s+main/, score: 3 },
                { pattern: /func\s+main\(\)/, score: 4 },
                { pattern: /import\s+\([\s\S]*?\)/, score: 3 },
                { pattern: /func\s+\(\w+\s+\*?\w+\)\s+\w+/, score: 3 },
                { pattern: /fmt\.(Print|Println|Printf)\(/, score: 2 },
                { pattern: /\bvar\s+\w+\s+\w+/, score: 2 },
                { pattern: /\b:=\b/, score: 2 },
                { pattern: /\bgo\s+func\b/, score: 3 },
                { pattern: /\bchan\b/, score: 2 },
                { pattern: /\bdefer\b/, score: 2 },
                { pattern: /\binterface\{\}/, score: 2 }
            ]
        }
    ];

    const scores = {};
    languageRules.forEach(lang => {
        scores[lang.name] = 0;
        lang.rules.forEach(rule => {
            const matches = (code.match(rule.pattern) || []).length;
            scores[lang.name] += matches * rule.score;
        });
    });

    // 根据得分确定最可能的语言
    const detectedLanguage = Object.keys(scores).reduce((a, b) => scores[a] > scores[b] ? a : b);
    const confidence = scores[detectedLanguage] / Object.values(scores).reduce((a, b) => a + b, 0);

    return detectedLanguage;  // 只返回检测到的语言名称，不返回置信度
}

export default Chat;


// function detectLanguage(code) {
//     const languagePatterns = [
//         { name: 'javascript', pattern: /(?:function|const|let|var|console\.log|=>|async|await|document\.|window\.|import\s+React|\/\/[^\n]*|\/\*[\s\S]*?\*\/)/ },
//         { name: 'python', pattern: /(?:def|print\(|import|self|lambda|async|await|class\s+\w+|__init__|#.*)/ },
//         {
//             name: 'java',
//             pattern: /(?:public\s+class|System\.out\.println|void\s+main|@Override|new|@Autowired|@Component|@Service|@Repository|@Controller|@Configuration|@RestController|@SpringBootApplication|@RequestMapping|@PathVariable|@Value|@Bean|@PostConstruct|import\s+org\.springframework\.|import\s+javax\.|Logger\.getLogger|\/\/[^\n]*|\/\*[\s\S]*?\*\/)/
//         },
//         { name: 'cpp', pattern: /(?:#include\s+<|std::|int\s+main|#define|->|cout\s*<<|using\s+namespace|\/\/[^\n]*|\/\*[\s\S]*?\*\/)/ },
//         { name: 'ruby', pattern: /(?:def\s+|puts\s+|end|do\s+|:|\$|class\s+\w+|module\s+\w+|#.*)/ },
//         { name: 'php', pattern: /(?:<\?php|echo|->|::|\$|function\s+|use\s+|namespace\s+|\/\/[^\n]*|\/\*[\s\S]*?\*\/)/ },
//         { name: 'csharp', pattern: /(?:using\s+System|namespace\s+|class\s+|Console\.WriteLine|new\s+|public\s+static\s+void\s+Main|\/\/[^\n]*|\/\*[\s\S]*?\*\/)/ },
//         { name: 'go', pattern: /(?:package\s+main|import\s+|func\s+|fmt\.Println|var\s+|:=|\/\/[^\n]*|\/\*[\s\S]*?\*\/)/ },
//         { name: 'kotlin', pattern: /(?:fun\s+main|println\(|val\s+|var\s+|class\s+|data\s+class|\/\/[^\n]*|\/\*[\s\S]*?\*\/)/ },
//         { name: 'swift', pattern: /(?:import\s+Foundation|let\s+|var\s+|func\s+|print\(|class\s+\w+|\/\/[^\n]*|\/\*[\s\S]*?\*\/)/ },
//         // 添加更多语言特征匹配
//     ];
//
//     for (const { name, pattern } of languagePatterns) {
//         if (pattern.test(code)) {
//             return name;
//         }
//     }
//
//     return 'plaintext'; // 默认语言
// }
