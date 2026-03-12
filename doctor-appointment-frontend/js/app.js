console.log("App Initialized");

// Base URL for API calls
const API_BASE_URL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? "http://localhost:8080/api"
    : "https://doctor-appointment-system-yhsg.onrender.com/api";

// Fetch with JWT Support
async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('token');
    
    // Initialize headers
    if (!options.headers) {
        options.headers = {};
    }

    // Add Authorization if token exists
    if (token) {
        if (options.headers instanceof Headers) {
            options.headers.set('Authorization', `Bearer ${token}`);
        } else {
            options.headers['Authorization'] = `Bearer ${token}`;
        }
    }

    // DO NOT set Content-Type if it's FormData (browser will set it with boundary)
    if (!(options.body instanceof FormData)) {
        if (options.headers instanceof Headers) {
            if (!options.headers.get('Content-Type')) {
                options.headers.set('Content-Type', 'application/json');
            }
        } else {
            if (!options.headers['Content-Type']) {
                options.headers['Content-Type'] = 'application/json';
            }
        }
    }

    const response = await fetch(`${API_BASE_URL}${url}`, options);
    if (response.status === 401) {
        window.location.href = 'login.html';
    }
    return response;
}
