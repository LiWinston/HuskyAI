import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import axios from 'axios';
import Confirm from './Confirm';

jest.mock('axios');

describe('Confirm Component', () => {
    const token = 'sample-token';

    beforeEach(() => {
        axios.get.mockReset();
    });

    it('detects local environment successfully and confirms registration', async () => {
        axios.get.mockImplementation(url => {
            if (url.includes('localhost')) {
                return Promise.resolve();  // Simulate successful local connection
            }
            if (url.includes('confirm')) {
                return Promise.resolve({
                    data: { code: 1, data: 'JohnDoe' }
                });  // Simulate successful confirmation
            }
            return Promise.reject(new Error('URL not handled by mock'));
        });

        render(
            <MemoryRouter initialEntries={[`/confirm/${token}`]}>
                <Routes>
                    <Route path="confirm/:token" element={<Confirm />} />
                </Routes>
            </MemoryRouter>
        );

        await waitFor(() => expect(screen.getByText('Admin registration confirmed! Welcome, JohnDoe')).toBeInTheDocument());
    });

    it('falls back to remote environment when local fails', async () => {
        axios.get.mockImplementation(url => {
            if (url.includes('localhost')) {
                return Promise.reject(new Error('Local connection failed'));  // Simulate failed local connection
            }
            if (url.includes('health')) {
                return Promise.resolve();  // Simulate successful remote connection
            }
            return Promise.reject(new Error('URL not handled by mock'));
        });

        render(
            <MemoryRouter initialEntries={[`/confirm/${token}`]}>
                <Routes>
                    <Route path="confirm/:token" element={<Confirm />} />
                </Routes>
            </MemoryRouter>
        );

        await waitFor(() => expect(axios.get).toHaveBeenCalledWith(REMOTE_URL));
    });

    it('displays an error message when unable to connect to any service', async () => {
        axios.get.mockRejectedValue(new Error('Connection failed'));  // Simulate connection failure for all attempts

        render(
            <MemoryRouter initialEntries={[`/confirm/${token}`]}>
                <Routes>
                    <Route path="confirm/:token" element={<Confirm />} />
                </Routes>
            </MemoryRouter>
        );

        await waitFor(() => expect(screen.getByText('Failed to connect to any service.')).toBeInTheDocument());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });
});
