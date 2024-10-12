import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import axios from 'axios';
import ConversationHistory from './ConversationHistory';

jest.mock('axios');

describe('ConversationHistory Component', () => {
    const userId = 'user123';
    const mockConversations = [
        { id: 'conv1', title: 'Conversation 1' },
        { id: 'conv2', title: 'Conversation 2' }
    ];
    const mockMessages = [
        { role: 'user', content: 'Hello!', timestamp: '2020-01-01T00:00:00Z' },
        { role: 'admin', content: 'Hi, how can I help?', timestamp: '2020-01-01T00:01:00Z' }
    ];

    beforeEach(() => {
        axios.get.mockImplementation((url) => {
            if (url === `/chat/${userId}`) {
                return Promise.resolve({ data: mockConversations });
            } else if (url.includes('/chat/')) {
                return Promise.resolve({ data: mockMessages });
            }
            throw new Error('not found');
        });
    });

    it('fetches and displays conversations initially', async () => {
        render(<ConversationHistory userId={userId} />);

        await waitFor(() => {
            expect(screen.getByText('Conversation 1')).toBeInTheDocument();
            expect(screen.getByText('Conversation 2')).toBeInTheDocument();
        });
    });

    it('loads messages when a conversation is clicked', async () => {
        render(<ConversationHistory userId={userId} />);

        await waitFor(() => {
            fireEvent.click(screen.getByText('Conversation 1'));
        });

        await waitFor(() => {
            expect(screen.getByText('Hello!')).toBeInTheDocument();
            expect(screen.getByText('Hi, how can I help?')).toBeInTheDocument();
        });
    });

    it('handles errors during conversation fetching', async () => {
        axios.get.mockRejectedValueOnce(new Error('Failed to fetch conversations'));
        console.error = jest.fn(); // To avoid polluting logs during the test

        render(<ConversationHistory userId={userId} />);

        await waitFor(() => {
            expect(console.error).toHaveBeenCalledWith('Error fetching conversations:', expect.any(Error));
        });
    });

    it('handles errors during message loading', async () => {
        axios.get.mockImplementation((url) => {
            if (url === `/chat/${userId}`) {
                return Promise.resolve({ data: mockConversations });
            }
            return Promise.reject(new Error('Failed to load messages'));
        });
        console.error = jest.fn(); // To keep the test logs clean

        render(<ConversationHistory userId={userId} />);

        await waitFor(() => {
            fireEvent.click(screen.getByText('Conversation 1'));
        });

        await waitFor(() => {
            expect(console.error).toHaveBeenCalledWith('Error loading conversation:', expect.any(Error));
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });
});
