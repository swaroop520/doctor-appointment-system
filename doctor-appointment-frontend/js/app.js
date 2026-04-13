console.log("App Initialized");

// Configuration - Change this if you want to force production
const CLOUD_API_URL = "https://doctor-appointment-system-yhsg.onrender.com/api";
const LOCAL_API_URL = "http://localhost:8080/api";

// Base URL for API calls - Initial Guess
const getApiBaseUrl = () => {
    const urlParams = new URLSearchParams(window.location.search);
    const manualApi = urlParams.get('api');
    if (manualApi) return manualApi;
    if (urlParams.get('env') === 'prod') return CLOUD_API_URL;

    const hostname = window.location.hostname;
    const protocol = window.location.protocol;

    const isLocal = hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '' || protocol === 'file:';
    
    // If we've already detected that localhost is down in this session, use cloud
    if (isLocal && sessionStorage.getItem('backend_env') === 'cloud') {
        return CLOUD_API_URL;
    }

    return isLocal ? LOCAL_API_URL : CLOUD_API_URL;
};

window.API_BASE_URL = getApiBaseUrl();
let API_BASE_URL = window.API_BASE_URL;

// Smart Backend Detection
async function autoDetectBackend() {
    const hostname = window.location.hostname;
    const protocol = window.location.protocol;
    const isLocal = hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '' || protocol === 'file:';

    if (isLocal && API_BASE_URL === LOCAL_API_URL) {
        console.log("🔍 Probing local backend...");
        try {
            const controller = new AbortController();
            const id = setTimeout(() => controller.abort(), 2000); // 2s quick probe
            
            const resp = await fetch(`${LOCAL_API_URL}/auth/ping`, { signal: controller.signal });
            clearTimeout(id);
            
            if (resp.ok) {
                console.log("✅ Local backend detected.");
                sessionStorage.setItem('backend_env', 'local');
            } else {
                throw new Error("Not OK");
            }
        } catch (err) {
            console.warn("⚠️ Local backend unreachable. Switching to Cloud API.");
            window.API_BASE_URL = CLOUD_API_URL;
            API_BASE_URL = CLOUD_API_URL;
            sessionStorage.setItem('backend_env', 'cloud');
            // Re-run wakeup call for cloud
            wakeUpServer();
        }
    }
}

console.log("🚀 Initial API Base URL:", API_BASE_URL);

// Connection Tester for Troubleshooting
async function testConnection() {
    console.log("Testing connection to:", window.API_BASE_URL);
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 10000); // 10s limit for diagnostics

    try {
        const start = Date.now();
        const response = await fetch(`${window.API_BASE_URL}/auth/ping`, { 
            mode: 'cors',
            signal: controller.signal 
        });
        clearTimeout(timeoutId);
        const duration = Date.now() - start;
        
        if (response.ok) {
            return { ok: true, message: `Connected successfully to ${window.API_BASE_URL} in ${duration}ms!` };
        } else {
            return { ok: false, message: `Server reached but returned status ${response.status}.` };
        }
    } catch (err) {
        clearTimeout(timeoutId);
        console.error("Connection Test Failed:", err);
        const isTimeout = err.name === 'AbortError';
        return { 
            ok: false, 
            message: isTimeout 
                ? `Connection timed out after 10s. The server might be sleeping or unreachable.`
                : `Failed to connect to ${window.API_BASE_URL}. <br>Please ensure the backend is running.` 
        };
    }
}
window.testConnection = testConnection;

// Fetch with JWT Support and Timeout
async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('token');
    const timeout = options.timeout || 60000; 

    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeout);

    if (!options.headers) options.headers = {};

    if (token) {
        if (options.headers instanceof Headers) {
            options.headers.set('Authorization', `Bearer ${token}`);
        } else {
            options.headers['Authorization'] = `Bearer ${token}`;
        }
    }

    if (!(options.body instanceof FormData)) {
        if (options.headers instanceof Headers) {
            if (!options.headers.get('Content-Type')) options.headers.set('Content-Type', 'application/json');
        } else {
            if (!options.headers['Content-Type']) options.headers['Content-Type'] = 'application/json';
        }
    }

    try {
        const response = await fetch(`${window.API_BASE_URL}${url}`, {
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
            const msg = window.API_BASE_URL.includes('render.com') 
                ? 'The cloud server is taking too long to wake up. Please wait 30 seconds and try again.'
                : 'Connection timed out. Please ensure your local backend is running on port 8080.';
            throw new Error(msg);
        }
        throw error;
    }
}

window.fetchWithAuth = fetchWithAuth;

function initMobileMenu() {
    const mobileMenu = document.getElementById('mobile-menu');
    const navLinks = document.getElementById('nav-links');

    if (mobileMenu && navLinks) {
        mobileMenu.addEventListener('click', () => {
            navLinks.classList.toggle('active');
        });
        navLinks.querySelectorAll('a, button').forEach(item => {
            item.addEventListener('click', () => navLinks.classList.remove('active'));
        });
    }
}

async function wakeUpServer() {
    try {
        console.log("Waking up server:", window.API_BASE_URL);
        await fetch(`${window.API_BASE_URL}/auth/ping`);
    } catch (err) {}
}

document.addEventListener('DOMContentLoaded', () => {
    initMobileMenu();
    autoDetectBackend().then(() => {
        wakeUpServer();
    });
});

