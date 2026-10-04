/**
 * Cricket App - Public Player Profile JavaScript
 * Handles complete public profile view (/api/players/{userId})
 * Strictly READ-ONLY (No edit buttons, passwords, tokens, or private emails)
 */

document.addEventListener('DOMContentLoaded', () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    initLogout();
    loadSidebarUser();
    loadPublicProfile();
    initTabButtons();
    initShareButtons();
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

async function loadPublicProfile() {
    const urlParams = new URLSearchParams(window.location.search);
    const userId = urlParams.get('id') || urlParams.get('userId') || urlParams.get('crkId');
    const matchId = urlParams.get('matchId');
    const teamId = urlParams.get('teamId');

    if (!userId) {
        showAlert('public-profile-alert', 'No player ID specified in URL.');
        return;
    }

    hideAlert('public-profile-alert');

    try {
        const player = await ApiService.get('/players/' + encodeURIComponent(userId));
        renderPublicProfile(player);

        // Check if opened from Playing XI with match context
        if (matchId) {
            loadPlayingXiContext(matchId, teamId, player);
        }

    } catch (err) {
        console.error('Failed to load public profile:', err);
        showAlert('public-profile-alert', err.message || 'Player profile not found.');
    }
}

async function loadPlayingXiContext(matchId, teamId, player) {
    const banner = document.getElementById('pxi-context-banner');
    if (!banner) return;

    try {
        const match = await ApiService.get('/matches/' + encodeURIComponent(matchId));
        if (match) {
            const teamA = match.teamA || {};
            const teamB = match.teamB || {};
            const matchTitle = `${teamA.name || 'Team A'} vs ${teamB.name || 'Team B'}`;

            let playingTeamName = 'Playing XI';
            if (teamId) {
                if (teamA.teamId === teamId) playingTeamName = teamA.name || 'Team A';
                else if (teamB.teamId === teamId) playingTeamName = teamB.name || 'Team B';
            }

            const role = formatEnum(player.playingRole) || 'Player';

            if (document.getElementById('pxi-banner-match-title')) document.getElementById('pxi-banner-match-title').textContent = matchTitle;
            if (document.getElementById('pxi-banner-match-id')) document.getElementById('pxi-banner-match-id').textContent = matchId;
            if (document.getElementById('pxi-banner-team')) document.getElementById('pxi-banner-team').textContent = playingTeamName;
            if (document.getElementById('pxi-banner-role')) document.getElementById('pxi-banner-role').textContent = role;

            const linkEl = document.getElementById('pxi-banner-match-link');
            if (linkEl) linkEl.href = `match-details.html?id=${encodeURIComponent(matchId)}`;

            banner.style.display = 'block';
        }
    } catch (ignored) {}
}

function renderPublicProfile(player) {
    if (!player) return;

    const name = player.name || 'Player Profile';
    const crkId = player.userId || 'CRK-------';
    const role = formatEnum(player.playingRole) || 'All-Rounder';
    const batting = formatEnum(player.battingStyle) || 'Not Specified';
    const bowling = formatEnum(player.bowlingStyle) || 'Not Specified';
    const location = player.location || 'Location Not Specified';
    const bio = player.bio || 'No bio provided by player.';
    const dob = formatDate(player.dateOfBirth);
    const gender = formatEnum(player.gender) || 'Not Specified';

    // Header & Identity
    if (document.getElementById('pub-page-title')) document.getElementById('pub-page-title').textContent = name;
    if (document.getElementById('pub-name')) document.getElementById('pub-name').textContent = name;
    if (document.getElementById('pub-user-id')) document.getElementById('pub-user-id').textContent = crkId;
    if (document.getElementById('pub-info-full-name')) document.getElementById('pub-info-full-name').textContent = name;
    if (document.getElementById('pub-info-user-id')) document.getElementById('pub-info-user-id').textContent = crkId;

    // Badges & Quick Meta
    if (document.getElementById('pub-playing-role-badge')) document.getElementById('pub-playing-role-badge').textContent = role.toUpperCase();
    if (document.getElementById('pub-info-role')) document.getElementById('pub-info-role').textContent = role;
    if (document.getElementById('pub-info-batting')) document.getElementById('pub-info-batting').textContent = batting;
    if (document.getElementById('pub-info-bowling')) document.getElementById('pub-info-bowling').textContent = bowling;
    if (document.getElementById('pub-info-dob')) document.getElementById('pub-info-dob').textContent = dob;
    if (document.getElementById('pub-info-gender')) document.getElementById('pub-info-gender').textContent = gender;
    if (document.getElementById('pub-location')) document.getElementById('pub-location').textContent = location;
    if (document.getElementById('pub-info-location')) document.getElementById('pub-info-location').textContent = location;
    if (document.getElementById('pub-bio')) document.getElementById('pub-bio').textContent = bio;

    // Photo
    const photoImg = document.getElementById('pub-photo-img');
    const avatarFallback = document.getElementById('pub-avatar-fallback');

    if (player.profilePhotoUrl) {
        const fullPhotoUrl = ApiService.getImageUrl(player.profilePhotoUrl);
        if (photoImg) {
            photoImg.src = fullPhotoUrl;
            photoImg.style.display = 'block';
        }
        if (avatarFallback) avatarFallback.style.display = 'none';
    } else {
        if (photoImg) photoImg.style.display = 'none';
        if (avatarFallback) avatarFallback.style.display = 'flex';
    }

    // Calculate Profile Completion Percentage
    let score = 0;
    if (player.name) score += 15;
    if (player.userId) score += 15;
    if (player.profilePhotoUrl) score += 15;
    if (player.playingRole) score += 15;
    if (player.battingStyle) score += 10;
    if (player.bowlingStyle) score += 10;
    if (player.location) score += 10;
    if (player.dateOfBirth) score += 5;
    if (player.bio) score += 5;

    const completionPercent = Math.min(100, Math.max(20, score));
    const percentEl = document.getElementById('pub-completion-percent');
    const barEl = document.getElementById('pub-completion-bar');
    if (percentEl) percentEl.textContent = `${completionPercent}%`;
    if (barEl) barEl.style.width = `${completionPercent}%`;

    // Render Career Stats (Batting, Bowling, Fielding)
    renderBattingStats(player.batting);
    renderBowlingStats(player.bowling);
    renderFieldingStats(player.fielding);

    // Render Teams Played For
    renderTeams(player.teams);

    // Render Match History & Achievements (empty states if no match data exists yet)
    renderMatchHistory(player.recentMatches || player.matchHistory);
    renderAchievements(player.achievements);
}

function renderBattingStats(b) {
    const grid = document.getElementById('pub-batting-grid');
    if (!grid) return;

    if (!b || b.matches === 0) {
        grid.innerHTML = `
            <div class="search-state-box" style="grid-column: 1 / -1; padding: 2rem 1rem;">
                <div class="state-icon-circle emerald">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
                </div>
                <h3 class="state-title">No batting statistics yet</h3>
                <p class="state-subtext">Statistics will update automatically when completed match scores are recorded.</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = `
        <div class="stat-box"><span class="stat-val">${b.matches}</span><span class="stat-lbl">Matches</span></div>
        <div class="stat-box"><span class="stat-val">${b.innings}</span><span class="stat-lbl">Innings</span></div>
        <div class="stat-box"><span class="stat-val">${b.runs}</span><span class="stat-lbl">Runs</span></div>
        <div class="stat-box"><span class="stat-val">${b.highestScore || '0'}</span><span class="stat-lbl">High Score</span></div>
        <div class="stat-box"><span class="stat-val">${b.average != null ? b.average.toFixed(2) : '-'}</span><span class="stat-lbl">Average</span></div>
        <div class="stat-box"><span class="stat-val">${b.strikeRate != null ? b.strikeRate.toFixed(1) : '-'}</span><span class="stat-lbl">Strike Rate</span></div>
        <div class="stat-box"><span class="stat-val">${b.fifties} / ${b.hundreds}</span><span class="stat-lbl">50s / 100s</span></div>
        <div class="stat-box"><span class="stat-val">${b.ballsFaced}</span><span class="stat-lbl">Balls Faced</span></div>
        <div class="stat-box"><span class="stat-val">${b.fours} / ${b.sixes}</span><span class="stat-lbl">4s / 6s</span></div>
        <div class="stat-box"><span class="stat-val">${b.notOuts}</span><span class="stat-lbl">Not Outs</span></div>
    `;
}

function renderBowlingStats(bw) {
    const grid = document.getElementById('pub-bowling-grid');
    if (!grid) return;

    if (!bw || bw.matches === 0) {
        grid.innerHTML = `
            <div class="search-state-box" style="grid-column: 1 / -1; padding: 2rem 1rem;">
                <div class="state-icon-circle navy">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"></path></svg>
                </div>
                <h3 class="state-title">No bowling statistics yet</h3>
                <p class="state-subtext">Statistics will update automatically when completed match scores are recorded.</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = `
        <div class="stat-box"><span class="stat-val">${bw.overs}</span><span class="stat-lbl">Overs</span></div>
        <div class="stat-box"><span class="stat-val">${bw.wickets}</span><span class="stat-lbl">Wickets</span></div>
        <div class="stat-box"><span class="stat-val">${bw.bestBowling || '-'}</span><span class="stat-lbl">Best Bowling</span></div>
        <div class="stat-box"><span class="stat-val">${bw.economy != null ? bw.economy.toFixed(2) : '-'}</span><span class="stat-lbl">Economy</span></div>
        <div class="stat-box"><span class="stat-val">${bw.average != null ? bw.average.toFixed(2) : '-'}</span><span class="stat-lbl">Average</span></div>
        <div class="stat-box"><span class="stat-val">${bw.fourWickets} / ${bw.fiveWickets}</span><span class="stat-lbl">4w / 5w</span></div>
        <div class="stat-box"><span class="stat-val">${bw.balls}</span><span class="stat-lbl">Balls Bowled</span></div>
        <div class="stat-box"><span class="stat-val">${bw.runsConceded}</span><span class="stat-lbl">Runs Conceded</span></div>
        <div class="stat-box"><span class="stat-val">${bw.maidens}</span><span class="stat-lbl">Maidens</span></div>
    `;
}

function renderFieldingStats(f) {
    const grid = document.getElementById('pub-fielding-grid');
    if (!grid) return;

    if (!f || (f.catches === 0 && f.runOuts === 0 && f.stumpings === 0)) {
        grid.innerHTML = `
            <div class="search-state-box" style="grid-column: 1 / -1; padding: 2rem 1rem;">
                <div class="state-icon-circle gold">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
                </div>
                <h3 class="state-title">No fielding statistics yet</h3>
                <p class="state-subtext">Catches, run outs, and stumpings will populate as matches are logged.</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = `
        <div class="stat-box"><span class="stat-val">${f.catches}</span><span class="stat-lbl">Catches</span></div>
        <div class="stat-box"><span class="stat-val">${f.runOuts}</span><span class="stat-lbl">Run Outs</span></div>
        <div class="stat-box"><span class="stat-val">${f.stumpings}</span><span class="stat-lbl">Stumpings</span></div>
    `;
}

function renderTeams(teams) {
    const container = document.getElementById('pub-teams-list');
    if (!container) return;

    if (!teams || teams.length === 0) {
        container.innerHTML = `
            <div class="search-state-box" style="padding: 2rem 1rem;">
                <div class="state-icon-circle gold">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                </div>
                <h3 class="state-title">No teams joined yet</h3>
                <p class="state-subtext">This player has not joined any public teams yet.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = teams.map(team => {
        const logoUrl = ApiService.getImageUrl(team.logoUrl);
        const name = escapeHtml(team.teamName || 'Team');
        const teamId = escapeHtml(team.teamId || 'TM-------');
        const role = formatEnum(team.role) || 'Member';
        const joinedDate = formatDate(team.joinedDate);

        return `
            <div class="prof-team-card" style="display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; margin-bottom: 0.75rem;">
                <div style="display: flex; align-items: center; gap: 0.85rem; min-width: 0;">
                    <div style="width: 44px; height: 44px; border-radius: 10px; background: #e2e8f0; display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0;">
                        ${logoUrl 
                            ? `<img src="${logoUrl}" alt="${name}" style="width:100%; height:100%; object-fit:cover;">` 
                            : `<span style="font-size: 1.25rem;">🛡️</span>`}
                    </div>
                    <div style="min-width: 0;">
                        <span style="font-size: 0.72rem; font-weight: 700; color: #0d5c3a; letter-spacing: 0.04em;">${teamId}</span>
                        <h4 style="margin: 0; font-size: 0.95rem; font-weight: 700; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${name}</h4>
                        <span style="font-size: 0.78rem; color: #64748b;">Joined ${joinedDate}</span>
                    </div>
                </div>
                <div style="text-align: right; flex-shrink: 0;">
                    <span class="player-role-tag" style="background: #e0f2fe; color: #0369a1; border-color: #bae6fd;">${role}</span>
                    <a href="team-details.html?id=${encodeURIComponent(teamId)}" style="display: block; font-size: 0.78rem; color: #0d5c3a; font-weight: 600; text-decoration: none; margin-top: 0.35rem;">
                        View Team &rarr;
                    </a>
                </div>
            </div>
        `;
    }).join('');
}

function renderMatchHistory(matches) {
    const container = document.getElementById('pub-match-history-container');
    if (!container) return;

    if (!matches || matches.length === 0) {
        container.innerHTML = `
            <div class="search-state-box" style="padding: 2rem 1rem;">
                <div class="state-icon-circle navy">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                </div>
                <h3 class="state-title">No completed matches yet</h3>
                <p class="state-subtext">Match history will be listed here after matches are played and scored.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="prof-table-responsive">
            <table class="prof-history-table">
                <thead>
                    <tr>
                        <th>Opponent Team</th>
                        <th>Format</th>
                        <th>Date</th>
                        <th>Performance</th>
                        <th>Match Result</th>
                    </tr>
                </thead>
                <tbody>
                    ${matches.map(m => `
                        <tr>
                            <td><strong style="color: #0f172a;">${escapeHtml(m.opponentName || 'Opponent')}</strong></td>
                            <td><span class="format-badge">${escapeHtml(m.format || 'Match')}</span></td>
                            <td>${formatDate(m.date)}</td>
                            <td><span class="perf-highlight">${m.runs != null ? m.runs + ' runs' : ''} ${m.wickets != null ? '& ' + m.wickets + ' wkts' : ''}</span></td>
                            <td><span class="result-badge win">${escapeHtml(m.result || 'Completed')}</span></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

function renderAchievements(achievements) {
    const container = document.getElementById('pub-achievements-container');
    if (!container) return;

    if (!achievements || achievements.length === 0) {
        container.innerHTML = `
            <div class="search-state-box" style="padding: 2rem 1rem;">
                <div class="state-icon-circle gold">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="7"></circle><polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"></polyline></svg>
                </div>
                <h3 class="state-title">No achievements yet</h3>
                <p class="state-subtext">Key career milestones will unlock as match performances are logged.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="prof-achievements-grid">
            ${achievements.map(a => `
                <div class="achievement-badge">
                    <div class="badge-icon-box ${(a.type || 'gold').toLowerCase()}">
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                    </div>
                    <div class="badge-content">
                        <h4 class="badge-name">${escapeHtml(a.title)}</h4>
                        <span class="badge-count-tag">${escapeHtml(a.description || a.type)}</span>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
}

function initTabButtons() {
    const tabBtns = document.querySelectorAll('.stats-tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const tabName = btn.getAttribute('data-tab');
            const panes = document.querySelectorAll('.stats-tab-pane');
            panes.forEach(pane => {
                pane.style.display = 'none';
                pane.classList.remove('active');
            });

            const targetPane = document.getElementById('stats-tab-' + tabName);
            if (targetPane) {
                targetPane.style.display = 'block';
                targetPane.classList.add('active');
            }
        });
    });
}

function initShareButtons() {
    const copyUrl = () => {
        const url = window.location.href;
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(url)
                .then(() => ApiService.showToast('Profile link copied to clipboard!', 'success'))
                .catch(() => ApiService.showToast('Profile Link: ' + url, 'info'));
        } else {
            ApiService.showToast('Profile Link: ' + url, 'info');
        }
    };

    const shareBtn = document.getElementById('btn-share-pub-profile');
    if (shareBtn) shareBtn.addEventListener('click', copyUrl);

    const copyBtn = document.getElementById('btn-copy-pub-link');
    if (copyBtn) copyBtn.addEventListener('click', copyUrl);
}

function formatEnum(val) {
    if (!val) return null;
    return val
        .split('_')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
}

function formatDate(dateStr) {
    if (!dateStr) return 'Not Specified';
    try {
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch (e) {
        return dateStr;
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
