/**
 * Cricket App - My Teams JavaScript
 */

document.addEventListener('DOMContentLoaded', () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    initLogout();
    loadMyTeams();
    loadInvitations();
    initCreateTeamModal();
    initInvitationsModal();
});

function initLogout() {
    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.addEventListener('click', async () => {
            try {
                await ApiService.post('/auth/logout', {});
            } catch (ignored) {}
            ApiService.clearToken();
            ApiService.showToast('Logged out successfully.', 'info');
            window.location.href = 'login.html';
        });
    }
}

/* 1. Load My Teams */
async function loadMyTeams() {
    const grid = document.getElementById('teams-grid');
    hideAlert('teams-alert');

    try {
        const teams = await ApiService.get('/teams/my');
        renderTeamsGrid(teams);
    } catch (err) {
        showAlert('teams-alert', err.message || 'Failed to load teams.');
        if (grid) grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--color-danger); padding: 2rem;">Failed to load teams.</div>';
    }
}

function renderTeamsGrid(teams) {
    const grid = document.getElementById('teams-grid');
    if (!grid) return;

    if (!teams || teams.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 3rem 1.5rem; background: rgba(17, 28, 56, 0.6); border: 1px dashed var(--border-color); border-radius: var(--radius-xl);">
                <div style="font-size: 3.5rem; margin-bottom: 0.75rem;">🏏</div>
                <h3 style="font-family: var(--font-heading); font-size: 1.4rem; color: #fff; margin-bottom: 0.5rem;">No Cricket Teams Joined Yet</h3>
                <p style="color: var(--text-secondary); max-width: 480px; margin: 0 auto 1.5rem; font-size: 0.95rem;">You are not currently a member of any team. Create your own cricket team or join using an invite code/CRK ID!</p>
                <button class="btn btn-primary" onclick="document.getElementById('btn-open-create-modal').click()">
                    ➕ Create Your First Team
                </button>
            </div>
        `;
        return;
    }

    grid.innerHTML = teams.map(team => {
        const logoUrl = ApiService.getImageUrl(team.logoUrl);
        const roleLabel = team.currentUserRole ? team.currentUserRole : 'PLAYER';
        const isOwner = roleLabel === 'OWNER' || roleLabel === 'CAPTAIN';

        return `
            <div class="team-card">
                <div style="display: flex; gap: 1rem; align-items: center; margin-bottom: 1rem;">
                    <div style="width: 54px; height: 54px; border-radius: var(--radius-md); background: rgba(11, 19, 41, 0.8); border: 1px solid var(--color-pitch-emerald); display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0;">
                        ${logoUrl ? `<img src="${logoUrl}" alt="${escapeHtml(team.name)}" style="width:100%; height:100%; object-fit:cover;">` : `<span style="font-size: 1.75rem;">🛡️</span>`}
                    </div>
                    <div>
                        <div style="display: flex; gap: 0.4rem; align-items: center; margin-bottom: 0.2rem;">
                            <span class="permanent-id-badge small">${escapeHtml(team.teamId)}</span>
                            <span class="role-badge ${isOwner ? 'owner' : ''}">${escapeHtml(roleLabel)}</span>
                        </div>
                        <h3 style="font-family: var(--font-heading); font-size: 1.2rem; font-weight: 700; color: #fff;">${escapeHtml(team.name)}</h3>
                    </div>
                </div>

                <p style="color: var(--text-secondary); font-size: 0.88rem; line-height: 1.5; margin-bottom: 1.25rem; flex: 1;">${escapeHtml(team.description || 'No team description provided.')}</p>

                <div style="display: flex; justify-content: space-between; align-items: center; padding-top: 0.85rem; border-top: 1px solid var(--border-color); font-size: 0.82rem; color: var(--text-muted);">
                    <div style="display: flex; gap: 0.85rem;">
                        <span>👑 <strong>${escapeHtml(team.createdByName)}</strong></span>
                        <span>👥 <strong>${team.memberCount}</strong> Members</span>
                    </div>

                    <a href="team-details.html?id=${team.teamId}" class="btn btn-secondary btn-sm">
                        View Team &rarr;
                    </a>
                </div>
            </div>
        `;
    }).join('');
}

/* 2. Load Pending Invitations */
async function loadInvitations() {
    try {
        const invitations = await ApiService.get('/team-invitations/me');
        const badge = document.getElementById('invitation-count-badge');
        
        if (badge) {
            if (invitations && invitations.length > 0) {
                badge.textContent = invitations.length;
                badge.style.display = 'inline-flex';
            } else {
                badge.style.display = 'none';
            }
        }
        return invitations;
    } catch (err) {
        console.error('Failed to load invitations:', err);
        return [];
    }
}

/* 3. Create Team Modal */
function initCreateTeamModal() {
    const modal = document.getElementById('create-team-modal');
    const btnOpen = document.getElementById('btn-open-create-modal');
    const btnClose = document.getElementById('btn-close-create-modal');
    const btnCancel = document.getElementById('btn-cancel-create-modal');
    const form = document.getElementById('create-team-form');

    if (!modal) return;

    if (btnOpen) {
        btnOpen.addEventListener('click', () => {
            if (form) form.reset();
            hideAlert('create-modal-alert');
            modal.style.display = 'flex';
        });
    }

    const closeModal = () => modal.style.display = 'none';
    if (btnClose) btnClose.addEventListener('click', closeModal);
    if (btnCancel) btnCancel.addEventListener('click', closeModal);

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            hideAlert('create-modal-alert');

            const name = document.getElementById('create-team-name').value.trim();
            const description = document.getElementById('create-team-desc').value.trim();

            const btnSubmit = document.getElementById('btn-submit-create-team');
            btnSubmit.disabled = true;
            btnSubmit.textContent = 'Creating...';

            try {
                const newTeam = await ApiService.post('/teams', { name, description });
                closeModal();
                ApiService.showToast(`Team "${newTeam.name}" created successfully!`, 'success');
                loadMyTeams();
            } catch (err) {
                showAlert('create-modal-alert', err.message || 'Failed to create team.');
            } finally {
                btnSubmit.disabled = false;
                btnSubmit.textContent = 'Create Team';
            }
        });
    }
}

/* 4. Invitations Modal */
function initInvitationsModal() {
    const modal = document.getElementById('invitations-modal');
    const btnOpen = document.getElementById('btn-open-invitations');
    const btnClose = document.getElementById('btn-close-invitations-modal');
    const btnDone = document.getElementById('btn-done-invitations-modal');

    if (!modal) return;

    const closeModal = () => modal.style.display = 'none';
    if (btnClose) btnClose.addEventListener('click', closeModal);
    if (btnDone) btnDone.addEventListener('click', closeModal);

    if (btnOpen) {
        btnOpen.addEventListener('click', async () => {
            hideAlert('invitations-modal-alert');
            modal.style.display = 'flex';
            await renderInvitationsList();
        });
    }
}

async function renderInvitationsList() {
    const listContainer = document.getElementById('invitations-list');
    if (!listContainer) return;

    listContainer.innerHTML = '<div class="loading-box">Loading pending invitations...</div>';
    const invitations = await loadInvitations();

    if (!invitations || invitations.length === 0) {
        listContainer.innerHTML = '<div style="text-align:center; color:var(--text-muted); padding:1.5rem;">No pending team invitations.</div>';
        return;
    }

    listContainer.innerHTML = invitations.map(inv => {
        const logoUrl = ApiService.getImageUrl(inv.teamLogoUrl);

        return `
            <div style="background: rgba(11, 19, 41, 0.8); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 1rem; display: flex; justify-content: space-between; align-items: center; gap: 1rem;">
                <div style="display: flex; gap: 0.85rem; align-items: center;">
                    <div style="width: 44px; height: 44px; border-radius: var(--radius-md); background: rgba(22, 35, 71, 0.9); border: 1px solid var(--color-pitch-emerald); display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0;">
                        ${logoUrl ? `<img src="${logoUrl}" alt="${escapeHtml(inv.teamName)}" style="width:100%; height:100%; object-fit:cover;">` : `<span>🛡️</span>`}
                    </div>
                    <div>
                        <h4 style="font-family: var(--font-heading); font-size: 1.05rem; font-weight: 700; color: #fff; margin-bottom: 0.15rem;">${escapeHtml(inv.teamName)}</h4>
                        <span style="font-size: 0.82rem; color: var(--text-secondary);">Invited by: <strong>${escapeHtml(inv.invitedByName)}</strong> (${escapeHtml(inv.invitedByUserId)})</span>
                    </div>
                </div>

                <div style="display: flex; gap: 0.5rem;">
                    <button class="btn btn-primary btn-sm" onclick="handleAcceptInvitation(${inv.invitationId})">
                        Accept
                    </button>
                    <button class="btn btn-secondary btn-sm" onclick="handleRejectInvitation(${inv.invitationId})">
                        Reject
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

async function handleAcceptInvitation(invitationId) {
    try {
        const res = await ApiService.post(`/team-invitations/${invitationId}/accept`, {});
        ApiService.showToast(res.message || 'Invitation accepted!', 'success');
        await loadInvitations();
        await renderInvitationsList();
        loadMyTeams();
    } catch (err) {
        showAlert('invitations-modal-alert', err.message || 'Failed to accept invitation.');
    }
}

async function handleRejectInvitation(invitationId) {
    try {
        const res = await ApiService.post(`/team-invitations/${invitationId}/reject`, {});
        ApiService.showToast(res.message || 'Invitation rejected.', 'info');
        await loadInvitations();
        await renderInvitationsList();
    } catch (err) {
        showAlert('invitations-modal-alert', err.message || 'Failed to reject invitation.');
    }
}

/* Helpers */
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

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, match => {
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return map[match];
    });
}
