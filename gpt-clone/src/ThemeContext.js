import React, { createContext, useState, useEffect } from 'react';

export const ThemeContext = createContext();

export const ThemeProvider = ({ children }) => {
    const [theme, setTheme] = useState(() => {
        const savedTheme = localStorage.getItem('theme');
        return savedTheme || 'auto';  // 默认为自动模式
    });

    const getCurrentPosition = () => {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error('Geolocation is not supported'));
                return;
            }
            
            navigator.geolocation.getCurrentPosition(
                position => resolve(position),
                error => reject(error),
                { timeout: 5000 }
            );
        });
    };

    const checkDaylight = async () => {
        try {
            // 获取位置信息
            const position = await getCurrentPosition();
            const { latitude, longitude } = position.coords;

            // 调用日出日落API
            const response = await fetch(
                `https://api.sunrise-sunset.org/json?lat=${latitude}&lng=${longitude}&formatted=0`
            );
            const data = await response.json();

            if (data.status === 'OK') {
                const now = new Date();
                const sunrise = new Date(data.results.sunrise);
                const sunset = new Date(data.results.sunset);

                // 判断当前是否在日出日落之间
                return now > sunrise && now < sunset ? 'light' : 'dark';
            }
        } catch (error) {
            console.warn('Failed to determine daylight status:', error);
            // 如果获取失败，根据时间粗略判断（6点到18点为白天）
            const hour = new Date().getHours();
            return hour >= 6 && hour < 18 ? 'light' : 'dark';
        }
    };

    useEffect(() => {
        const updateTheme = async () => {
            if (theme === 'auto') {
                const preferredTheme = await checkDaylight();
                document.documentElement.setAttribute('data-theme', preferredTheme);
                localStorage.setItem('actualTheme', preferredTheme);
            } else {
                document.documentElement.setAttribute('data-theme', theme);
                localStorage.setItem('actualTheme', theme);
            }
        };

        updateTheme();

        // 如果是自动模式，设置定时检查
        let interval;
        if (theme === 'auto') {
            // 每30分钟检查一次
            interval = setInterval(updateTheme, 30 * 60 * 1000);
        }

        return () => {
            if (interval) {
                clearInterval(interval);
            }
        };
    }, [theme]);

    return (
        <ThemeContext.Provider value={{ theme, setTheme }}>
            {children}
        </ThemeContext.Provider>
    );
}; 