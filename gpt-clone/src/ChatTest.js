import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import axios from 'axios';
import Chat from './Chat';

jest.mock('axios');

describe('Chat Component', () => {
    beforeEach(() => {
        axios.get.mockResolvedValueOnce({
            data: {
                data: [{conversationId: '123', firstMessage: 'Hello', createdAt: '2021-01-01T12:00:00Z', lastMessageAt: '2021-01-01T12:00:00Z'}]
            }
        });
        axios.post.mockResolvedValueOnce({
            data: {
                code: 1,
                data: ['Model1', 'Model2']
            }
        });
    });

    it('fetches initial data and renders conversation list and model selector on mount', async () => {
        render(<Chat />);

        await waitFor(() => {
            expect(screen.getByText('Current Model: Model1')).toBeInTheDocument();
            expect(screen.getByText('Hello')).toBeInTheDocument();
        });
    });

    it('handles input changes and sends a message', async () => {
        render(<Chat />);
        const input = screen.getByPlaceholderText('Type your message...');

        fireEvent.change(input, { target: { value: 'New message' } });
        expect(input.value).toBe('New message');

        fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });

        await waitFor(() => {
            expect(axios.post).toHaveBeenCalledWith(expect.any(String), expect.objectContaining({
                prompt: 'New message'
            }));
        });
    });

    it('displays notifications when a new message is sent', async () => {
        axios.post.mockResolvedValueOnce({
            data: { data: 'Response from model', msg: 'Message sent successfully' }
        });

        render(<Chat />);
        const input = screen.getByPlaceholderText('Type your message...');
        fireEvent.change(input, { target: { value: 'Test message' } });
        fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });

        await waitFor(() => {
            expect(screen.getByText('Message sent successfully')).toBeInTheDocument();
        });
    });

    it('toggles streaming mode', () => {
        render(<Chat />);
        const toggle = screen.getByText('Stream off').closest('input');
        fireEvent.click(toggle);
        expect(toggle.checked).toBeTruthy();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });
});
