/**
 * Cricket App - Scorer Control Room JavaScript
 * Stage 8 / Stage 9 Scorer Dashboard Operations & Settings
 */

let currentMatchId = null;
let scoringState = null;
let currentMatchMeta = null;
let currentUserId = null;
let pendingBowlerAction = null;

// Cross-tab zero-latency communication channel
const liveChannel = (typeof BroadcastChannel !== 'undefined') ? new BroadcastChannel('cricket_live_channel') : null;
if (liveChannel) {
    liveChannel.onmessage = (event) => {
        if (event && event.data && event.data.type === 'CELEBRATION' && window.CelebrationManager) {
            CelebrationManager.trigger(event.data.event, event.data.celebrationId, event.data.matchId);
        }
    };
}

document.addEventListener('DOMContentLoaded', async () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    const urlParams = new URLSearchParams(window.location.search);
    currentMatchId = urlParams.get('matchId') || urlParams.get('id');

    if (!currentMatchId) {
        showScoringAlert('No match ID specified in URL.');
        return;
    }

    initEventListeners();
    await loadScoringDashboardState();
    connectScoringWebSocket();
});

function initEventListeners() {
    // Start Match Trigger Button
    const triggerStartBtn = document.getElementById('btn-start-match-trigger');
    const startModal = document.getElementById('start-match-modal');
    const cancelStartBtn = document.getElementById('btn-cancel-start-modal');
    const confirmStartBtn = document.getElementById('btn-confirm-start-match');

    const handleStartMatchAction = async () => {
        if (!currentMatchId) return;
        try {
            if (triggerStartBtn) {
                triggerStartBtn.disabled = true;
                triggerStartBtn.textContent = '🚀 Starting Match...';
            }
            if (confirmStartBtn) {
                confirmStartBtn.disabled = true;
                confirmStartBtn.textContent = 'Starting...';
            }

            await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/start`, {});
            ApiService.showToast('Match is now LIVE! Commence toss setup.', 'success');

            if (startModal) startModal.style.display = 'none';
            await loadScoringDashboardState();

        } catch (err) {
            console.error('Failed to start match:', err);
            showScoringAlert(err.message || 'Failed to start match.');
        } finally {
            if (triggerStartBtn) {
                triggerStartBtn.disabled = false;
                triggerStartBtn.textContent = '🚀 Start Match (Change to LIVE)';
            }
            if (confirmStartBtn) {
                confirmStartBtn.disabled = false;
                confirmStartBtn.textContent = 'Yes, Start Match';
            }
        }
    };

    if (triggerStartBtn) {
        triggerStartBtn.addEventListener('click', handleStartMatchAction);
    }

    if (confirmStartBtn) {
        confirmStartBtn.addEventListener('click', handleStartMatchAction);
    }

    if (cancelStartBtn && startModal) {
        cancelStartBtn.addEventListener('click', () => {
            startModal.style.display = 'none';
        });
    }

    // Confirm Toss
    const confirmTossBtn = document.getElementById('btn-confirm-toss');
    if (confirmTossBtn) {
        confirmTossBtn.addEventListener('click', async () => {
            if (!currentMatchId) return;
            const winnerSelect = document.getElementById('toss-winner-select');
            const decisionSelect = document.getElementById('toss-decision-select');

            const tossWinnerTeamId = winnerSelect ? winnerSelect.value : '';
            const decision = decisionSelect ? decisionSelect.value : 'BAT';

            if (!tossWinnerTeamId) {
                showScoringAlert('Please select the Toss Winner team.');
                return;
            }

            try {
                confirmTossBtn.disabled = true;
                confirmTossBtn.textContent = 'Saving Toss...';

                await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/toss`, {
                    tossWinnerTeamId,
                    decision
                });

                ApiService.showToast('Toss recorded successfully!', 'success');
                await loadScoringDashboardState();

            } catch (err) {
                console.error('Failed to record toss:', err);
                showScoringAlert(err.message || 'Failed to record toss.');
            } finally {
                confirmTossBtn.disabled = false;
                confirmTossBtn.textContent = 'Confirm Toss';
            }
        });
    }

    // Start 2nd Innings Trigger Button (from Innings Break)
    const btnStartInnings2 = document.getElementById('btn-trigger-start-innings2');
    if (btnStartInnings2) {
        btnStartInnings2.addEventListener('click', () => {
            if (!scoringState) return;
            const viewInningsBreak = document.getElementById('view-innings-break');
            const viewInningsSetup = document.getElementById('view-innings-setup');

            if (viewInningsBreak) viewInningsBreak.style.display = 'none';
            if (viewInningsSetup) viewInningsSetup.style.display = 'block';

            // Swap teams for Innings 2 setup
            scoringState.activeInningsNumber = 2;
            const tempBatting = scoringState.battingPlayingXi;
            scoringState.battingPlayingXi = scoringState.bowlingPlayingXi;
            scoringState.bowlingPlayingXi = tempBatting;

            const tempBatName = scoringState.battingTeamName;
            scoringState.battingTeamName = scoringState.bowlingTeamName;
            scoringState.bowlingTeamName = tempBatName;

            populateInningsSetup(scoringState);
        });
    }

    // Start Innings
    const startInningsBtn = document.getElementById('btn-start-innings');
    if (startInningsBtn) {
        startInningsBtn.addEventListener('click', async () => {
            if (!currentMatchId) return;

            const strikerUserId = document.getElementById('select-striker')?.value || '';
            const nonStrikerUserId = document.getElementById('select-non-striker')?.value || '';
            const bowlerUserId = document.getElementById('select-bowler')?.value || '';

            if (!strikerUserId || !nonStrikerUserId || !bowlerUserId) {
                showScoringAlert('Please select Striker, Non-Striker, and Opening Bowler.');
                return;
            }

            if (strikerUserId === nonStrikerUserId) {
                showScoringAlert('Striker and Non-Striker must be different players.');
                return;
            }

            const innNum = (scoringState && scoringState.activeInningsNumber === 2) || (scoringState && scoringState.isInningsBreak) ? 2 : 1;

            if (isPlayerCurrentKeeper(bowlerUserId)) {
                pendingBowlerAction = {
                    type: 'START_INNINGS',
                    data: { inningsNumber: innNum, strikerUserId, nonStrikerUserId, bowlerUserId }
                };
                openChangeKeeperModal(bowlerUserId, 'The selected opening bowler is currently designated as Wicketkeeper. Please select a new Wicketkeeper for the fielding team.');
                return;
            }

            try {
                startInningsBtn.disabled = true;
                startInningsBtn.textContent = 'Starting Innings...';

                await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/innings/start`, {
                    inningsNumber: innNum,
                    strikerUserId,
                    nonStrikerUserId,
                    bowlerUserId
                });

                ApiService.showToast(`Innings ${innNum} Started! Ready for scoring.`, 'success');
                await loadScoringDashboardState();

            } catch (err) {
                console.error('Failed to start innings:', err);
                showScoringAlert(err.message || 'Failed to start innings.');
            } finally {
                startInningsBtn.disabled = false;
                startInningsBtn.textContent = 'Start Innings Scoring';
            }
        });
    }

    // Ball Delivery Trigger Button (⚾ BALL)
    const btnDeliverBall = document.getElementById('btn-deliver-ball');
    const ballBanner = document.getElementById('ball-in-progress-banner');
    if (btnDeliverBall) {
        btnDeliverBall.addEventListener('click', async () => {
            if (!verifyAuthorizedScorer()) return;

            btnDeliverBall.style.background = 'linear-gradient(135deg, #f59e0b, #d97706)';
            btnDeliverBall.style.borderColor = '#fbbf24';
            btnDeliverBall.innerHTML = '⚡ BALL IN PROGRESS...';

            if (ballBanner) ballBanner.style.display = 'block';
            ApiService.showToast('⚾ Ball in flight! Select outcome (Runs / Extra / Wicket) below.', 'info');

            const payload = {
                type: 'BALL_IN_PROGRESS',
                matchId: currentMatchId,
                bowlerName: scoringState?.currentBowler?.name || 'Bowler',
                strikerName: scoringState?.striker?.name || 'Striker',
                overNumber: scoringState?.completedOvers || 0,
                ballNumber: (scoringState?.currentBalls || 0) + 1
            };

            // 1. Instant cross-tab broadcast (0ms latency!)
            if (liveChannel) {
                try { liveChannel.postMessage(payload); } catch (e) {}
            }

            // 2. Broadcast over backend WebSocket for other viewers
            if (currentMatchId) {
                try {
                    ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/ball-in-progress`, {}).catch(() => {});
                } catch (e) {}
            }
        });
    }

    // Quick Delivery Scoring Run Buttons (0, 1, 2, 3, 4, 6)
    document.querySelectorAll('.sc-btn-run, .sc-btn-boundary-four, .sc-btn-boundary-six').forEach(btn => {
        btn.addEventListener('click', async () => {
            if (!verifyAuthorizedScorer()) return;
            const runs = parseInt(btn.getAttribute('data-runs'), 10);
            const isBoundary = btn.classList.contains('sc-btn-boundary-four') || btn.classList.contains('sc-btn-boundary-six') || runs === 4 || runs === 6;
            await recordBallEvent(runs, 'NONE', isBoundary);
        });
    });

    // Extras Buttons (WIDE, NO_BALL, BYE, LEG_BYE)
    document.querySelectorAll('.sc-btn-extra').forEach(btn => {
        btn.addEventListener('click', async () => {
            if (!verifyAuthorizedScorer()) return;
            const extraType = btn.getAttribute('data-extra');
            if (extraType === 'NO_BALL') {
                openNoBallModal();
            } else if (extraType === 'BYE' || extraType === 'LEG_BYE') {
                await recordBallEvent(1, extraType, false);
            } else {
                await recordBallEvent(0, extraType, false);
            }
        });
    });

    // No Ball Run Selection Modal Buttons
    document.querySelectorAll('.btn-noball-option').forEach(btn => {
        btn.addEventListener('click', async () => {
            if (!verifyAuthorizedScorer()) return;
            const runs = parseInt(btn.getAttribute('data-runs'), 10) || 0;
            const isBoundary = runs === 4 || runs === 6;
            closeNoBallModal();
            await recordBallEvent(runs, 'NO_BALL', isBoundary);
        });
    });

    const closeNbBtn = document.getElementById('btn-close-no-ball-modal');
    const cancelNbBtn = document.getElementById('btn-cancel-no-ball-modal');
    if (closeNbBtn) closeNbBtn.addEventListener('click', closeNoBallModal);
    if (cancelNbBtn) cancelNbBtn.addEventListener('click', closeNoBallModal);

    // Wicket Button Trigger
    const btnWicketTrigger = document.getElementById('btn-trigger-wicket');
    if (btnWicketTrigger) {
        btnWicketTrigger.addEventListener('click', () => {
            if (!verifyAuthorizedScorer()) return;
            openNextBatterModal();
        });
    }

    // Wicket Type change event (toggle "Who is Out?", "Who Caught It?", "Runs Scored", and "Crossed")
    const selectWicketType = document.getElementById('select-wicket-type');
    const groupDismissedPlayer = document.getElementById('group-dismissed-player');
    const groupCatcherPlayer = document.getElementById('group-catcher-player');
    const groupWicketRuns = document.getElementById('group-wicket-runs');
    const selectWicketRuns = document.getElementById('select-wicket-runs');
    const groupWicketCrossed = document.getElementById('group-wicket-crossed');

    if (selectWicketType) {
        selectWicketType.addEventListener('change', () => {
            const val = selectWicketType.value;
            if (val === 'RUN_OUT') {
                if (groupDismissedPlayer) groupDismissedPlayer.style.display = 'block';
                if (groupCatcherPlayer) groupCatcherPlayer.style.display = 'none';
                if (groupWicketRuns) groupWicketRuns.style.display = 'block';
                if (groupWicketCrossed) groupWicketCrossed.style.display = 'block';
            } else if (val === 'CAUGHT') {
                if (groupDismissedPlayer) groupDismissedPlayer.style.display = 'none';
                if (groupCatcherPlayer) groupCatcherPlayer.style.display = 'block';
                if (groupWicketRuns) groupWicketRuns.style.display = 'none';
                if (groupWicketCrossed) groupWicketCrossed.style.display = 'none';
                if (selectWicketRuns) selectWicketRuns.value = '0';
            } else {
                if (groupDismissedPlayer) groupDismissedPlayer.style.display = 'none';
                if (groupCatcherPlayer) groupCatcherPlayer.style.display = 'none';
                if (groupWicketRuns) groupWicketRuns.style.display = 'none';
                if (groupWicketCrossed) groupWicketCrossed.style.display = 'none';
                if (selectWicketRuns) selectWicketRuns.value = '0';
            }
        });
    }

    // Confirm Wicket & Next Batter Modal Button
    const btnConfirmBatter = document.getElementById('btn-confirm-next-batter');
    if (btnConfirmBatter) {
        btnConfirmBatter.addEventListener('click', async () => {
            if (!currentMatchId) return;
            const wicketType = document.getElementById('select-wicket-type')?.value || 'BOWLED';
            const groupDropdown = document.getElementById('group-next-batter-select');
            const dropdown = document.getElementById('select-next-batter-dropdown');
            const dismissedDropdown = document.getElementById('select-dismissed-player-dropdown');
            const catcherDropdown = document.getElementById('select-catcher-player-dropdown');
            const crossedDropdown = document.getElementById('select-wicket-crossed');

            const isAllOut = groupDropdown && groupDropdown.style.display === 'none';
            const batterUserId = dropdown ? dropdown.value : '';
            const strikerUserId = scoringState && scoringState.striker ? scoringState.striker.userId : '';
            const nonStrikerUserId = scoringState && scoringState.nonStriker ? scoringState.nonStriker.userId : '';

            // For RUN_OUT, use selected dismissed player; for all other wicket types, default to Striker
            const dismissedUserId = (wicketType === 'RUN_OUT')
                ? (dismissedDropdown && dismissedDropdown.value ? dismissedDropdown.value : strikerUserId)
                : strikerUserId;

            // For CAUGHT, capture selected opponent fielder who took the catch
            const fielderUserId = (wicketType === 'CAUGHT')
                ? (catcherDropdown ? catcherDropdown.value : '')
                : '';

            const crossedAtDismissal = (wicketType === 'RUN_OUT')
                ? (crossedDropdown && crossedDropdown.value === 'true')
                : false;

            if (!isAllOut && !batterUserId) {
                showScoringAlert('Please select the next batter entering the crease.');
                return;
            }

            // Only count runs scored on wicket ball if dismissal is RUN_OUT
            const wicketRuns = (wicketType === 'RUN_OUT')
                ? (parseInt(document.getElementById('select-wicket-runs')?.value, 10) || 0)
                : 0;

            try {
                btnConfirmBatter.disabled = true;

                // 1. Record ball with Wicket, runs, dismissedUserId, fielderUserId, crossedAtDismissal, and newBatterUserId
                const updatedState = await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/ball`, {
                    runs: wicketRuns,
                    runsCompleted: wicketRuns,
                    extraType: 'NONE',
                    isWicket: true,
                    wicketType: wicketType,
                    dismissedUserId: dismissedUserId,
                    fielderUserId: fielderUserId,
                    crossedAtDismissal: crossedAtDismissal,
                    newBatterUserId: isAllOut ? null : batterUserId
                });

                // Also call next-batter endpoint as backup if newBatter was not set
                if (!isAllOut && batterUserId && (!updatedState || !updatedState.striker || (updatedState.striker.userId !== batterUserId && updatedState.nonStriker?.userId !== batterUserId))) {
                    const isNonStrikerOut = (nonStrikerUserId && dismissedUserId === nonStrikerUserId);
                    const position = isNonStrikerOut ? 'NON_STRIKER' : 'STRIKER';

                    await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/next-batter`, {
                        batterUserId: batterUserId,
                        position: position
                    });
                }

                // Trigger Wicket celebration
                if (window.CelebrationManager && updatedState && updatedState.latestCelebrationType && updatedState.latestCelebrationId) {
                    CelebrationManager.trigger(updatedState.latestCelebrationType, updatedState.latestCelebrationId, currentMatchId);
                }
                if (liveChannel && updatedState && updatedState.latestCelebrationType && updatedState.latestCelebrationId) {
                    try {
                        liveChannel.postMessage({
                            type: 'CELEBRATION',
                            event: updatedState.latestCelebrationType,
                            matchId: currentMatchId,
                            celebrationId: updatedState.latestCelebrationId
                        });
                    } catch (e) {}
                }

                ApiService.showToast(isAllOut ? '10th Wicket Recorded! Innings All Out.' : 'Wicket & Next Batter recorded!', 'success');
                const modal = document.getElementById('next-batter-modal');
                if (modal) modal.style.display = 'none';

                resetBallDeliveryUI();
                await loadScoringDashboardState();

            } catch (err) {
                console.error('Failed to record wicket:', err);
                showScoringAlert(err.message || 'Failed to record wicket.');
            } finally {
                btnConfirmBatter.disabled = false;
            }
        });
    }

    // Confirm Next Bowler Modal Button (Over Completion)
    const btnConfirmBowler = document.getElementById('btn-confirm-next-bowler');
    if (btnConfirmBowler) {
        btnConfirmBowler.addEventListener('click', async () => {
            if (!currentMatchId) return;
            const bowlerUserId = document.getElementById('select-next-bowler-dropdown')?.value || '';

            if (!bowlerUserId) {
                showScoringAlert('Please select the bowler for the next over.');
                return;
            }

            if (isPlayerCurrentKeeper(bowlerUserId)) {
                const modal = document.getElementById('next-bowler-modal');
                if (modal) modal.style.display = 'none';
                pendingBowlerAction = {
                    type: 'NEXT_BOWLER',
                    data: { bowlerUserId }
                };
                openChangeKeeperModal(bowlerUserId, 'The selected bowler is currently designated as Wicketkeeper. Please select a new Wicketkeeper for the fielding team.');
                return;
            }

            try {
                btnConfirmBowler.disabled = true;

                await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/next-bowler`, {
                    bowlerUserId: bowlerUserId
                });

                ApiService.showToast('Next Bowler set successfully!', 'success');
                const modal = document.getElementById('next-bowler-modal');
                if (modal) modal.style.display = 'none';

                await loadScoringDashboardState();

            } catch (err) {
                console.error('Failed to set next bowler:', err);
                showScoringAlert(err.message || 'Failed to set next bowler.');
            } finally {
                btnConfirmBowler.disabled = false;
            }
        });
    }

    // Undo Last Ball
    const btnUndo = document.getElementById('btn-undo-last-ball');
    if (btnUndo) {
        btnUndo.addEventListener('click', async () => {
            if (!verifyAuthorizedScorer()) return;
            try {
                btnUndo.disabled = true;
                await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/undo`, {});
                ApiService.showToast('Undid last ball delivery.', 'info');
                await loadScoringDashboardState();
            } catch (err) {
                showScoringAlert(err.message || 'Failed to undo last ball.');
            } finally {
                btnUndo.disabled = false;
            }
        });
    }

    // Match Settings Modal Events
    const settingsBtn = document.getElementById('btn-open-settings');
    const settingsModal = document.getElementById('match-settings-modal');
    const closeSettingsBtn = document.getElementById('btn-close-settings-modal');
    const cancelSettingsBtn = document.getElementById('btn-cancel-settings');
    const saveSettingsBtn = document.getElementById('btn-save-settings');

    if (settingsBtn && settingsModal) {
        settingsBtn.addEventListener('click', () => {
            if (!verifyAuthorizedScorer()) return;
            if (!scoringState) return;
            populateSettingsModal(scoringState);
            syncCelebToggles();
            settingsModal.style.display = 'flex';
        });
    }

    const closeSettingsFunc = () => {
        if (settingsModal) settingsModal.style.display = 'none';
    };

    if (closeSettingsBtn) closeSettingsBtn.addEventListener('click', closeSettingsFunc);
    if (cancelSettingsBtn) cancelSettingsBtn.addEventListener('click', closeSettingsFunc);

    if (saveSettingsBtn) {
        saveSettingsBtn.addEventListener('click', async () => {
            if (!currentMatchId) return;
            const wideRunEnabled = document.getElementById('set-wide-run')?.checked ?? true;
            const noBallRunEnabled = document.getElementById('set-noball-run')?.checked ?? true;
            const noBallFreeHitEnabled = document.getElementById('set-freehit')?.checked ?? true;
            const byeRunEnabled = document.getElementById('set-byes-run')?.checked ?? true;
            const legByeRunEnabled = document.getElementById('set-byes-run')?.checked ?? true;
            const allowConsecutiveOvers = document.getElementById('set-consecutive-overs')?.checked ?? false;
            const maxOversPerBowler = parseInt(document.getElementById('set-max-overs-bowler')?.value, 10) || 4;

            try {
                saveSettingsBtn.disabled = true;

                await ApiService.put(`/matches/${encodeURIComponent(currentMatchId)}/scoring/settings`, {
                    wideRunEnabled,
                    noBallRunEnabled,
                    noBallFreeHitEnabled,
                    byeRunEnabled,
                    legByeRunEnabled,
                    maxOversPerBowler,
                    allowConsecutiveOvers
                });

                ApiService.showToast('Match scoring settings saved!', 'success');
                closeSettingsFunc();
                await loadScoringDashboardState();

            } catch (err) {
                showScoringAlert(err.message || 'Failed to save match settings.');
            } finally {
                saveSettingsBtn.disabled = false;
            }
        });
    }

    // Celebration Animations Modal & Preview Handlers
    const celebModalBtn = document.getElementById('btn-open-celeb-settings');
    const celebModal = document.getElementById('celebration-settings-modal');
    const closeCelebModalBtn = document.getElementById('btn-close-celeb-modal');
    const closeCelebFooterBtn = document.getElementById('btn-close-celeb-modal-footer');

    const syncCelebToggles = () => {
        const isEnabled = (window.CelebrationManager) ? CelebrationManager.isEnabled() : true;
        const t1 = document.getElementById('set-celeb-enabled-toggle');
        const t2 = document.getElementById('celeb-modal-toggle-enabled');
        if (t1) t1.checked = isEnabled;
        if (t2) t2.checked = isEnabled;
    };

    if (celebModalBtn && celebModal) {
        celebModalBtn.addEventListener('click', () => {
            if (!verifyAuthorizedScorer()) return;
            syncCelebToggles();
            celebModal.style.display = 'flex';
        });
    }

    const closeCelebFunc = () => {
        if (celebModal) celebModal.style.display = 'none';
    };
    if (closeCelebModalBtn) closeCelebModalBtn.addEventListener('click', closeCelebFunc);
    if (closeCelebFooterBtn) closeCelebFooterBtn.addEventListener('click', closeCelebFunc);

    const onCelebToggleChange = (e) => {
        const val = e.target.checked;
        if (window.CelebrationManager) {
            CelebrationManager.setEnabled(val);
        }
        const t1 = document.getElementById('set-celeb-enabled-toggle');
        const t2 = document.getElementById('celeb-modal-toggle-enabled');
        if (t1) t1.checked = val;
        if (t2) t2.checked = val;
        ApiService.showToast(`Celebration animations ${val ? 'enabled' : 'disabled'}`, 'info');
    };

    const celebToggle1 = document.getElementById('set-celeb-enabled-toggle');
    const celebToggle2 = document.getElementById('celeb-modal-toggle-enabled');
    if (celebToggle1) celebToggle1.addEventListener('change', onCelebToggleChange);
    if (celebToggle2) celebToggle2.addEventListener('change', onCelebToggleChange);

    // Initial sync
    syncCelebToggles();

    // Wire up all Preview Buttons (Six, Four, Wicket) across both modals
    document.querySelectorAll('.btn-celeb-preview-six').forEach(btn => {
        btn.addEventListener('click', () => {
            if (window.CelebrationManager) CelebrationManager.preview('SIX');
        });
    });
    document.querySelectorAll('.btn-celeb-preview-four').forEach(btn => {
        btn.addEventListener('click', () => {
            if (window.CelebrationManager) CelebrationManager.preview('FOUR');
        });
    });
    document.querySelectorAll('.btn-celeb-preview-wicket').forEach(btn => {
        btn.addEventListener('click', () => {
            if (window.CelebrationManager) CelebrationManager.preview('WICKET');
        });
    });

    // Wicketkeeper Change Modal Handlers
    const openKeeperBtn = document.getElementById('btn-open-keeper-modal');
    if (openKeeperBtn) {
        openKeeperBtn.addEventListener('click', () => {
            if (!verifyAuthorizedScorer()) return;
            pendingBowlerAction = null;
            openChangeKeeperModal();
        });
    }

    const closeKeeperBtn = document.getElementById('btn-close-keeper-modal');
    const cancelKeeperBtn = document.getElementById('btn-cancel-change-keeper');
    if (closeKeeperBtn) closeKeeperBtn.addEventListener('click', () => { pendingBowlerAction = null; closeChangeKeeperModal(); });
    if (cancelKeeperBtn) cancelKeeperBtn.addEventListener('click', () => { pendingBowlerAction = null; closeChangeKeeperModal(); });

    const confirmKeeperBtn = document.getElementById('btn-confirm-change-keeper');
    if (confirmKeeperBtn) {
        confirmKeeperBtn.addEventListener('click', async () => {
            if (!currentMatchId) return;
            const newKeeperUserId = document.getElementById('select-new-keeper-dropdown')?.value || '';

            if (!newKeeperUserId) {
                showScoringAlert('Please select a player to be the new Wicketkeeper.');
                return;
            }

            try {
                confirmKeeperBtn.disabled = true;
                confirmKeeperBtn.textContent = 'Updating...';

                await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/change-keeper`, {
                    keeperUserId: newKeeperUserId
                });

                ApiService.showToast('Wicketkeeper updated successfully!', 'success');
                closeChangeKeeperModal();

                if (pendingBowlerAction) {
                    const action = pendingBowlerAction;
                    pendingBowlerAction = null;

                    if (action.type === 'START_INNINGS') {
                        await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/innings/start`, action.data);
                        ApiService.showToast(`Innings ${action.data.inningsNumber} Started!`, 'success');
                        await loadScoringDashboardState();
                    } else if (action.type === 'NEXT_BOWLER') {
                        await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/next-bowler`, action.data);
                        ApiService.showToast('Next Bowler set successfully!', 'success');
                        await loadScoringDashboardState();
                    }
                } else {
                    await loadScoringDashboardState();
                }

            } catch (err) {
                console.error('Failed to change Wicketkeeper:', err);
                showScoringAlert(err.message || 'Failed to change Wicketkeeper.');
            } finally {
                confirmKeeperBtn.disabled = false;
                confirmKeeperBtn.textContent = 'Confirm Wicketkeeper';
            }
        });
    }

    // Retire / Change Batter Modal Handlers
    const openRetireBtn = document.getElementById('btn-open-retire-modal');
    if (openRetireBtn) {
        openRetireBtn.addEventListener('click', () => {
            if (!verifyAuthorizedScorer()) return;
            openRetireBatterModal();
        });
    }

    const closeRetireBtn = document.getElementById('btn-close-retire-modal');
    const cancelRetireBtn = document.getElementById('btn-cancel-retire-batter');
    if (closeRetireBtn) closeRetireBtn.addEventListener('click', closeRetireBatterModal);
    if (cancelRetireBtn) cancelRetireBtn.addEventListener('click', closeRetireBatterModal);

    const confirmRetireBtn = document.getElementById('btn-confirm-retire-batter');
    if (confirmRetireBtn) {
        confirmRetireBtn.addEventListener('click', async () => {
            if (!currentMatchId) return;

            const retiringUserId = document.getElementById('select-retiring-batter-dropdown')?.value || '';
            const newBatterUserId = document.getElementById('select-incoming-batter-dropdown')?.value || '';
            const retirementType = document.getElementById('select-retire-type-dropdown')?.value || 'RETIRED_OUT';

            if (!retiringUserId) {
                showScoringAlert('Please select which active batter is retiring.');
                return;
            }

            if (!newBatterUserId) {
                showScoringAlert('Please select the new incoming batter.');
                return;
            }

            try {
                confirmRetireBtn.disabled = true;
                confirmRetireBtn.textContent = 'Updating...';

                await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/retire-batter`, {
                    retiringUserId,
                    newBatterUserId,
                    retirementType
                });

                ApiService.showToast('Batter retired and replaced successfully!', 'success');
                closeRetireBatterModal();
                await loadScoringDashboardState();

            } catch (err) {
                console.error('Failed to retire batter:', err);
                showScoringAlert(err.message || 'Failed to retire batter.');
            } finally {
                confirmRetireBtn.disabled = false;
                confirmRetireBtn.textContent = 'Confirm Batter Change';
            }
        });
    }

    // Pause Match Modal Events
    const btnPauseMatch = document.getElementById('btn-pause-match');
    const pauseModal = document.getElementById('pause-match-modal');
    const btnClosePause = document.getElementById('btn-close-pause-modal');
    const btnCancelPause = document.getElementById('btn-cancel-pause-modal');
    const btnConfirmPause = document.getElementById('btn-confirm-pause-match');
    const customReasonGroup = document.getElementById('group-pause-custom-reason');
    const customReasonInput = document.getElementById('input-custom-pause-reason');

    const openPauseModal = () => {
        if (!pauseModal) return;
        if (customReasonInput) customReasonInput.value = '';
        if (customReasonGroup) customReasonGroup.style.display = 'none';
        const defaultRadio = document.querySelector('input[name="pause-reason-opt"][value="Rain"]');
        if (defaultRadio) defaultRadio.checked = true;
        pauseModal.style.display = 'flex';
    };

    const closePauseModal = () => {
        if (pauseModal) pauseModal.style.display = 'none';
    };

    if (btnPauseMatch) btnPauseMatch.addEventListener('click', openPauseModal);
    if (btnClosePause) btnClosePause.addEventListener('click', closePauseModal);
    if (btnCancelPause) btnCancelPause.addEventListener('click', closePauseModal);

    document.querySelectorAll('input[name="pause-reason-opt"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            if (customReasonGroup) {
                customReasonGroup.style.display = (e.target.value === 'Other') ? 'block' : 'none';
            }
        });
    });

    if (btnConfirmPause) {
        btnConfirmPause.addEventListener('click', async () => {
            if (!currentMatchId) return;
            const selectedRadio = document.querySelector('input[name="pause-reason-opt"]:checked');
            const reason = selectedRadio ? selectedRadio.value : 'Rain';
            let customReason = null;
            if (reason === 'Other') {
                customReason = customReasonInput ? customReasonInput.value.trim() : '';
                if (!customReason) {
                    showScoringAlert('Please specify the custom reason for pausing.');
                    return;
                }
            }

            try {
                btnConfirmPause.disabled = true;
                btnConfirmPause.textContent = 'Pausing...';

                const updatedState = await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/pause`, {
                    reason,
                    customReason
                });

                closePauseModal();
                ApiService.showToast('Match PAUSED successfully.', 'info');

                if (liveChannel && updatedState) {
                    try {
                        liveChannel.postMessage({
                            type: 'MATCH_UPDATE',
                            matchId: currentMatchId,
                            state: updatedState
                        });
                    } catch (e) {}
                }

                await loadScoringDashboardState();

            } catch (err) {
                console.error('Failed to pause match:', err);
                showScoringAlert(err.message || 'Failed to pause match.');
            } finally {
                btnConfirmPause.disabled = false;
                btnConfirmPause.textContent = 'Confirm Pause';
            }
        });
    }

    // Resume Match Modal Events
    const btnResumeMatch = document.getElementById('btn-resume-match');
    const btnBannerResume = document.getElementById('btn-banner-resume');
    const resumeModal = document.getElementById('resume-match-modal');
    const btnCancelResume = document.getElementById('btn-cancel-resume-modal');
    const btnConfirmResume = document.getElementById('btn-confirm-resume-match');

    const openResumeModal = () => {
        if (resumeModal) resumeModal.style.display = 'flex';
    };

    const closeResumeModal = () => {
        if (resumeModal) resumeModal.style.display = 'none';
    };

    if (btnResumeMatch) btnResumeMatch.addEventListener('click', openResumeModal);
    if (btnBannerResume) btnBannerResume.addEventListener('click', openResumeModal);
    if (btnCancelResume) btnCancelResume.addEventListener('click', closeResumeModal);

    if (btnConfirmResume) {
        btnConfirmResume.addEventListener('click', async () => {
            if (!currentMatchId) return;
            try {
                btnConfirmResume.disabled = true;
                btnConfirmResume.textContent = 'Resuming...';

                const updatedState = await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/resume`, {});

                closeResumeModal();
                ApiService.showToast('Match RESUMED! Scoring re-enabled.', 'success');

                if (liveChannel && updatedState) {
                    try {
                        liveChannel.postMessage({
                            type: 'MATCH_UPDATE',
                            matchId: currentMatchId,
                            state: updatedState
                        });
                    } catch (e) {}
                }

                await loadScoringDashboardState();

            } catch (err) {
                console.error('Failed to resume match:', err);
                showScoringAlert(err.message || 'Failed to resume match.');
            } finally {
                btnConfirmResume.disabled = false;
                btnConfirmResume.textContent = 'Yes, Resume Match';
            }
        });
    }
}

function verifyAuthorizedScorer() {
    if (scoringState && (scoringState.status === 'COMPLETED' || scoringState.isMatchCompleted === true)) {
        ApiService.showToast('Match is already completed. Scoring controls are disabled.', 'warning');
        return false;
    }
    if (!checkIsScorerMode(scoringState)) {
        ApiService.showToast('Only the authorized scorer or match captain can operate scoring controls.', 'warning');
        return false;
    }
    if (scoringState && (scoringState.status === 'PAUSED' || scoringState.isPaused)) {
        ApiService.showToast('Match is currently paused. Resume match before scoring.', 'warning');
        return false;
    }
    return true;
}

function disableScoringControls(disabled) {
    const selector = '.sc-btn-run, .sc-btn-boundary-four, .sc-btn-boundary-six, .sc-btn-extra, .sc-btn-wicket-large, #btn-deliver-ball, #btn-undo-last-ball';
    document.querySelectorAll(selector).forEach(btn => {
        btn.disabled = disabled;
        btn.style.opacity = disabled ? '0.4' : '1';
        btn.style.pointerEvents = disabled ? 'none' : 'auto';
    });
}

function resetBallDeliveryUI() {
    const btnDeliverBall = document.getElementById('btn-deliver-ball');
    const ballBanner = document.getElementById('ball-in-progress-banner');
    if (btnDeliverBall) {
        btnDeliverBall.style.background = 'linear-gradient(135deg, #10b981, #059669)';
        btnDeliverBall.style.borderColor = '#34d399';
        btnDeliverBall.innerHTML = '⚾ BALL';
    }
    if (ballBanner) ballBanner.style.display = 'none';
}

async function recordBallEvent(runs, extraType, isBoundary = false) {
    if (!currentMatchId) return;

    // 1. Instant Optimistic UI feedback (< 1ms)
    if (scoringState) {
        try {
            if (extraType === 'NONE') {
                scoringState.totalRuns = (scoringState.totalRuns || 0) + runs;
                const curBalls = (scoringState.currentBalls || 0) + 1;
                if (curBalls === 6) {
                    scoringState.completedOvers = (scoringState.completedOvers || 0) + 1;
                    scoringState.currentBalls = 0;
                    scoringState.bowlingEnd = scoringState.bowlingEnd === 'END_B' ? 'END_A' : 'END_B';
                } else {
                    scoringState.currentBalls = curBalls;
                }
                if (scoringState.striker) {
                    scoringState.striker.runs = (scoringState.striker.runs || 0) + runs;
                    scoringState.striker.balls = (scoringState.striker.balls || 0) + 1;
                    if (runs === 4) scoringState.striker.fours = (scoringState.striker.fours || 0) + 1;
                    if (runs === 6) scoringState.striker.sixes = (scoringState.striker.sixes || 0) + 1;
                }
                if (scoringState.currentBowler) {
                    scoringState.currentBowler.runsConceded = (scoringState.currentBowler.runsConceded || 0) + runs;
                    scoringState.currentBowler.ballsBowled = (scoringState.currentBowler.ballsBowled || 0) + 1;
                }
                if (!scoringState.overSummaryBalls) scoringState.overSummaryBalls = [];
                scoringState.overSummaryBalls.push(String(runs));

                if (!isBoundary && runs % 2 !== 0 && scoringState.battingEndA && scoringState.battingEndB) {
                    const tmp = scoringState.battingEndA;
                    scoringState.battingEndA = scoringState.battingEndB;
                    scoringState.battingEndB = tmp;
                }
                if (scoringState.battingEndA && scoringState.battingEndB) {
                    if (scoringState.bowlingEnd === 'END_B') {
                        scoringState.striker = scoringState.battingEndA;
                        scoringState.nonStriker = scoringState.battingEndB;
                    } else {
                        scoringState.striker = scoringState.battingEndB;
                        scoringState.nonStriker = scoringState.battingEndA;
                    }
                }
            } else if (extraType === 'WIDE') {
                const totalExtra = 1 + runs;
                scoringState.totalRuns = (scoringState.totalRuns || 0) + totalExtra;
                scoringState.totalExtras = (scoringState.totalExtras || 0) + totalExtra;
                if (scoringState.currentBowler) {
                    scoringState.currentBowler.runsConceded = (scoringState.currentBowler.runsConceded || 0) + totalExtra;
                }
                if (!scoringState.overSummaryBalls) scoringState.overSummaryBalls = [];
                scoringState.overSummaryBalls.push(runs > 0 ? `Wd+${runs}` : 'Wd');

                if (runs % 2 !== 0 && scoringState.battingEndA && scoringState.battingEndB) {
                    const tmp = scoringState.battingEndA;
                    scoringState.battingEndA = scoringState.battingEndB;
                    scoringState.battingEndB = tmp;
                    if (scoringState.bowlingEnd === 'END_B') {
                        scoringState.striker = scoringState.battingEndA;
                        scoringState.nonStriker = scoringState.battingEndB;
                    } else {
                        scoringState.striker = scoringState.battingEndB;
                        scoringState.nonStriker = scoringState.battingEndA;
                    }
                }
            } else if (extraType === 'NO_BALL') {
                const totalExtra = 1 + runs;
                scoringState.totalRuns = (scoringState.totalRuns || 0) + totalExtra;
                scoringState.totalExtras = (scoringState.totalExtras || 0) + 1;
                if (scoringState.striker && runs > 0) {
                    scoringState.striker.runs = (scoringState.striker.runs || 0) + runs;
                    scoringState.striker.balls = (scoringState.striker.balls || 0) + 1;
                    if (runs === 4) scoringState.striker.fours = (scoringState.striker.fours || 0) + 1;
                    if (runs === 6) scoringState.striker.sixes = (scoringState.striker.sixes || 0) + 1;
                }
                if (scoringState.currentBowler) {
                    scoringState.currentBowler.runsConceded = (scoringState.currentBowler.runsConceded || 0) + totalExtra;
                }
                if (!scoringState.overSummaryBalls) scoringState.overSummaryBalls = [];
                scoringState.overSummaryBalls.push(runs > 0 ? `Nb+${runs}` : 'Nb');

                if (!isBoundary && runs % 2 !== 0 && scoringState.battingEndA && scoringState.battingEndB) {
                    const tmp = scoringState.battingEndA;
                    scoringState.battingEndA = scoringState.battingEndB;
                    scoringState.battingEndB = tmp;
                    if (scoringState.bowlingEnd === 'END_B') {
                        scoringState.striker = scoringState.battingEndA;
                        scoringState.nonStriker = scoringState.battingEndB;
                    } else {
                        scoringState.striker = scoringState.battingEndB;
                        scoringState.nonStriker = scoringState.battingEndA;
                    }
                }
            } else if (extraType === 'BYE' || extraType === 'LEG_BYE') {
                scoringState.totalRuns = (scoringState.totalRuns || 0) + runs;
                scoringState.totalExtras = (scoringState.totalExtras || 0) + runs;
                const curBalls = (scoringState.currentBalls || 0) + 1;
                if (curBalls === 6) {
                    scoringState.completedOvers = (scoringState.completedOvers || 0) + 1;
                    scoringState.currentBalls = 0;
                    scoringState.bowlingEnd = scoringState.bowlingEnd === 'END_B' ? 'END_A' : 'END_B';
                } else {
                    scoringState.currentBalls = curBalls;
                }
                if (scoringState.striker) {
                    scoringState.striker.balls = (scoringState.striker.balls || 0) + 1;
                }
                if (scoringState.currentBowler) {
                    scoringState.currentBowler.ballsBowled = (scoringState.currentBowler.ballsBowled || 0) + 1;
                }
                if (!scoringState.overSummaryBalls) scoringState.overSummaryBalls = [];
                scoringState.overSummaryBalls.push(extraType === 'BYE' ? `B+${runs}` : `LB+${runs}`);

                if (!isBoundary && runs % 2 !== 0 && scoringState.battingEndA && scoringState.battingEndB) {
                    const tmp = scoringState.battingEndA;
                    scoringState.battingEndA = scoringState.battingEndB;
                    scoringState.battingEndB = tmp;
                }
                if (scoringState.battingEndA && scoringState.battingEndB) {
                    if (scoringState.bowlingEnd === 'END_B') {
                        scoringState.striker = scoringState.battingEndA;
                        scoringState.nonStriker = scoringState.battingEndB;
                    } else {
                        scoringState.striker = scoringState.battingEndB;
                        scoringState.nonStriker = scoringState.battingEndA;
                    }
                }
            }

            // Instant render with 0ms delay!
            renderScoringDashboard(scoringState);

            // Broadcast immediately to live viewer tab!
            if (liveChannel) {
                try {
                    liveChannel.postMessage({
                        type: 'MATCH_UPDATE',
                        matchId: currentMatchId,
                        state: scoringState
                    });
                } catch (e) {}
            }
        } catch (e) {}
    }

    try {
        const updatedState = await ApiService.post(`/matches/${encodeURIComponent(currentMatchId)}/scoring/ball`, {
            runs: runs,
            extraType: extraType,
            isWicket: false,
            isBoundary: isBoundary
        });

        resetBallDeliveryUI();

        // 2. Direct State Adoption: update immediately without redundant GET request!
        if (updatedState && updatedState.matchId) {
            scoringState = updatedState;
            renderScoringDashboard(updatedState);

            // Trigger celebration on SIX / FOUR / WICKET if recorded
            if (window.CelebrationManager && updatedState.latestCelebrationType && updatedState.latestCelebrationId) {
                CelebrationManager.trigger(updatedState.latestCelebrationType, updatedState.latestCelebrationId, currentMatchId);
            }

            if (liveChannel) {
                try {
                    liveChannel.postMessage({
                        type: 'MATCH_UPDATE',
                        matchId: currentMatchId,
                        state: updatedState
                    });
                    if (updatedState.latestCelebrationType && updatedState.latestCelebrationId) {
                        liveChannel.postMessage({
                            type: 'CELEBRATION',
                            event: updatedState.latestCelebrationType,
                            matchId: currentMatchId,
                            celebrationId: updatedState.latestCelebrationId
                        });
                    }
                } catch (e) {}
            }
        } else {
            await loadScoringDashboardState();
        }
    } catch (err) {
        console.error('Failed to record ball event:', err);
        showScoringAlert(err.message || 'Failed to record ball delivery.');
        await loadScoringDashboardState();
    }
}

function checkIsScorerMode(state) {
    if (!state) return false;

    // 1. Completed matches are NEVER in scorer mode — they are purely scorecard/result views
    const isMatchDone = (state.status === 'COMPLETED' || state.isMatchCompleted === true);
    if (isMatchDone) {
        return false;
    }

    // 2. Explicit spectator/viewer mode requested in query parameters
    const urlParams = new URLSearchParams(window.location.search);
    const modeParam = (urlParams.get('mode') || '').toLowerCase().trim();
    if (modeParam === 'view' || modeParam === 'spectator') {
        return false;
    }

    // 3. Match metadata permissions check
    if (currentMatchMeta) {
        if (currentMatchMeta.isScorer || currentMatchMeta.isCaptain || currentMatchMeta.isCreator) {
            return true;
        }
        if (currentUserId && state.scorerUserId && String(currentUserId).trim().toLowerCase() === String(state.scorerUserId).trim().toLowerCase()) {
            return true;
        }
        return false;
    }

    // Fallback if match metadata failed to load:
    if (currentUserId && state.scorerUserId && String(currentUserId).trim().toLowerCase() === String(state.scorerUserId).trim().toLowerCase()) {
        return true;
    }

    return Boolean(state.authorizedScorer || state.isAuthorizedScorer);
}

async function loadScoringDashboardState() {
    hideScoringAlert();
    try {
        const [stateRes, matchRes, userRes] = await Promise.allSettled([
            ApiService.get(`/matches/${encodeURIComponent(currentMatchId)}/scoring`),
            ApiService.get(`/matches/${encodeURIComponent(currentMatchId)}`),
            ApiService.get('/profile/me')
        ]);

        if (stateRes.status === 'fulfilled') {
            scoringState = stateRes.value;
        } else {
            throw stateRes.reason;
        }

        if (matchRes.status === 'fulfilled') {
            currentMatchMeta = matchRes.value;
        }

        if (userRes.status === 'fulfilled' && userRes.value) {
            currentUserId = userRes.value.userId;
        }

        renderScoringDashboard(scoringState);
        populateTossOptions(scoringState.teamA, scoringState.teamB);

        // Mark existing celebration as processed to avoid replaying on refresh / reload
        if (window.CelebrationManager && scoringState && scoringState.latestCelebrationId) {
            CelebrationManager.markProcessed(scoringState.latestCelebrationId);
        }
    } catch (err) {
        console.error('Failed to load scoring dashboard state:', err);
        showScoringAlert(err.message || 'Failed to load match scoring state.');
    }
}

function renderScoringDashboard(state) {
    if (!state) return;

    const matchId = state.matchId || 'MATCH------';
    const venue = state.venue || 'Venue Not Specified';
    const dateStr = formatDate(state.matchDate);
    const timeStr = formatTime(state.matchTime);
    const format = state.format || 'T20';
    const overs = state.overs || 20;
    const status = state.status || 'SCHEDULED';
    const scorerName = state.scorerName || 'Not Assigned';

    const isScorerMode = checkIsScorerMode(state);

    const teamA = state.teamA || {};
    const teamB = state.teamB || {};

    // Header info
    const isPaused = (status === 'PAUSED' || state.isPaused === true);
    const isInningsBreak = (status === 'INNINGS_BREAK' || state.isInningsBreak === true);
    const isMatchDone = (status === 'COMPLETED' || state.isMatchCompleted === true);

    if (document.getElementById('sc-match-title')) {
        document.getElementById('sc-match-title').textContent = `${teamA.name || 'Team A'} vs ${teamB.name || 'Team B'}`;
    }
    if (document.getElementById('sc-status-badge')) {
        const badge = document.getElementById('sc-status-badge');
        if (isPaused) {
            badge.textContent = '⏸️ PAUSED';
            badge.style.background = '#fef3c7';
            badge.style.color = '#b45309';
            badge.style.borderColor = '#fcd34d';
        } else if (isInningsBreak) {
            badge.textContent = '☕ INNINGS BREAK';
            badge.style.background = '#fef3c7';
            badge.style.color = '#b45309';
            badge.style.borderColor = '#fcd34d';
        } else if (status === 'LIVE') {
            badge.textContent = '🔴 LIVE';
            badge.style.background = '#fee2e2';
            badge.style.color = '#dc2626';
            badge.style.borderColor = '#fca5a5';
        } else if (isMatchDone) {
            badge.textContent = '🏆 COMPLETED';
            badge.style.background = '#064e3b';
            badge.style.color = '#34d399';
            badge.style.borderColor = '#10b981';
        } else {
            badge.textContent = status === 'PENDING_CONFIRMATION' ? 'PENDING' : status;
            badge.style.background = '#334155';
            badge.style.color = '#cbd5e1';
            badge.style.borderColor = '#475569';
        }
    }
    if (document.getElementById('sc-match-teams')) document.getElementById('sc-match-teams').textContent = `${teamA.name || 'Team A'} vs ${teamB.name || 'Team B'}`;
    if (document.getElementById('sc-match-venue')) document.getElementById('sc-match-venue').textContent = `📍 ${venue} \u2022 ${dateStr} \u2022 ${timeStr}`;

    // Header Scorer Action Controls
    const headerActions = document.getElementById('sc-header-actions') || document.querySelector('.sc-header-actions');
    const pauseBtn = document.getElementById('btn-pause-match');
    const resumeBtn = document.getElementById('btn-resume-match');
    const retireBtn = document.getElementById('btn-open-retire-modal');
    const keeperBtn = document.getElementById('btn-open-keeper-modal');
    const celebBtn = document.getElementById('btn-open-celeb-settings');
    const settingsBtn = document.getElementById('btn-open-settings');
    const btnBannerResume = document.getElementById('btn-banner-resume');
    const pauseBanner = document.getElementById('sc-pause-banner');
    const pauseReasonText = document.getElementById('sc-pause-reason-text');

    if (isScorerMode) {
        if (headerActions) headerActions.style.display = 'flex';
        if (retireBtn) retireBtn.style.display = 'inline-flex';
        if (keeperBtn) keeperBtn.style.display = 'inline-flex';
        if (celebBtn) celebBtn.style.display = 'inline-flex';
        if (settingsBtn) settingsBtn.style.display = 'inline-flex';

        if (isPaused) {
            if (pauseBtn) pauseBtn.style.display = 'none';
            if (resumeBtn) resumeBtn.style.display = 'inline-flex';
            if (btnBannerResume) btnBannerResume.style.display = 'inline-flex';
            if (pauseBanner) {
                pauseBanner.style.display = 'flex';
                if (pauseReasonText) pauseReasonText.textContent = `Reason: ${state.pauseReason || 'Match Paused'}`;
            }
            disableScoringControls(true);
        } else {
            if (pauseBtn) pauseBtn.style.display = 'inline-flex';
            if (resumeBtn) resumeBtn.style.display = 'none';
            if (btnBannerResume) btnBannerResume.style.display = 'none';
            if (pauseBanner) pauseBanner.style.display = 'none';
            disableScoringControls(false);
        }
    } else {
        if (headerActions) headerActions.style.display = 'none';
        if (retireBtn) retireBtn.style.display = 'none';
        if (keeperBtn) keeperBtn.style.display = 'none';
        if (celebBtn) celebBtn.style.display = 'none';
        if (settingsBtn) settingsBtn.style.display = 'none';
        if (pauseBtn) pauseBtn.style.display = 'none';
        if (resumeBtn) resumeBtn.style.display = 'none';
        if (btnBannerResume) btnBannerResume.style.display = 'none';

        if (isPaused && !isMatchDone) {
            if (pauseBanner) {
                pauseBanner.style.display = 'flex';
                if (pauseReasonText) pauseReasonText.textContent = `Reason: ${state.pauseReason || 'Match Paused'}`;
            }
        } else {
            if (pauseBanner) pauseBanner.style.display = 'none';
        }
        disableScoringControls(true);
    }

    // 4 Top Score Pills
    const totalRuns = state.totalRuns || 0;
    const totalWkts = state.totalWickets || 0;
    const completedOvers = state.completedOvers || 0;
    const currentBalls = state.currentBalls || 0;
    const crr = state.runRate || 0.0;

    if (document.getElementById('sc-pill-score')) document.getElementById('sc-pill-score').textContent = `${totalRuns}/${totalWkts}`;
    if (document.getElementById('sc-pill-overs')) document.getElementById('sc-pill-overs').textContent = `${completedOvers}.${currentBalls} /${overs}`;
    if (document.getElementById('sc-pill-crr')) document.getElementById('sc-pill-crr').textContent = crr.toFixed(2);

    if (state.targetRuns && document.getElementById('sc-pill-target-box')) {
        document.getElementById('sc-pill-target-box').style.display = 'block';
        if (document.getElementById('sc-pill-target')) document.getElementById('sc-pill-target').textContent = state.targetRuns;
    }

    // Read-only banner check (only show during ongoing/live match if user is in spectator view)
    const readonlyBanner = document.getElementById('sc-readonly-banner');
    if (readonlyBanner) {
        if (!isScorerMode && !isMatchDone) {
            readonlyBanner.style.display = 'block';
            const assignedNameEl = document.getElementById('sc-assigned-scorer-name');
            if (assignedNameEl) assignedNameEl.textContent = scorerName;
        } else {
            readonlyBanner.style.display = 'none';
        }
    }

    // Determine which View to display
    const viewPreMatch = document.getElementById('view-pre-match');
    const viewToss = document.getElementById('view-toss');
    const viewInningsSetup = document.getElementById('view-innings-setup');
    const viewInningsBreak = document.getElementById('view-innings-break');
    const viewCompleted = document.getElementById('view-completed-match');
    const viewWorkspace = document.getElementById('view-scoring-workspace');

    if (isMatchDone) {
        if (viewPreMatch) viewPreMatch.style.display = 'none';
        if (viewToss) viewToss.style.display = 'none';
        if (viewInningsSetup) viewInningsSetup.style.display = 'none';
        if (viewInningsBreak) viewInningsBreak.style.display = 'none';
        if (viewWorkspace) viewWorkspace.style.display = 'none';
        if (readonlyBanner) readonlyBanner.style.display = 'none';
        if (viewCompleted) viewCompleted.style.display = 'block';

        renderCompletedMatchView(state);

    } else if (!state.tossRecorded) {
        if (viewPreMatch) viewPreMatch.style.display = 'none';
        if (viewToss) viewToss.style.display = 'block';
        if (viewInningsSetup) viewInningsSetup.style.display = 'none';
        if (viewInningsBreak) viewInningsBreak.style.display = 'none';
        if (viewCompleted) viewCompleted.style.display = 'none';
        if (viewWorkspace) viewWorkspace.style.display = 'none';

        populateTossOptions(teamA, teamB);

    } else if ((status === 'LIVE' || isPaused) && state.tossRecorded && (!state.inningsStatus || state.inningsStatus === 'NOT_STARTED') && !isInningsBreak) {
        if (viewPreMatch) viewPreMatch.style.display = 'none';
        if (viewToss) viewToss.style.display = 'none';
        if (viewInningsSetup) viewInningsSetup.style.display = 'block';
        if (viewInningsBreak) viewInningsBreak.style.display = 'none';
        if (viewCompleted) viewCompleted.style.display = 'none';
        if (viewWorkspace) viewWorkspace.style.display = 'none';

        populateInningsSetup(state);

    } else if (isInningsBreak) {
        if (viewPreMatch) viewPreMatch.style.display = 'none';
        if (viewToss) viewToss.style.display = 'none';
        if (viewInningsSetup) viewInningsSetup.style.display = 'none';
        if (viewInningsBreak) viewInningsBreak.style.display = 'block';
        if (viewCompleted) viewCompleted.style.display = 'none';
        if (viewWorkspace) viewWorkspace.style.display = 'none';

        renderInningsBreak(state, isScorerMode);

    } else {
        if (viewPreMatch) viewPreMatch.style.display = 'none';
        if (viewToss) viewToss.style.display = 'none';
        if (viewInningsSetup) viewInningsSetup.style.display = 'none';
        if (viewInningsBreak) viewInningsBreak.style.display = 'none';
        if (viewCompleted) viewCompleted.style.display = 'none';
        if (viewWorkspace) viewWorkspace.style.display = 'block';

        renderScoringWorkspace(state);

        // Check if Over Completed -> Trigger Next Bowler Modal
        if (state.isOverCompleted && isScorerMode) {
            openNextBowlerModal(state.availableBowlers, state.previousBowlerUserId);
        }
    }
}

function renderCompletedMatchView(state) {
    const winnerEl = document.getElementById('sc-completed-winner-text');
    if (winnerEl) {
        winnerEl.textContent = state.matchResultSummary || 'Match Completed!';
    }

    const container = document.getElementById('sc-full-scorecard-container');
    if (!container) return;

    let html = '';

    const sc1 = state.innings1Scorecard;
    const sc2 = state.innings2Scorecard;

    if (sc1) {
        html += renderSingleInningsScorecard(sc1, 1);
    }
    if (sc2) {
        html += renderSingleInningsScorecard(sc2, 2);
    }

    if (!sc1 && !sc2) {
        html = '<div style="text-align: center; color: #94a3b8; padding: 2rem;">No scorecard data available for this match.</div>';
    }

    container.innerHTML = html;
}

function renderSingleInningsScorecard(sc, inningsNum) {
    const totalRuns = sc.totalRuns || 0;
    const totalWkts = sc.totalWickets || 0;
    const ov = sc.completedOvers || 0;
    const bl = sc.currentBalls || 0;
    const extras = sc.totalExtras || 0;
    const battingTeam = escapeHtml(sc.battingTeamName || `Innings ${inningsNum}`);
    const bowlingTeam = escapeHtml(sc.bowlingTeamName || `Opponent`);

    const battingList = sc.battingList || [];
    const bowlingList = sc.bowlingList || [];

    let battingRows = battingList.map((b, idx) => {
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

    if (battingRows === '') {
        battingRows = '<tr><td colspan="6" style="padding: 0.75rem; text-align: center; color: #64748b;">No batting data recorded</td></tr>';
    }

    // Requirement 2: ONLY DISPLAY a bowler if ballsBowled > 0
    const activeBowlers = bowlingList.filter(b => (b.ballsBowled || 0) > 0);
    let bowlingRows = activeBowlers.map(b => {
        const totalBalls = b.ballsBowled || 0;
        const ov = Math.floor(totalBalls / 6);
        const bl = totalBalls % 6;
        const oversStr = `${ov}.${bl}`;
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

    if (bowlingRows === '') {
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

function renderInningsBreak(state, isAuthorized) {
    const teamName = state.firstInningsTeamName || state.battingTeamName || 'Team';
    const runs = state.firstInningsRuns !== undefined ? state.firstInningsRuns : (state.totalRuns || 0);
    const wkts = state.firstInningsWickets !== undefined ? state.firstInningsWickets : (state.totalWickets || 0);
    const ovs = state.firstInningsOvers !== undefined ? state.firstInningsOvers : (state.completedOvers || 0);
    const bls = state.firstInningsBalls !== undefined ? state.firstInningsBalls : (state.currentBalls || 0);
    const target = state.targetRuns || (runs + 1);

    if (document.getElementById('sc-break-team-name')) {
        document.getElementById('sc-break-team-name').textContent = teamName;
    }
    if (document.getElementById('sc-break-score')) {
        document.getElementById('sc-break-score').textContent = `${runs}/${wkts}`;
    }
    if (document.getElementById('sc-break-overs')) {
        document.getElementById('sc-break-overs').textContent = `${ovs}${bls ? '.' + bls : ''} Overs`;
    }
    if (document.getElementById('sc-break-target')) {
        document.getElementById('sc-break-target').textContent = target;
    }
    if (document.getElementById('sc-break-summary')) {
        document.getElementById('sc-break-summary').textContent = 'Waiting for the second innings to begin...';
    }
    const controls = document.getElementById('sc-break-scorer-controls');
    if (controls) {
        controls.style.display = isAuthorized ? 'block' : 'none';
    }
}

function renderScoringWorkspace(state) {
    // Metrics Bar
    if (document.getElementById('metric-partnership')) {
        document.getElementById('metric-partnership').textContent = `${state.partnershipRuns || 0} (${state.partnershipBalls || 0})`;
    }
    if (document.getElementById('metric-last5-rr')) {
        document.getElementById('metric-last5-rr').textContent = `${(state.last5OversRunRate || state.runRate || 0).toFixed(2)} RR`;
    }
    if (document.getElementById('metric-required-rr')) {
        document.getElementById('metric-required-rr').textContent = (state.requiredRunRate || 0.0).toFixed(2);
    }
    if (document.getElementById('metric-balls-left')) {
        const totalMaxBalls = (state.overs || 20) * 6;
        const currentTotalBalls = ((state.completedOvers || 0) * 6) + (state.currentBalls || 0);
        document.getElementById('metric-balls-left').textContent = Math.max(0, totalMaxBalls - currentTotalBalls);
    }

    // Striker
    const striker = state.striker || {};
    if (document.getElementById('name-striker')) document.getElementById('name-striker').textContent = striker.name || 'Striker';
    if (document.getElementById('subrole-striker')) document.getElementById('subrole-striker').textContent = striker.battingStyle || striker.playingRole || 'Batter';
    if (document.getElementById('runs-striker')) document.getElementById('runs-striker').innerHTML = `${striker.runs || 0} <span style="font-size: 1rem; color: #94a3b8; font-weight: 600;">(${striker.balls || 0} balls)</span>`;
    if (document.getElementById('fours-striker')) document.getElementById('fours-striker').textContent = striker.fours || 0;
    if (document.getElementById('sixes-striker')) document.getElementById('sixes-striker').textContent = striker.sixes || 0;
    if (document.getElementById('sr-striker')) document.getElementById('sr-striker').textContent = (striker.strikeRate || 0).toFixed(1);

    // Non-Striker
    const nonStriker = state.nonStriker || {};
    if (document.getElementById('name-non-striker')) document.getElementById('name-non-striker').textContent = nonStriker.name || 'Non-Striker';
    if (document.getElementById('subrole-non-striker')) document.getElementById('subrole-non-striker').textContent = nonStriker.battingStyle || 'Non-Striker';
    if (document.getElementById('runs-non-striker')) document.getElementById('runs-non-striker').innerHTML = `${nonStriker.runs || 0} <span style="font-size: 0.9rem; color: #94a3b8;">(${nonStriker.balls || 0} balls)</span>`;

    // Bowler
    const bowler = state.currentBowler || {};
    if (document.getElementById('name-bowler')) document.getElementById('name-bowler').textContent = bowler.name || 'Bowler';
    if (document.getElementById('subrole-bowler')) document.getElementById('subrole-bowler').textContent = bowler.bowlingStyle || bowler.playingRole || 'Bowler';
    if (document.getElementById('figs-bowler')) document.getElementById('figs-bowler').textContent = `${bowler.wicketsTaken || 0}/${bowler.runsConceded || 0}`;
    if (document.getElementById('overs-bowler')) document.getElementById('overs-bowler').textContent = `${bowler.oversBowled || 0}.${bowler.ballsBowled || 0}`;
    if (document.getElementById('econ-bowler')) document.getElementById('econ-bowler').textContent = (bowler.economyRate || 0).toFixed(1);
    if (document.getElementById('maidens-bowler')) document.getElementById('maidens-bowler').textContent = bowler.maidens || 0;

    // Current ball indicator
    if (document.getElementById('this-ball-indicator')) {
        document.getElementById('this-ball-indicator').textContent = `This Ball: ${state.completedOvers || 0}.${(state.currentBalls || 0) + 1}`;
    }

    // Right Column: This Over Ball Log List
    const logList = document.getElementById('this-over-log-list');
    if (logList) {
        const events = state.thisOverBallEvents || [];
        if (events.length === 0) {
            logList.innerHTML = '<div style="font-size: 0.8rem; color: #64748b; text-align: center; padding: 0.75rem;">No balls bowled in this over yet</div>';
        } else {
            logList.innerHTML = events.map(b => {
                let classType = '';
                let label = b.runsScored;
                if (b.wicket) {
                    classType = 'wicket';
                    label = b.runsScored > 0 ? `W+${b.runsScored}` : 'W';
                } else if (b.extraType && b.extraType !== 'NONE') {
                    classType = 'extra';
                    if (b.extraType === 'NO_BALL') {
                        label = `Nb+${b.runsScored ?? 0}`;
                    } else if (b.extraType === 'WIDE') {
                        label = b.runsScored > 0 ? `Wd+${b.runsScored}` : 'Wd';
                    } else {
                        label = b.extraType;
                    }
                } else if (b.runsScored === 4) {
                    classType = 'four';
                } else if (b.runsScored === 6) {
                    classType = 'six';
                }

                return `
                    <div class="ball-log-item">
                        <div class="ball-icon-pill ${classType}" style="min-width: 28px; width: auto; padding: 0 6px; border-radius: 14px;">${label}</div>
                        <div>
                            <strong>Ball ${b.overNumber + 1}.${b.ballNumber}</strong>
                            <span style="color: #94a3b8; font-size: 0.78rem;"> &bull; ${b.strikerName} to ${b.bowlerName}</span>
                        </div>
                    </div>
                `;
            }).join('');
        }
    }

    // Over Summary Pills
    const summaryPills = document.getElementById('over-summary-pills');
    if (summaryPills) {
        const ballsArr = state.overSummaryBalls || [];
        if (ballsArr.length === 0) {
            summaryPills.innerHTML = '<span style="font-size: 0.8rem; color: #64748b;">-</span>';
        } else {
            summaryPills.innerHTML = ballsArr.map(bText => {
                let colorBg = 'rgba(255,255,255,0.1)';
                if (bText === 'W' || bText.startsWith('W+')) colorBg = '#ef4444';
                else if (bText === '4') colorBg = '#0ea5e9';
                else if (bText === '6') colorBg = '#a855f7';
                else if (bText.startsWith('Wd') || bText.startsWith('Nb')) colorBg = '#f59e0b';

                return `<span style="display:inline-flex; min-width:28px; height:28px; padding:0 6px; border-radius:14px; background:${colorBg}; color:#fff; font-weight:800; align-items:center; justify-content:center; font-size:0.78rem;">${bText}</span>`;
            }).join('');
        }
    }

    // Extras & FOW
    if (document.getElementById('summary-extras')) document.getElementById('summary-extras').textContent = state.totalExtras || 0;
    if (document.getElementById('summary-fow')) document.getElementById('summary-fow').textContent = state.fallOfWicketsCount || 0;
}

function isPlayerCurrentKeeper(userId) {
    if (!userId || !scoringState) return false;
    if (scoringState.currentWicketKeeper && scoringState.currentWicketKeeper.userId === userId) {
        return true;
    }
    const bowlingXi = scoringState.bowlingPlayingXi || [];
    const player = bowlingXi.find(p => p.userId === userId);
    return player && player.isWicketKeeper === true;
}

function openChangeKeeperModal(targetBowlerUserId = null, customPrompt = null) {
    const modal = document.getElementById('change-keeper-modal');
    const dropdown = document.getElementById('select-new-keeper-dropdown');
    const promptText = document.getElementById('change-keeper-prompt-text');

    if (!modal || !dropdown) return;

    if (promptText) {
        promptText.textContent = customPrompt || 'Select a player from the fielding team to take over as Wicketkeeper.';
    }

    dropdown.innerHTML = '<option value="">-- Select New Wicketkeeper --</option>';

    const bowlingXi = scoringState ? (scoringState.bowlingPlayingXi || []) : [];
    bowlingXi.forEach(p => {
        const isCurrentBowler = targetBowlerUserId ? (p.userId === targetBowlerUserId) : (scoringState && scoringState.currentBowler && scoringState.currentBowler.userId === p.userId);
        if (!isCurrentBowler) {
            const opt = document.createElement('option');
            opt.value = p.userId;
            const keeperTag = p.isWicketKeeper ? ' (Current Keeper)' : '';
            opt.textContent = `${p.name} (${p.userId})${keeperTag}`;
            if (p.isWicketKeeper) opt.selected = true;
            dropdown.appendChild(opt);
        }
    });

    modal.style.display = 'flex';
}

function closeChangeKeeperModal() {
    const modal = document.getElementById('change-keeper-modal');
    if (modal) modal.style.display = 'none';
}

function openRetireBatterModal() {
    const modal = document.getElementById('retire-batter-modal');
    const selectRetiring = document.getElementById('select-retiring-batter-dropdown');
    const selectIncoming = document.getElementById('select-incoming-batter-dropdown');
    const groupIncoming = document.getElementById('group-incoming-batter-select');
    const msgNoBatters = document.getElementById('msg-retire-no-batters-warning');
    const btnConfirm = document.getElementById('btn-confirm-retire-batter');

    if (!modal || !scoringState) return;

    // 1. Populate Retiring Batter (Striker or Non-Striker only!)
    if (selectRetiring) {
        selectRetiring.innerHTML = '<option value="">-- Select Active Batter --</option>';
        const striker = scoringState.striker;
        const nonStriker = scoringState.nonStriker;

        if (striker && striker.userId) {
            selectRetiring.innerHTML += `<option value="${striker.userId}">${escapeHtml(striker.name)} (Striker)</option>`;
        }
        if (nonStriker && nonStriker.userId) {
            selectRetiring.innerHTML += `<option value="${nonStriker.userId}">${escapeHtml(nonStriker.name)} (Non-Striker)</option>`;
        }
    }

    // 2. Populate Incoming Batter dropdown (excluding active batters & dismissed/retired batters to avoid duplicates)
    let availBatters = scoringState.availableBatters || [];
    if (availBatters.length === 0 && scoringState.battingPlayingXi) {
        const strikerId = scoringState.striker ? scoringState.striker.userId : '';
        const nonStrikerId = scoringState.nonStriker ? scoringState.nonStriker.userId : '';
        availBatters = scoringState.battingPlayingXi.filter(p =>
            p.userId !== strikerId &&
            p.userId !== nonStrikerId &&
            !p.isOut
        );
    }

    if (availBatters.length === 0) {
        if (groupIncoming) groupIncoming.style.display = 'none';
        if (msgNoBatters) msgNoBatters.style.display = 'block';
        if (btnConfirm) btnConfirm.disabled = true;
    } else {
        if (groupIncoming) groupIncoming.style.display = 'block';
        if (msgNoBatters) msgNoBatters.style.display = 'none';
        if (btnConfirm) btnConfirm.disabled = false;

        if (selectIncoming) {
            selectIncoming.innerHTML = '<option value="">-- Select New Batter --</option>';
            availBatters.forEach(p => {
                const opt = document.createElement('option');
                opt.value = p.userId;
                opt.textContent = `${p.name} (${p.userId}) - ${p.battingStyle || p.playingRole || 'Batter'}`;
                selectIncoming.appendChild(opt);
            });
        }
    }

    modal.style.display = 'flex';
}

function closeRetireBatterModal() {
    const modal = document.getElementById('retire-batter-modal');
    if (modal) modal.style.display = 'none';
}

function openNextBowlerModal(availableBowlers, prevBowlerId) {
    const modal = document.getElementById('next-bowler-modal');
    const dropdown = document.getElementById('select-next-bowler-dropdown');
    if (!modal || !dropdown) return;

    dropdown.innerHTML = '<option value="">-- Select Next Bowler --</option>';

    if (!availableBowlers || availableBowlers.length === 0) {
        dropdown.innerHTML += '<option value="">No available bowlers</option>';
    } else {
        availableBowlers.forEach(b => {
            const opt = document.createElement('option');
            opt.value = b.userId;
            opt.textContent = `${b.name} (${b.oversBowled} ov - ${b.wicketsTaken}/${b.runsConceded})`;
            dropdown.appendChild(opt);
        });
    }

    modal.style.display = 'flex';
}

function openNextBatterModal() {
    const modal = document.getElementById('next-batter-modal');
    const dropdown = document.getElementById('select-next-batter-dropdown');
    const groupDropdown = document.getElementById('group-next-batter-select');
    const msgAllOut = document.getElementById('msg-all-out-warning');
    const groupDismissed = document.getElementById('group-dismissed-player');
    const selectDismissed = document.getElementById('select-dismissed-player-dropdown');
    const groupCatcher = document.getElementById('group-catcher-player');
    const selectCatcher = document.getElementById('select-catcher-player-dropdown');
    const selectWicketType = document.getElementById('select-wicket-type');
    const groupWicketRuns = document.getElementById('group-wicket-runs');
    const selectWicketRuns = document.getElementById('select-wicket-runs');
    const groupWicketCrossed = document.getElementById('group-wicket-crossed');
    const selectWicketCrossed = document.getElementById('select-wicket-crossed');
    if (!modal) return;

    // Reset Wicket Type to BOWLED, hide optional groups by default, reset runs to 0
    if (selectWicketType) selectWicketType.value = 'BOWLED';
    if (groupDismissed) groupDismissed.style.display = 'none';
    if (groupCatcher) groupCatcher.style.display = 'none';
    if (groupWicketRuns) groupWicketRuns.style.display = 'none';
    if (selectWicketRuns) selectWicketRuns.value = '0';
    if (groupWicketCrossed) groupWicketCrossed.style.display = 'none';
    if (selectWicketCrossed) selectWicketCrossed.value = 'false';

    // Populate Dismissed Batter options for Run Out
    if (selectDismissed && scoringState) {
        selectDismissed.innerHTML = '';
        const striker = scoringState.striker;
        const nonStriker = scoringState.nonStriker;
        if (striker && striker.userId) {
            selectDismissed.innerHTML += `<option value="${striker.userId}" selected>${escapeHtml(striker.name)} (Striker)</option>`;
        }
        if (nonStriker && nonStriker.userId) {
            selectDismissed.innerHTML += `<option value="${nonStriker.userId}">${escapeHtml(nonStriker.name)} (Non-Striker)</option>`;
        }
    }

    // Populate Opponent / Bowling Team players for Caught Out
    if (selectCatcher && scoringState) {
        selectCatcher.innerHTML = '<option value="">-- Select Fielder who took Catch --</option>';
        const bowlingXi = scoringState.bowlingPlayingXi || [];
        bowlingXi.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.userId;
            opt.textContent = `${p.name} (${p.userId})`;
            selectCatcher.appendChild(opt);
        });
    }

    let availBatters = scoringState ? scoringState.availableBatters || [] : [];
    if (availBatters.length === 0 && scoringState && scoringState.battingPlayingXi) {
        const strikerId = scoringState.striker ? scoringState.striker.userId : '';
        const nonStrikerId = scoringState.nonStriker ? scoringState.nonStriker.userId : '';
        availBatters = scoringState.battingPlayingXi.filter(p =>
            p.userId !== strikerId &&
            p.userId !== nonStrikerId &&
            !p.isOut
        );
    }

    const currentWkts = scoringState ? scoringState.totalWickets || 0 : 0;
    const isAllOut = (currentWkts >= 9 || availBatters.length === 0);

    if (isAllOut) {
        if (groupDropdown) groupDropdown.style.display = 'none';
        if (msgAllOut) {
            msgAllOut.style.display = 'block';
            msgAllOut.textContent = '⚡ All Out! 10 Wickets fallen. Innings completed.';
        }
    } else {
        if (groupDropdown) groupDropdown.style.display = 'block';
        if (msgAllOut) msgAllOut.style.display = 'none';

        if (dropdown) {
            dropdown.innerHTML = '<option value="">-- Select Next Batter --</option>';
            availBatters.forEach(p => {
                const opt = document.createElement('option');
                opt.value = p.userId;
                opt.textContent = `${p.name} (${p.userId}) - ${p.battingStyle || p.playingRole || 'Batter'}`;
                dropdown.appendChild(opt);
            });
        }
    }

    modal.style.display = 'flex';
}

function openNoBallModal() {
    const modal = document.getElementById('no-ball-modal');
    if (modal) modal.style.display = 'flex';
}

function closeNoBallModal() {
    const modal = document.getElementById('no-ball-modal');
    if (modal) modal.style.display = 'none';
}

function populateSettingsModal(state) {
    if (document.getElementById('set-wide-run')) document.getElementById('set-wide-run').checked = state.wideRunEnabled !== false;
    if (document.getElementById('set-noball-run')) document.getElementById('set-noball-run').checked = state.noBallRunEnabled === true;
    if (document.getElementById('set-freehit')) document.getElementById('set-freehit').checked = state.noBallFreeHitEnabled === true;
    if (document.getElementById('set-byes-run')) document.getElementById('set-byes-run').checked = state.byeRunEnabled !== false;
    if (document.getElementById('set-consecutive-overs')) document.getElementById('set-consecutive-overs').checked = state.allowConsecutiveOvers === true;
    if (document.getElementById('set-max-overs-bowler')) document.getElementById('set-max-overs-bowler').value = state.maxOversPerBowler || 4;
}

function renderChecklist(chk, isAuthorized) {
    if (!chk) return;

    setChkItem('chk-team-a', chk.teamAConfirmed);
    setChkItem('chk-team-b', chk.teamBConfirmed);
    setChkItem('chk-pxi-a', chk.teamAPlayingXiReady);
    setChkItem('chk-pxi-b', chk.teamBPlayingXiReady);
    setChkItem('chk-scorer', chk.scorerAssigned);

    const startBtn = document.getElementById('btn-start-match-trigger');

    if (startBtn) {
        if (isAuthorized) {
            startBtn.disabled = false;
            startBtn.style.opacity = '1';
            startBtn.style.cursor = 'pointer';
        } else {
            startBtn.disabled = true;
            startBtn.style.opacity = '0.5';
            startBtn.style.cursor = 'not-allowed';
        }
    }
}

function setChkItem(elementId, isPassed) {
    const el = document.getElementById(elementId);
    if (el) {
        el.textContent = isPassed ? '✓' : '❌';
        el.style.color = isPassed ? '#10b981' : '#ef4444';
    }
}

function populateTossOptions(teamA, teamB) {
    const select = document.getElementById('toss-winner-select');
    if (!select) return;

    if (!teamA && scoringState) teamA = scoringState.teamA || (scoringState.match && scoringState.match.teamA);
    if (!teamB && scoringState) teamB = scoringState.teamB || (scoringState.match && scoringState.match.teamB);

    const idA = (teamA && (teamA.teamId || teamA.id)) || '';
    const nameA = (teamA && teamA.name) || 'Team A';

    const idB = (teamB && (teamB.teamId || teamB.id)) || '';
    const nameB = (teamB && teamB.name) || 'Team B';

    select.innerHTML = `
        <option value="">-- Select Toss Winner --</option>
        <option value="${escapeHtml(idA)}">${escapeHtml(nameA)}</option>
        <option value="${escapeHtml(idB)}">${escapeHtml(nameB)}</option>
    `;
}

function populateInningsSetup(state) {
    if (document.getElementById('inn-batting-team-name')) {
        document.getElementById('inn-batting-team-name').textContent = state.battingTeamName || 'Batting Team';
    }
    if (document.getElementById('inn-bowling-team-name')) {
        document.getElementById('inn-bowling-team-name').textContent = state.bowlingTeamName || 'Bowling Team';
    }

    const strikerSelect = document.getElementById('select-striker');
    const nonStrikerSelect = document.getElementById('select-non-striker');
    const bowlerSelect = document.getElementById('select-bowler');

    const battingXi = state.battingPlayingXi || [];
    const bowlingXi = state.bowlingPlayingXi || [];

    if (strikerSelect) {
        strikerSelect.innerHTML = '<option value="">-- Select Striker --</option>' +
            battingXi.map(p => `<option value="${p.userId}">${escapeHtml(p.name)} (${p.userId})</option>`).join('');
    }
    if (nonStrikerSelect) {
        nonStrikerSelect.innerHTML = '<option value="">-- Select Non-Striker --</option>' +
            battingXi.map(p => `<option value="${p.userId}">${escapeHtml(p.name)} (${p.userId})</option>`).join('');
    }
    if (bowlerSelect) {
        bowlerSelect.innerHTML = '<option value="">-- Select Opening Bowler --</option>' +
            bowlingXi.map(p => `<option value="${p.userId}">${escapeHtml(p.name)} (${p.userId})</option>`).join('');
    }
}

function showScoringAlert(msg) {
    const el = document.getElementById('scoring-alert');
    if (el) {
        el.textContent = msg;
        el.style.display = 'block';
    }
}

function hideScoringAlert() {
    const el = document.getElementById('scoring-alert');
    if (el) el.style.display = 'none';
}

function formatDate(dateStr) {
    if (!dateStr) return 'TBD';
    try {
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch (e) { return dateStr; }
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
    } catch (e) { return timeStr; }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, match => {
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return map[match];
    });
}

function connectScoringWebSocket() {
    if (!currentMatchId || typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    try {
        const wsUrl = typeof BACKEND_BASE_URL !== 'undefined' ? `${BACKEND_BASE_URL}/ws-cricket` : 'https://cricket-app-production-9e11.up.railway.app/ws-cricket';
        const socket = new SockJS(wsUrl);
        const stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({}, () => {
            stompClient.subscribe(`/topic/matches/${currentMatchId}/live`, (message) => {
                try {
                    const data = JSON.parse(message.body);
                    if (data && data.type === 'CELEBRATION' && window.CelebrationManager) {
                        CelebrationManager.trigger(data.event, data.celebrationId, data.matchId);
                    }
                } catch (e) {}
            });
        }, () => {
            setTimeout(connectScoringWebSocket, 6000);
        });
    } catch (err) {
        console.warn('Scoring WebSocket connection error:', err);
    }
}
