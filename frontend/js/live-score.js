/**
 * Cricket App - Live Match Viewer (Stage 10)
 * Real-Time WebSocket live score viewer for fans/users
 */

let stompClient = null;
let currentMatchId = null;
let reconnectTimer = null;
let currentMatchView = 'live'; // 'live' | 'scoreboard'
let selectedScoreboardInnings = null; // 1 | 2 (null = auto-select active innings)
let lastMatchState = null;
let expandedBatterIds = new Set();

// Cross-tab zero-latency communication channel
const liveChannel = (typeof BroadcastChannel !== 'undefined') ? new BroadcastChannel('cricket_live_channel') : null;
if (liveChannel) {
    liveChannel.onmessage = (event) => {
        if (!event.data) return;
        if (event.data.matchId && currentMatchId && String(event.data.matchId) !== String(currentMatchId)) return;
        if (event.data.type === 'BALL_IN_PROGRESS') {
            triggerBallDeliveryAnimation(event.data);
        } else if (event.data.type === 'CELEBRATION') {
            if (window.CelebrationManager) {
                window.CelebrationManager.trigger(event.data.event, event.data.celebrationId, currentMatchId);
            }
        } else if (event.data.type === 'MATCH_UPDATE' && event.data.state) {
            renderLiveState(event.data.state, true);
        }
    };
}

document.addEventListener('DOMContentLoaded', () => {
    initLiveMatchViewer();
});

async function initLiveMatchViewer() {
    const urlParams = new URLSearchParams(window.location.search);
    currentMatchId = urlParams.get('matchId') || urlParams.get('id');

    if (!currentMatchId) {
        showErrorAlert('No match ID provided in URL parameters.');
        setConnectionStatus('disconnected', 'NO MATCH ID');
        return;
    }

    setConnectionStatus('connecting', 'Connecting...');

    try {
        // Fetch initial state via REST API endpoint (support both /live and /scoring)
        let state = null;
        try {
            state = await ApiService.get(`/matches/${encodeURIComponent(currentMatchId)}/live`);
        } catch (fetchErr) {
            console.warn('GET /live failed, attempting /scoring fallback:', fetchErr);
            state = await ApiService.get(`/matches/${encodeURIComponent(currentMatchId)}/scoring`);
        }
        
        if (!state) {
            showErrorAlert('Match live data not found.');
            setConnectionStatus('disconnected', 'OFFLINE');
            return;
        }

        renderLiveState(state);
        if (!state.isMatchCompleted && state.status !== 'COMPLETED') {
            connectWebSocket();
        }

    } catch (err) {
        console.error('Failed to load initial live match state:', err);
        showErrorAlert(err.message || 'Unable to connect to live match stream.');
        setConnectionStatus('disconnected', 'OFFLINE');
    }
}

function setConnectionStatus(status, label) {
    const badge = document.getElementById('ls-conn-badge');
    const dot = document.getElementById('ls-conn-dot');
    const text = document.getElementById('ls-conn-text');

    if (!badge || !dot || !text) return;

    badge.className = `ls-conn-badge ${status}`;
    text.textContent = label;

    if (status === 'connected') {
        dot.className = 'ls-dot green';
    } else if (status === 'completed') {
        dot.className = 'ls-dot completed-dot';
    } else if (status === 'connecting') {
        dot.className = 'ls-dot amber';
    } else {
        dot.className = 'ls-dot red';
    }
}

function connectWebSocket() {
    if (!currentMatchId) return;

    if (stompClient !== null) {
        try { stompClient.disconnect(); } catch (e) {}
    }

    setConnectionStatus('connecting', 'Connecting...');

    const wsUrl = typeof BACKEND_BASE_URL !== 'undefined' ? `${BACKEND_BASE_URL}/ws-cricket` : 'https://cricket-app-production-9e11.up.railway.app/ws-cricket';
    const socket = new SockJS(wsUrl);
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Suppress verbose console logs

    stompClient.connect({}, (frame) => {
        setConnectionStatus('connected', 'LIVE');
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }

        const topic = `/topic/matches/${currentMatchId}/live`;
        stompClient.subscribe(topic, (message) => {
            if (message && message.body) {
                try {
                    const data = JSON.parse(message.body);
                    if (data && data.type === 'BALL_IN_PROGRESS') {
                        triggerBallDeliveryAnimation(data);
                    } else if (data && data.type === 'CELEBRATION') {
                        if (window.CelebrationManager) {
                            window.CelebrationManager.trigger(data.event, data.celebrationId, currentMatchId);
                        }
                    } else {
                        renderLiveState(data, true);
                    }
                } catch (e) {
                    console.error('Failed to parse live STOMP message:', e);
                }
            }
        });
    }, (error) => {
        console.warn('STOMP WebSocket Connection Error:', error);
        setConnectionStatus('disconnected', 'RECONNECTING...');
        
        // Auto-reconnect after 4 seconds
        if (!reconnectTimer) {
            reconnectTimer = setTimeout(() => {
                reconnectTimer = null;
                connectWebSocket();
            }, 4000);
        }
    });
}

function renderLiveState(state, isWebSocketUpdate = false) {
    if (!state) return;
    lastMatchState = state;

    // Handle Celebration event from state update (with deduplication)
    if (!isWebSocketUpdate && state.latestCelebrationId && window.CelebrationManager) {
        // Initial load / manual refresh: mark as processed to prevent replaying past celebrations
        window.CelebrationManager.markProcessed(state.latestCelebrationId);
    } else if (isWebSocketUpdate && state.latestCelebrationType && state.latestCelebrationId && window.CelebrationManager) {
        window.CelebrationManager.trigger(state.latestCelebrationType, state.latestCelebrationId, currentMatchId);
    }

    const isPaused = (state.status === 'PAUSED' || state.isPaused === true);
    const isInningsBreak = (state.status === 'INNINGS_BREAK' || state.isInningsBreak === true);
    const isMatchCompleted = (state.status === 'COMPLETED' || state.isMatchCompleted === true);

    // Update connection status label based on match state
    if (isMatchCompleted) {
        setConnectionStatus('completed', '🏆 MATCH OVER');
        if (stompClient !== null) {
            try { stompClient.disconnect(); } catch (e) {}
            stompClient = null;
        }
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }
    } else if (isPaused) {
        setConnectionStatus('connecting', '⏸️ PAUSED');
    } else if (isInningsBreak) {
        setConnectionStatus('connecting', '☕ INNINGS BREAK');
    } else {
        setConnectionStatus('connected', 'LIVE');
    }

    // Header & Meta Details
    const matchName = state.matchName || 'Cricket Match';
    const formatStr = (state.format || 'T20') + ' • ' + (state.overs || 20) + ' OVERS';
    const tossSummary = state.tossSummary || 'Toss not yet recorded';

    if (document.getElementById('ls-match-format')) document.getElementById('ls-match-format').textContent = formatStr;
    if (document.getElementById('ls-toss-summary')) document.getElementById('ls-toss-summary').textContent = '🪙 ' + tossSummary;

    // Teams
    const teamAName = (state.teamA && state.teamA.name) || state.battingTeamName || 'Team A';
    const teamBName = (state.teamB && state.teamB.name) || state.bowlingTeamName || 'Team B';

    if (document.getElementById('ls-team-a-name')) document.getElementById('ls-team-a-name').textContent = teamAName;
    if (document.getElementById('ls-team-b-name')) document.getElementById('ls-team-b-name').textContent = teamBName;

    // Logos
    if (state.battingTeamLogoUrl) {
        const logoA = ApiService.getImageUrl(state.battingTeamLogoUrl);
        const imgA = document.getElementById('ls-team-a-logo');
        const fbA = document.getElementById('ls-team-a-fallback');
        if (imgA && logoA) {
            imgA.src = logoA;
            imgA.style.display = 'block';
            if (fbA) fbA.style.display = 'none';
        }
    }
    if (state.bowlingTeamLogoUrl) {
        const logoB = ApiService.getImageUrl(state.bowlingTeamLogoUrl);
        const imgB = document.getElementById('ls-team-b-logo');
        const fbB = document.getElementById('ls-team-b-fallback');
        if (imgB && logoB) {
            imgB.src = logoB;
            imgB.style.display = 'block';
            if (fbB) fbB.style.display = 'none';
        }
    }

    // Score & Overs
    const totalRuns = state.totalRuns !== undefined ? state.totalRuns : 0;
    const totalWickets = state.totalWickets !== undefined ? state.totalWickets : 0;
    const completedOvers = state.completedOvers !== undefined ? state.completedOvers : 0;
    const currentBalls = state.currentBalls !== undefined ? state.currentBalls : 0;
    const maxOvers = state.overs || 20;

    if (document.getElementById('ls-score-text')) {
        document.getElementById('ls-score-text').innerHTML = `
            ${totalRuns} / ${totalWickets} 
            <span id="ls-overs-text">(${completedOvers}.${currentBalls} / ${maxOvers} Overs)</span>
        `;
    }

    // Toggle widgets: Live Widgets vs Completed Mode Widgets
    const liveWidgets = document.getElementById('ls-live-widgets');
    const completedWidgets = document.getElementById('ls-completed-widgets');

    if (isMatchCompleted) {
        if (liveWidgets) liveWidgets.style.display = 'none';
        if (completedWidgets) completedWidgets.style.display = 'block';

        // Update Completed Header Announcement
        const matchTitleEl = document.getElementById('ls-completed-match-title');
        if (matchTitleEl) {
            matchTitleEl.textContent = `${teamAName} vs ${teamBName}`;
        }
        const resultTextEl = document.getElementById('ls-completed-result-text');
        if (resultTextEl) {
            resultTextEl.textContent = state.matchResultSummary || state.matchResult || (state.winnerTeamName ? `${state.winnerTeamName} won the match` : 'Match Completed');
        }

        // Equation banner summary
        const eqBanner = document.getElementById('ls-equation-banner');
        if (eqBanner) {
            eqBanner.textContent = state.matchResultSummary || state.matchResult || '🏆 Match Completed';
            eqBanner.style.display = 'block';
            eqBanner.style.background = 'rgba(16, 185, 129, 0.2)';
            eqBanner.style.color = '#34d399';
        }

        // Hide CRR / RRR / Pause Banner / Innings Break
        const rrrBox = document.getElementById('ls-rrr-box');
        if (rrrBox) rrrBox.style.display = 'none';
        const pauseBanner = document.getElementById('ls-paused-banner');
        if (pauseBanner) pauseBanner.style.display = 'none';
        const breakCard = document.getElementById('ls-innings-break-card');
        if (breakCard) breakCard.style.display = 'none';

        // Render Completed Scorecards, Ball-by-ball history, and Match Info
        renderCompletedScorecards(state);
        renderCompletedHistoricalDeliveries(state);
        renderCompletedMatchInfo(state);
        renderScoreboardView(state);

        return; // Early return for completed matches: do not render live controls or pitch
    } else {
        if (liveWidgets) liveWidgets.style.display = 'block';
        if (completedWidgets) completedWidgets.style.display = 'none';
    }

    // CRR & RRR
    if (document.getElementById('ls-crr')) document.getElementById('ls-crr').textContent = (state.runRate !== undefined ? state.runRate : 0.0).toFixed(2);
    
    const activeInnNum = state.activeInningsNumber || state.currentInningsNumber || 1;
    const rrrBox = document.getElementById('ls-rrr-box');
    const rrrVal = document.getElementById('ls-rrr');
    if (rrrBox && rrrVal) {
        if (state.requiredRunRate !== undefined && state.requiredRunRate !== null && activeInnNum === 2 && !isInningsBreak && !isMatchCompleted) {
            rrrVal.textContent = state.requiredRunRate.toFixed(2);
            rrrBox.style.display = 'block';
        } else {
            rrrBox.style.display = 'none';
        }
    }

    // Match Paused Banner (Feature 3)
    const pauseBanner = document.getElementById('ls-paused-banner');
    if (pauseBanner) {
        if (isPaused) {
            pauseBanner.style.display = 'block';
            const pauseReasonEl = document.getElementById('ls-paused-reason');
            if (pauseReasonEl) {
                pauseReasonEl.textContent = `Reason: ${state.pauseReason || 'Match Paused'}`;
            }
        } else {
            pauseBanner.style.display = 'none';
        }
    }

    // Innings Break Card (Feature 1)
    const breakCard = document.getElementById('ls-innings-break-card');
    const stadiumCard = document.getElementById('ls-stadium-card');
    if (breakCard) {
        if (isInningsBreak && !isMatchCompleted) {
            breakCard.style.display = 'block';
            const breakTeam = state.firstInningsTeamName || state.battingTeamName || 'Team';
            const breakRuns = state.firstInningsRuns !== undefined ? state.firstInningsRuns : totalRuns;
            const breakWkts = state.firstInningsWickets !== undefined ? state.firstInningsWickets : totalWickets;
            const breakOvs = state.firstInningsOvers !== undefined ? state.firstInningsOvers : completedOvers;
            const breakBalls = state.firstInningsBalls !== undefined ? state.firstInningsBalls : currentBalls;
            const target = state.targetRuns || (breakRuns + 1);

            if (document.getElementById('ls-break-team-name')) document.getElementById('ls-break-team-name').textContent = breakTeam;
            if (document.getElementById('ls-break-score')) document.getElementById('ls-break-score').textContent = `${breakRuns}/${breakWkts}`;
            if (document.getElementById('ls-break-overs')) document.getElementById('ls-break-overs').textContent = `${breakOvs}${breakBalls ? '.' + breakBalls : ''} Overs`;
            if (document.getElementById('ls-break-target')) document.getElementById('ls-break-target').textContent = `Target: ${target} runs`;

            if (stadiumCard) stadiumCard.style.display = 'none';
        } else {
            breakCard.style.display = 'none';
            if (stadiumCard) stadiumCard.style.display = 'block';
        }
    }

    // Equation / Match Situation Banner (Feature 2 - Strictly 2nd Innings Only)
    const eqBanner = document.getElementById('ls-equation-banner');
    if (eqBanner) {
        if (isMatchCompleted) {
            eqBanner.textContent = state.matchResultSummary || state.matchResult || '🏆 Match Completed';
            eqBanner.style.display = 'block';
            eqBanner.style.background = 'rgba(16, 185, 129, 0.2)';
            eqBanner.style.color = '#34d399';
        } else if (activeInnNum === 2 && !isInningsBreak) {
            // ONLY during 2nd innings!
            const target = state.targetRuns || ((state.firstInningsRuns || 0) + 1);
            const reqRuns = state.requiredRuns !== undefined && state.requiredRuns !== null ? state.requiredRuns : (target - totalRuns);
            const totalMaxBalls = maxOvers * 6;
            const currentTotalBalls = (completedOvers * 6) + currentBalls;
            const remBalls = state.ballsRemaining !== undefined && state.ballsRemaining !== null ? state.ballsRemaining : Math.max(0, totalMaxBalls - currentTotalBalls);

            let equationText = state.targetEquation;
            if (!equationText || !equationText.includes('ball')) {
                if (reqRuns <= 0) {
                    equationText = `${teamAName} WON`;
                } else {
                    const runsWord = reqRuns === 1 ? 'run' : 'runs';
                    const ballsWord = remBalls === 1 ? 'ball' : 'balls';
                    equationText = `${teamAName} need ${reqRuns} ${runsWord} from ${remBalls} ${ballsWord} to win`;
                }
            }
            eqBanner.textContent = equationText;
            eqBanner.style.display = 'block';
            eqBanner.style.background = 'rgba(16, 185, 129, 0.15)';
            eqBanner.style.color = '#34d399';
        } else {
            // First innings or innings break -> target equation is NEVER shown!
            eqBanner.style.display = 'none';
        }
    }

    // Current Over Ball Pills
    const overBalls = (state.currentOverBalls && state.currentOverBalls.length > 0)
        ? state.currentOverBalls
        : (state.overSummaryBalls && state.overSummaryBalls.length > 0)
            ? state.overSummaryBalls
            : (state.thisOverBallEvents && state.thisOverBallEvents.length > 0)
                ? state.thisOverBallEvents
                : [];
    renderCurrentOverPills(overBalls, state);

    // Batters Table
    renderBatters(state.striker, state.nonStriker);

    // Bowler Table
    renderBowler(state.currentBowler);

    // Recent Overs List
    renderRecentOvers(state.recentOvers || []);

    // Live Commentary List
    renderCommentary(state.commentary || []);

    // Live Stadium Pitch Simulation
    renderStadiumPitchSimulation(state, isWebSocketUpdate);

    // Live Scoreboard Tab View Update (Stage 11)
    renderScoreboardView(state);
}

function triggerBallDeliveryAnimation(data) {
    const ballEl = document.getElementById('ls-sim-ball');
    const badgeEl = document.getElementById('ls-outcome-badge');
    const descEl = document.getElementById('ls-outcome-desc');
    const metaOver = document.getElementById('ls-sim-meta-over');
    const bowlerTag = document.getElementById('ls-sim-bowler-tag');

    if (data.bowlerName && document.getElementById('ls-sim-bowler-name')) {
        document.getElementById('ls-sim-bowler-name').textContent = data.bowlerName;
    }
    if (data.strikerName && document.getElementById('ls-sim-striker-name')) {
        document.getElementById('ls-sim-striker-name').textContent = data.strikerName + '*';
    }
    if (metaOver && data.overNumber !== undefined && data.ballNumber !== undefined) {
        metaOver.textContent = `Ball ${data.overNumber}.${data.ballNumber}`;
    }

    if (bowlerTag) {
        bowlerTag.style.borderColor = '#10b981';
        bowlerTag.style.boxShadow = '0 0 12px rgba(16, 185, 129, 0.6)';
        setTimeout(() => { if (bowlerTag) { bowlerTag.style.borderColor = ''; bowlerTag.style.boxShadow = ''; } }, 1200);
    }

    if (badgeEl) {
        badgeEl.className = 'ls-outcome-badge delivering';
        badgeEl.innerHTML = '⚡ BALL IN FLIGHT...';
    }
    if (descEl) {
        descEl.textContent = `${data.bowlerName || 'Bowler'} running in to deliver to ${data.strikerName || 'Striker'}...`;
    }

    if (ballEl) {
        // Reset instantly to bowler's crease
        ballEl.style.transition = 'none';
        ballEl.style.left = '5%';
        ballEl.style.transform = 'translateY(-50%) scale(0.9)';
        ballEl.style.boxShadow = '0 2px 6px rgba(0, 0, 0, 0.5)';
        ballEl.className = 'ls-sim-ball';
        void ballEl.offsetWidth; // Force synchronous layout reflow

        // Animate smooth delivery arc towards striker crease
        requestAnimationFrame(() => {
            ballEl.style.transition = 'left 0.75s cubic-bezier(0.25, 1, 0.5, 1), transform 0.75s ease';
            ballEl.style.left = '84%';
            ballEl.style.transform = 'translateY(-50%) scale(1)';
            ballEl.className = 'ls-sim-ball delivering';
        });
    }
}

function renderStadiumPitchSimulation(state, shouldAnimate = false) {
    if (!state) return;

    const bowlerName = state.currentBowler ? state.currentBowler.name : 'Bowler';
    const strikerName = state.striker ? state.striker.name : 'Striker';

    if (document.getElementById('ls-sim-bowler-name')) {
        document.getElementById('ls-sim-bowler-name').textContent = bowlerName;
    }
    if (document.getElementById('ls-sim-striker-name')) {
        document.getElementById('ls-sim-striker-name').textContent = strikerName + '*';
    }

    const completedOvers = state.completedOvers !== undefined ? state.completedOvers : 0;
    const currentBalls = state.currentBalls !== undefined ? state.currentBalls : 0;
    if (document.getElementById('ls-sim-meta-over')) {
        document.getElementById('ls-sim-meta-over').textContent = `Over ${completedOvers}.${currentBalls}`;
    }

    const badgeEl = document.getElementById('ls-outcome-badge');
    const descEl = document.getElementById('ls-outcome-desc');
    const ballEl = document.getElementById('ls-sim-ball');
    const stumpsEl = document.getElementById('ls-striker-stumps');
    const strikerTag = document.getElementById('ls-sim-striker-tag');

    // Find the latest ball delivery
    const commList = state.commentary || [];
    const latestComm = commList.length > 0 ? commList[0] : null;

    if (!latestComm) {
        if (badgeEl) {
            badgeEl.className = 'ls-outcome-badge dot';
            badgeEl.textContent = 'READY FOR NEXT BALL';
        }
        if (descEl) {
            descEl.textContent = 'Waiting for bowler to begin run-up...';
        }
        if (ballEl) {
            ballEl.style.transition = 'none';
            ballEl.style.left = '5%';
            ballEl.style.transform = 'translateY(-50%)';
            ballEl.style.boxShadow = '0 2px 6px rgba(0,0,0,0.5)';
            ballEl.className = 'ls-sim-ball';
        }
        return;
    }

    const titleUpper = (latestComm.title || latestComm.badgeText || '').toUpperCase();
    const overBall = latestComm.overBall || latestComm.overNumber || `${completedOvers}.${currentBalls}`;

    let badgeClass = 'dot';
    let badgeText = 'DOT BALL (0)';
    let outcomeDesc = latestComm.description || `${bowlerName} to ${strikerName}`;
    let ballPos = '84%';
    let ballTransform = 'translateY(-50%) scale(1)';
    let ballShadow = '0 2px 6px rgba(0,0,0,0.5)';
    let ballClass = '';

    if (titleUpper.includes('WICKET') || titleUpper.includes('OUT')) {
        badgeClass = 'wicket';
        badgeText = '💥 WICKET! OUT';
        ballPos = '86%';
        ballTransform = 'translateY(-50%) scale(1.25)';
        ballShadow = '0 0 25px #ef4444, 0 0 45px #dc2626';
        ballClass = 'wicket-hit';
        outcomeDesc = latestComm.description || `WICKET! ${strikerName} dismissed off ${bowlerName}!`;
        if (stumpsEl && shouldAnimate) {
            stumpsEl.style.animation = 'stumps-shatter 0.65s ease-out';
            setTimeout(() => { if (stumpsEl) stumpsEl.style.animation = ''; }, 700);
        }
    } else if (titleUpper.includes('SIX') || titleUpper === '6') {
        badgeClass = 'six';
        badgeText = '🚀 MAXIMUM SIX! (6)';
        ballPos = '96%';
        ballTransform = 'translateY(-220%) scale(1.7)';
        ballShadow = '0 0 25px #a855f7, 0 0 50px #c084fc';
        ballClass = 'boundary-six';
        outcomeDesc = latestComm.description || `SIX! Massive strike by ${strikerName}! Launched into the stands off ${bowlerName}!`;
    } else if (titleUpper.includes('FOUR') || titleUpper === '4') {
        badgeClass = 'four';
        badgeText = '🏏 BOUNDARY FOUR! (4)';
        ballPos = '98%';
        ballTransform = 'translateY(-50%) scale(1.15)';
        ballShadow = '0 0 22px #10b981, 0 0 40px #34d399';
        ballClass = 'boundary-four';
        outcomeDesc = latestComm.description || `FOUR! ${strikerName} struck a glorious boundary off ${bowlerName}!`;
        if (strikerTag && shouldAnimate) {
            strikerTag.style.borderColor = '#10b981';
            strikerTag.style.boxShadow = '0 0 15px rgba(16, 185, 129, 0.7)';
            setTimeout(() => { if (strikerTag) { strikerTag.style.borderColor = ''; strikerTag.style.boxShadow = ''; } }, 1200);
        }
    } else if (titleUpper.includes('WIDE')) {
        badgeClass = 'extra';
        badgeText = latestComm.title ? `⚠️ ${latestComm.title.toUpperCase()}` : '⚠️ WIDE BALL (+1)';
        ballPos = '80%';
        ballTransform = 'translateY(120%) scale(1.05)';
        ballShadow = '0 0 18px #f59e0b';
        ballClass = 'extra-wide';
        outcomeDesc = latestComm.description || `Wide delivery from ${bowlerName} outside the tramline. +1 extra run.`;
    } else if (titleUpper.includes('NO BALL')) {
        badgeClass = 'extra';
        badgeText = latestComm.title ? `🚨 ${latestComm.title.toUpperCase()} (FREE HIT)` : '🚨 NO BALL (FREE HIT)';
        ballPos = '24%';
        ballTransform = 'translateY(-50%) scale(1.1)';
        ballShadow = '0 0 22px #f59e0b, 0 0 35px #ef4444';
        ballClass = 'extra-noball';
        outcomeDesc = latestComm.description || `NO BALL! ${bowlerName} crossed the line. Extra run & Free Hit!`;
    } else if (titleUpper.includes('BYE')) {
        badgeClass = 'extra';
        badgeText = `🛡️ ${latestComm.title ? latestComm.title.toUpperCase() : 'BYE'}`;
        ballPos = '90%';
        ballTransform = 'translateY(-60%) scale(0.95)';
        ballShadow = '0 0 14px #38bdf8';
        outcomeDesc = latestComm.description || `Extras conceded off ${bowlerName}.`;
    } else if (titleUpper.includes('RUN') || ['1', '2', '3'].includes(titleUpper)) {
        badgeClass = 'runs';
        badgeText = `🏃 ${latestComm.title || 'RUNS'}`.toUpperCase();
        ballPos = titleUpper.includes('3') ? '92%' : titleUpper.includes('2') ? '90%' : '88%';
        ballTransform = 'translateY(-60%) scale(0.95)';
        ballShadow = '0 0 15px #3b82f6';
        outcomeDesc = latestComm.description || `${strikerName} scored off ${bowlerName}.`;
    } else if (titleUpper.includes('DOT') || titleUpper === '0') {
        badgeClass = 'dot';
        badgeText = '🎯 DOT BALL (0)';
        ballPos = '83%';
        ballTransform = 'translateY(-50%) scale(0.95)';
        ballShadow = '0 2px 6px rgba(0, 0, 0, 0.5)';
        outcomeDesc = latestComm.description || `Solid defense or beaten, no run scored.`;
    } else {
        badgeClass = 'dot';
        badgeText = latestComm.title ? `🎯 ${latestComm.title.toUpperCase()}` : 'BALL RECORDED';
        ballPos = '84%';
        outcomeDesc = latestComm.description || `${bowlerName} to ${strikerName}`;
    }

    // Animate ball
    if (ballEl) {
        if (shouldAnimate) {
            ballEl.style.transition = 'left 0.55s cubic-bezier(0.2, 0.8, 0.4, 1), transform 0.55s ease, box-shadow 0.4s ease';
            ballEl.style.left = ballPos;
            ballEl.style.transform = ballTransform;
            ballEl.style.boxShadow = ballShadow;
            ballEl.className = `ls-sim-ball ${ballClass}`;
        } else {
            ballEl.style.transition = 'none';
            ballEl.style.left = ballPos;
            ballEl.style.transform = ballTransform;
            ballEl.style.boxShadow = ballShadow;
            ballEl.className = `ls-sim-ball ${ballClass}`;
        }
    }

    // Update Outcome Badge & Description
    if (badgeEl) {
        badgeEl.className = `ls-outcome-badge ${badgeClass}`;
        badgeEl.textContent = badgeText;
        if (shouldAnimate) {
            void badgeEl.offsetWidth; // trigger re-animation
            badgeEl.style.animation = 'outcome-appear 0.4s ease-out';
        }
    }

    if (descEl) {
        descEl.innerHTML = `<strong style="color:#ffffff;">[Ball ${overBall}]</strong> ${escapeHtml(outcomeDesc)}`;
    }
}

function getBallPillConfig(ball) {
    let rawText = '';
    if (typeof ball === 'string') {
        rawText = ball.trim();
    } else if (ball && typeof ball === 'object') {
        if (ball.ballText) rawText = String(ball.ballText);
        else if (ball.isWicket) rawText = (ball.runsScored > 0 ? `W+${ball.runsScored}` : 'W');
        else if (ball.extraType === 'WIDE') rawText = (ball.extraRuns > 1 || ball.runsScored > 0 ? `WD+${ball.runsScored || ball.extraRuns}` : 'WD');
        else if (ball.extraType === 'NO_BALL') rawText = `NB+${ball.runsScored || 0}`;
        else rawText = String(ball.runsScored ?? ball.runs ?? '0');
    } else {
        rawText = String(ball ?? '0');
    }

    const upper = rawText.toUpperCase();
    let bg = '#334155'; // default run-0 (slate)
    let color = '#f8fafc';
    let border = 'rgba(255, 255, 255, 0.12)';

    if (upper === 'W' || upper.startsWith('W+')) {
        bg = '#ef4444'; // Red for Wickets
        color = '#ffffff';
        border = 'rgba(239, 68, 68, 0.5)';
    } else if (upper.startsWith('WD') || upper.startsWith('NB')) {
        bg = '#f59e0b'; // Amber for Extras (Wide, No Ball)
        color = '#ffffff';
        border = 'rgba(245, 158, 11, 0.5)';
    } else if (upper === '6') {
        bg = '#9333ea'; // Purple for 6
        color = '#ffffff';
        border = 'rgba(147, 51, 234, 0.5)';
    } else if (upper === '4') {
        bg = '#16a34a'; // Green for 4
        color = '#ffffff';
        border = 'rgba(22, 163, 74, 0.5)';
    } else if (upper === '1' || upper === '2' || upper === '3') {
        bg = '#2563eb'; // Blue for 1, 2, 3 runs
        color = '#ffffff';
        border = 'rgba(37, 99, 235, 0.5)';
    } else if (upper === '0') {
        bg = '#334155'; // Slate for Dot balls
        color = '#cbd5e1';
        border = 'rgba(255, 255, 255, 0.08)';
    } else {
        bg = '#475569';
        color = '#ffffff';
    }

    return {
        text: upper,
        bg,
        color,
        border
    };
}

function renderCurrentOverPills(balls, state = {}) {
    const container = document.getElementById('ls-current-over-pills');
    if (!container) return;

    if (!balls || balls.length === 0) {
        // If current over has no balls yet, check if there's a recently completed over
        const recent = state.recentOvers || [];
        if (recent.length > 0 && recent[0].balls && recent[0].balls.length > 0) {
            let pillsHtml = '';
            recent[0].balls.forEach(b => {
                const cfg = getBallPillConfig(b);
                pillsHtml += `<span class="ball-pill" style="min-width: 32px; height: 32px; padding: 0 6px; border-radius: 16px; background: ${cfg.bg}; color: ${cfg.color}; font-family: 'Outfit', sans-serif; font-weight: 800; font-size: 0.82rem; display: inline-flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(0,0,0,0.3); border: 1px solid ${cfg.border};">${escapeHtml(cfg.text)}</span>`;
            });
            const runs = recent[0].runs !== undefined ? recent[0].runs : (recent[0].runsConceded || 0);
            container.innerHTML = `
                <div style="display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap;">
                    <span style="color: #94a3b8; font-size: 0.82rem; font-weight: 600;">Over ${recent[0].overNumber} (${runs} ${runs === 1 ? 'run' : 'runs'}):</span>
                    ${pillsHtml}
                </div>
            `;
            return;
        }

        container.innerHTML = `<span style="color: #64748b; font-size: 0.85rem;">Waiting for first ball...</span>`;
        return;
    }

    let html = '';
    balls.forEach(ball => {
        const cfg = getBallPillConfig(ball);
        html += `<span class="ball-pill" style="min-width: 32px; height: 32px; padding: 0 6px; border-radius: 16px; background: ${cfg.bg}; color: ${cfg.color}; font-family: 'Outfit', sans-serif; font-weight: 800; font-size: 0.82rem; display: inline-flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(0,0,0,0.3); border: 1px solid ${cfg.border};">${escapeHtml(cfg.text)}</span>`;
    });

    container.innerHTML = html;
}

function renderBatters(striker, nonStriker) {
    const tbody = document.getElementById('ls-batters-body');
    if (!tbody) return;

    if (!striker && !nonStriker) {
        tbody.innerHTML = `<tr><td colspan="6" style="color: #64748b; text-align: center;">No batters currently at the crease</td></tr>`;
        return;
    }

    let html = '';
    if (striker) {
        const sr = striker.balls > 0 ? ((striker.runs / striker.balls) * 100).toFixed(1) : '0.0';
        html += `
            <tr>
                <td><strong>${escapeHtml(striker.name)}</strong><span class="ls-striker-star">*</span></td>
                <td style="font-weight: 700; color: #ffffff;">${striker.runs || 0}</td>
                <td>${striker.balls || 0}</td>
                <td>${striker.fours || 0}</td>
                <td>${striker.sixes || 0}</td>
                <td style="color: #38bdf8;">${sr}</td>
            </tr>
        `;
    }
    if (nonStriker) {
        const sr = nonStriker.balls > 0 ? ((nonStriker.runs / nonStriker.balls) * 100).toFixed(1) : '0.0';
        html += `
            <tr>
                <td>${escapeHtml(nonStriker.name)}</td>
                <td style="font-weight: 700; color: #ffffff;">${nonStriker.runs || 0}</td>
                <td>${nonStriker.balls || 0}</td>
                <td>${nonStriker.fours || 0}</td>
                <td>${nonStriker.sixes || 0}</td>
                <td style="color: #38bdf8;">${sr}</td>
            </tr>
        `;
    }

    tbody.innerHTML = html;
}

function renderBowler(bowler) {
    const tbody = document.getElementById('ls-bowler-body');
    if (!tbody) return;

    if (!bowler) {
        tbody.innerHTML = `<tr><td colspan="6" style="color: #64748b; text-align: center;">No active bowler selected</td></tr>`;
        return;
    }

    const oversStr = `${bowler.oversBowled || 0}.${bowler.ballsBowled || 0}`;
    const totalBalls = ((bowler.oversBowled || 0) * 6) + (bowler.ballsBowled || 0);
    const eco = totalBalls > 0 ? (((bowler.runsConceded || 0) / totalBalls) * 6).toFixed(2) : '0.00';

    tbody.innerHTML = `
        <tr>
            <td><strong>${escapeHtml(bowler.name)}</strong></td>
            <td>${oversStr}</td>
            <td>${bowler.maidens || 0}</td>
            <td>${bowler.runsConceded || 0}</td>
            <td style="font-weight: 700; color: #ef4444;">${bowler.wicketsTaken || 0}</td>
            <td style="color: #38bdf8;">${eco}</td>
        </tr>
    `;
}

function renderRecentOvers(recentOvers) {
    const list = document.getElementById('ls-recent-overs-list');
    if (!list) return;

    if (!recentOvers || !Array.isArray(recentOvers) || recentOvers.length === 0) {
        list.innerHTML = `<div style="color: #64748b; font-size: 0.85rem;">No completed overs yet.</div>`;
        return;
    }

    // Backend provides recentOvers with index 0 as the most recent over
    const displayOvers = recentOvers.slice(0, 4);

    let html = '';
    displayOvers.forEach(over => {
        let pillsHtml = '';
        const balls = Array.isArray(over.balls) ? over.balls : [];
        balls.forEach(b => {
            const cfg = getBallPillConfig(b);
            pillsHtml += `<span class="ball-pill" style="min-width: 26px; height: 26px; padding: 0 4px; border-radius: 13px; background: ${cfg.bg}; color: ${cfg.color}; font-family: 'Outfit', sans-serif; font-weight: 800; font-size: 0.72rem; display: inline-flex; align-items: center; justify-content: center; box-shadow: 0 2px 4px rgba(0,0,0,0.25); border: 1px solid ${cfg.border};">${escapeHtml(cfg.text)}</span>`;
        });

        const totalRuns = over.runs !== undefined ? over.runs : (over.runsConceded !== undefined ? over.runsConceded : 0);

        html += `
            <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 0.65rem 0.95rem; display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                <div style="font-family: 'Outfit', sans-serif; font-weight: 700; font-size: 0.88rem; color: #94a3b8;">
                    Ov ${over.overNumber}:
                </div>
                <div style="display: flex; gap: 0.35rem; align-items: center; flex-wrap: wrap;">
                    ${pillsHtml || '<span style="color:#64748b; font-size:0.75rem;">-</span>'}
                </div>
                <div style="font-family: 'Outfit', sans-serif; font-weight: 700; font-size: 0.88rem; color: #10b981;">
                    ${totalRuns} ${totalRuns === 1 ? 'run' : 'runs'}
                </div>
            </div>
        `;
    });

    list.innerHTML = html;
}

function renderCommentary(commentary) {
    const list = document.getElementById('ls-commentary-list');
    if (!list) return;

    if (!commentary || !Array.isArray(commentary) || commentary.length === 0) {
        list.innerHTML = `<div style="color: #64748b; font-size: 0.85rem;">Commentary will appear when balls are bowled...</div>`;
        return;
    }

    let html = '';
    commentary.slice(0, 25).forEach(item => {
        let badgeBg = 'rgba(100, 116, 139, 0.2)';
        let badgeColor = '#94a3b8';

        const badge = item.badgeText || item.title || '';
        const badgeUpper = badge.toUpperCase();

        if (badgeUpper.includes('WICKET')) {
            badgeBg = 'rgba(239, 68, 68, 0.2)';
            badgeColor = '#ef4444';
        } else if (badgeUpper.includes('SIX')) {
            badgeBg = 'rgba(168, 85, 247, 0.2)';
            badgeColor = '#c084fc';
        } else if (badgeUpper.includes('FOUR')) {
            badgeBg = 'rgba(16, 185, 129, 0.2)';
            badgeColor = '#34d399';
        } else if (badgeUpper.includes('WIDE') || badgeUpper.includes('NO BALL')) {
            badgeBg = 'rgba(245, 158, 11, 0.2)';
            badgeColor = '#f59e0b';
        }

        // Support both overBall and overNumber; guard against 0.0
        let overDisplay = item.overBall || item.overNumber || '';
        if (!overDisplay || overDisplay === '0.0') {
            overDisplay = '0.1';
        }

        html += `
            <div class="ls-comm-item">
                <div class="ls-comm-over">${escapeHtml(overDisplay)}</div>
                <div class="ls-comm-desc">
                    ${item.bowlerToBatter ? `<span style="font-weight: 600; color: #cbd5e1; margin-right: 0.35rem;">${escapeHtml(item.bowlerToBatter)}:</span>` : ''}
                    ${escapeHtml(item.description || '')}
                </div>
                ${badge ? `<div class="ls-comm-badge" style="background: ${badgeBg}; color: ${badgeColor}; border: 1px solid ${badgeColor}40;">${escapeHtml(badge)}</div>` : ''}
            </div>
        `;
    });

    list.innerHTML = html;
}

function showErrorAlert(msg) {
    const alertEl = document.getElementById('ls-alert');
    if (alertEl) {
        alertEl.textContent = msg;
        alertEl.style.display = 'block';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, match => {
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return map[match];
    });
}

/**
 * COMPLETED MATCH FINAL SCOREBOARD MODE FUNCTIONS
 */

function renderCompletedScorecards(state) {
    const container = document.getElementById('ls-full-scorecards-container');
    if (!container) return;

    const sc1 = state.innings1Scorecard;
    const sc2 = state.innings2Scorecard;

    let html = '';
    if (sc1) {
        html += renderSingleInningsScorecard(sc1, 1);
    }
    if (sc2) {
        html += renderSingleInningsScorecard(sc2, 2);
    }

    if (!sc1 && !sc2) {
        html = '<div style="text-align: center; color: #94a3b8; padding: 2.5rem; font-size: 0.95rem;">No scorecard records found for this match.</div>';
    }

    container.innerHTML = html;
}

function renderSingleInningsScorecard(sc, inningsNum) {
    const totalRuns = sc.totalRuns !== undefined ? sc.totalRuns : 0;
    const totalWkts = sc.totalWickets !== undefined ? sc.totalWickets : 0;
    const ov = sc.completedOvers !== undefined ? sc.completedOvers : 0;
    const bl = sc.currentBalls !== undefined ? sc.currentBalls : 0;
    const extras = sc.totalExtras !== undefined ? sc.totalExtras : 0;
    const battingTeam = escapeHtml(sc.battingTeamName || `Innings ${inningsNum}`);
    const bowlingTeam = escapeHtml(sc.bowlingTeamName || `Opponent`);

    const battingList = sc.battingList || [];
    const bowlingList = sc.bowlingList || [];

    // Batting rows with strict PLAYED vs NOT PLAYED distinction
    let battingRows = battingList.map(b => {
        const isPlayed = Boolean(b.played === true || b.hasBatted === true || (b.balls > 0) || (b.runs > 0) || b.isOut);
        let statusSubtext = '';
        let runsDisplay = '-';
        let ballsDisplay = '-';
        let foursDisplay = '-';
        let sixesDisplay = '-';
        let srDisplay = '-';

        if (isPlayed) {
            if (b.isOut) {
                const detail = b.dismissalText || (b.dismissalType ? b.dismissalType.toLowerCase().replace('_', ' ') : 'out');
                statusSubtext = `<span style="color: #fca5a5; font-size: 0.75rem; font-weight: 600; display: block; margin-top: 0.15rem;"><span style="background: rgba(239, 68, 68, 0.2); color: #f87171; padding: 1px 6px; border-radius: 4px; font-weight: 800; font-size: 0.7rem; text-transform: uppercase; margin-right: 4px;">Played</span> ${escapeHtml(detail)}</span>`;
            } else {
                statusSubtext = `<span style="color: #34d399; font-size: 0.75rem; font-weight: 600; display: block; margin-top: 0.15rem;"><span style="background: rgba(16, 185, 129, 0.2); color: #10b981; padding: 1px 6px; border-radius: 4px; font-weight: 800; font-size: 0.7rem; text-transform: uppercase; margin-right: 4px;">Played</span> not out</span>`;
            }
            runsDisplay = `<strong style="color: #10b981;">${b.runs !== undefined && b.runs !== null ? b.runs : 0}</strong>`;
            ballsDisplay = `${b.balls !== undefined && b.balls !== null ? b.balls : 0}`;
            foursDisplay = `${b.fours !== undefined && b.fours !== null ? b.fours : 0}`;
            sixesDisplay = `${b.sixes !== undefined && b.sixes !== null ? b.sixes : 0}`;
            srDisplay = b.strikeRate !== undefined && b.strikeRate !== null ? Number(b.strikeRate).toFixed(1) : (b.balls > 0 ? ((b.runs * 100.0) / b.balls).toFixed(1) : '0.0');
        } else {
            statusSubtext = `<span style="background: rgba(100, 116, 139, 0.2); color: #94a3b8; padding: 1px 6px; border-radius: 4px; font-weight: 700; font-size: 0.7rem; text-transform: uppercase; display: inline-block; margin-top: 0.2rem;">Not Played</span>`;
            runsDisplay = `<span style="color: #64748b;">-</span>`;
            ballsDisplay = `<span style="color: #64748b;">-</span>`;
            foursDisplay = `<span style="color: #64748b;">-</span>`;
            sixesDisplay = `<span style="color: #64748b;">-</span>`;
            srDisplay = `<span style="color: #64748b;">-</span>`;
        }

        return `
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.06);">
                <td style="padding: 0.65rem 0.85rem;">
                    <strong style="color: #fff; font-size: 0.92rem; display: block;">${escapeHtml(b.name)}</strong>
                    ${statusSubtext}
                </td>
                <td style="padding: 0.65rem 0.85rem; font-weight: 800; text-align: right;">${runsDisplay}</td>
                <td style="padding: 0.65rem 0.85rem; text-align: right; color: #cbd5e1;">${ballsDisplay}</td>
                <td style="padding: 0.65rem 0.85rem; text-align: right; color: #cbd5e1;">${foursDisplay}</td>
                <td style="padding: 0.65rem 0.85rem; text-align: right; color: #cbd5e1;">${sixesDisplay}</td>
                <td style="padding: 0.65rem 0.85rem; text-align: right; color: #cbd5e1;">${srDisplay}</td>
            </tr>
        `;
    }).join('');

    if (!battingRows) {
        battingRows = '<tr><td colspan="6" style="padding: 0.75rem; text-align: center; color: #64748b;">No batting data recorded</td></tr>';
    }

    // Bowling rows: ONLY bowlers with ballsBowled > 0
    const activeBowlers = bowlingList.filter(b => (b.ballsBowled || 0) > 0);
    let bowlingRows = activeBowlers.map(b => {
        const totalBalls = b.ballsBowled || 0;
        const ovr = Math.floor(totalBalls / 6);
        const bls = totalBalls % 6;
        const oversStr = `${ovr}.${bls}`;
        const econ = totalBalls > 0 ? ((b.runsConceded || 0) / (totalBalls / 6.0)).toFixed(1) : '0.0';
        return `
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.06);">
                <td style="padding: 0.65rem 0.85rem; font-weight: 700; color: #fff;">${escapeHtml(b.name)}</td>
                <td style="padding: 0.65rem 0.85rem; text-align: right; color: #cbd5e1;">${oversStr}</td>
                <td style="padding: 0.65rem 0.85rem; text-align: right; color: #cbd5e1;">${b.maidens || 0}</td>
                <td style="padding: 0.65rem 0.85rem; text-align: right; color: #cbd5e1;">${b.runsConceded || 0}</td>
                <td style="padding: 0.65rem 0.85rem; font-weight: 800; color: #ef4444; text-align: right;">${b.wicketsTaken || 0}</td>
                <td style="padding: 0.65rem 0.85rem; text-align: right; color: #cbd5e1;">${econ}</td>
            </tr>
        `;
    }).join('');

    if (!bowlingRows) {
        bowlingRows = '<tr><td colspan="6" style="padding: 0.75rem; text-align: center; color: #64748b;">No bowlers with > 0 balls bowled</td></tr>';
    }

    return `
        <div style="background: rgba(11, 26, 19, 0.85); border: 1px solid rgba(255,255,255,0.1); border-radius: 14px; padding: 1.25rem; margin-bottom: 1.5rem;">
            <!-- Innings Header Banner -->
            <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(15, 23, 42, 0.7); border-radius: 10px; padding: 0.85rem 1.25rem; margin-bottom: 1rem; flex-wrap: wrap; gap: 0.5rem;">
                <div>
                    <span style="font-size: 0.75rem; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; display: block;">${inningsNum === 1 ? '1ST INNINGS' : '2ND INNINGS'} SCORECARD</span>
                    <strong style="font-family: 'Outfit', sans-serif; font-size: 1.35rem; color: #10b981;">${battingTeam}</strong>
                </div>
                <div style="text-align: right;">
                    <span style="font-family: 'Outfit', sans-serif; font-size: 1.8rem; font-weight: 900; color: #ffffff;">${totalRuns}/${totalWkts}</span>
                    <span style="font-size: 0.9rem; color: #94a3b8; margin-left: 0.4rem;">(${ov}.${bl} ov)</span>
                </div>
            </div>

            <!-- Batting Scorecard Table -->
            <h4 style="font-size: 0.85rem; font-weight: 800; color: #38bdf8; text-transform: uppercase; margin: 1rem 0 0.5rem 0;">Batting — ${battingTeam}</h4>
            <div style="overflow-x: auto; margin-bottom: 1.25rem;">
                <table style="width: 100%; border-collapse: collapse; font-size: 0.88rem; text-align: left;">
                    <thead>
                        <tr style="background: rgba(255,255,255,0.04); color: #94a3b8; font-size: 0.75rem; text-transform: uppercase;">
                            <th style="padding: 0.6rem 0.85rem;">Batter</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">R</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">B</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">4s</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">6s</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">SR</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${battingRows}
                    </tbody>
                </table>
            </div>

            <!-- Bowling Scorecard Table -->
            <h4 style="font-size: 0.85rem; font-weight: 800; color: #f59e0b; text-transform: uppercase; margin: 1rem 0 0.5rem 0;">Bowling — ${bowlingTeam}</h4>
            <div style="overflow-x: auto;">
                <table style="width: 100%; border-collapse: collapse; font-size: 0.88rem; text-align: left;">
                    <thead>
                        <tr style="background: rgba(255,255,255,0.04); color: #94a3b8; font-size: 0.75rem; text-transform: uppercase;">
                            <th style="padding: 0.6rem 0.85rem;">Bowler</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">O</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">M</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">R</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">W</th>
                            <th style="padding: 0.6rem 0.85rem; text-align: right;">Econ</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${bowlingRows}
                    </tbody>
                </table>
            </div>

            <!-- Extras Summary Bar -->
            <div style="margin-top: 1rem; padding-top: 0.75rem; border-top: 1px dashed rgba(255,255,255,0.1); font-size: 0.82rem; color: #94a3b8;">
                Extras: <strong style="color: #f59e0b;">${extras}</strong> runs
            </div>
        </div>
    `;
}

function renderCompletedHistoricalDeliveries(state) {
    const oversList = document.getElementById('ls-completed-overs-list');
    const commList = document.getElementById('ls-completed-commentary-list');

    // 1. Overs Breakdown
    if (oversList) {
        const recent = state.recentOvers || [];
        if (recent.length === 0) {
            oversList.innerHTML = `<div style="color: #64748b; font-size: 0.85rem;">No overs data available.</div>`;
        } else {
            let html = '';
            recent.forEach(over => {
                let pillsHtml = '';
                const balls = Array.isArray(over.balls) ? over.balls : [];
                balls.forEach(b => {
                    const cfg = getBallPillConfig(b);
                    pillsHtml += `<span class="ball-pill" style="min-width: 26px; height: 26px; padding: 0 4px; border-radius: 13px; background: ${cfg.bg}; color: ${cfg.color}; font-family: 'Outfit', sans-serif; font-weight: 800; font-size: 0.72rem; display: inline-flex; align-items: center; justify-content: center; box-shadow: 0 2px 4px rgba(0,0,0,0.25); border: 1px solid ${cfg.border};">${escapeHtml(cfg.text)}</span>`;
                });
                const totalRuns = over.runs !== undefined ? over.runs : (over.runsConceded !== undefined ? over.runsConceded : 0);
                html += `
                    <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 0.65rem 0.95rem; display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                        <div style="font-family: 'Outfit', sans-serif; font-weight: 700; font-size: 0.88rem; color: #94a3b8;">
                            Ov ${over.overNumber}:
                        </div>
                        <div style="display: flex; gap: 0.35rem; align-items: center; flex-wrap: wrap;">
                            ${pillsHtml || '<span style="color:#64748b; font-size:0.75rem;">-</span>'}
                        </div>
                        <div style="font-family: 'Outfit', sans-serif; font-weight: 700; font-size: 0.88rem; color: #10b981;">
                            ${totalRuns} ${totalRuns === 1 ? 'run' : 'runs'}
                        </div>
                    </div>
                `;
            });
            oversList.innerHTML = html;
        }
    }

    // 2. Commentary Log (Historical, no live tags)
    if (commList) {
        const commentary = state.commentary || [];
        if (commentary.length === 0) {
            commList.innerHTML = `<div style="color: #64748b; font-size: 0.85rem;">No historical commentary available.</div>`;
        } else {
            let html = '';
            commentary.forEach(item => {
                let badgeBg = 'rgba(100, 116, 139, 0.2)';
                let badgeColor = '#94a3b8';
                const badge = item.badgeText || item.title || '';
                const badgeUpper = badge.toUpperCase();

                if (badgeUpper.includes('WICKET')) {
                    badgeBg = 'rgba(239, 68, 68, 0.2)';
                    badgeColor = '#ef4444';
                } else if (badgeUpper.includes('SIX')) {
                    badgeBg = 'rgba(168, 85, 247, 0.2)';
                    badgeColor = '#c084fc';
                } else if (badgeUpper.includes('FOUR')) {
                    badgeBg = 'rgba(16, 185, 129, 0.2)';
                    badgeColor = '#34d399';
                } else if (badgeUpper.includes('WIDE') || badgeUpper.includes('NO BALL')) {
                    badgeBg = 'rgba(245, 158, 11, 0.2)';
                    badgeColor = '#f59e0b';
                }

                let overDisplay = item.overBall || item.overNumber || '';
                if (!overDisplay || overDisplay === '0.0') overDisplay = '0.1';

                html += `
                    <div class="ls-comm-item">
                        <div class="ls-comm-over">${escapeHtml(overDisplay)}</div>
                        <div class="ls-comm-desc">
                            ${item.bowlerToBatter ? `<span style="font-weight: 600; color: #cbd5e1; margin-right: 0.35rem;">${escapeHtml(item.bowlerToBatter)}:</span>` : ''}
                            ${escapeHtml(item.description || '')}
                        </div>
                        ${badge ? `<div class="ls-comm-badge" style="background: ${badgeBg}; color: ${badgeColor}; border: 1px solid ${badgeColor}40;">${escapeHtml(badge)}</div>` : ''}
                    </div>
                `;
            });
            commList.innerHTML = html;
        }
    }
}

function renderCompletedMatchInfo(state) {
    const grid = document.getElementById('ls-completed-info-grid');
    if (!grid) return;

    const items = [
        { label: 'Date & Time', value: `${state.matchDate || 'TBD'} ${state.matchTime || ''}` },
        { label: 'Venue', value: state.venue || 'TBD' },
        { label: 'Match Format', value: `${state.format || 'T20'} • ${state.overs || 20} Overs` },
        { label: 'Toss Result', value: state.tossSummary || 'Toss not recorded' },
        { label: 'Winner', value: state.winnerTeamName || state.matchResultSummary || 'Completed' },
        { label: 'Margin', value: state.resultMargin || 'Match Finished' }
    ];

    grid.innerHTML = items.map(item => `
        <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 1rem;">
            <div style="font-size: 0.75rem; text-transform: uppercase; color: #94a3b8; font-weight: 700; margin-bottom: 0.35rem;">${escapeHtml(item.label)}</div>
            <div style="font-family: 'Outfit', sans-serif; font-size: 1rem; color: #ffffff; font-weight: 600;">${escapeHtml(item.value)}</div>
        </div>
    `).join('');
}

function switchCompletedTab(tabName) {
    const tabs = ['scorecard', 'ballbyball', 'info'];
    tabs.forEach(t => {
        const btn = document.getElementById(`tab-btn-${t}`);
        const content = document.getElementById(`tab-content-${t}`);
        if (btn) {
            if (t === tabName) btn.classList.add('active');
            else btn.classList.remove('active');
        }
        if (content) {
            content.style.display = (t === tabName) ? 'block' : 'none';
        }
    });
}
window.switchCompletedTab = switchCompletedTab;

/**
 * ========================================================
 * STAGE 11 — SCOREBOARD TAB ENGINE
 * ========================================================
 */

function switchMatchView(viewName) {
    currentMatchView = viewName;
    const btnLive = document.getElementById('tab-nav-live');
    const btnSb = document.getElementById('tab-nav-scoreboard');
    const liveView = document.getElementById('ls-live-tab-view');
    const sbView = document.getElementById('ls-scoreboard-tab-view');

    if (viewName === 'scoreboard') {
        if (btnLive) btnLive.classList.remove('active');
        if (btnSb) btnSb.classList.add('active');
        if (liveView) liveView.style.display = 'none';
        if (sbView) sbView.style.display = 'block';

        if (lastMatchState) {
            renderScoreboardView(lastMatchState);
        }
    } else {
        if (btnSb) btnSb.classList.remove('active');
        if (btnLive) btnLive.classList.add('active');
        if (sbView) sbView.style.display = 'none';
        if (liveView) liveView.style.display = 'block';
    }
}
window.switchMatchView = switchMatchView;

function selectScoreboardInnings(inningsNum) {
    selectedScoreboardInnings = inningsNum;
    if (lastMatchState) {
        renderScoreboardView(lastMatchState);
    }
}
window.selectScoreboardInnings = selectScoreboardInnings;

function toggleBatterExpansion(batterId) {
    if (!batterId) return;
    if (expandedBatterIds.has(batterId)) {
        expandedBatterIds.delete(batterId);
    } else {
        expandedBatterIds.add(batterId);
    }
    if (lastMatchState) {
        renderScoreboardView(lastMatchState);
    }
}
window.toggleBatterExpansion = toggleBatterExpansion;

function renderScoreboardView(state) {
    if (!state) return;
    lastMatchState = state;

    const activeInnNum = state.activeInningsNumber || state.currentInningsNumber || 1;
    const currentInningsToDisplay = selectedScoreboardInnings || (activeInnNum === 2 ? 2 : 1);

    // Identify teams for Innings 1 and Innings 2 from actual backend match state
    let team1Name = (state.innings1Scorecard && state.innings1Scorecard.battingTeamName)
        || state.firstInningsTeamName
        || (state.teamA && state.teamA.name)
        || 'Team A';

    let team2Name = (state.innings1Scorecard && state.innings1Scorecard.bowlingTeamName)
        || (state.innings2Scorecard && state.innings2Scorecard.battingTeamName)
        || (state.teamB && state.teamB.name)
        || 'Team B';

    // 1. UPDATE INNINGS / TEAM SELECTOR (Section 3)
    renderScoreboardInningsSelector(state, team1Name, team2Name, activeInnNum, currentInningsToDisplay);

    // 2. RETRIEVE OR CONSTRUCT SCORECARD FOR CHOSEN INNINGS
    let sc = null;
    if (currentInningsToDisplay === 1) {
        sc = state.innings1Scorecard;
        if (!sc && activeInnNum === 1) {
            sc = buildLiveScorecardFromState(state, 1, team1Name, team2Name);
        }
    } else {
        sc = state.innings2Scorecard;
        if (!sc && activeInnNum === 2) {
            sc = buildLiveScorecardFromState(state, 2, team2Name, team1Name);
        }
    }

    const battingTitleEl = document.getElementById('sb-batting-title');
    const battingInningsTotalEl = document.getElementById('sb-batting-innings-total');
    const battingBodyEl = document.getElementById('sb-batting-body');
    const bowlingTitleEl = document.getElementById('sb-bowling-title');
    const bowlingBodyEl = document.getElementById('sb-bowling-body');
    const fowCardEl = document.getElementById('sb-fow-card');
    const fowGridEl = document.getElementById('sb-fow-grid');
    const sumTitleEl = document.getElementById('sb-summary-title');

    // If selected innings has not started yet (e.g. 2nd innings while in 1st innings)
    if (!sc) {
        const teamNotStarted = (currentInningsToDisplay === 1 ? team1Name : team2Name);
        if (battingTitleEl) battingTitleEl.textContent = `BATTING — ${teamNotStarted}`;
        if (battingInningsTotalEl) battingInningsTotalEl.textContent = 'Yet to bat';
        if (battingBodyEl) battingBodyEl.innerHTML = `<tr><td colspan="7" style="padding: 2.5rem; text-align: center; color: #94a3b8; font-size: 0.95rem;">${escapeHtml(teamNotStarted)} has not batted yet.</td></tr>`;

        if (bowlingTitleEl) bowlingTitleEl.textContent = `BOWLING — ${currentInningsToDisplay === 1 ? team2Name : team1Name}`;
        if (bowlingBodyEl) bowlingBodyEl.innerHTML = `<tr><td colspan="6" style="padding: 1.5rem; text-align: center; color: #64748b;">Innings has not started yet</td></tr>`;

        if (fowCardEl) fowCardEl.style.display = 'none';
        if (sumTitleEl) sumTitleEl.textContent = `INNINGS SUMMARY — ${teamNotStarted}`;
        updateScoreboardSummary(0, 0, 0, 0, 0, 0, 0.0);
        return;
    }

    if (fowCardEl) fowCardEl.style.display = 'block';

    const battingTeamName = sc.battingTeamName || (currentInningsToDisplay === 1 ? team1Name : team2Name);
    const bowlingTeamName = sc.bowlingTeamName || (currentInningsToDisplay === 1 ? team2Name : team1Name);

    // 3. RENDER BATTING SCORECARD (Section 4, 5, 6, 7)
    if (battingTitleEl) battingTitleEl.textContent = `BATTING — ${battingTeamName}`;
    if (battingInningsTotalEl) battingInningsTotalEl.textContent = `${sc.totalRuns || 0}/${sc.totalWickets || 0} (${sc.completedOvers || 0}.${sc.currentBalls || 0} ov)`;

    renderScoreboardBatting(sc, state, activeInnNum, currentInningsToDisplay);

    // 4. RENDER BOWLING SCORECARD (Section 8)
    if (bowlingTitleEl) bowlingTitleEl.textContent = `BOWLING — ${bowlingTeamName}`;
    renderScoreboardBowling(sc, state, activeInnNum, currentInningsToDisplay);

    // 5. RENDER FALL OF WICKETS (Section 9)
    renderScoreboardFow(sc);

    // 6. RENDER INNINGS SUMMARY (Section 10)
    if (sumTitleEl) sumTitleEl.textContent = `INNINGS SUMMARY — ${battingTeamName}`;
    const totRuns = sc.totalRuns || 0;
    const totWkts = sc.totalWickets || 0;
    const compOv = sc.completedOvers || 0;
    const curBalls = sc.currentBalls || 0;
    const totExtras = sc.totalExtras || 0;
    const totFours = (sc.totalFours !== undefined && sc.totalFours !== null) ? sc.totalFours : (sc.battingList || []).reduce((acc, b) => acc + (b.fours || 0), 0);
    const totSixes = (sc.totalSixes !== undefined && sc.totalSixes !== null) ? sc.totalSixes : (sc.battingList || []).reduce((acc, b) => acc + (b.sixes || 0), 0);
    const totOvDec = compOv + (curBalls / 6.0);
    const runRate = totOvDec > 0 ? (totRuns / totOvDec).toFixed(2) : '0.00';

    updateScoreboardSummary(totRuns, totWkts, compOv, curBalls, totExtras, totFours, totSixes, runRate);
}

function renderScoreboardInningsSelector(state, team1Name, team2Name, activeInnNum, currentInningsToDisplay) {
    const card1 = document.getElementById('sb-card-inn-1');
    const card2 = document.getElementById('sb-card-inn-2');

    const name1El = document.getElementById('sb-name-inn-1');
    const score1El = document.getElementById('sb-score-inn-1');
    const overs1El = document.getElementById('sb-overs-inn-1');
    const fallback1El = document.getElementById('sb-fallback-inn-1');

    const name2El = document.getElementById('sb-name-inn-2');
    const score2El = document.getElementById('sb-score-inn-2');
    const overs2El = document.getElementById('sb-overs-inn-2');
    const fallback2El = document.getElementById('sb-fallback-inn-2');

    if (name1El) name1El.textContent = team1Name;
    if (fallback1El) fallback1El.textContent = team1Name.charAt(0).toUpperCase() || '1';

    if (name2El) name2El.textContent = team2Name;
    if (fallback2El) fallback2El.textContent = team2Name.charAt(0).toUpperCase() || '2';

    // Innings 1 score
    let s1 = '0/0';
    let ov1 = '(0.0 ov)';
    if (state.innings1Scorecard) {
        s1 = `${state.innings1Scorecard.totalRuns || 0}/${state.innings1Scorecard.totalWickets || 0}`;
        ov1 = `(${state.innings1Scorecard.completedOvers || 0}.${state.innings1Scorecard.currentBalls || 0} ov)`;
    } else if (activeInnNum === 1) {
        s1 = `${state.totalRuns || 0}/${state.totalWickets || 0}`;
        ov1 = `(${state.completedOvers || 0}.${state.currentBalls || 0} ov)`;
    } else if (state.firstInningsRuns !== undefined && state.firstInningsRuns !== null) {
        s1 = `${state.firstInningsRuns}/${state.firstInningsWickets || 0}`;
        ov1 = `(${state.firstInningsOvers || 0}.${state.firstInningsBalls || 0} ov)`;
    }
    if (score1El) score1El.textContent = s1;
    if (overs1El) overs1El.textContent = ov1;

    // Innings 2 score
    let s2 = 'Yet to bat';
    let ov2 = '';
    if (state.innings2Scorecard) {
        s2 = `${state.innings2Scorecard.totalRuns || 0}/${state.innings2Scorecard.totalWickets || 0}`;
        ov2 = `(${state.innings2Scorecard.completedOvers || 0}.${state.innings2Scorecard.currentBalls || 0} ov)`;
    } else if (activeInnNum === 2) {
        s2 = `${state.totalRuns || 0}/${state.totalWickets || 0}`;
        ov2 = `(${state.completedOvers || 0}.${state.currentBalls || 0} ov)`;
    }
    if (score2El) score2El.textContent = s2;
    if (overs2El) overs2El.textContent = ov2;

    // Highlight selected card
    if (card1 && card2) {
        if (currentInningsToDisplay === 1) {
            card1.classList.add('active');
            card2.classList.remove('active');
        } else {
            card2.classList.add('active');
            card1.classList.remove('active');
        }
    }
}

function buildLiveScorecardFromState(state, innNum, battingName, bowlingName) {
    const fours = (state.currentInningsTotalFours !== undefined && state.currentInningsTotalFours !== null)
        ? state.currentInningsTotalFours
        : (state.battingPlayingXi || []).reduce((acc, b) => acc + (b.fours || 0), 0);
    const sixes = (state.currentInningsTotalSixes !== undefined && state.currentInningsTotalSixes !== null)
        ? state.currentInningsTotalSixes
        : (state.battingPlayingXi || []).reduce((acc, b) => acc + (b.sixes || 0), 0);

    return {
        inningsNumber: innNum,
        battingTeamName: battingName,
        bowlingTeamName: bowlingName,
        totalRuns: state.totalRuns || 0,
        totalWickets: state.totalWickets || 0,
        completedOvers: state.completedOvers || 0,
        currentBalls: state.currentBalls || 0,
        totalExtras: state.totalExtras || 0,
        totalFours: fours,
        totalSixes: sixes,
        battingList: state.battingPlayingXi || [],
        bowlingList: (state.bowlingPlayingXi || []).filter(b => (b.ballsBowled || 0) > 0),
        fallOfWickets: state.currentInningsFallOfWickets || []
    };
}

function renderScoreboardBatting(sc, state, activeInnNum, currentInningsToDisplay) {
    const tbody = document.getElementById('sb-batting-body');
    if (!tbody) return;

    const battingList = sc.battingList || [];
    if (battingList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: #64748b; padding: 1.5rem;">No batting data recorded</td></tr>`;
        return;
    }

    let html = '';
    battingList.forEach((b, idx) => {
        const isPlayed = Boolean(b.played === true || b.hasBatted === true || (b.balls > 0) || (b.runs > 0) || b.isOut);
        const isAtCrease = (activeInnNum === currentInningsToDisplay && !state.isMatchCompleted && (state.striker?.userId === b.userId || state.nonStriker?.userId === b.userId));
        const isStriker = isAtCrease && (state.striker?.userId === b.userId);
        const isExpanded = expandedBatterIds.has(b.userId);

        let statusText = '';
        let statusClass = 'not-played';

        if (b.isOut) {
            statusClass = 'dismissed';
            statusText = b.dismissalText || (b.dismissalType ? b.dismissalType.toLowerCase().replace('_', ' ') : 'out');
        } else if (isAtCrease) {
            statusClass = 'batting';
            statusText = 'Batting';
        } else if (isPlayed) {
            statusClass = 'not-out';
            statusText = 'not out';
        } else {
            statusClass = 'not-played';
            statusText = 'Not Played';
        }

        let runsVal = '-';
        let ballsVal = '-';
        let foursVal = '-';
        let sixesVal = '-';
        let srVal = '-';

        if (isPlayed) {
            runsVal = `<strong style="color: #10b981; font-size: 0.95rem;">${b.runs !== undefined && b.runs !== null ? b.runs : 0}</strong>`;
            ballsVal = `${b.balls !== undefined && b.balls !== null ? b.balls : 0}`;
            foursVal = `${b.fours !== undefined && b.fours !== null ? b.fours : 0}`;
            sixesVal = `${b.sixes !== undefined && b.sixes !== null ? b.sixes : 0}`;
            srVal = b.strikeRate !== undefined && b.strikeRate !== null
                ? Number(b.strikeRate).toFixed(2)
                : (b.balls > 0 ? ((b.runs * 100.0) / b.balls).toFixed(2) : '0.00');
        }

        const creaseIndicator = isStriker
            ? `<span class="sb-crease-dot"></span>`
            : isAtCrease
                ? `<span class="sb-crease-dot" style="opacity: 0.65;"></span>`
                : '';

        const starTag = isStriker ? `<span class="sb-striker-star">*</span>` : '';
        const chevron = isExpanded ? '▲' : '▼';

        html += `
            <tr class="sb-player-row" onclick="toggleBatterExpansion('${escapeHtml(b.userId || String(idx))}')">
                <td>
                    <div class="sb-player-name-line">
                        ${creaseIndicator}
                        <span>${escapeHtml(b.name || 'Batter')}</span>
                        ${starTag}
                    </div>
                    <span class="sb-status-text ${statusClass}">${escapeHtml(statusText)}</span>
                </td>
                <td>${runsVal}</td>
                <td>${ballsVal}</td>
                <td>${foursVal}</td>
                <td>${sixesVal}</td>
                <td style="color: #38bdf8; font-weight: 600;">${srVal}</td>
                <td style="text-align: center;">
                    <span class="sb-expand-icon">${chevron}</span>
                </td>
            </tr>
        `;

        if (isExpanded) {
            const rawRuns = b.runs || 0;
            const rawBalls = b.balls || 0;
            const rawFours = b.fours || 0;
            const rawSixes = b.sixes || 0;
            const rawSr = srVal !== '-' ? srVal : '0.00';

            html += `
                <tr class="sb-drawer-row">
                    <td colspan="7" style="padding: 0; background: rgba(15, 23, 42, 0.9);">
                        <div class="sb-player-drawer">
                            <div style="font-weight: 700; color: #34d399; margin-bottom: 0.35rem; display: flex; align-items: center; gap: 0.5rem;">
                                <span>🏏 Current Innings Details</span>
                                <span style="font-size: 0.75rem; color: #94a3b8; font-weight: 500;">(${escapeHtml(b.name)})</span>
                            </div>
                            <div style="display: flex; gap: 1rem; flex-wrap: wrap; color: #cbd5e1;">
                                <span>Runs: <strong style="color:#ffffff;">${rawRuns}</strong></span>
                                <span>Balls: <strong style="color:#ffffff;">${rawBalls}</strong></span>
                                <span>Fours: <strong style="color:#ffffff;">${rawFours}</strong></span>
                                <span>Sixes: <strong style="color:#ffffff;">${rawSixes}</strong></span>
                                <span>Strike Rate: <strong style="color:#38bdf8;">${rawSr}</strong></span>
                                ${b.isOut ? `<span>Dismissal: <strong style="color:#fca5a5;">${escapeHtml(b.dismissalText || 'Out')}</strong></span>` : ''}
                            </div>
                        </div>
                    </td>
                </tr>
            `;
        }
    });

    tbody.innerHTML = html;
}

function renderScoreboardBowling(sc, state, activeInnNum, currentInningsToDisplay) {
    const tbody = document.getElementById('sb-bowling-body');
    if (!tbody) return;

    const bowlingList = (sc.bowlingList || []).filter(b => (b.ballsBowled || 0) > 0);
    if (bowlingList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="padding: 1.5rem; text-align: center; color: #64748b;">No bowlers with > 0 balls bowled</td></tr>`;
        return;
    }

    let html = '';
    bowlingList.forEach(b => {
        const totalBalls = b.ballsBowled || 0;
        const ov = Math.floor(totalBalls / 6);
        const bl = totalBalls % 6;
        const oversStr = `${ov}.${bl}`;
        const econ = totalBalls > 0 ? ((b.runsConceded || 0) / (totalBalls / 6.0)).toFixed(2) : '0.00';
        const isCurrentBowler = (activeInnNum === currentInningsToDisplay && !state.isMatchCompleted && state.currentBowler?.userId === b.userId);

        html += `
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                <td>
                    <div style="display: flex; align-items: center; gap: 0.4rem; font-weight: 700; color: #ffffff;">
                        ${isCurrentBowler ? `<span class="sb-crease-dot" style="background:#f59e0b; box-shadow:0 0 6px #f59e0b;"></span>` : ''}
                        <span>${escapeHtml(b.name || 'Bowler')}</span>
                        ${isCurrentBowler ? `<span style="font-size: 0.72rem; color: #f59e0b; font-weight: 600;">(bowling)</span>` : ''}
                    </div>
                </td>
                <td style="color: #cbd5e1;">${oversStr}</td>
                <td style="color: #cbd5e1;">${b.maidens || 0}</td>
                <td style="color: #cbd5e1;">${b.runsConceded || 0}</td>
                <td><strong style="color: #ef4444; font-size: 0.95rem;">${b.wicketsTaken || 0}</strong></td>
                <td style="color: #38bdf8; font-weight: 600;">${econ}</td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
}

function renderScoreboardFow(sc) {
    const grid = document.getElementById('sb-fow-grid');
    if (!grid) return;

    const fow = sc.fallOfWickets || [];
    if (fow.length === 0) {
        grid.innerHTML = `<span style="color: #64748b; font-size: 0.85rem;">No wickets fallen yet.</span>`;
        return;
    }

    let html = '';
    fow.forEach(f => {
        html += `
            <div class="sb-fow-item">
                <span class="sb-fow-score">${f.wicketNumber}-${f.teamRuns}</span>
                <span class="sb-fow-name">${escapeHtml(f.playerName || 'Batter')}</span>
                <span class="sb-fow-ov">(${f.overBall || ''} ov)</span>
            </div>
        `;
    });

    grid.innerHTML = html;
}

function updateScoreboardSummary(totRuns, totWkts, compOv, curBalls, totExtras, totFours, totSixes, runRate) {
    const sScore = document.getElementById('sb-sum-score');
    const sOvers = document.getElementById('sb-sum-overs');
    const sRr = document.getElementById('sb-sum-rr');
    const sExtras = document.getElementById('sb-sum-extras');
    const sFours = document.getElementById('sb-sum-fours');
    const sSixes = document.getElementById('sb-sum-sixes');

    if (sScore) sScore.textContent = `${totRuns}/${totWkts}`;
    if (sOvers) sOvers.textContent = `${compOv}.${curBalls}`;
    if (sRr) sRr.textContent = runRate;
    if (sExtras) sExtras.textContent = totExtras;
    if (sFours) sFours.textContent = totFours;
    if (sSixes) sSixes.textContent = totSixes;
}
