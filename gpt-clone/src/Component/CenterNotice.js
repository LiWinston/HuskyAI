import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import './CenterNotice.css';

const CenterNotice = ({ message, isVisible, onClose, duration = 3000 }) => {
    React.useEffect(() => {
        if (isVisible && onClose) {
            const timer = setTimeout(onClose, duration);
            return () => clearTimeout(timer);
        }
    }, [isVisible, onClose, duration]);

    return (
        <AnimatePresence>
            {isVisible && (
                <motion.div 
                    className="center-notice"
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                    transition={{ duration: 0.3 }}
                >
                    <div className="notice-content">
                        {message}
                    </div>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default CenterNotice; 