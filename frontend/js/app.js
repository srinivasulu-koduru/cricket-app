/**
 * Cricket App - Portal & Common App Logic
 */

const BACKEND_HEALTH_URL = 'http://localhost:8080/api/health';

document.addEventListener('DOMContentLoaded', () => {
    initHealthCheck();
    initMobileNav();
});

function initHealthCheck() {
    const connectionPill = document.getElementById('connection-pill');
    const connectionText = document.getElementById('connection-text');
    const statusMessage = document.getElementById('status-message');
    const lastCheckedTime = document.getElementById('last-checked-time');
    const btnRecheck = document.getElementById('btn-recheck');

    if (!connectionPill) return;

    async function checkBackendHealth() {
        connectionPill.className = 'status-pill status-checking';
        if (connectionText) connectionText.textContent = 'Connecting...';
        if (statusMessage) statusMessage.textContent = 'Sending GET request to /api/health...';
        
        const now = new Date();
        if (lastCheckedTime) lastCheckedTime.textContent = now.toLocaleTimeString();

        try {
            const response = await fetch(BACKEND_HEALTH_URL, {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });

            if (response.ok) {
                const data = await response.json();
                connectionPill.className = 'status-pill status-connected';
                if (connectionText) connectionText.textContent = 'Backend Online';
                if (statusMessage) statusMessage.textContent = `Status: ${data.status} - "${data.message}"`;
            } else {
                throw new Error(`HTTP Error ${response.status}`);
            }
        } catch (error) {
            console.warn('Backend connection failed:', error.message);
            connectionPill.className = 'status-pill status-disconnected';
            if (connectionText) connectionText.textContent = 'Backend Offline';
            if (statusMessage) statusMessage.textContent = `Unable to reach Spring Boot server at ${BACKEND_HEALTH_URL} (${error.message}).`;
        }
    }

    checkBackendHealth();

    if (btnRecheck) {
        btnRecheck.addEventListener('click', () => {
            checkBackendHealth();
            if (typeof ApiService !== 'undefined') {
                ApiService.showToast('Re-checking backend connectivity...', 'info');
            }
        });
    }
}

function initMobileNav() {
    const btnToggle = document.getElementById('mobile-menu-toggle');
    const drawer = document.getElementById('mobile-nav-drawer');
    const btnClose = document.getElementById('mobile-drawer-close');

    if (!btnToggle || !drawer) return;

    btnToggle.addEventListener('click', () => {
        drawer.classList.add('open');
    });

    if (btnClose) {
        btnClose.addEventListener('click', () => {
            drawer.classList.remove('open');
        });
    }

    // Close when clicking outside
    document.addEventListener('click', (e) => {
        if (drawer.classList.contains('open') && !drawer.contains(e.target) && !btnToggle.contains(e.target)) {
            drawer.classList.remove('open');
        }
    });
}
