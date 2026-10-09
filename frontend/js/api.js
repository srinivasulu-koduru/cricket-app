/**
 * Cricket App - API Helper
 * Centralized fetch helper for backend REST API requests.
 */

const API_BASE_URL = 'https://cricket-app-production-9e11.up.railway.app/api';
const BACKEND_BASE_URL = 'https://cricket-app-production-9e11.up.railway.app';
const TOKEN_KEY = 'cricketAppToken';

class ApiService {

    /**
     * Decode JWT payload safely without external libraries.
     */
    static parseJwt(token) {
        if (!token || typeof token !== 'string') return null;
        try {
            const parts = token.split('.');
            if (parts.length !== 3) return null;
            let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
            while (base64.length % 4 !== 0) {
                base64 += '=';
            }
            try {
                const jsonPayload = decodeURIComponent(
                    atob(base64)
                        .split('')
                        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                        .join('')
                );
                return JSON.parse(jsonPayload);
            } catch (e) {
                return JSON.parse(atob(base64));
            }
        } catch (e) {
            return null;
        }
    }

    /**
     * Check if a JWT token exists and has not expired.
     * Incorporates a 30-second clock skew tolerance buffer.
     */
    static isTokenValid(token = null) {
        const jwt = token || this.getRawToken();
        if (!jwt) return false;
        const payload = this.parseJwt(jwt);
        if (!payload || typeof payload.exp !== 'number') {
            return false;
        }
        const nowInSeconds = Math.floor(Date.now() / 1000);
        return payload.exp > (nowInSeconds + 30);
    }

    /**
     * Retrieve the raw stored token directly from localStorage.
     */
    static getRawToken() {
        try {
            return localStorage.getItem(TOKEN_KEY);
        } catch (e) {
            return null;
        }
    }

    /**
     * Get valid JWT token. Automatically purges expired or corrupt tokens.
     */
    static getToken() {
        const token = this.getRawToken();
        if (!token) return null;
        if (!this.isTokenValid(token)) {
            console.warn('[ApiService] Stored JWT is expired or malformed. Purging token.');
            this.clearToken();
            return null;
        }
        return token;
    }

    static setToken(token) {
        try {
            localStorage.setItem(TOKEN_KEY, token);
        } catch (e) {
            console.error('[ApiService] Failed to save token to localStorage:', e);
        }
    }

    static clearToken() {
        try {
            localStorage.removeItem(TOKEN_KEY);
        } catch (e) {
            console.error('[ApiService] Failed to clear token from localStorage:', e);
        }
    }

    /**
     * Get decoded user payload from stored token if available.
     */
    static getUserPayload() {
        const token = this.getToken();
        return token ? this.parseJwt(token) : null;
    }

    static getImageUrl(path) {
        if (!path) return null;
        if (path.startsWith('http://') || path.startsWith('https://')) {
            return path;
        }
        return `${BACKEND_BASE_URL}${path}`;
    }

    static showToast(message, type = 'info') {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = `toast-item toast-${type}`;
        
        const icon = type === 'success' ? '⚡' : type === 'error' ? '⚠️' : 'ℹ️';
        toast.innerHTML = `<span style="font-size:1.1rem;">${icon}</span> <span>${message}</span>`;
        
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 3500);
    }

    static handleUnauthorized(endpoint) {
        console.warn(`[ApiService] 401 Unauthorized received for ${endpoint}. Session expired.`);
        this.clearToken();
        const path = window.location.pathname.toLowerCase();
        const isAuthOrPublicPage = path.endsWith('login.html') || 
                                   path.endsWith('register.html') || 
                                   path.endsWith('verify-otp.html') || 
                                   path.endsWith('forgot-password.html') || 
                                   path.endsWith('reset-password.html') || 
                                   path.endsWith('index.html') || 
                                   path === '/' || 
                                   path === '';
        if (!isAuthOrPublicPage) {
            window.location.replace('login.html');
        }
    }

    static async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token && !options.skipAuth) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            method: options.method || 'GET',
            headers,
            ...(options.body ? { body: JSON.stringify(options.body) } : {})
        };

        try {
            const response = await fetch(url, config);
            let data = {};
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            }

            if (!response.ok) {
                const errorMessage = data.message || `Request failed with status ${response.status}`;
                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = data;

                if (response.status === 401 && token && !options.skipAuth) {
                    this.handleUnauthorized(endpoint);
                }

                throw error;
            }

            return data;
        } catch (error) {
            console.error(`API Error [${endpoint}]:`, error.message);
            throw error;
        }
    }

    static get(endpoint, options = {}) {
        return this.request(endpoint, { ...options, method: 'GET' });
    }

    static post(endpoint, body, options = {}) {
        return this.request(endpoint, { ...options, method: 'POST', body });
    }

    static put(endpoint, body, options = {}) {
        return this.request(endpoint, { ...options, method: 'PUT', body });
    }

    static delete(endpoint, options = {}) {
        return this.request(endpoint, { ...options, method: 'DELETE' });
    }

    static async postMultipart(endpoint, formData, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const headers = {
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token && !options.skipAuth) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            method: 'POST',
            headers,
            body: formData
        };

        try {
            const response = await fetch(url, config);
            let data = {};
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            }

            if (!response.ok) {
                const errorMessage = data.message || `Request failed with status ${response.status}`;
                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = data;

                if (response.status === 401 && token && !options.skipAuth) {
                    this.handleUnauthorized(endpoint);
                }

                throw error;
            }

            return data;
        } catch (error) {
            console.error(`API Multipart Error [${endpoint}]:`, error.message);
            throw error;
        }
    }
}
