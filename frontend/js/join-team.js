/**
 * Cricket App - Shareable Join Team JavaScript
 */

let joinToken = null;

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    joinToken = urlParams.get('token');

    if (!joinToken) {
        showAlert('join-alert', 'Invalid or missing shareable team join link.');
        return;
    }

    loadTeamPreview(joinToken);
    initJoinButton();
});

async function loadTeamPreview(token) {
    hideAlert('join-alert');

    try {
        const team = await ApiService.get(`/teams/join/${token}`, { skipAuth: true });
        renderPreview(team);
    } catch (err) {
        showAlert('join-alert', err.message || 'Invalid or expired shareable team join link.');
        const btn = document.getElementById('btn-join-team');
        if (btn) btn.style.display = 'none';
    }
}

function renderPreview(team) {
    if (!team) return;

    if (document.getElementById('preview-team-id')) document.getElementById('preview-team-id').textContent = team.teamId || 'TEAM-------';
    if (document.getElementById('preview-team-name')) document.getElementById('preview-team-name').textContent = team.name || 'Cricket Team';
    if (document.getElementById('preview-team-desc')) document.getElementById('preview-team-desc').textContent = team.description || 'No team description provided.';
    if (document.getElementById('preview-captain')) document.getElementById('preview-captain').textContent = team.createdByName || 'Unknown';
    if (document.getElementById('preview-members')) document.getElementById('preview-members').textContent = team.memberCount || 1;

    const logoImg = document.getElementById('preview-logo-img');
    const logoFallback = document.getElementById('preview-logo-fallback');

    if (team.logoUrl) {
        const fullLogoUrl = ApiService.getImageUrl(team.logoUrl);
        if (logoImg) {
            logoImg.src = fullLogoUrl;
            logoImg.style.display = 'block';
        }
        if (logoFallback) logoFallback.style.display = 'none';
    } else {
        if (logoImg) logoImg.style.display = 'none';
        if (logoFallback) logoFallback.style.display = 'flex';
    }
}

function initJoinButton() {
    const btnJoin = document.getElementById('btn-join-team');
    const notice = document.getElementById('login-required-notice');
    const authToken = ApiService.getToken();

    if (!authToken) {
        if (btnJoin) btnJoin.style.display = 'none';
        if (notice) notice.style.display = 'block';
        return;
    }

    if (btnJoin) {
        btnJoin.addEventListener('click', async () => {
            hideAlert('join-alert');
            btnJoin.disabled = true;
            btnJoin.textContent = 'Joining Team...';

            try {
                const res = await ApiService.post(`/teams/join/${joinToken}`, {});
                ApiService.showToast(res.message || 'Successfully joined team!', 'success');
                setTimeout(() => window.location.href = 'teams.html', 1000);
            } catch (err) {
                showAlert('join-alert', err.message || 'Failed to join team.');
                btnJoin.disabled = false;
                btnJoin.textContent = '⚡ Join Team Now';
            }
        });
    }
}

function showAlert(elementId, message, type = 'error') {
    const el = document.getElementById(elementId);
    if (el) {
        el.className = `alert alert-${type}`;
        el.textContent = message;
        el.style.display = 'block';
    }
    if (typeof ApiService !== 'undefined' && ApiService.showToast) {
        ApiService.showToast(message, type);
    }
}

function hideAlert(elementId) {
    const el = document.getElementById(elementId);
    if (el) el.style.display = 'none';
}
