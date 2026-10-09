/**
 * Public Scorecard Logic
 */

const API_BASE_URL = 'https://cricket-app-production-9e11.up.railway.app/api';
let currentMatchId = null;
let state = null; // Store the DTO globally for the view

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    currentMatchId = urlParams.get('matchId');

    if (!currentMatchId) {
        showError("No Match ID provided in the URL.");
        return;
    }

    // Since this is a public page, we don't need authentication.
    // Fetch directly from the public endpoint.
    fetchScorecardData();
});

async function fetchScorecardData() {
    try {
        const response = await fetch(`${API_BASE_URL}/public/matches/${encodeURIComponent(currentMatchId)}/scorecard`);
        
        if (!response.ok) {
            if (response.status === 409) {
                throw new Error("This match is not yet completed. Scorecards are only public for finished matches.");
            } else if (response.status === 404) {
                throw new Error("Match not found.");
            }
            throw new Error(`Failed to load scorecard (Status: ${response.status})`);
        }
        
        state = await response.json();
        renderScorecard();
        
    } catch (error) {
        console.error("Scorecard Error:", error);
        showError(error.message);
    }
}

function showError(message) {
    document.getElementById('loader').style.display = 'none';
    document.getElementById('scorecard-content').style.display = 'none';
    const errorState = document.getElementById('error-state');
    errorState.style.display = 'block';
    if (message) {
        document.getElementById('error-message').textContent = message;
    }
}

function renderScorecard() {
    // Hide loader and show content
    document.getElementById('loader').style.display = 'none';
    document.getElementById('scorecard-content').style.display = 'block';
    
    // Show top share button
    document.getElementById('btn-share-top').style.display = 'inline-flex';

    // Populate Banner
    document.getElementById('match-title').textContent = state.matchName || `${state.teamA?.name || 'Team A'} vs ${state.teamB?.name || 'Team B'}`;
    document.getElementById('match-result').textContent = state.matchResultSummary || 'Match Completed';

    // Populate Info Grid
    document.getElementById('info-venue').textContent = state.venue || 'N/A';
    
    const dateStr = state.matchDate ? state.matchDate : '';
    const timeStr = state.matchTime ? state.matchTime : '';
    document.getElementById('info-datetime').textContent = (dateStr || timeStr) ? `${dateStr} ${timeStr}`.trim() : 'N/A';
    
    let formatStr = state.format || 'T20';
    if (state.overs) formatStr += ` (${state.overs} Overs)`;
    document.getElementById('info-format').textContent = formatStr;
    
    document.getElementById('info-toss').textContent = state.tossSummary || 'N/A';

    // Build Innings Data
    const inn1 = state.innings1Scorecard;
    const inn2 = state.innings2Scorecard;

    if (inn1) {
        document.getElementById('innings-tabs').style.display = 'flex';
        document.getElementById('tab-inn-1').textContent = `${inn1.battingTeamName || '1st Innings'}`;
        document.getElementById('innings-1-container').innerHTML = buildInningsHtml(inn1);
    }
    
    if (inn2) {
        document.getElementById('tab-inn-2').textContent = `${inn2.battingTeamName || '2nd Innings'}`;
        document.getElementById('innings-2-container').innerHTML = buildInningsHtml(inn2);
    } else {
        // Only one innings exists (maybe shortened match or forfeit)
        document.getElementById('tab-inn-2').style.display = 'none';
    }
}

function buildInningsHtml(inn) {
    if (!inn) return '';
    
    return `
        <!-- Batting Table -->
        <div class="sc-card">
            <div class="sc-card-header">
                <h3 class="sc-card-title">BATTING — ${inn.battingTeamName || 'TEAM'}</h3>
                <div style="font-family: 'Outfit', sans-serif; font-size: 1.1rem; font-weight: 800; color: #10b981;">
                    ${inn.totalRuns || 0}/${inn.totalWickets || 0} <span style="font-size: 0.8rem; color: #94a3b8; font-weight: 500;">(${formatOvers(inn.completedOvers, inn.currentBalls)} ov)</span>
                </div>
            </div>
            <div class="sc-table-wrap">
                <table class="sc-table">
                    <thead>
                        <tr>
                            <th>BATTER</th>
                            <th>R</th>
                            <th>B</th>
                            <th>4s</th>
                            <th>6s</th>
                            <th>SR</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${inn.battingList && inn.battingList.length > 0 ? inn.battingList.map(b => `
                            <tr>
                                <td>
                                    <div class="player-name">${b.name || 'Unknown'} ${b.isWicketKeeper ? '🧤' : ''}</div>
                                    <div class="player-desc">${getDismissalText(b)}</div>
                                </td>
                                <td class="stat-strong">${b.runs || 0}</td>
                                <td>${b.balls || 0}</td>
                                <td>${b.fours || 0}</td>
                                <td>${b.sixes || 0}</td>
                                <td>${((b.runs || 0) / (b.balls || 1) * 100).toFixed(2)}</td>
                            </tr>
                        `).join('') : '<tr><td colspan="6" style="text-align:center; color:#64748b;">No batting data</td></tr>'}
                    </tbody>
                </table>
            </div>
            <div style="padding: 1rem 1.5rem; background: rgba(255,255,255,0.02); border-top: 1px solid rgba(255,255,255,0.05); display: flex; justify-content: space-between; align-items: center;">
                <span style="font-size: 0.85rem; color: #94a3b8; font-weight: 700;">EXTRAS</span>
                <span style="font-weight: 700; color: #f59e0b;">${inn.totalExtras || 0}</span>
            </div>
        </div>

        <!-- Bowling Table -->
        <div class="sc-card">
            <div class="sc-card-header">
                <h3 class="sc-card-title" style="color: #f59e0b;">BOWLING — ${inn.bowlingTeamName || 'OPPONENT'}</h3>
            </div>
            <div class="sc-table-wrap">
                <table class="sc-table">
                    <thead>
                        <tr>
                            <th>BOWLER</th>
                            <th>O</th>
                            <th>M</th>
                            <th>R</th>
                            <th>W</th>
                            <th>ECON</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${inn.bowlingList && inn.bowlingList.length > 0 ? inn.bowlingList.map(b => `
                            <tr>
                                <td><div class="player-name">${b.name || 'Unknown'}</div></td>
                                <td class="stat-strong">${formatOvers(b.oversBowled, b.ballsBowled)}</td>
                                <td>${b.maidens || 0}</td>
                                <td>${b.runsConceded || 0}</td>
                                <td class="stat-strong" style="color: #ef4444;">${b.wicketsTaken || 0}</td>
                                <td>${b.economyRate ? b.economyRate.toFixed(2) : '0.00'}</td>
                            </tr>
                        `).join('') : '<tr><td colspan="6" style="text-align:center; color:#64748b;">No bowling data</td></tr>'}
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Fall of Wickets -->
        <div class="sc-card">
            <div class="sc-card-header">
                <h3 class="sc-card-title" style="color: #ef4444;">FALL OF WICKETS</h3>
            </div>
            <div style="padding: 1.25rem 1.5rem; display: flex; flex-wrap: wrap; gap: 0.75rem;">
                ${inn.fallOfWickets && inn.fallOfWickets.length > 0 ? inn.fallOfWickets.map(fow => `
                    <div style="background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.2); padding: 0.5rem 0.75rem; border-radius: 8px;">
                        <span style="color: #fca5a5; font-size: 0.8rem;">${fow.teamRuns || 0}-${fow.wicketNumber || 0}</span>
                        <div style="color: #fff; font-weight: 600; font-size: 0.85rem; margin-top: 0.2rem;">${fow.playerName || 'Unknown'}</div>
                        <div style="color: #94a3b8; font-size: 0.75rem;">${fow.overBall || '0.0'} ov</div>
                    </div>
                `).join('') : '<span style="color:#64748b; font-size: 0.85rem;">No wickets fallen</span>'}
            </div>
        </div>
    `;
}

function formatOvers(fullOvers, extraBalls) {
    let o = fullOvers || 0;
    let b = extraBalls || 0;
    while (b >= 6) {
        o++;
        b -= 6;
    }
    return `${o}.${b}`;
}

function getDismissalText(player) {
    if (!player.hasBatted && !player.isOut) return 'Did not bat';
    if (!player.isOut) return '<span style="color: #10b981; font-weight: 600;">Not Out</span>';
    return player.dismissalText || 'Out';
}

function switchInnings(innNum) {
    if (innNum === 1) {
        document.getElementById('innings-1-container').style.display = 'block';
        document.getElementById('innings-2-container').style.display = 'none';
        document.getElementById('tab-inn-1').classList.add('active');
        document.getElementById('tab-inn-2').classList.remove('active');
    } else {
        document.getElementById('innings-1-container').style.display = 'none';
        document.getElementById('innings-2-container').style.display = 'block';
        document.getElementById('tab-inn-1').classList.remove('active');
        document.getElementById('tab-inn-2').classList.add('active');
    }
}
