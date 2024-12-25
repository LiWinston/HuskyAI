import React, { useState, useEffect } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { CODE_EXAMPLES } from '../constants/codeExamples';
import './CodeThemePreview.css';

// 使用localStorage来保存上次选择的语言
const LAST_SELECTED_LANGUAGE_KEY = 'lastSelectedCodeLanguage';

const CodeThemePreview = React.memo(({ theme, isZH }) => {
    const [selectedLanguage, setSelectedLanguage] = useState(() => {
        return localStorage.getItem(LAST_SELECTED_LANGUAGE_KEY) || 'javascript';
    });
    
    const languages = Object.keys(CODE_EXAMPLES);

    useEffect(() => {
        localStorage.setItem(LAST_SELECTED_LANGUAGE_KEY, selectedLanguage);
    }, [selectedLanguage]);

    return (
        <div className="code-theme-preview">
            <div className="preview-header">
                <span className="preview-title">{isZH ? "预览" : "Preview"}</span>
                <select 
                    value={selectedLanguage}
                    onChange={(e) => setSelectedLanguage(e.target.value)}
                    className="language-selector"
                >
                    {languages.map(lang => (
                        <option key={lang} value={lang}>
                            {CODE_EXAMPLES[lang].name}
                        </option>
                    ))}
                </select>
            </div>
            <div className="preview-content">
                <SyntaxHighlighter
                    language={selectedLanguage}
                    style={theme.style}
                    className="preview-code"
                >
                    {CODE_EXAMPLES[selectedLanguage].code}
                </SyntaxHighlighter>
            </div>
        </div>
    );
}, (prevProps, nextProps) => {
    // 只有当主题样式或语言真正改变时才重新渲染
    return (
        prevProps.theme.style === nextProps.theme.style &&
        prevProps.isZH === nextProps.isZH
    );
});

export default CodeThemePreview; 