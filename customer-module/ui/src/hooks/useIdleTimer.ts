import { useEffect, useRef, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';

const SESSION_TIMEOUT = 5 * 60 * 1000; // 5 minutes in milliseconds
const WARNING_TIME = 1 * 60 * 1000; // 1 minute warning

export const useIdleTimer = () => {
  const { logout, isAuthenticated } = useAuth();
  const timeoutRef = useRef<number | null>(null);
  const warningRef = useRef<number | null>(null);
  const lastActivityRef = useRef<number>(Date.now());

  const showWarning = useCallback(() => {
    const shouldLogout = window.confirm(
      'Your session will expire in 1 minute due to inactivity. Do you want to continue your session?'
    );
    
    if (shouldLogout) {
      resetTimer();
    } else {
      logout();
    }
  }, [logout]);

  const resetTimer = useCallback(() => {
    lastActivityRef.current = Date.now();
    
    // Clear existing timers
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    if (warningRef.current) {
      clearTimeout(warningRef.current);
    }

    if (isAuthenticated) {
      // Set warning timer
      warningRef.current = window.setTimeout(() => {
        showWarning();
      }, SESSION_TIMEOUT - WARNING_TIME);

      // Set logout timer
      timeoutRef.current = window.setTimeout(() => {
        logout();
      }, SESSION_TIMEOUT);
    }
  }, [isAuthenticated, logout, showWarning]);

  const handleActivity = useCallback(() => {
    if (isAuthenticated) {
      resetTimer();
    }
  }, [isAuthenticated, resetTimer]);

  useEffect(() => {
    if (isAuthenticated) {
      // Events to track user activity
      const events = [
        'mousedown',
        'mousemove',
        'keypress',
        'scroll',
        'touchstart',
        'click',
      ];

      // Add event listeners
      events.forEach((event) => {
        document.addEventListener(event, handleActivity, true);
      });

      // Start the timer
      resetTimer();

      // Cleanup function
      return () => {
        events.forEach((event) => {
          document.removeEventListener(event, handleActivity, true);
        });
        
        if (timeoutRef.current) {
          clearTimeout(timeoutRef.current);
        }
        if (warningRef.current) {
          clearTimeout(warningRef.current);
        }
      };
    }
  }, [isAuthenticated, handleActivity, resetTimer]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      if (warningRef.current) {
        clearTimeout(warningRef.current);
      }
    };
  }, []);

  return {
    resetTimer,
    lastActivity: lastActivityRef.current,
  };
};
