import React, { useState, useRef } from 'react';

function MessageInput({ onSendMessage }) {
    const [input, setInput] = useState('');
    const textareaRef = useRef(null);

    const handleInputChange = (event) => {
        setInput(event.target.value);
    };

    const handleSend = () => {
        onSendMessage(input);
        setInput('');
    };

    return (
        <div className="input-container">
            <textarea
                ref={textareaRef}
                value={input}
                onChange={handleInputChange}
                onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault();
                        handleSend();
                    }
                }}
                placeholder="Type your message..."
            />
            <button onClick={handleSend}>Send</button>
        </div>
    );
}

export default MessageInput;
