import React from 'react';
import { motion } from 'framer-motion';

function NotificationBanner({ message }) {
    return (
        <motion.div
            className="notification-banner"
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 50 }}
            transition={{ duration: 0.5 }}
        >
            {message}
        </motion.div>
    );
}

export default NotificationBanner;
