console.log("App Initialized");

// Base URL for API calls
const getApiBaseUrl = () => {
    const hostname = window.location.hostname;
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return "http://localhost:8080/api";
    }
    // Hardcoded production URL as fallback if not otherwise determined
    return "https://doctor-appointment-system-yhsg.onrender.com/api";
};

window.API_BASE_URL = getApiBaseUrl();
const API_BASE_URL = window.API_BASE_URL;

// Fetch with JWT Support and Timeout
async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('token');
    const timeout = options.timeout || 60000; // Increased to 60s for cold starts

    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeout);

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

    try {
        const response = await fetch(`${API_BASE_URL}${url}`, {
            ...options,
            signal: controller.signal
        });
        clearTimeout(id);
        
        if (response.status === 401) {
            localStorage.clear();
            window.location.href = 'login.html';
        }
        return response;
    } catch (error) {
        clearTimeout(id);
        if (error.name === 'AbortError') {
            throw new Error('Connection timed out. Please ensure the backend server is running (usually on port 8080 locally) or wait for the cloud server to spin up.');
        }
        throw error;
    }
}

// Attach fetchWithAuth to window
window.fetchWithAuth = fetchWithAuth;

// Mobile Menu Toggle logic (helper for all pages)
function initMobileMenu() {
    const mobileMenu = document.getElementById('mobile-menu');
    const navLinks = document.getElementById('nav-links');

    if (mobileMenu && navLinks) {
        mobileMenu.addEventListener('click', () => {
            navLinks.classList.toggle('active');
        });

        // Close menu on link click
        navLinks.querySelectorAll('a, button').forEach(item => {
            item.addEventListener('click', () => {
                navLinks.classList.remove('active');
            });
        });
    }
}

// Wake up the server on load
async function wakeUpServer() {
    try {
        console.log("Waking up server...");
        await fetch(`${API_BASE_URL}/auth/ping`);
        console.log("Server is awake.");
    } catch (err) {
        console.warn("Wake up call failed. Server might still be spinning up.", err);
    }
}

// Auto-init on DOM content load
document.addEventListener('DOMContentLoaded', () => {
    initMobileMenu();
    wakeUpServer();
});
