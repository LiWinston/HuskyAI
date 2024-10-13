import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import axios from 'axios';
import Swal from "sweetalert2";
import UserManagement from './UserManagement';

jest.mock('axios');
jest.mock("sweetalert2", () => ({
    fire: jest.fn(() => Promise.resolve({ isConfirmed: true }))
}));

describe('UserManagement Component', () => {
    beforeEach(() => {
        axios.get.mockImplementation(url => {
            if (url.includes('/admin/user')) {
                return Promise.resolve({
                    data: {
                        data: [{ uuid: '1', username: 'JohnDoe', role: 'User' }]
                    }
                });
            } else if (url.includes('/admin/user/modelAccess')) {
                return Promise.resolve({
                    data: {
                        data: [
                            { userId: '1', allowedModels: [{ model: 'Model1', accessLevel: 'No Restriction' }] }
                        ]
                    }
                });
            }
            return Promise.reject(new Error('Not found'));
        });
    });

    it('renders and displays user information', async () => {
        render(<UserManagement />);

        await waitFor(() => {
            expect(screen.getByText('JohnDoe (User)')).toBeInTheDocument();
        });
    });

    it('expands user details on click', async () => {
        render(<UserManagement />);
        await waitFor(() => {
            fireEvent.click(screen.getByText('JohnDoe (User)'));
        });

        expect(screen.getByText('Allowed Models:')).toBeInTheDocument();
    });

    it('handles model access form interactions', async () => {
        render(<UserManagement />);
        await waitFor(() => {
            fireEvent.click(screen.getByText('JohnDoe (User)'));
        });

        const saveButton = screen.getByText('Save Model Access');
        fireEvent.click(saveButton);

        // Expect the SweetAlert to be called after a successful save
        await waitFor(() => {
            expect(Swal.fire).toHaveBeenCalledWith({
                icon: 'success',
                title: 'Success',
                text: expect.any(String),
                showCancelButton: true,
                confirmButtonText: 'Yes, refresh!',
                cancelButtonText: 'No, stay here',
            });
        });
    });

    it('sends updated data to the server', async () => {
        render(<UserManagement />);
        await waitFor(() => {
            fireEvent.click(screen.getByText('JohnDoe (User)'));
        });

        const input = screen.getByPlaceholderText('New Key');
        fireEvent.change(input, { target: { value: 'NewKey' } });
        const addButton = screen.getByText('Add');
        fireEvent.click(addButton);

        await waitFor(() => {
            expect(axios.put).toHaveBeenCalled();
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });
});
