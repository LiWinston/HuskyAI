import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import AdminDashboard from './AdminDashboard';

jest.mock('./ModelManagement', () => {
    return {
        __esModule: true,
        default: () => <div>ModelManagement Component</div>,
    };
});

jest.mock('./UserManagement', () => {
    return {
        __esModule: true,
        default: () => <div>UserManagement Component</div>,
    };
});

jest.mock('./ConversationHistory', () => {
    return {
        __esModule: true,
        default: () => <div>ConversationHistory Component</div>,
    };
});

describe('AdminDashboard Component', () => {
    it('renders correctly with all sections', () => {
        render(<AdminDashboard />);

        expect(screen.getByText('Admin Dashboard')).toBeInTheDocument();
        expect(screen.getByText('Model Management')).toBeInTheDocument();
        expect(screen.getByText('User Management')).toBeInTheDocument();
        expect(screen.getByText('Conversation History')).toBeInTheDocument();

        expect(screen.getByText('ModelManagement Component')).toBeInTheDocument();
        expect(screen.getByText('UserManagement Component')).toBeInTheDocument();
        expect(screen.getByText('ConversationHistory Component')).toBeInTheDocument();
    });
});
