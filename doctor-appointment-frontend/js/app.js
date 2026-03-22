console.log("App Initialized");

// Base URL for API calls
const getApiBaseUrl = () => {
    // Priority 1: Check for manual override in URL parameter (?api=http://...)
    const urlParams = new URLSearchParams(window.location.search);
    const manualApi = urlParams.get('api');
    if (manualApi) {
        console.log("Environment: Manual Override. Using:", manualApi);
        return manualApi;
    }

    const hostname = window.location.hostname;
    const protocol = window.location.protocol;
    
    // Priority 2: Local development (localhost, 127.0.0.1, or local file)
    if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '' || protocol === 'file:') {
        console.log("Environment: Local. Using localhost:8080");
        return "http://localhost:8080/api";
    }
    
    // Priority 3: Production/Remote fallback
    console.log("Environment: Production/Remote. Using Render URL.");
    return "https://doctor-appointment-system-yhsg.onrender.com/api";
};

window.API_BASE_URL = getApiBaseUrl();
console.log("API Base URL set to:", window.API_BASE_URL);
const API_BASE_URL = window.API_BASE_URL;

// Connection Tester for Troubleshooting
async function testConnection() {
    console.log("Testing connection to:", API_BASE_URL);
    try {
        const start = Date.now();
        const response = await fetch(`${API_BASE_URL}/auth/ping`, { mode: 'cors' });
        const duration = Date.now() - start;
        
        if (response.ok) {
            return { ok: true, message: `Connected successfully in ${duration}ms!` };
        } else {
            return { ok: false, message: `Server reached but returned status ${response.status} (${response.statusText}).` };
        }
    } catch (err) {
        console.error("Connection Test Failed:", err);
        return { 
            ok: false, 
            message: `Failed to connect to ${API_BASE_URL}. Possible reasons: <br>1. Backend is not running.<br>2. CORS blocking (if using a different port).<br>3. Firewall/Network issue.` 
        };
    }
}
window.testConnection = testConnection;

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
