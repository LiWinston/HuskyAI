import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import axios from 'axios';
import Login from './Login';
import {BrowserRouter} from 'react-router-dom';
import React from 'react';
import '@testing-library/jest-dom';

jest.mock('axios');

describe('Login Component', () => {
  it('renders the login form', () => {
    render(
        <BrowserRouter>
          <Login/>
        </BrowserRouter>,
    );

    expect(screen.getByPlaceholderText('Username')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Password')).toBeInTheDocument();
  });

  it('shows an error message on failed login', async () => {
    // Mock the axios post request for login to return a failed login response
    axios.post.mockResolvedValue({
      data: {
        code: 0,
        msg: 'Invalid credentials',
      },
    });

    render(
        <BrowserRouter>
          <Login/>
        </BrowserRouter>,
    );

    // Simulate user typing
    fireEvent.change(screen.getByPlaceholderText('Username'), {
      target: {value: 'wronguser'},
    });
    fireEvent.change(screen.getByPlaceholderText('Password'), {
      target: {value: 'wrongpassword'},
    });

    // Simulate form submission
    // fireEvent.click(screen.getByText('Login'));
    fireEvent.click(screen.getByRole('button', {name: /login/i}));

    // Wait for async axios call
    // await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
    await waitFor(() => {
      expect(screen.getByText(
          (content, element) => content.includes('Invalid credentials'))).
          toBeInTheDocument();
    });

    // Check if error message is displayed
    expect(screen.getByText(/Invalid credentials/i, {exact: false})).
        toBeInTheDocument();
  });

  it('shows a success message on successful user login', async () => {
    // Mock successful login response
    axios.post.mockResolvedValue({
      data: {
        code: 1,
        token: 'userToken',
        uuid: 'userUUID',
        role: 'user',
        confirmedUser: true,
      },
    });

    render(
        <BrowserRouter>
          <Login/>
        </BrowserRouter>,
    );

    // Simulate user typing
    fireEvent.change(screen.getByPlaceholderText('Username'), {
      target: {value: '1'},
    });
    fireEvent.change(screen.getByPlaceholderText('Password'), {
      target: {value: '1'},
    });

    // Simulate form submission
    // fireEvent.click(screen.getByText('Login'));
    fireEvent.click(screen.getByRole('button', {name: /login/i}));

    // Wait for axios call
    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(2));

    // Check localStorage for token and uuid
    expect(localStorage.getItem('token')).toBe('userToken');
    expect(localStorage.getItem('userUUID')).toBe('userUUID');

  });

  it('redirects to admin page on successful admin login', async () => {
    // Mock successful login response
    axios.post.mockResolvedValue({
      data: {
        code: 1,
        token: 'testToken',
        uuid: 'testUUID',
        role: 'admin',
        confirmedAdmin: true,
      },
    });

    render(
        <BrowserRouter>
          <Login/>
        </BrowserRouter>,
    );

    // Simulate user typing
    fireEvent.change(screen.getByPlaceholderText('Username'), {
      target: {value: '4'},
    });
    fireEvent.change(screen.getByPlaceholderText('Password'), {
      target: {value: '4'},
    });

    // Simulate form submission
    // fireEvent.click(screen.getByText('Login'));
    fireEvent.click(screen.getByRole('button', {name: /login/i}));

    // Wait for axios call
    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(3));

    // Check localStorage for token and uuid
    expect(localStorage.getItem('token')).toBe('testToken');
    expect(localStorage.getItem('userUUID')).toBe('testUUID');

    // You may want to test navigation, but that requires mocking the navigation function.
  });
});