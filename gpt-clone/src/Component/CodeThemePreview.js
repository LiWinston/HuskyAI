import React, { useState } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { CODE_EXAMPLES } from '../constants/codeExamples';
import './CodeThemePreview.css';

const CodeThemePreview = ({ theme, isZH }) => {
    const [selectedLanguage, setSelectedLanguage] = useState('javascript');
    const languages = Object.keys(CODE_EXAMPLES);

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
};

export default CodeThemePreview; 