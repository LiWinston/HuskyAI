import React from 'react';
import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import '@testing-library/jest-dom';
import axios from 'axios';
import ModelManagement from './ModelManagement';
import {BrowserRouter} from 'react-router-dom';

jest.mock('axios');

describe('ModelManagement Component', () => {
    beforeEach(() => {
        axios.get.mockResolvedValue({
            data: {
                data: {
                    modelServices: [
                        {url: 'http://example.com/api', name: 'TestModel', apiKey: 'abc123', allowedModels: ['model1', 'model2']}
                    ]
                }
            }
        });
    });

    it('fetches and displays models initially', async () => {
        render(
            <BrowserRouter>
                <ModelManagement />
            </BrowserRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('TestModel | http://example.com/api')).toBeInTheDocument();
        });
    });

    it('registers a new model', async () => {
        axios.post.mockResolvedValue({
            data: {
                success: true
            }
        });

        render(
            <BrowserRouter>
                <ModelManagement />
            </BrowserRouter>
        );

        fireEvent.change(screen.getByPlaceholderText('Model URL'), {target: {value: 'http://newmodel.com/api'}});
        fireEvent.change(screen.getByPlaceholderText('Model Name'), {target: {value: 'NewModel'}});
        fireEvent.change(screen.getByPlaceholderText('API Key (optional)'), {target: {value: 'newapikey'}});
        fireEvent.change(screen.getByPlaceholderText('Allowed Models'), {target: {value: 'model1,model2'}});
        fireEvent.click(screen.getByText('Register Model'));

        await waitFor(() => {
            expect(axios.post).toHaveBeenCalledWith(expect.any(String), {
                url: 'http://newmodel.com/api',
                name: 'NewModel',
                apiKey: 'newapikey',
                allowedModels: ['model1', 'model2']
            });
            expect(screen.getByText('Model registered successfully.')).toBeInTheDocument();
        });
    });

    it('refreshes models', async () => {
        axios.post.mockResolvedValue({
            data: {
                data: {
                    add: 1,
                    remove: 1
                },
                code: 1,
                msg: "Models refreshed."
            }
        });

        render(
            <BrowserRouter>
                <ModelManagement />
            </BrowserRouter>
        );

        fireEvent.click(screen.getByText('Refresh Models'));
        await waitFor(() => {
            expect(screen.getByText('Added 1 models, removed 1 models. Refreshed Log: Models refreshed.')).toBeInTheDocument();
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });
});
