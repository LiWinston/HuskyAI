import React, {useEffect, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import axios from 'axios'; // If using React Router
const LOCAL_URLS = ['http://localhost:8090/health'];
const REMOTE_URL = '/health';
export default function Confirm() {
    const [message, setMessage] = useState('Confirming...');
    // eslint-disable-next-line no-unused-vars
    const [username, setUsername] = useState('');
    const {token} = useParams(); // Obtain token parameter from routers
    const navigate = useNavigate(); // Used for page navigation.
    // eslint-disable-next-line no-unused-vars
    const [error, setError] = useState(null);

    useEffect(() => {
        const detectEnvironment = async () => {
            let isLocalServiceAvailable = false;
            for (const url of LOCAL_URLS) {
                try {
                    await axios.get(url);
                    window.API_BASE_URL = url.replace('/health', '');
                    isLocalServiceAvailable = true;
                    return;  // If the connection is successful, return early.
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
                    // Failed to connect, returning early.
                }
            }
        };

        // Call the environment detection function.
        detectEnvironment().then(() => {
            if (!window.API_BASE_URL) {
                setMessage('No available service.');
                return;
            }

            // Initiate a GET request to the confirmation controller.
            axios.get(`/api/user/register/confirm/${token}`).then((response) => {
                const data = response.data;

                if (data.code === 1) {
                    setMessage(
                        `Admin registration confirmed! Welcome, ${data.data}.`);
                    setUsername(data.data); // Obtain the username after success.
                    // Redirect to the login page after waiting for 3 seconds and paste the username.
                    setTimeout(() => {
                        navigate('/login', {state: {username: data.data}});
                    }, 3000);
                } else {
                    setMessage(data.msg || 'Confirmation failed. code: ' + data.code);
                }
            }).catch((error) => {
                setMessage(error.message || 'Confirmation failed.');
            });
        });
    }, [token, navigate]);

    return (
        <div style={styles.container}>
            <h1>{message}</h1>
        </div>
    );
}

// Simple CSS styles.
const styles = {
    container: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        textAlign: 'center',
    },
};
