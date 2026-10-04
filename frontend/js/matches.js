/**
 * Cricket App - My Matches JavaScript
 * Stage 5 - Match Management
 */

let allMatches = [];
let currentFilter = 'ALL';

document.addEventListener('DOMContentLoaded', () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    initLogout();
    loadSidebarUser();
    loadMyMatches();
    initFilterTabs();
});

function initLogout() {
    const logoutBtns = document.querySelectorAll('#btn-logout, .btn-logout');
    logoutBtns.forEach(btn => {
        btn.addEventListener('click', async () => {
            try {
                await ApiService.post('/auth/logout', {});
            } catch (ignored) {}
            ApiService.clearToken();
            ApiService.showToast('Logged out successfully.', 'info');
            window.location.href = 'login.html';
        });
    });
}

async function loadSidebarUser() {
    try {
        const user = await ApiService.get('/profile/me');
        if (user) {
            const sidebarNameEl = document.getElementById('sidebar-user-name');
            if (sidebarNameEl) sidebarNameEl.textContent = user.name || 'Player';

            const avatarImgEls = document.querySelectorAll('.user-avatar-img');
            const avatarFallbackEls = document.querySelectorAll('.user-avatar-fallback');

            if (user.profilePhotoUrl) {
                const photoUrl = ApiService.getImageUrl(user.profilePhotoUrl);
                avatarImgEls.forEach(img => {
                    img.src = photoUrl;
                    img.style.display = 'block';
                });
                avatarFallbackEls.forEach(fb => fb.style.display = 'none');
            }
        }
    } catch (ignored) {}
}

let pendingInvitations = [];

async function loadMyMatches() {
    const grid = document.getElementById('matches-grid');
    if (!grid) return;

    renderSkeletonState();

    try {
        const [matchesRes, invRes] = await Promise.all([
            ApiService.get('/matches/my'),
            ApiService.get('/match-invitations/me').catch(() => [])
        ]);

        allMatches = matchesRes || [];
        pendingInvitations = (invRes || []).filter(i => i.invitationStatus === 'PENDING');

        renderPendingInvitations();
        renderMatches();
    } catch (err) {
        console.error('Failed to load matches:', err);
        showAlert('matches-alert', err.message || 'Failed to load matches.');
        renderErrorState();
    }
}

function renderPendingInvitations() {
    const container = document.getElementById('pending-invitations-container');
    const list = document.getElementById('pending-invitations-list');
    if (!container || !list) return;

    if (pendingInvitations.length === 0) {
        container.style.display = 'none';
        list.innerHTML = '';
        return;
    }

    container.style.display = 'block';
    list.innerHTML = pendingInvitations.map(inv => {
        const invId = inv.invitationId;
        const matchId = escapeHtml(inv.matchId);
        const matchName = escapeHtml(inv.matchName);
        const inviter = escapeHtml(inv.invitedByName);
        const invitingTeam = escapeHtml(inv.invitingTeamName);
        const opponentTeam = escapeHtml(inv.opponentTeamName);
        const venue = escapeHtml(inv.venue);
        const dateStr = formatDate(inv.matchDate);
        const timeStr = formatTime(inv.matchTime);

        return `
            <div style="background: #ffffff; border: 1px solid #fde68a; border-radius: 10px; padding: 0.85rem 1rem; display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap;">
                <div>
                    <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem;">
                        <strong style="font-size: 0.95rem; color: #0f172a;">${matchName}</strong>
                        <span class="player-role-tag" style="background: #fef3c7; color: #b45309;">${matchId}</span>
                    </div>
                    <p style="margin: 0; font-size: 0.82rem; color: #475569;">
                        <strong>${inviter}</strong> (${invitingTeam}) challenged <strong>${opponentTeam}</strong> &bull; ${dateStr} at ${timeStr} (${venue})
                    </p>
                </div>
                <div style="display: flex; align-items: center; gap: 0.5rem;">
                    <button class="btn btn-primary btn-sm btn-accept-inv" data-id="${invId}">Accept</button>
                    <button class="btn btn-secondary btn-sm btn-reject-inv" data-id="${invId}" style="border-color: #ef4444; color: #ef4444;">Reject</button>
                </div>
            </div>
        `;
    }).join('');

    // Attach listeners
    list.querySelectorAll('.btn-accept-inv').forEach(btn => {
        btn.addEventListener('click', () => handleInvitationResponse(btn.getAttribute('data-id'), 'accept'));
    });

    list.querySelectorAll('.btn-reject-inv').forEach(btn => {
        btn.addEventListener('click', () => handleInvitationResponse(btn.getAttribute('data-id'), 'reject'));
    });
}

async function handleInvitationResponse(invitationId, action) {
    if (!invitationId) return;
    try {
        if (action === 'accept') {
            await ApiService.post(`/match-invitations/${invitationId}/accept`, {});
            ApiService.showToast('Match invitation accepted! Match is now scheduled.', 'success');
        } else {
            if (!confirm('Are you sure you want to reject this match invitation?')) return;
            await ApiService.post(`/match-invitations/${invitationId}/reject`, {});
            ApiService.showToast('Match invitation rejected.', 'info');
        }
        await loadMyMatches();
    } catch (err) {
        console.error(`Failed to ${action} invitation:`, err);
        ApiService.showToast(err.message || `Failed to ${action} invitation.`, 'error');
    }
}

function initFilterTabs() {
    const tabBtns = document.querySelectorAll('.stats-tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            currentFilter = btn.getAttribute('data-filter') || 'ALL';
            renderMatches();
        });
    });
}

function renderMatches() {
    const grid = document.getElementById('matches-grid');
    if (!grid) return;

    let filtered = allMatches;
    if (currentFilter !== 'ALL') {
        filtered = allMatches.filter(m => m.status === currentFilter);
    }

    if (filtered.length === 0) {
        grid.innerHTML = `
            <div class="search-state-box" style="grid-column: 1 / -1; padding: 3rem 1.5rem;">
                <div class="state-icon-circle emerald">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><path d="M12 8v4l3 3"></path></svg>
                </div>
                <h3 class="state-title">${currentFilter === 'ALL' ? 'No matches scheduled yet' : 'No ' + currentFilter.toLowerCase().replace('_', ' ') + ' matches'}</h3>
                <p class="state-subtext" style="margin-bottom: 1.25rem;">Create a match between two existing teams to start tracking cricket fixtures.</p>
                <a href="create-match.html" class="btn btn-primary">
                    + Schedule New Match
                </a>
            </div>
        `;
        return;
    }

    grid.innerHTML = filtered.map(match => {
        const matchId = escapeHtml(match.matchId);
        const name = escapeHtml(match.matchName);
        const format = escapeHtml(match.format);
        const overs = match.overs;
        const status = match.status;
        const venue = escapeHtml(match.venue);
        const dateStr = formatDate(match.matchDate);
        const timeStr = formatTime(match.matchTime);

        const teamA = match.teamA || {};
        const teamB = match.teamB || {};

        const teamAName = escapeHtml(teamA.name || 'Team A');
        const teamAId = escapeHtml(teamA.teamId || '');
        const teamALogo = ApiService.getImageUrl(teamA.logoUrl);

        const teamBName = escapeHtml(teamB.name || 'Team B');
        const teamBId = escapeHtml(teamB.teamId || '');
        const teamBLogo = ApiService.getImageUrl(teamB.logoUrl);

        let statusClass = 'status-scheduled';
        let statusLabel = status;
        if (status === 'PENDING_CONFIRMATION') {
            statusClass = 'role-badge';
            statusLabel = 'PENDING APPROVAL';
        } else if (status === 'SCHEDULED') {
            statusClass = 'role-badge owner';
        } else if (status === 'LIVE') {
            statusClass = 'prof-id-pill';
        } else if (status === 'COMPLETED') {
            statusClass = 'verified-pill';
        } else if (status === 'CANCELLED') {
            statusClass = 'role-badge';
        }

        return `
            <div class="player-result-card" style="padding: 1.35rem;">
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; margin-bottom: 0.75rem;">
                    <div style="display: flex; align-items: center; gap: 0.5rem;">
                        <span class="player-crk-badge" style="background: #0d5c3a; color: #ffffff;">${matchId}</span>
                        <span class="player-role-tag">${format} &bull; ${overs} Overs</span>
                    </div>
                    <span class="${statusClass}" ${status === 'PENDING_CONFIRMATION' ? 'style="background:#fef3c7; color:#b45309; border:1px solid #fde68a;"' : ''}>${statusLabel}</span>
                </div>

                <h3 style="margin: 0 0 1rem 0; font-family: 'Outfit', sans-serif; font-size: 1.15rem; font-weight: 800; color: #0f172a; word-break: break-word;">${name}</h3>

                <!-- Teams VS Box -->
                <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 1rem; margin-bottom: 1rem; display: flex; align-items: center; justify-content: space-between; gap: 0.5rem;">
                    <!-- Team A -->
                    <div style="flex: 1; text-align: center; min-width: 0;">
                        <div style="width: 44px; height: 44px; border-radius: 10px; background: #e2e8f0; margin: 0 auto 0.4rem; display: flex; align-items: center; justify-content: center; overflow: hidden;">
                            ${teamALogo ? `<img src="${teamALogo}" style="width:100%;height:100%;object-fit:cover;">` : `<span style="font-size:1.2rem;">🛡️</span>`}
                        </div>
                        <strong style="display: block; font-size: 0.88rem; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${teamAName}</strong>
                        <span style="font-size: 0.72rem; color: #64748b;">${teamAId}</span>
                    </div>

                    <!-- VS Circle -->
                    <div style="width: 32px; height: 32px; border-radius: 50%; background: #0f172a; color: #10b981; font-family: 'Outfit', sans-serif; font-weight: 800; font-size: 0.75rem; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                        VS
                    </div>

                    <!-- Team B -->
                    <div style="flex: 1; text-align: center; min-width: 0;">
                        <div style="width: 44px; height: 44px; border-radius: 10px; background: #e2e8f0; margin: 0 auto 0.4rem; display: flex; align-items: center; justify-content: center; overflow: hidden;">
                            ${teamBLogo ? `<img src="${teamBLogo}" style="width:100%;height:100%;object-fit:cover;">` : `<span style="font-size:1.2rem;">🛡️</span>`}
                        </div>
                        <strong style="display: block; font-size: 0.88rem; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${teamBName}</strong>
                        <span style="font-size: 0.72rem; color: #64748b;">${teamBId}</span>
                    </div>
                </div>

                <!-- Meta Specs -->
                <div style="font-size: 0.82rem; color: #475569; display: flex; flex-direction: column; gap: 0.35rem; margin-bottom: 1.15rem;">
                    <div style="display: flex; align-items: center; gap: 0.4rem;">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                        <span>${dateStr} &bull; ${timeStr}</span>
                    </div>
                    <div style="display: flex; align-items: center; gap: 0.4rem;">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
                        <span style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${venue}</span>
                    </div>
                </div>

                <!-- Action Button -->
                <div style="border-top: 1px solid #f1f5f9; padding-top: 0.75rem;">
                    <a href="match-details.html?id=${encodeURIComponent(matchId)}" class="player-view-btn">
                        <span>View Match Details</span>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
                    </a>
                </div>
            </div>
        `;
    }).join('');
}

function renderSkeletonState() {
    const grid = document.getElementById('matches-grid');
    if (!grid) return;
    grid.innerHTML = `
        <div class="player-card-skeleton"></div>
        <div class="player-card-skeleton"></div>
    `;
}

function renderErrorState() {
    const grid = document.getElementById('matches-grid');
    if (!grid) return;
    grid.innerHTML = `
        <div class="search-state-box" style="grid-column: 1 / -1;">
            <div class="state-icon-circle danger">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
            </div>
            <h3 class="state-title">Unable to load matches</h3>
            <p class="state-subtext">Please try refreshing the page.</p>
        </div>
    `;
}

function formatDate(dateStr) {
    if (!dateStr) return 'TBD';
    try {
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch (e) {
        return dateStr;
    }
}

function formatTime(timeStr) {
    if (!timeStr) return '';
    try {
        const parts = timeStr.split(':');
        let hours = parseInt(parts[0], 10);
        const minutes = parts[1] || '00';
        const ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12;
        return `${hours}:${minutes} ${ampm}`;
    } catch (e) {
        return timeStr;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, match => {
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return map[match];
    });
}

function showAlert(elementId, message) {
    const el = document.getElementById(elementId);
    if (el) {
        el.className = 'alert alert-error';
        el.textContent = message;
        el.style.display = 'block';
    }
}

function hideAlert(elementId) {
    const el = document.getElementById(elementId);
    if (el) el.style.display = 'none';
}
