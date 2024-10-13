import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import axios from 'axios';
import ConversationItem from './ConversationItem';

jest.mock('axios');

describe('ConversationItem Component', () => {
    const conversation = {
        id: '1',
        title: 'Test Conversation'
    };

    const mockLoadConversation = jest.fn();
    const mockFetchConversations = jest.fn();
    const mockSetMessages = jest.fn();
    const mockSetSelectedConversation = jest.fn();
    const mockSetNotification = jest.fn();
    const mockOpenShareModal = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders conversation title and reacts to click', async () => {
        render(<ConversationItem
            conversation={conversation}
            conversations={[conversation]}
            messages={[]}
            loadConversation={mockLoadConversation}
            fetchConversations={mockFetchConversations}
            selectedConversation={null}
            setSelectedConversation={mockSetSelectedConversation}
            setMessages={mockSetMessages}
            setNotification={mockSetNotification}
            openShareModal={mockOpenShareModal}
            setShareMessages={() => {}}
            setSharedCid={() => {}}
        />);

        fireEvent.click(screen.getByText('Test Conversation'));
        expect(mockSetSelectedConversation).toHaveBeenCalledWith(conversation.id);
        expect(mockLoadConversation).toHaveBeenCalledWith(conversation.id);
    });

    it('shows and hides options menu', async () => {
        render(<ConversationItem
            conversation={conversation}
            conversations={[conversation]}
            messages={[]}
            loadConversation={mockLoadConversation}
            fetchConversations={mockFetchConversations}
            selectedConversation={null}
            setSelectedConversation={mockSetSelectedConversation}
            setMessages={mockSetMessages}
            setNotification={mockSetNotification}
            openShareModal={mockOpenShareModal}
            setShareMessages={() => {}}
            setSharedCid={() => {}}
        />);

        // Open options menu
        fireEvent.click(screen.getByRole('button', { name: 'Ellipsis' }));
        expect(screen.getByText('Delete')).toBeInTheDocument();

        // Click outside to close
        fireEvent.mouseDown(document);
        await waitFor(() => {
            expect(screen.queryByText('Delete')).not.toBeInTheDocument();
        });
    });

    it('deletes a conversation', async () => {
        axios.delete.mockResolvedValueOnce({});
        mockFetchConversations.mockResolvedValueOnce([]);

        render(<ConversationItem
            conversation={conversation}
            conversations={[conversation]}
            messages={[]}
            loadConversation={mockLoadConversation}
            fetchConversations={mockFetchConversations}
            selectedConversation={conversation.id}
            setSelectedConversation={mockSetSelectedConversation}
            setMessages={mockSetMessages}
            setNotification={mockSetNotification}
            openShareModal={mockOpenShareModal}
            setShareMessages={() => {}}
            setSharedCid={() => {}}
        />);

        fireEvent.click(screen.getByText('Delete'));
        await waitFor(() => {
            expect(axios.delete).toHaveBeenCalled();
            expect(mockFetchConversations).toHaveBeenCalled();
            expect(mockSetNotification).toHaveBeenCalledWith('Conversation deleted successfully');
        });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });
});
