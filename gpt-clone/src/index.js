import React, {useEffect, useState} from 'react';
import ReactDOM from 'react-dom/client';
import {BrowserRouter, Route, Routes, useLocation, useNavigate,} from 'react-router-dom';
import Chat from './Chat';
import Login from './Login';
import Confirm from './Confirm';
import axios from 'axios';
import AdminDashboard from './Pages/AdminDashboard';
import './index.css';
import reportWebVitals from './reportWebVitals';
import {AnimatePresence, motion} from 'framer-motion';
import SharePage from './SharePage';

const root = ReactDOM.createRoot(document.getElementById('root'));
const LOCAL_URLS = ['http://localhost:8090/health'];
const REMOTE_URL = '/health';

function detectEnvironment(updateStatus, setError, finishDetection) {
    let isLocalServiceAvailable = false;

    const checkServices = async () => {
        for (const url of LOCAL_URLS) {
            try {
                updateStatus(`Trying to connect to local service: ${url}`);
                await axios.get(url);
                window.API_BASE_URL = url.replace('/health', '');
                localStorage.setItem('API_BASE_URL', window.API_BASE_URL);
                isLocalServiceAvailable = true;
                updateStatus(`Connected to local service: ${url}`);
                // await new Promise(resolve => setTimeout(resolve, 100));
                finishDetection();
                return;
            } catch (error) {
                updateStatus(`Failed to connect to local service: ${url}`);
            }
        }

        if (!isLocalServiceAvailable) {
            try {
                updateStatus(`Trying to connect to remote server: ${REMOTE_URL}`);
                await axios.get(REMOTE_URL);
                window.API_BASE_URL = REMOTE_URL.replace('/health', '/api');
                localStorage.setItem('API_BASE_URL', window.API_BASE_URL);
                updateStatus('Connected to remote server');
                // await new Promise(resolve => setTimeout(resolve, 100));
                finishDetection();
            } catch (error) {
                setError('Failed to connect to any service.');
            }
        }
    };

    checkServices();
}

function LoadingContainer() {
    const [statusMessage, setStatusMessage] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [isDetectionComplete, setDetectionComplete] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        detectEnvironment(setStatusMessage, setErrorMessage, () => {
            setDetectionComplete(true);
            navigate('/login');
        });
    }, [navigate]);

    if (errorMessage) {
        return <div>Error: {errorMessage}</div>;
    }

    if (isDetectionComplete) {
        return null;
    }

    return <div>Loading: {statusMessage}</div>;
}

function AnimatedRoutes() {
    const location = useLocation();

    return (
        <AnimatePresence exitBeforeEnter>
            <Routes location={location} key={location.pathname}>
                <Route
                    path="/"
                    element={
                        <motion.div
                            initial="initial"
                            animate="in"
                            exit="out"
                            variants={pageVariants}
                            transition={pageTransition}
                        >
                            <LoadingContainer/>
                        </motion.div>
                    }
                />
                <Route
                    path="/login"
                    element={
                        <motion.div
                            initial="initial"
                            animate="in"
                            exit="out"
                            variants={loginVariants}
                            transition={loginTransition}
                        >
                            <Login/>
                        </motion.div>
                    }
                />
                {/*AdminRegistrationConfirm*/}
                <Route
                    path="/user/register/confirm/:token"
                    element={
                        <motion.div
                            initial="initial"
                            animate="in"
                            exit="out"
                            variants={pageVariants}
                            transition={pageTransition}
                        >
                            <Confirm/>
                        </motion.div>
                    }
                />
                <Route
                    path="/chat"
                    element={
                        <motion.div
                            initial="initial"
                            animate="in"
                            exit="out"
                            variants={pageVariants}
                            transition={pageTransition}
                        >
                            <Chat/>
                        </motion.div>
                    }
                />
                <Route
                    path="/chat/share/:shareCode"
                    element={<SharePage/>}
                />

                <Route
                    path="/admin"
                    element={
                        <motion.div
                            initial="initial"
                            animate="in"
                            exit="out"
                            variants={pageVariants}
                            transition={pageTransition}
                        >
                            <AdminDashboard/> {/* Admin Page */}
                        </motion.div>
                    }
                />

            </Routes>
        </AnimatePresence>
    );
}

// Call AnimatedRoutes during root rendering.
root.render(
    <BrowserRouter>
        <AnimatedRoutes/>
    </BrowserRouter>,
);
reportWebVitals();
// Define animation parameters.
const pageVariants = {
    initial: {
        opacity: 0,
        x: '-100vw',
    },
    in: {
        opacity: 1,
        x: 0,
    },
    out: {
        opacity: 0,
        x: '100vw',
    },
};

const pageTransition = {
    type: 'tween',
    ease: [0.65, 0, 0.35, 1],
    duration: 0.21,
};

// Unique login zoom animation from large to set size.
const loginVariants = {
    initial: {
        opacity: 0,
        scale: 8,
    },
    in: {
        opacity: 1,
        scale: 1,
    },
    out: {
        opacity: 0,
        scale: 0.3,
    },
};

const loginTransition = {
    type: 'tween',
    ease: 'anticipate',
    duration: 0.35,
};

