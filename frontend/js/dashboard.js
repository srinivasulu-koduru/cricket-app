/**
 * Cricket App - Dashboard JavaScript
 * Fully integrated with real backend APIs (/auth/me, /profile/me, /teams/my, /team-invitations/me)
 */

document.addEventListener('DOMContentLoaded', () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    initLogout();
    initMobileNav();
    loadDashboardData();
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

function initMobileNav() {
    const toggleBtn = document.getElementById('mobile-menu-toggle');
    const drawer = document.getElementById('mobile-nav-drawer');
    const closeBtn = document.getElementById('mobile-drawer-close');

    if (toggleBtn && drawer) {
        toggleBtn.addEventListener('click', () => drawer.classList.add('open'));
    }
    if (closeBtn && drawer) {
        closeBtn.addEventListener('click', () => drawer.classList.remove('open'));
    }
}

async function loadDashboardData() {
    await Promise.all([
        loadUserProfile(),
        loadUserTeams(),
        loadUserInvitations(),
        loadLiveMatches(),
        loadAssignedScorerMatches()
    ]);
}

/* 1. Load User & Profile Info */
async function loadUserProfile() {
    try {
        // Fetch profile details
        const profile = await ApiService.get('/profile/me');
        renderProfileInfo(profile);
    } catch (err) {
        console.warn('Failed to load full profile, falling back to /auth/me:', err.message);
        try {
            const user = await ApiService.get('/auth/me');
            renderProfileInfo(user);
        } catch (authErr) {
            console.error('Session invalid:', authErr);
            ApiService.clearToken();
            window.location.href = 'login.html';
        }
    }
}

function renderProfileInfo(data) {
    if (!data) return;

    const userName = data.name || 'Player';
    const userId = data.userId || 'CRK-------';

    // Welcome greeting
    const welcomeNameEl = document.getElementById('dash-welcome-name');
    if (welcomeNameEl) welcomeNameEl.textContent = userName;

    // Sidebar user pill
    const sidebarNameEl = document.getElementById('sidebar-user-name');
    if (sidebarNameEl) sidebarNameEl.textContent = userName;

    // Right profile summary card
    const cardNameEl = document.getElementById('card-user-name');
    if (cardNameEl) cardNameEl.textContent = userName;

    const cardIdEl = document.getElementById('card-user-id');
    if (cardIdEl) cardIdEl.textContent = userId;

    // Avatar Photos
    const avatarImgEls = document.querySelectorAll('.user-avatar-img');
    const avatarFallbackEls = document.querySelectorAll('.user-avatar-fallback');

    if (data.profilePhotoUrl) {
        const photoUrl = ApiService.getImageUrl(data.profilePhotoUrl);
        avatarImgEls.forEach(img => {
            img.src = photoUrl;
            img.style.display = 'block';
        });
        avatarFallbackEls.forEach(fb => fb.style.display = 'none');
    } else {
        avatarImgEls.forEach(img => img.style.display = 'none');
        avatarFallbackEls.forEach(fb => fb.style.display = 'flex');
    }

    // Role & Batting Style Pills
    const roleBadgeEl = document.getElementById('card-playing-role');
    if (roleBadgeEl) {
        roleBadgeEl.textContent = formatEnum(data.playingRole) || 'All-Rounder';
    }

    const battingBadgeEl = document.getElementById('card-batting-style');
    if (battingBadgeEl) {
        battingBadgeEl.textContent = formatEnum(data.battingStyle) || 'Right-Handed';
    }
}

/* 2. Load User Teams */
async function loadUserTeams() {
    const teamsGrid = document.getElementById('dash-teams-grid');
    const emptyTeamsCard = document.getElementById('empty-teams-card');
    if (!teamsGrid) return;

    try {
        const teams = await ApiService.get('/teams/my');
        renderTeams(teams);
    } catch (err) {
        console.error('Failed to load teams:', err);
        teamsGrid.innerHTML = `<div style="grid-column: 1/-1; color: var(--color-danger); text-align: center; padding: 1.5rem;">Failed to load teams.</div>`;
    }
}

function renderTeams(teams) {
    const teamsGrid = document.getElementById('dash-teams-grid');
    const emptyTeamsCard = document.getElementById('empty-teams-card');
    const teamCountBadge = document.getElementById('team-count-badge');
    if (!teamsGrid) return;

    if (teamCountBadge) {
        teamCountBadge.textContent = teams ? teams.length : 0;
    }

    if (!teams || teams.length === 0) {
        teamsGrid.innerHTML = `
            <div class="dash-empty-teams-row">
                <p style="color: #64748b; font-size: 0.9rem; margin: 0;">You haven't joined any cricket teams yet.</p>
                <a href="teams.html" class="dash-action-link" style="font-weight: 600;">Create or Join Team &rarr;</a>
            </div>
        `;
        if (emptyTeamsCard) emptyTeamsCard.style.display = 'block';
        return;
    }

    if (emptyTeamsCard) emptyTeamsCard.style.display = 'none';

    // Render up to 3 teams in the dashboard row
    const displayTeams = teams.slice(0, 3);
    teamsGrid.innerHTML = displayTeams.map(team => {
        const logoUrl = ApiService.getImageUrl(team.logoUrl);
        const role = team.currentUserRole || 'PLAYER';
        const isOwner = role === 'OWNER' || role === 'CAPTAIN';

        return `
            <div class="dash-team-card">
                <div class="dash-team-card-header">
                    <div class="dash-team-logo">
                        ${logoUrl 
                            ? `<img src="${logoUrl}" alt="${escapeHtml(team.name)}">` 
                            : `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>`}
                    </div>
                    <div>
                        <h4 class="dash-team-name">${escapeHtml(team.name)}</h4>
                        <span class="dash-team-code">${escapeHtml(team.teamId)}</span>
                    </div>
                </div>

                <div class="dash-team-meta-list">
                    <div class="dash-team-meta-item">
                        <span class="meta-label">Team ID</span>
                    </div>
                    <div class="dash-team-meta-item">
                        <span class="meta-label">${team.memberCount || 1} Members</span>
                    </div>
                    <div class="dash-team-meta-item">
                        <span class="dash-role-badge ${isOwner ? 'owner' : ''}">${escapeHtml(role)}</span>
                    </div>
                </div>

                <div class="dash-team-card-footer">
                    <a href="team-details.html?id=${team.teamId}" class="dash-card-arrow-link">
                        <span>Action</span>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
                    </a>
                </div>
            </div>
        `;
    }).join('');
}

/* 3. Load User Invitations */
async function loadUserInvitations() {
    const invContainer = document.getElementById('dash-invitations-container');
    const invBadge = document.getElementById('invitation-count-badge');
    if (!invContainer) return;

    try {
        const invitations = await ApiService.get('/team-invitations/me');
        renderInvitations(invitations);
    } catch (err) {
        console.error('Failed to load invitations:', err);
        invContainer.innerHTML = `<div style="text-align: center; color: #94a3b8; padding: 1rem; font-size: 0.85rem;">No pending invitations.</div>`;
    }
}

function renderInvitations(invitations) {
    const invContainer = document.getElementById('dash-invitations-container');
    const invBadge = document.getElementById('invitation-count-badge');
    if (!invContainer) return;

    if (invBadge) {
        if (invitations && invitations.length > 0) {
            invBadge.textContent = invitations.length;
            invBadge.style.display = 'inline-flex';
        } else {
            invBadge.style.display = 'none';
        }
    }

    if (!invitations || invitations.length === 0) {
        invContainer.innerHTML = `
            <div class="dash-invitation-card empty">
                <div class="dash-empty-icon">📩</div>
                <p style="color: #64748b; font-size: 0.85rem; margin: 0;">No pending team invitations.</p>
            </div>
        `;
        return;
    }

    // Render first pending invitation in right panel
    const inv = invitations[0];
    const logoUrl = ApiService.getImageUrl(inv.teamLogoUrl);

    invContainer.innerHTML = `
        <div class="dash-invitation-card">
            <div class="dash-invitation-header">
                <div class="dash-inv-logo">
                    ${logoUrl 
                        ? `<img src="${logoUrl}" alt="${escapeHtml(inv.teamName)}">` 
                        : `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"></path></svg>`}
                </div>
                <div>
                    <h4 class="dash-inv-team">${escapeHtml(inv.teamName)}</h4>
                    <p class="dash-inv-subtext">Invited you to join the team</p>
                </div>
            </div>

            <div class="dash-inv-actions">
                <button class="dash-btn-accept" onclick="handleDashboardAcceptInv(${inv.invitationId})">Accept</button>
                <button class="dash-btn-decline" onclick="handleDashboardRejectInv(${inv.invitationId})">Decline</button>
            </div>
        </div>
    `;
}

async function handleDashboardAcceptInv(invitationId) {
    try {
        const res = await ApiService.post(`/team-invitations/${invitationId}/accept`, {});
        ApiService.showToast(res.message || 'Invitation accepted successfully!', 'success');
        loadDashboardData();
    } catch (err) {
        ApiService.showToast(err.message || 'Failed to accept invitation.', 'error');
    }
}

async function handleDashboardRejectInv(invitationId) {
    try {
        const res = await ApiService.post(`/team-invitations/${invitationId}/reject`, {});
        ApiService.showToast(res.message || 'Invitation declined.', 'info');
        loadDashboardData();
    } catch (err) {
        ApiService.showToast(err.message || 'Failed to decline invitation.', 'error');
    }
}

/* Live Matches Dashboard Section */
async function loadLiveMatches() {
    const container = document.getElementById('dash-live-matches-container');
    if (!container) return;

    try {
        const liveMatches = await ApiService.get('/matches/live');
        if (!liveMatches || liveMatches.length === 0) {
            renderLiveMatches([]);
            return;
        }

        // Fetch live scoring state for each live match
        const matchDataList = await Promise.all(
            liveMatches.map(async (match) => {
                try {
                    const scoring = await ApiService.get(`/matches/${encodeURIComponent(match.matchId)}/scoring`);
                    return { match, scoring };
                } catch (e) {
                    return { match, scoring: null };
                }
            })
        );

        renderLiveMatches(matchDataList);
    } catch (err) {
        console.warn('Failed to load live matches:', err.message);
        renderLiveMatches([]);
    }
}

function renderLiveMatches(matchDataList) {
    const container = document.getElementById('dash-live-matches-container');
    if (!container) return;

    if (!matchDataList || matchDataList.length === 0) {
        container.innerHTML = `
            <div style="padding: 1.75rem 1.25rem; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 14px; text-align: center;">
                <div style="width: 44px; height: 44px; border-radius: 50%; background: #f1f5f9; color: #64748b; display: flex; align-items: center; justify-content: center; margin: 0 auto 0.6rem; font-size: 1.25rem;">
                    🏏
                </div>
                <h4 style="margin: 0 0 0.25rem 0; font-family: 'Outfit', sans-serif; font-size: 1.05rem; font-weight: 700; color: #0f172a;">No live matches right now</h4>
                <p style="margin: 0; font-size: 0.85rem; color: #64748b;">Scheduled matches will appear here when they go live.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = matchDataList.map(item => {
        const match = item.match || item;
        const scoring = item.scoring || {};
        const matchId = escapeHtml(match.matchId);
        const teamA = match.teamA || {};
        const teamB = match.teamB || {};
        const teamAName = escapeHtml(teamA.name || 'Team A');
        const teamBName = escapeHtml(teamB.name || 'Team B');
        const logoA = ApiService.getImageUrl(teamA.logoUrl);
        const logoB = ApiService.getImageUrl(teamB.logoUrl);
        const format = escapeHtml(match.format || 'T20');

        // Active Innings Score details
        const totalRuns = scoring.totalRuns !== undefined ? scoring.totalRuns : 0;
        const totalWickets = scoring.totalWickets !== undefined ? scoring.totalWickets : 0;
        const completedOvers = scoring.completedOvers !== undefined ? scoring.completedOvers : 0;
        const currentBalls = scoring.currentBalls !== undefined ? scoring.currentBalls : 0;
        const oversFormatted = `${completedOvers}${currentBalls > 0 ? '.' + currentBalls : ''}`;
        
        const battingTeamId = scoring.battingTeamId;
        const isTeamABatting = battingTeamId ? (battingTeamId === teamA.teamId) : true;
        const isTeamBBatting = battingTeamId ? (battingTeamId === teamB.teamId) : false;

        const scoreAStr = isTeamABatting ? `${totalRuns}/${totalWickets} (${oversFormatted})` : 'Yet to bat';
        const scoreBStr = isTeamBBatting ? `${totalRuns}/${totalWickets} (${oversFormatted})` : 'Yet to bat';

        const tossSummary = scoring.tossSummary ? escapeHtml(scoring.tossSummary) : 'Match in progress';
        const equationText = (scoring.activeInningsNumber === 2 && scoring.targetEquation) 
            ? escapeHtml(scoring.targetEquation) 
            : (scoring.activeInningsNumber === 2 ? `${scoring.battingTeamName} in pursuit of target` : tossSummary);

        return `
            <div class="dash-live-card-mockup" onclick="window.location.href='live-score.html?matchId=${encodeURIComponent(matchId)}'" style="background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px; padding: 1.25rem 1.5rem; cursor: pointer; transition: all 0.2s ease-in-out; position: relative; box-shadow: 0 2px 10px rgba(0,0,0,0.04); margin-bottom: 1rem;" onmouseover="this.style.boxShadow='0 6px 18px rgba(16,185,129,0.15)'; this.style.borderColor='#10b981';" onmouseout="this.style.boxShadow='0 2px 10px rgba(0,0,0,0.04)'; this.style.borderColor='#e2e8f0';">
                <!-- Top Row: Format & Match ID + LIVE Badge -->
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.9rem;">
                    <span style="font-family: 'Outfit', 'Inter', sans-serif; font-size: 0.8rem; color: #64748b; font-weight: 600;">
                        ${format} &bull; ${matchId}
                    </span>
                    <span style="font-size: 0.78rem; font-weight: 800; color: #10b981; display: flex; align-items: center; gap: 0.35rem;">
                        <span style="width: 8px; height: 8px; border-radius: 50%; background: #10b981; display: inline-block;"></span> LIVE
                    </span>
                </div>

                <!-- Team A Row -->
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
                    <div style="display: flex; align-items: center; gap: 0.75rem;">
                        <div style="width: 34px; height: 34px; border-radius: 50%; background: #145a37; display: flex; align-items: center; justify-content: center; overflow: hidden; border: 1px solid #e2e8f0; flex-shrink: 0;">
                            ${logoA ? `<img src="${logoA}" style="width:100%;height:100%;object-fit:cover;">` : `<span style="font-size: 0.9rem;">🛡️</span>`}
                        </div>
                        <span style="font-family: 'Outfit', sans-serif; font-size: 1.05rem; font-weight: 700; color: #0f172a; display: flex; align-items: center; gap: 0.4rem;">
                            ${teamAName} ${isTeamABatting ? '<span style="font-size:0.95rem;">🏏</span>' : ''}
                        </span>
                    </div>
                    <div style="font-family: 'Outfit', sans-serif; font-size: 1.15rem; font-weight: 800; color: #0f172a;">
                        ${scoreAStr}
                    </div>
                </div>

                <!-- Team B Row -->
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.9rem;">
                    <div style="display: flex; align-items: center; gap: 0.75rem;">
                        <div style="width: 34px; height: 34px; border-radius: 50%; background: #145a37; display: flex; align-items: center; justify-content: center; overflow: hidden; border: 1px solid #e2e8f0; flex-shrink: 0;">
                            ${logoB ? `<img src="${logoB}" style="width:100%;height:100%;object-fit:cover;">` : `<span style="font-size: 0.9rem;">🛡️</span>`}
                        </div>
                        <span style="font-family: 'Outfit', sans-serif; font-size: 1.05rem; font-weight: 700; color: #0f172a; display: flex; align-items: center; gap: 0.4rem;">
                            ${teamBName} ${isTeamBBatting ? '<span style="font-size:0.95rem;">🏏</span>' : ''}
                        </span>
                    </div>
                    <div style="font-family: 'Outfit', sans-serif; font-size: 1.15rem; font-weight: 800; color: #0f172a;">
                        ${scoreBStr}
                    </div>
                </div>

                <!-- Bottom Equation / Toss Status Line -->
                <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #f1f5f9; padding-top: 0.75rem; margin-top: 0.25rem;">
                    <span style="font-family: 'Inter', sans-serif; font-size: 0.88rem; font-weight: 700; color: #0b5d35;">
                        ${equationText}
                    </span>
                    <div style="width: 26px; height: 26px; border-radius: 50%; background: #f8fafc; border: 1px solid #cbd5e1; display: flex; align-items: center; justify-content: center; color: #64748b; font-size: 0.9rem; font-weight: 800;">
                        &rsaquo;
                    </div>
                </div>
            </div>
        `;
    }).join('');
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

/* Scorer Assigned Matches Dashboard Section */
async function loadAssignedScorerMatches() {
    const section = document.getElementById('dash-scorer-matches-section');
    const container = document.getElementById('dash-scorer-matches-container');
    if (!section || !container) return;

    try {
        const matches = await ApiService.get('/matches/scorer');
        const activeMatches = (matches || []).filter(m => m.status !== 'COMPLETED' && m.status !== 'CANCELLED');

        if (!activeMatches || activeMatches.length === 0) {
            section.style.display = 'none';
            return;
        }

        section.style.display = 'block';
        container.innerHTML = activeMatches.map(match => {
            const matchId = match.matchId || 'MATCH------';
            const name = escapeHtml(match.matchName || 'Cricket Match');
            const format = match.format || 'T20';
            const overs = match.overs || 20;
            const status = match.status || 'SCHEDULED';
            const dateStr = formatDate(match.matchDate);
            const timeStr = formatTime(match.matchTime);
            const venue = escapeHtml(match.venue || 'Venue Not Specified');

            const teamA = match.teamA || {};
            const teamB = match.teamB || {};
            const teamAName = escapeHtml(teamA.name || 'Team A');
            const teamBName = escapeHtml(teamB.name || 'Team B');

            const isLive = status === 'LIVE';
            const isCompleted = status === 'COMPLETED';

            let btnLabel = 'Start Scoring / Match Setup';
            if (isLive) btnLabel = 'Score Match';
            if (isCompleted) btnLabel = '📊 View Final Scoreboard';

            return `
                <div style="background: rgba(15, 23, 42, 0.85); border: 1.5px solid rgba(16, 185, 129, 0.4); border-radius: 14px; padding: 1.25rem; box-shadow: 0 4px 20px rgba(0,0,0,0.3);">
                    <div style="display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; margin-bottom: 0.75rem;">
                        <h4 style="font-family: 'Outfit', sans-serif; font-size: 1.15rem; font-weight: 800; color: #ffffff; margin: 0;">
                            ${teamAName} vs ${teamBName}
                        </h4>
                        <span class="role-badge ${isLive ? 'owner' : ''}" style="${isLive ? 'background:#ef4444; color:#fff;' : ''}">
                            ${isLive ? '🔴 LIVE' : status}
                        </span>
                    </div>

                    <p style="font-size: 0.85rem; color: #94a3b8; margin: 0 0 1rem 0; line-height: 1.5;">
                        <strong style="color: #10b981;">${matchId}</strong> &bull; ${dateStr} &bull; ${timeStr} &bull; 📍 ${venue} &bull; ${format} (${overs} Overs)
                    </p>

                    <div style="display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center;">
                        <a href="scoring-dashboard.html?matchId=${encodeURIComponent(matchId)}" class="btn btn-primary" style="background: #10b981; color: #000; font-weight: 800; text-decoration: none; padding: 0.55rem 1.1rem; border-radius: 8px; display: inline-flex; align-items: center; gap: 0.4rem; font-size: 0.88rem;">
                            <span>${btnLabel}</span> &rarr;
                        </a>
                        <a href="match-details.html?id=${encodeURIComponent(matchId)}" style="color: #94a3b8; font-size: 0.85rem; font-weight: 600; text-decoration: none;">
                            View Details
                        </a>
                    </div>
                </div>
            `;
        }).join('');

    } catch (err) {
        console.warn('Failed to load assigned scorer matches:', err.message);
        section.style.display = 'none';
    }
}

/* Utility Helpers */
function formatEnum(val) {
    if (!val) return null;
    return val.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('-');
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, match => {
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return map[match];
    });
}
