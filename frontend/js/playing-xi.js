/**
 * Cricket App - Playing XI View JavaScript
 * Matches exact mobile/responsive card layout of screenshot media_1790965416119.png
 */

let currentMatchId = null;

document.addEventListener('DOMContentLoaded', () => {
    loadPlayingXi();
});

async function loadPlayingXi() {
    const urlParams = new URLSearchParams(window.location.search);
    const matchId = urlParams.get('matchId') || urlParams.get('id');

    if (!matchId) {
        showAlert('pxi-alert', 'No match ID specified in URL parameters.');
        return;
    }

    currentMatchId = matchId;

    if (document.getElementById('pxi-match-id')) {
        document.getElementById('pxi-match-id').textContent = matchId;
    }

    const selectBtn = document.getElementById('btn-select-xi-nav');
    if (selectBtn) {
        selectBtn.href = `select-playing-xi.html?matchId=${encodeURIComponent(matchId)}`;
    }

    try {
        const match = await ApiService.get('/matches/' + encodeURIComponent(matchId));
        renderMatchHeader(match);

        const teamA = match.teamA || {};
        const teamB = match.teamB || {};

        // Fetch saved Playing XI & user profile
        const [savedXi, userProfile] = await Promise.all([
            ApiService.get(`/matches/${encodeURIComponent(matchId)}/playing-xi`).catch(() => null),
            ApiService.get('/profile/me').catch(() => null)
        ]);

        const teamAData = savedXi ? savedXi.teamA : null;
        const teamBData = savedXi ? savedXi.teamB : null;

        renderTeamColumn('A', teamA, teamAData, match);
        renderTeamColumn('B', teamB, teamBData, match);

        renderBottomSummary(teamAData, teamBData);

    } catch (err) {
        console.error('Failed to load Playing XI:', err);
        showAlert('pxi-alert', err.message || 'Failed to load match Playing XI.');
    }
}

function renderMatchHeader(match) {
    if (!match) return;

    const teamA = match.teamA || {};
    const teamB = match.teamB || {};

    if (document.getElementById('pxi-team-a-name')) document.getElementById('pxi-team-a-name').textContent = teamA.name || 'Team A';
    if (document.getElementById('pxi-col-team-a-name')) document.getElementById('pxi-col-team-a-name').textContent = teamA.name || 'Team A';
    if (document.getElementById('pxi-team-a-id')) document.getElementById('pxi-team-a-id').textContent = teamA.teamId || '';
    if (document.getElementById('pxi-circle-text-a')) document.getElementById('pxi-circle-text-a').textContent = (teamA.name || 'A').substring(0, 3).toUpperCase();
    
    const logoA = ApiService.getImageUrl(teamA.logoUrl);
    const imgA = document.getElementById('pxi-team-a-logo');
    const fbA = document.getElementById('pxi-team-a-fallback');
    if (logoA && imgA) {
        imgA.src = logoA;
        imgA.style.display = 'block';
        if (fbA) fbA.style.display = 'none';
    }

    if (document.getElementById('pxi-team-b-name')) document.getElementById('pxi-team-b-name').textContent = teamB.name || 'Team B';
    if (document.getElementById('pxi-col-team-b-name')) document.getElementById('pxi-col-team-b-name').textContent = teamB.name || 'Team B';
    if (document.getElementById('pxi-team-b-id')) document.getElementById('pxi-team-b-id').textContent = teamB.teamId || '';
    if (document.getElementById('pxi-circle-text-b')) document.getElementById('pxi-circle-text-b').textContent = (teamB.name || 'B').substring(0, 3).toUpperCase();

    const logoB = ApiService.getImageUrl(teamB.logoUrl);
    const imgB = document.getElementById('pxi-team-b-logo');
    const fbB = document.getElementById('pxi-team-b-fallback');
    if (logoB && imgB) {
        imgB.src = logoB;
        imgB.style.display = 'block';
        if (fbB) fbB.style.display = 'none';
    }

    if (document.getElementById('pxi-format-overs')) {
        document.getElementById('pxi-format-overs').textContent = `${match.format || 'T20'} \u2022 ${match.overs || 20} OVERS`;
    }
    if (document.getElementById('pxi-format-tag')) {
        document.getElementById('pxi-format-tag').textContent = match.format || 'T20';
    }
}

function renderTeamColumn(letter, team, teamData, match) {
    const containerId = `pxi-squad-team-${letter.toLowerCase()}`;
    const badgeId = `pxi-col-team-${letter.toLowerCase()}-badge`;
    const subId = `pxi-col-team-${letter.toLowerCase()}-sub`;

    const container = document.getElementById(containerId);
    const badge = document.getElementById(badgeId);
    const sub = document.getElementById(subId);

    const teamName = team ? (team.name || `Team ${letter}`) : `Team ${letter}`;

    if (!teamData || !teamData.isSaved || !teamData.players || teamData.players.length === 0) {
        // UNANNOUNCED STATE: Captain has not selected Playing XI yet!
        if (badge) {
            badge.className = 'status-badge-pending';
            badge.textContent = 'UNCONFIRMED';
        }
        if (sub) {
            sub.textContent = 'Playing 11 • Not Announced';
        }

        const isCap = teamData && teamData.isCaptainOfTeam === true;

        if (container) {
            container.innerHTML = `
                <div style="padding: 2.2rem 1.25rem; text-align: center; background: rgba(255,255,255,0.02); border: 1.5px dashed rgba(255,255,255,0.1); border-radius: 12px; margin-top: 0.5rem;">
                    <div style="font-size: 2.2rem; margin-bottom: 0.6rem;">📋</div>
                    <h4 style="margin: 0 0 0.35rem 0; font-family: 'Outfit', sans-serif; font-size: 1.05rem; color: #ffffff;">
                        Playing XI not announced yet
                    </h4>
                    <p style="margin: 0; font-size: 0.83rem; color: #94a3b8; line-height: 1.4;">
                        The official 11 players for <strong style="color:#e2e8f0;">${escapeHtml(teamName)}</strong> will be displayed here as soon as the captain selects and saves the team.
                    </p>
                    ${isCap ? `
                        <div style="margin-top: 1.25rem;">
                            <a href="select-playing-xi.html?matchId=${encodeURIComponent(currentMatchId)}" class="btn btn-primary btn-sm" style="background: #10b981; color: #000000; font-weight: 800; text-decoration: none; padding: 0.55rem 1.2rem; border-radius: 8px; display: inline-flex; align-items: center; gap: 0.4rem;">
                                ⚡ Select & Announce Playing XI
                            </a>
                        </div>
                    ` : ''}
                </div>
            `;
        }
        return;
    }

    // CONFIRMED STATE: Render 11 Official Playing XI Players!
    if (badge) {
        badge.className = 'status-badge-confirmed';
        badge.textContent = '✓ CONFIRMED';
    }
    if (sub) {
        sub.textContent = `Playing 11 • ${teamData.players.length} of 11 announced`;
    }

    if (container) {
        container.innerHTML = teamData.players.map((p, index) => {
            const photoUrl = ApiService.getImageUrl(p.profilePhotoUrl);
            const name = escapeHtml(p.name);
            const userId = escapeHtml(p.userId);
            const playingRole = formatEnum(p.playingRole) || 'Player';
            const isCap = p.isCaptain === true;
            const isWk = p.isWicketKeeper === true;

            return `
                <div class="player-row-item" 
                     onclick="openPlayerProfile('${userId}', '${team.teamId || ''}', '${currentMatchId || ''}')"
                     title="Click to view player profile for ${name}">
                    <div style="display: flex; align-items: center; gap: 0.75rem; min-width: 0;">
                        <span class="player-row-number">${index + 1}</span>
                        <div class="player-avatar-sm">
                            ${photoUrl ? `<img src="${photoUrl}" alt="${name}">` : `<span style="font-size:1rem;">👤</span>`}
                        </div>
                        <div style="min-width: 0;">
                            <div style="font-weight: 700; color: #ffffff; font-size: 0.92rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                ${name}
                            </div>
                            <div style="font-size: 0.73rem; color: #94a3b8; margin-top: 0.1rem;">
                                ${playingRole}
                            </div>
                        </div>
                    </div>

                    <div style="display: flex; align-items: center; gap: 0.4rem; flex-shrink: 0;">
                        ${isCap ? `<div class="badge-captain-gold" title="Captain">C</div>` : ''}
                        ${isWk ? `<span class="badge-keeper-teal" title="Wicket Keeper">WK</span>` : ''}
                        <span style="font-size: 0.75rem; color: #10b981; margin-left: 0.2rem;">&rarr;</span>
                    </div>
                </div>
            `;
        }).join('');
    }
}

function renderBottomSummary(teamAData, teamBData) {
    const isSavedA = teamAData && teamAData.isSaved && teamAData.players && teamAData.players.length > 0;
    const isSavedB = teamBData && teamBData.isSaved && teamBData.players && teamBData.players.length > 0;

    const countA = isSavedA ? teamAData.players.length : 0;
    const countB = isSavedB ? teamBData.players.length : 0;
    const totalCount = countA + countB;

    const titleEl = document.getElementById('pxi-bottom-summary-title');
    const descEl = document.getElementById('pxi-bottom-summary-desc');
    const iconEl = document.getElementById('pxi-bottom-summary-icon');

    const topBadge = document.getElementById('pxi-match-confirmed-badge');
    const topSubDesc = document.getElementById('pxi-sub-status-desc');

    if (isSavedA && isSavedB) {
        if (titleEl) titleEl.textContent = `${totalCount} Players • ${countA} vs ${countB}`;
        if (descEl) descEl.textContent = '✓ Playing XI confirmed for both teams';
        if (iconEl) {
            iconEl.innerHTML = '✅';
            iconEl.style.background = 'rgba(16, 185, 129, 0.2)';
        }

        if (topBadge) {
            topBadge.className = 'status-badge-confirmed';
            topBadge.textContent = '✓ CONFIRMED PLAYERS';
        }
        if (topSubDesc) {
            topSubDesc.textContent = 'Confirmed lineups for today\'s match';
        }
    } else if (isSavedA || isSavedB) {
        if (titleEl) titleEl.textContent = `${totalCount} Players Announced`;
        if (descEl) descEl.textContent = '1 of 2 teams confirmed Playing XI';
        if (iconEl) {
            iconEl.innerHTML = '⏳';
            iconEl.style.background = 'rgba(245, 158, 11, 0.2)';
        }

        if (topBadge) {
            topBadge.className = 'status-badge-pending';
            topBadge.textContent = '⏳ PARTIALLY ANNOUNCED';
        }
    } else {
        if (titleEl) titleEl.textContent = '0 Players Announced';
        if (descEl) descEl.textContent = 'Playing XI confirmation pending captain selections';
        if (iconEl) {
            iconEl.innerHTML = '⏳';
            iconEl.style.background = 'rgba(255, 255, 255, 0.08)';
        }

        if (topBadge) {
            topBadge.className = 'status-badge-pending';
            topBadge.textContent = '⏳ PENDING ANNOUNCEMENT';
        }
    }
}

function openPlayerProfile(userId, teamId, matchId) {
    if (!userId) return;
    let url = `public-profile.html?id=${encodeURIComponent(userId)}`;
    if (matchId) url += `&matchId=${encodeURIComponent(matchId)}`;
    if (teamId) url += `&teamId=${encodeURIComponent(teamId)}`;
    window.open(url, '_blank');
}

function formatEnum(val) {
    if (!val) return null;
    return val.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ');
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
