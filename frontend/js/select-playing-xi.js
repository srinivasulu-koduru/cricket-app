/**
 * Cricket App - Select & Manage Playing XI JavaScript
 * Stage 5 UI/UX Correction & Bug Fixes:
 * 1. Captains can manage THEIR team only (Opponent team is Read-Only).
 * 2. Cross-team duplicate player lock with clear locked message.
 * 3. Team Captain MUST be in Playing XI and CANNOT be removed.
 */

let currentMatchId = null;
let matchData = null;
let currentTab = 'A'; // 'A' or 'B'
let currentUserId = null;
let currentUserEmail = null;

let state = {
    A: {
        team: null,
        squad: [],
        selectedUserIds: [], // array of userIds
        wicketKeeperUserId: null,
        captainUserId: null,
        isCaptainOrOwner: false
    },
    B: {
        team: null,
        squad: [],
        selectedUserIds: [],
        wicketKeeperUserId: null,
        captainUserId: null,
        isCaptainOrOwner: false
    }
};

document.addEventListener('DOMContentLoaded', async () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    try {
        const user = await ApiService.get('/profile/me').catch(() => null);
        if (user) {
            currentUserId = user.userId;
            currentUserEmail = user.email;
        }
    } catch (e) {}

    const urlParams = new URLSearchParams(window.location.search);
    currentMatchId = urlParams.get('matchId') || urlParams.get('id');

    if (!currentMatchId) {
        showAlert('No match ID specified in URL parameters.');
        return;
    }

    await loadMatchAndSquads();
});

async function loadMatchAndSquads() {
    try {
        matchData = await ApiService.get('/matches/' + encodeURIComponent(currentMatchId));
        renderMatchHeader(matchData);

        const teamA = matchData.teamA || {};
        const teamB = matchData.teamB || {};

        state.A.team = teamA;
        state.B.team = teamB;

        // Fetch members and existing saved Playing XI in parallel
        const [membersA, membersB, playingXiAll] = await Promise.all([
            teamA.teamId ? ApiService.get(`/teams/${teamA.teamId}/members`).catch(() => []) : [],
            teamB.teamId ? ApiService.get(`/teams/${teamB.teamId}/members`).catch(() => []) : [],
            ApiService.get(`/matches/${encodeURIComponent(currentMatchId)}/playing-xi`).catch(() => null)
        ]);

        state.A.squad = membersA || [];
        state.B.squad = membersB || [];

        // Determine captain / owner permission for logged in user (3-tier check)
        const isCapAFromMatch = matchData && matchData.isCreator === true;
        const isCapBFromMatch = matchData && matchData.isInvitedCaptain === true;

        const isCapAFromXi = playingXiAll && playingXiAll.teamA ? playingXiAll.teamA.isCaptainOfTeam : null;
        const isCapBFromXi = playingXiAll && playingXiAll.teamB ? playingXiAll.teamB.isCaptainOfTeam : null;

        state.A.isCaptainOrOwner = checkIsCaptainOrOwner(state.A.squad, isCapAFromXi, isCapAFromMatch);
        state.B.isCaptainOrOwner = checkIsCaptainOrOwner(state.B.squad, isCapBFromXi, isCapBFromMatch);

        // Identify default Captain for each team from squad
        const capA = state.A.squad.find(m => m.role === 'CAPTAIN' || m.role === 'OWNER');
        if (capA) state.A.captainUserId = capA.userId;

        const capB = state.B.squad.find(m => m.role === 'CAPTAIN' || m.role === 'OWNER');
        if (capB) state.B.captainUserId = capB.userId;

        // Process saved or default selection for Team A
        if (playingXiAll && playingXiAll.teamA) {
            setupTeamSelection('A', playingXiAll.teamA);
        } else {
            setupDefaultTeamSelection('A');
        }

        // Process saved or default selection for Team B
        if (playingXiAll && playingXiAll.teamB) {
            setupTeamSelection('B', playingXiAll.teamB);
        } else {
            setupDefaultTeamSelection('B');
        }

        // Default tab selection based on user permissions
        if (state.B.isCaptainOrOwner && !state.A.isCaptainOrOwner) {
            currentTab = 'B';
        } else {
            currentTab = 'A';
        }

        updateTabsUI();
        renderActiveTeamView();

    } catch (err) {
        console.error('Failed to load match or squads:', err);
        showAlert(err.message || 'Failed to load match squad details.');
    }
}

function checkIsCaptainOrOwner(squad, isCapFromXi, isCapFromMatch) {
    if (isCapFromXi === true) return true;
    if (isCapFromMatch === true) return true;
    if (!squad || squad.length === 0) return false;

    return squad.some(m => {
        const isUserMatch = currentUserId && m.userId && m.userId.toLowerCase() === currentUserId.toLowerCase();
        const isEmailMatch = currentUserEmail && m.email && m.email.toLowerCase() === currentUserEmail.toLowerCase();
        const isCapRole = m.role === 'CAPTAIN' || m.role === 'OWNER';
        return (isUserMatch || isEmailMatch) && isCapRole;
    });
}

function setupTeamSelection(tab, xiResponse) {
    const tState = state[tab];
    if (xiResponse && xiResponse.players && xiResponse.players.length > 0) {
        tState.selectedUserIds = xiResponse.players.map(p => p.userId);
        
        const wkPlayer = xiResponse.players.find(p => p.isWicketKeeper);
        if (wkPlayer) tState.wicketKeeperUserId = wkPlayer.userId;

        const capPlayer = xiResponse.players.find(p => p.isCaptain);
        if (capPlayer) tState.captainUserId = capPlayer.userId;
    } else {
        setupDefaultTeamSelection(tab);
    }

    // Ensure Captain is ALWAYS included in selectedUserIds
    if (tState.captainUserId && !tState.selectedUserIds.includes(tState.captainUserId)) {
        tState.selectedUserIds.unshift(tState.captainUserId);
    }
}

function setupDefaultTeamSelection(tab) {
    const tState = state[tab];
    // By default, select first 11 members if available
    const initial = tState.squad.slice(0, 11);
    tState.selectedUserIds = initial.map(m => m.userId);

    // Auto-detect captain
    const cap = tState.squad.find(m => m.role === 'CAPTAIN' || m.role === 'OWNER');
    if (cap) {
        tState.captainUserId = cap.userId;
        if (!tState.selectedUserIds.includes(cap.userId)) {
            tState.selectedUserIds.unshift(cap.userId);
        }
    }

    // Auto-detect wicket keeper
    const wk = tState.squad.find(m => m.playingRole === 'WICKET_KEEPER');
    if (wk) tState.wicketKeeperUserId = wk.userId;
}

function renderMatchHeader(match) {
    if (!match) return;

    const teamA = match.teamA || {};
    const teamB = match.teamB || {};

    document.getElementById('teams-title').textContent = `${teamA.name || 'Team A'} vs ${teamB.name || 'Team B'}`;
    document.getElementById('match-id-badge').textContent = match.matchId || currentMatchId;

    const statusBadge = document.getElementById('match-status-badge');
    if (statusBadge) {
        statusBadge.textContent = match.status || 'SCHEDULED';
    }

    const dateStr = match.matchDate ? formatDate(match.matchDate) : '';
    const timeStr = match.matchTime ? formatTime(match.matchTime) : '';
    const venueStr = match.venue || 'TBD';
    const formatStr = `${match.format || 'T20'} • ${match.overs || 20} Overs`;

    document.getElementById('match-info-bar').textContent = `${teamA.name} vs ${teamB.name} | ${dateStr} | ${timeStr} | ${venueStr} | ${formatStr}`;

    document.getElementById('tab-team-a').textContent = teamA.name || 'Team A';
    document.getElementById('tab-team-b').textContent = teamB.name || 'Team B';
}

function switchTeamTab(tab) {
    currentTab = tab;
    hideAlert();
    updateTabsUI();
    renderActiveTeamView();
}

function updateTabsUI() {
    const btnA = document.getElementById('tab-team-a');
    const btnB = document.getElementById('tab-team-b');

    if (currentTab === 'A') {
        btnA.classList.add('active');
        btnB.classList.remove('active');
    } else {
        btnB.classList.add('active');
        btnA.classList.remove('active');
    }

    const activeTeam = state[currentTab].team;
    const teamName = activeTeam ? activeTeam.name : `Team ${currentTab}`;
    document.getElementById('active-team-name').textContent = teamName;

    // Captain / Owner Authorization check
    const isCap = state[currentTab].isCaptainOrOwner;
    const readOnlyBanner = document.getElementById('read-only-banner');
    const readOnlyTitle = document.getElementById('read-only-title');
    const readOnlyText = document.getElementById('read-only-text');
    const saveBtn = document.getElementById('btn-save-xi');

    if (!isCap) {
        if (readOnlyBanner) {
            readOnlyBanner.style.display = 'flex';
            if (readOnlyTitle) readOnlyTitle.textContent = `Read-Only Mode (${teamName})`;
            if (readOnlyText) readOnlyText.textContent = `You are viewing ${teamName}'s Playing XI in read-only mode. Only ${teamName}'s Captain or Owner can select and save their Playing XI.`;
        }
        if (saveBtn) {
            saveBtn.disabled = true;
            saveBtn.style.opacity = '0.5';
            saveBtn.style.cursor = 'not-allowed';
            saveBtn.style.background = '#475569';
            saveBtn.textContent = 'Read-Only (Captain Access Required)';
            saveBtn.title = `Only Captain or Owner of ${teamName} can save Playing XI.`;
        }
    } else {
        if (readOnlyBanner) {
            readOnlyBanner.style.display = 'none';
        }
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.style.opacity = '1';
            saveBtn.style.cursor = 'pointer';
            saveBtn.style.background = '#10b981';
            saveBtn.textContent = 'Save Playing XI';
            saveBtn.title = `Save Playing XI for ${teamName}`;
        }
    }
}

function renderActiveTeamView() {
    renderAvailablePlayers();
    renderSelectedXi();
}

function filterAvailablePlayers() {
    renderAvailablePlayers();
}

function renderAvailablePlayers() {
    const container = document.getElementById('available-players-list');
    if (!container) return;

    const tState = state[currentTab];
    const squad = tState.squad || [];
    const selectedSet = new Set(tState.selectedUserIds);
    const searchVal = (document.getElementById('player-search').value || '').trim().toLowerCase();

    // Check opponent team's selected players to detect cross-team duplicates
    const opponentTab = currentTab === 'A' ? 'B' : 'A';
    const opponentState = state[opponentTab];
    const opponentSelectedSet = new Set(opponentState ? opponentState.selectedUserIds : []);
    const opponentTeamName = opponentState && opponentState.team ? opponentState.team.name : 'Opponent Team';

    const canEdit = tState.isCaptainOrOwner;

    // Filter available players (squad members not currently in selected Playing XI)
    const available = squad.filter(m => {
        if (selectedSet.has(m.userId)) return false;
        if (searchVal) {
            const nameMatch = (m.name || '').toLowerCase().includes(searchVal);
            const idMatch = (m.userId || '').toLowerCase().includes(searchVal);
            return nameMatch || idMatch;
        }
        return true;
    });

    if (available.length === 0) {
        container.innerHTML = `
            <div style="padding: 1.5rem 1rem; text-align: center; color: #94a3b8; font-size: 0.88rem;">
                ${squad.length === 0 ? 'No squad members found for this team.' : 'No more available players in squad.'}
            </div>
        `;
        return;
    }

    container.innerHTML = available.map(p => {
        const photoUrl = ApiService.getImageUrl(p.profilePhotoUrl);
        const name = escapeHtml(p.name);
        const userId = escapeHtml(p.userId);
        const playingRole = formatEnum(p.playingRole) || 'Player';
        const battingStyle = formatEnum(p.battingStyle) || 'Right-Handed';
        const bowlingStyle = formatEnum(p.bowlingStyle);

        // Check if player is already selected by opponent team!
        const isSelectedByOpponent = opponentSelectedSet.has(p.userId);

        let actionButtonHtml = '';
        if (isSelectedByOpponent) {
            actionButtonHtml = `
                <button class="btn-add-player" disabled style="opacity: 0.6; cursor: not-allowed; background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.4);" onclick="showAlert('Player ${name} (${userId}) is already selected in opponent team (${escapeHtml(opponentTeamName)})\'s Playing XI.')">
                    🔒 Locked
                </button>
            `;
        } else if (canEdit) {
            actionButtonHtml = `
                <button class="btn-add-player" onclick="addPlayerToXi('${userId}')">
                    [ Add ]
                </button>
            `;
        } else {
            actionButtonHtml = `<span style="font-size: 0.75rem; color: #64748b; font-weight: 600;">Read-Only</span>`;
        }

        return `
            <div class="player-item-card" style="${isSelectedByOpponent ? 'background: rgba(239, 68, 68, 0.05); border-color: rgba(239, 68, 68, 0.2);' : ''}">
                <div style="display: flex; align-items: center; gap: 0.75rem; min-width: 0;">
                    <div class="player-avatar" style="${isSelectedByOpponent ? 'border-color: #ef4444;' : ''}">
                        ${photoUrl ? `<img src="${photoUrl}" alt="${name}">` : `<span style="font-size:1.1rem;">👤</span>`}
                    </div>
                    <div style="min-width: 0;">
                        <div style="display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap;">
                            <strong style="color: #ffffff; font-size: 0.9rem;">${name}</strong>
                            ${isSelectedByOpponent ? `<span style="background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.4); font-size: 0.62rem; font-weight: 800; padding: 0.1rem 0.35rem; border-radius: 4px;">🔒 SELECTED BY OPPONENT</span>` : ''}
                        </div>
                        <div style="font-size: 0.75rem; color: #10b981; font-weight: 700;">${userId}</div>
                        <div style="font-size: 0.72rem; color: #94a3b8; margin-top: 0.1rem;">
                            ${playingRole} &bull; ${battingStyle}${bowlingStyle ? ' &bull; ' + bowlingStyle : ''}
                        </div>
                        ${isSelectedByOpponent ? `<div style="font-size: 0.7rem; color: #f87171; margin-top: 0.15rem; font-weight: 600;">Player already in opponent team (${escapeHtml(opponentTeamName)}) Playing XI</div>` : ''}
                    </div>
                </div>
                ${actionButtonHtml}
            </div>
        `;
    }).join('');
}

function renderSelectedXi() {
    const tState = state[currentTab];
    const squadMap = new Map(tState.squad.map(m => [m.userId, m]));
    const selectedList = tState.selectedUserIds.map(id => squadMap.get(id)).filter(Boolean);

    const count = selectedList.length;

    // Update Header Counter Badge
    document.getElementById('selected-count-badge').textContent = `${count} / 11 Selected`;

    // Update Guidance Box
    const statusBox = document.getElementById('count-status-box');
    const guidanceText = document.getElementById('count-guidance-text');
    if (statusBox) statusBox.textContent = `${count} / 11 players selected`;

    if (count === 11) {
        if (guidanceText) {
            guidanceText.textContent = '✓ Playing XI team complete (11/11)! Ready to save.';
            guidanceText.style.color = '#10b981';
        }
    } else if (count < 11) {
        const needed = 11 - count;
        if (guidanceText) {
            guidanceText.textContent = `Select ${needed} more player${needed > 1 ? 's' : ''} to complete the Playing XI.`;
            guidanceText.style.color = '#f59e0b';
        }
    } else {
        if (guidanceText) {
            guidanceText.textContent = `Too many players selected (${count}/11). Please remove ${count - 11}.`;
            guidanceText.style.color = '#ef4444';
        }
    }

    // Categorize Selected Players
    const captains = [];
    const keepers = [];
    const batters = [];
    const allRounders = [];
    const bowlers = [];
    const others = [];

    selectedList.forEach(p => {
        const isCap = p.userId === tState.captainUserId || p.role === 'CAPTAIN' || p.role === 'OWNER';
        const isWk = p.userId === tState.wicketKeeperUserId || p.playingRole === 'WICKET_KEEPER';

        if (isCap) {
            captains.push(p);
        } else if (isWk) {
            keepers.push(p);
        } else if (p.playingRole === 'BATTER') {
            batters.push(p);
        } else if (p.playingRole === 'ALL_ROUNDER') {
            allRounders.push(p);
        } else if (p.playingRole === 'BOWLER') {
            bowlers.push(p);
        } else {
            others.push(p);
        }
    });

    renderSelectedCategoryList('cat-captain-list', captains, true, false);
    renderSelectedCategoryList('cat-keeper-list', keepers, false, true);
    renderSelectedCategoryList('cat-batters-list', batters, false, false);
    renderSelectedCategoryList('cat-allrounders-list', allRounders, false, false);
    renderSelectedCategoryList('cat-bowlers-list', bowlers, false, false);

    const secOthers = document.getElementById('sec-others');
    if (secOthers) {
        if (others.length > 0) {
            secOthers.style.display = 'block';
            renderSelectedCategoryList('cat-others-list', others, false, false);
        } else {
            secOthers.style.display = 'none';
        }
    }
}

function renderSelectedCategoryList(containerId, players, isCaptainGroup, isKeeperGroup) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!players || players.length === 0) {
        const placeholder = isCaptainGroup ? 'No captain assigned' :
                            isKeeperGroup ? 'No wicketkeeper selected' : 'None added';
        container.innerHTML = `<p style="font-size:0.8rem; color:#94a3b8; margin: 0.2rem 0 0.5rem 0;">${placeholder}</p>`;
        return;
    }

    const tState = state[currentTab];
    const canEdit = tState.isCaptainOrOwner;

    container.innerHTML = players.map(p => {
        const photoUrl = ApiService.getImageUrl(p.profilePhotoUrl);
        const name = escapeHtml(p.name);
        const userId = escapeHtml(p.userId);
        const isCap = isCaptainGroup || p.userId === tState.captainUserId || p.role === 'CAPTAIN' || p.role === 'OWNER';
        const isWk = p.userId === tState.wicketKeeperUserId;
        const playingRole = formatEnum(p.playingRole) || 'Player';

        let actionControls = '';
        if (canEdit) {
            if (isCap) {
                // CAPTAIN CANNOT BE REMOVED! Show Locked Captain badge instead of Remove button.
                actionControls = `
                    <span style="font-size: 0.72rem; color: #f59e0b; font-weight: 800; padding: 0.25rem 0.55rem; background: rgba(245, 158, 11, 0.15); border-radius: 6px; border: 1px solid rgba(245, 158, 11, 0.3);" title="Captain is required and cannot be removed">
                        🔒 Captain (Required)
                    </span>
                `;
            } else {
                actionControls = `
                    ${!isWk ? `<button class="btn-wk-toggle" onclick="toggleWicketKeeper('${userId}')" title="Make Wicket Keeper">Make WK</button>` : ''}
                    <button class="btn-remove-player" onclick="removePlayerFromXi('${userId}')">
                        [ Remove ]
                    </button>
                `;
            }
        } else {
            actionControls = isCap ? `<span style="font-size: 0.72rem; color: #f59e0b; font-weight: 800;">CAPTAIN</span>` :
                             isWk ? `<span style="font-size: 0.72rem; color: #3b82f6; font-weight: 800;">WK</span>` : '';
        }

        return `
            <div class="player-item-card" style="background: rgba(16, 185, 129, 0.08); border-color: rgba(16, 185, 129, 0.25);">
                <div style="display: flex; align-items: center; gap: 0.75rem; min-width: 0;">
                    <div class="player-avatar">
                        ${photoUrl ? `<img src="${photoUrl}" alt="${name}">` : `<span style="font-size:1.1rem;">👤</span>`}
                    </div>
                    <div style="min-width: 0;">
                        <div style="display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap;">
                            <strong style="color: #ffffff; font-size: 0.9rem;">${name}</strong>
                            ${isCap ? `<span style="background: #f59e0b; color: #000; font-size: 0.62rem; font-weight: 800; padding: 0.1rem 0.35rem; border-radius: 4px;">CAPTAIN</span>` : ''}
                            ${isWk ? `<span style="background: #3b82f6; color: #fff; font-size: 0.62rem; font-weight: 800; padding: 0.1rem 0.35rem; border-radius: 4px;">WK</span>` : ''}
                        </div>
                        <div style="font-size: 0.75rem; color: #10b981; font-weight: 700;">${userId}</div>
                        <div style="font-size: 0.72rem; color: #cbd5e1;">${playingRole}</div>
                    </div>
                </div>

                <div style="display: flex; align-items: center; gap: 0.4rem;">
                    ${actionControls}
                </div>
            </div>
        `;
    }).join('');
}

function addPlayerToXi(userId) {
    const tState = state[currentTab];
    if (!tState.isCaptainOrOwner) {
        showAlert(`Only Captain or Owner of ${tState.team ? tState.team.name : 'this team'} can add players.`);
        return;
    }

    // Check if player is selected in opponent team's Playing XI
    const opponentTab = currentTab === 'A' ? 'B' : 'A';
    const opponentState = state[opponentTab];
    if (opponentState && opponentState.selectedUserIds.includes(userId)) {
        const oppName = opponentState.team ? opponentState.team.name : 'opponent team';
        showAlert(`Player ${userId} is already selected in the opponent team (${oppName}) Playing XI and cannot be selected.`);
        return;
    }

    if (tState.selectedUserIds.length >= 11) {
        showAlert('Maximum 11 players can be selected for the Playing XI.');
        return;
    }

    if (!tState.selectedUserIds.includes(userId)) {
        tState.selectedUserIds.push(userId);
        renderActiveTeamView();
    }
}

function removePlayerFromXi(userId) {
    const tState = state[currentTab];
    if (!tState.isCaptainOrOwner) {
        showAlert(`Only Captain or Owner of ${tState.team ? tState.team.name : 'this team'} can remove players.`);
        return;
    }

    // RULE: Captain CANNOT be removed!
    if (userId === tState.captainUserId) {
        showAlert('Team Captain cannot be removed from the Playing XI.');
        return;
    }

    const squadMember = tState.squad.find(m => m.userId === userId);
    if (squadMember && (squadMember.role === 'CAPTAIN' || squadMember.role === 'OWNER')) {
        showAlert('Team Captain or Owner cannot be removed from the Playing XI.');
        return;
    }

    tState.selectedUserIds = tState.selectedUserIds.filter(id => id !== userId);
    if (tState.wicketKeeperUserId === userId) {
        tState.wicketKeeperUserId = null;
    }
    renderActiveTeamView();
}

function toggleWicketKeeper(userId) {
    const tState = state[currentTab];
    if (!tState.isCaptainOrOwner) return;

    if (tState.wicketKeeperUserId === userId) {
        tState.wicketKeeperUserId = null;
    } else {
        tState.wicketKeeperUserId = userId;
    }
    renderActiveTeamView();
}

async function savePlayingXiSelection() {
    hideAlert();
    const saveMsg = document.getElementById('save-status-msg');
    if (saveMsg) saveMsg.style.display = 'none';

    const tState = state[currentTab];
    const team = tState.team;

    if (!tState.isCaptainOrOwner) {
        showAlert(`Only Captain or Owner of ${team ? team.name : 'this team'} can save Playing XI.`);
        return;
    }

    if (tState.selectedUserIds.length === 0) {
        showAlert('Please select players to form the Playing XI.');
        return;
    }

    if (tState.selectedUserIds.length > 11) {
        showAlert('Playing XI cannot exceed 11 players.');
        return;
    }

    // Ensure Captain is included in selectedUserIds
    if (tState.captainUserId && !tState.selectedUserIds.includes(tState.captainUserId)) {
        tState.selectedUserIds.unshift(tState.captainUserId);
    }

    const payload = {
        playerUserIds: tState.selectedUserIds,
        wicketKeeperUserId: tState.wicketKeeperUserId,
        captainUserId: tState.captainUserId
    };

    try {
        const response = await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/teams/${encodeURIComponent(team.teamId)}/playing-xi`, payload);
        
        if (saveMsg) {
            saveMsg.style.display = 'block';
            saveMsg.textContent = `✓ Playing XI for ${team.name} saved successfully!`;
        }

        // Re-fetch saved state
        const playingXiAll = await ApiService.get(`/matches/${encodeURIComponent(currentMatchId)}/playing-xi`).catch(() => null);
        if (playingXiAll && playingXiAll[currentTab === 'A' ? 'teamA' : 'teamB']) {
            setupTeamSelection(currentTab, playingXiAll[currentTab === 'A' ? 'teamA' : 'teamB']);
            renderActiveTeamView();
        }

    } catch (err) {
        console.error('Failed to save Playing XI:', err);
        showAlert(err.message || 'Failed to save Playing XI selection.');
    }
}

function goBackToMatchDetails() {
    if (currentMatchId) {
        window.location.href = `match-details.html?id=${encodeURIComponent(currentMatchId)}`;
    } else {
        window.location.href = 'matches.html';
    }
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-US', { day: 'numeric', month: 'long', year: 'numeric' });
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
        hours = hours % 12 || 12;
        return `${hours}:${minutes} ${ampm}`;
    } catch (e) {
        return timeStr;
    }
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

function showAlert(message) {
    const el = document.getElementById('select-xi-alert');
    if (el) {
        el.textContent = message;
        el.style.display = 'block';
    }
}

function hideAlert() {
    const el = document.getElementById('select-xi-alert');
    if (el) el.style.display = 'none';
}
