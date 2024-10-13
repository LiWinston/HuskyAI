import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import axios from 'axios';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AnimatedRoutes from './AnimatedRoutes';
import App from './App'; // Assuming App integrates AnimatedRoutes

jest.mock('axios');

describe('Application Routing and Environment Detection', () => {
    beforeEach(() => {
        axios.get.mockReset();
    });

    it('correctly detects local environment and navigates to login', async () => {
        axios.get.mockResolvedValueOnce({}); // Simulate successful health check
        render(
            <MemoryRouter initialEntries={['/']}>
                <App />  // App component should integrate AnimatedRoutes
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Loading: Trying to connect to local service: http://localhost:8090/health')).toBeInTheDocument();
        });

        // After successful detection
        await waitFor(() => {
            expect(screen.queryByText('Login')).toBeInTheDocument();
        });
    });

    it('displays error when no service is available', async () => {
        axios.get.mockRejectedValue(new Error('Connection failed'));  // Simulate connection failure

        render(
            <MemoryRouter initialEntries={['/']}>
                <App />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Error: Failed to connect to any service.')).toBeInTheDocument();
        });
    });

    it('animates route transitions on navigation', async () => {
        render(
            <MemoryRouter initialEntries={['/login']}>
                <AnimatedRoutes />
            </MemoryRouter>
        );

        fireEvent.click(screen.getByText('Login')); // Assuming a login button exists

        await waitFor(() => {
            // Check for the presence of an animated component
            expect(screen.getByRole('dialog', { name: 'Login' })).toHaveStyle('opacity: 1');
        });
    });
});
