/**
 * Cricket App - Match Details JavaScript
 * Stage 5 - Match Details & Creator Actions
 */

let currentMatchData = null;
let currentUserId = null;

document.addEventListener('DOMContentLoaded', () => {
    initLogout();
    loadSidebarUser();
    loadMatchDetails();
    initMatchInfoToggle();
    initModalEvents();
    initFormatChangeListener();
});

function initMatchInfoToggle() {
    const toggleBtn = document.getElementById('btn-toggle-match-info');
    const contentEl = document.getElementById('match-info-content');
    const caretEl = document.getElementById('caret-match-info');

    if (toggleBtn && contentEl) {
        toggleBtn.addEventListener('click', () => {
            const isHidden = contentEl.style.display === 'none';
            contentEl.style.display = isHidden ? 'flex' : 'none';
            if (caretEl) {
                caretEl.textContent = isHidden ? '▲' : '▼';
            }
        });
    }
}

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
            currentUserId = user.userId;
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

async function loadMatchDetails() {
    const urlParams = new URLSearchParams(window.location.search);
    const matchId = urlParams.get('id') || urlParams.get('matchId');

    if (!matchId) {
        showAlert('match-details-alert', 'No match ID specified in URL.');
        return;
    }

    hideAlert('match-details-alert');

    try {
        const match = await ApiService.get('/matches/' + encodeURIComponent(matchId));
        currentMatchData = match;
        renderMatchDetails(match);
    } catch (err) {
        console.error('Failed to load match details:', err);
        showAlert('match-details-alert', err.message || 'Match details not found.');
    }
}

function renderMatchDetails(match) {
    if (!match) return;

    const matchId = match.matchId || 'MATCH------';
    const name = match.matchName || 'Cricket Match';
    const format = match.format || 'T20';
    const overs = match.overs || 20;
    const status = match.status || 'SCHEDULED';
    const venue = match.venue || 'Venue Not Specified';
    const dateStr = formatDate(match.matchDate);
    const timeStr = formatTime(match.matchTime);
    const creatorName = match.createdByName || 'Player';
    const creatorId = match.createdByUserId || '';
    const description = match.description || 'No description provided for this match.';

    const teamA = match.teamA || {};
    const teamB = match.teamB || {};

    // Header & Titles
    if (document.getElementById('md-title')) document.getElementById('md-title').textContent = name;
    if (document.getElementById('md-match-id')) document.getElementById('md-match-id').textContent = matchId;
    if (document.getElementById('md-format-badge')) document.getElementById('md-format-badge').textContent = `${format} \u2022 ${overs} OVERS`;
    if (document.getElementById('md-status-badge')) {
        const statusBadge = document.getElementById('md-status-badge');
        statusBadge.textContent = status === 'PENDING_CONFIRMATION' ? 'PENDING APPROVAL' : status;
        if (status === 'PENDING_CONFIRMATION') {
            statusBadge.className = 'role-badge';
            statusBadge.style.background = '#fef3c7';
            statusBadge.style.color = '#b45309';
            statusBadge.style.border = '1px solid #fde68a';
        } else if (status === 'SCHEDULED') {
            statusBadge.className = 'role-badge owner';
            statusBadge.style = '';
        } else if (status === 'LIVE') {
            statusBadge.className = 'prof-id-pill';
            statusBadge.style = '';
        } else if (status === 'COMPLETED') {
            statusBadge.className = 'verified-pill';
            statusBadge.style = '';
        } else if (status === 'CANCELLED') {
            statusBadge.className = 'role-badge';
            statusBadge.style = '';
        }
    }
    if (document.getElementById('md-match-name')) document.getElementById('md-match-name').textContent = name;

    // Invitation Banner Logic
    const invBanner = document.getElementById('invitation-status-banner');
    const invBannerTitle = document.getElementById('inv-banner-title');
    const invBannerText = document.getElementById('inv-banner-text');

    if (invBanner) {
        if (status === 'PENDING_CONFIRMATION') {
            invBanner.style.display = 'block';
            if (match.isInvitedCaptain) {
                if (invBannerTitle) invBannerTitle.textContent = 'Opponent Match Challenge Received!';
                if (invBannerText) invBannerText.textContent = `You have received a match invitation from ${creatorName} (${teamA.name || 'Team A'}). Respond below to schedule this fixture.`;
            } else if (match.isCreator) {
                if (invBannerTitle) invBannerTitle.textContent = 'Match Invitation Sent';
                if (invBannerText) invBannerText.textContent = `Match invitation sent to opponent captain ${match.invitedCaptainName ? match.invitedCaptainName : ''} (${teamB.name || 'Team B'}). Waiting for acceptance.`;
            } else {
                if (invBannerTitle) invBannerTitle.textContent = 'Fixture Pending Opponent Confirmation';
                if (invBannerText) invBannerText.textContent = `This match fixture is pending acceptance by opponent team (${teamB.name || 'Team B'}).`;
            }
        } else {
            invBanner.style.display = 'none';
        }
    }

    // Team A
    if (document.getElementById('md-team-a-name')) document.getElementById('md-team-a-name').textContent = teamA.name || 'Team A';
    if (document.getElementById('md-team-a-id')) document.getElementById('md-team-a-id').textContent = teamA.teamId || '';
    const logoA = ApiService.getImageUrl(teamA.logoUrl);
    const imgA = document.getElementById('md-team-a-logo');
    const fbA = document.getElementById('md-team-a-fallback');
    if (logoA && imgA) {
        imgA.src = logoA;
        imgA.style.display = 'block';
        if (fbA) fbA.style.display = 'none';
    } else {
        if (imgA) imgA.style.display = 'none';
        if (fbA) fbA.style.display = 'inline';
    }

    // Team B
    if (document.getElementById('md-team-b-name')) document.getElementById('md-team-b-name').textContent = teamB.name || 'Team B';
    if (document.getElementById('md-team-b-id')) document.getElementById('md-team-b-id').textContent = teamB.teamId || '';
    const logoB = ApiService.getImageUrl(teamB.logoUrl);
    const imgB = document.getElementById('md-team-b-logo');
    const fbB = document.getElementById('md-team-b-fallback');
    if (logoB && imgB) {
        imgB.src = logoB;
        imgB.style.display = 'block';
        if (fbB) fbB.style.display = 'none';
    } else {
        if (imgB) imgB.style.display = 'none';
        if (fbB) fbB.style.display = 'inline';
    }

    // Match Info List
    if (document.getElementById('md-info-match-id')) document.getElementById('md-info-match-id').textContent = matchId;
    if (document.getElementById('md-info-date')) document.getElementById('md-info-date').textContent = dateStr;
    if (document.getElementById('md-info-time')) document.getElementById('md-info-time').textContent = timeStr;
    if (document.getElementById('md-info-venue')) document.getElementById('md-info-venue').textContent = venue;
    if (document.getElementById('md-info-format')) document.getElementById('md-info-format').textContent = format;
    if (document.getElementById('md-info-overs')) document.getElementById('md-info-overs').textContent = `${overs} Overs`;
    if (document.getElementById('md-info-creator')) document.getElementById('md-info-creator').textContent = creatorId ? `${creatorName} (${creatorId})` : creatorName;
    if (document.getElementById('md-info-scorer')) {
        document.getElementById('md-info-scorer').textContent = match.scorerName ? `${match.scorerName} (${match.scorerUserId})` : 'Not Assigned Yet';
    }
    if (document.getElementById('md-description')) document.getElementById('md-description').textContent = description;

    // Captain permission check (Only captains can assign scorer & select/manage Playing XI)
    const isCaptain = Boolean(
        match.isCaptain === true ||
        match.isCreator === true ||
        match.isInvitedCaptain === true ||
        (currentUserId && match.createdByUserId && currentUserId === match.createdByUserId)
    );

    // Match Action Buttons
    const btnAssignScorer = document.getElementById('btn-open-assign-scorer');
    const btnInfoAssignScorer = document.getElementById('btn-info-assign-scorer');
    const btnManagePxi = document.getElementById('btn-manage-playing-xi');
    const btnPlayingXi = document.getElementById('btn-open-playing-xi');
    const btnScorerDashboard = document.getElementById('btn-open-scorer-dashboard');
    const btnLiveScore = document.getElementById('btn-open-live-score');
    const btnMatchPoster = document.getElementById('btn-open-match-poster');

    const isCompleted = status === 'COMPLETED';

    if (isCompleted) {
        // Hide all pre-match/scorer setup controls
        if (btnAssignScorer) btnAssignScorer.style.display = 'none';
        if (btnInfoAssignScorer) btnInfoAssignScorer.style.display = 'none';
        if (btnManagePxi) btnManagePxi.style.display = 'none';
        if (btnPlayingXi) btnPlayingXi.style.display = 'none';
        if (btnMatchPoster) btnMatchPoster.style.display = 'none';
        if (btnScorerDashboard) btnScorerDashboard.style.display = 'none';

        // Show ONLY Scoreboard button under Match Activities (in viewer mode)
        if (btnLiveScore) {
            btnLiveScore.href = `scoring-dashboard.html?matchId=${encodeURIComponent(matchId)}&mode=view`;
            btnLiveScore.style.display = 'flex';
            btnLiveScore.style.background = '#10b981';
            btnLiveScore.style.color = '#000000';
            btnLiveScore.style.border = 'none';
            btnLiveScore.style.fontWeight = '800';
            const span = btnLiveScore.querySelector('span');
            if (span) span.textContent = '📊 View Final Scoreboard';
        }
    } else {
        const scorerBtnText = match.scorerName ? 'Change Match Scorer' : 'Assign Match Scorer';

        if (btnAssignScorer) {
            btnAssignScorer.style.display = isCaptain ? 'flex' : 'none';
            const btnTextSpan = btnAssignScorer.querySelector('span');
            if (btnTextSpan) btnTextSpan.textContent = scorerBtnText;
        }

        if (btnInfoAssignScorer) {
            btnInfoAssignScorer.style.display = isCaptain ? 'inline-block' : 'none';
            btnInfoAssignScorer.textContent = match.scorerName ? 'Change Scorer' : 'Assign Scorer';
        }

        if (btnManagePxi) {
            btnManagePxi.href = `select-playing-xi.html?matchId=${encodeURIComponent(matchId)}`;
            btnManagePxi.style.display = isCaptain ? 'flex' : 'none';
        }

        if (btnPlayingXi) {
            btnPlayingXi.style.display = 'flex';
            btnPlayingXi.onclick = () => {
                window.open(`playing-xi.html?matchId=${encodeURIComponent(matchId)}`, '_blank');
            };
        }
        const canScore = Boolean(match.isScorer || isCaptain || match.isCreator);
        if (btnScorerDashboard) {
            if (canScore) {
                btnScorerDashboard.href = `scoring-dashboard.html?matchId=${encodeURIComponent(matchId)}&mode=scorer`;
                btnScorerDashboard.style.display = 'flex';
            } else {
                btnScorerDashboard.style.display = 'none';
            }
        }
        if (btnLiveScore) {
            btnLiveScore.href = `live-score.html?matchId=${encodeURIComponent(matchId)}`;
            btnLiveScore.style.display = 'flex';
            btnLiveScore.style.background = '#0f172a';
            btnLiveScore.style.color = '#ffffff';
            btnLiveScore.style.border = '1.5px solid #10b981';
            const span = btnLiveScore.querySelector('span');
            if (span) span.textContent = 'Live Score';
        }
        if (btnMatchPoster) {
            btnMatchPoster.href = `match-poster.html?matchId=${encodeURIComponent(matchId)}`;
            btnMatchPoster.style.display = 'flex';
        }
    }

    // Invited Captain Card
    const invCard = document.getElementById('invitation-actions-card');
    if (invCard) {
        if (match.isInvitedCaptain && status === 'PENDING_CONFIRMATION' && match.invitationStatus === 'PENDING') {
            invCard.style.display = 'block';
        } else {
            invCard.style.display = 'none';
        }
    }

    // Creator Actions Card (Hidden for Completed match)
    const creatorCard = document.getElementById('creator-actions-card');
    if (creatorCard) {
        if (!isCompleted && match.isCreator && (status === 'SCHEDULED' || status === 'PENDING_CONFIRMATION')) {
            creatorCard.style.display = 'block';
        } else {
            creatorCard.style.display = 'none';
        }
    }
}

function initModalEvents() {
    const editBtn = document.getElementById('btn-open-edit-match');
    const cancelBtn = document.getElementById('btn-cancel-match');
    const acceptInvBtn = document.getElementById('btn-accept-match-invitation');
    const rejectInvBtn = document.getElementById('btn-reject-match-invitation');
    const modal = document.getElementById('edit-match-modal');
    const closeBtn = document.getElementById('btn-close-edit-modal');
    const cancelModalBtn = document.getElementById('btn-cancel-edit-modal');
    const form = document.getElementById('edit-match-form');

    if (acceptInvBtn) {
        acceptInvBtn.addEventListener('click', async () => {
            if (!currentMatchData || !currentMatchData.invitationId) return;
            try {
                acceptInvBtn.disabled = true;
                await ApiService.post(`/match-invitations/${currentMatchData.invitationId}/accept`, {});
                ApiService.showToast('Match invitation accepted! Match is now scheduled.', 'success');
                await loadMatchDetails();
            } catch (err) {
                console.error('Failed to accept match invitation:', err);
                showAlert('match-details-alert', err.message || 'Failed to accept match invitation.');
            } finally {
                acceptInvBtn.disabled = false;
            }
        });
    }

    if (rejectInvBtn) {
        rejectInvBtn.addEventListener('click', async () => {
            if (!currentMatchData || !currentMatchData.invitationId) return;
            if (!confirm('Are you sure you want to reject this match invitation?')) return;
            try {
                rejectInvBtn.disabled = true;
                await ApiService.post(`/match-invitations/${currentMatchData.invitationId}/reject`, {});
                ApiService.showToast('Match invitation rejected.', 'info');
                await loadMatchDetails();
            } catch (err) {
                console.error('Failed to reject match invitation:', err);
                showAlert('match-details-alert', err.message || 'Failed to reject match invitation.');
            } finally {
                rejectInvBtn.disabled = false;
            }
        });
    }

    if (editBtn && modal) {
        editBtn.addEventListener('click', () => {
            if (!currentMatchData) return;
            populateEditModal(currentMatchData);
            modal.style.display = 'flex';
        });
    }

    const closeModal = () => {
        if (modal) modal.style.display = 'none';
        hideAlert('edit-modal-alert');
    };

    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (cancelModalBtn) cancelModalBtn.addEventListener('click', closeModal);

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!currentMatchData) return;
            hideAlert('edit-modal-alert');

            const matchName = document.getElementById('edit-matchName').value.trim();
            const matchDate = document.getElementById('edit-matchDate').value;
            const matchTime = document.getElementById('edit-matchTime').value;
            const venue = document.getElementById('edit-venue').value.trim();
            const format = document.getElementById('edit-format').value;
            const overs = parseInt(document.getElementById('edit-overs').value, 10);
            const description = document.getElementById('edit-description').value.trim();

            if (!matchName || !matchDate || !matchTime || !venue) {
                showAlert('edit-modal-alert', 'Please fill in all required fields.');
                return;
            }

            if (format === 'CUSTOM' && (isNaN(overs) || overs <= 0 || overs > 100)) {
                showAlert('edit-modal-alert', 'Please enter a valid number of overs (1 to 100) for custom format.');
                return;
            }

            const payload = {
                matchName,
                matchDate,
                matchTime,
                venue,
                format,
                overs,
                description
            };

            try {
                const btnSave = document.getElementById('btn-save-match');
                if (btnSave) btnSave.disabled = true;

                const updated = await ApiService.put(`/matches/${encodeURIComponent(currentMatchData.matchId)}`, payload);
                currentMatchData = updated;
                renderMatchDetails(updated);
                ApiService.showToast('Match details updated successfully!', 'success');
                closeModal();
            } catch (err) {
                console.error('Failed to update match:', err);
                showAlert('edit-modal-alert', err.message || 'Failed to update match.');
            } finally {
                const btnSave = document.getElementById('btn-save-match');
                if (btnSave) btnSave.disabled = false;
            }
        });
    }

    if (cancelBtn) {
        cancelBtn.addEventListener('click', async () => {
            if (!currentMatchData) return;
            if (!confirm(`Are you sure you want to cancel match "${currentMatchData.matchName}" (${currentMatchData.matchId})? This action cannot be undone.`)) {
                return;
            }

            try {
                cancelBtn.disabled = true;
                const cancelled = await ApiService.post(`/matches/${encodeURIComponent(currentMatchData.matchId)}/cancel`, {});
                currentMatchData = cancelled;
                renderMatchDetails(cancelled);
                ApiService.showToast('Match has been cancelled.', 'info');
            } catch (err) {
                console.error('Failed to cancel match:', err);
                showAlert('match-details-alert', err.message || 'Failed to cancel match.');
            } finally {
                cancelBtn.disabled = false;
            }
        });
    }

    // Scorer Modal events
    const scorerModal = document.getElementById('assign-scorer-modal');
    const closeScorerBtn = document.getElementById('btn-close-scorer-modal');
    const cancelScorerBtn = document.getElementById('btn-cancel-scorer-modal');
    const saveScorerBtn = document.getElementById('btn-save-scorer');
    const scorerSearchInput = document.getElementById('scorer-search-input');
    const scorerSearchResults = document.getElementById('scorer-search-results');
    const scorerSelectDropdown = document.getElementById('scorer-select-dropdown');
    const scorerCustomInput = document.getElementById('scorer-custom-input');

    let selectedScorerUserId = null;

    const openModalFunc = () => {
        if (!scorerModal) return;
        hideAlert('scorer-modal-alert');
        selectedScorerUserId = null;
        if (scorerSearchInput) scorerSearchInput.value = '';
        if (scorerSearchResults) scorerSearchResults.innerHTML = '';
        if (scorerCustomInput) scorerCustomInput.value = '';
        
        // Show modal IMMEDIATELY
        scorerModal.style.display = 'flex';

        // Async populate squad dropdown in background
        if (scorerSelectDropdown && currentMatchData) {
            scorerSelectDropdown.innerHTML = '<option value="">-- Select from participating players --</option>';
            const teamA = currentMatchData.teamA || {};
            const teamB = currentMatchData.teamB || {};

            Promise.all([
                teamA.teamId ? ApiService.get(`/teams/${teamA.teamId}/members`).catch(() => []) : [],
                teamB.teamId ? ApiService.get(`/teams/${teamB.teamId}/members`).catch(() => []) : []
            ]).then(([membersA, membersB]) => {
                if (membersA && membersA.length > 0) {
                    const groupA = document.createElement('optgroup');
                    groupA.label = `${teamA.name || 'Team A'} Squad`;
                    membersA.forEach(m => {
                        const opt = document.createElement('option');
                        opt.value = m.userId;
                        opt.textContent = `${m.name} (${m.userId}) - ${m.role || 'Player'}`;
                        groupA.appendChild(opt);
                    });
                    scorerSelectDropdown.appendChild(groupA);
                }

                if (membersB && membersB.length > 0) {
                    const groupB = document.createElement('optgroup');
                    groupB.label = `${teamB.name || 'Team B'} Squad`;
                    membersB.forEach(m => {
                        const opt = document.createElement('option');
                        opt.value = m.userId;
                        opt.textContent = `${m.name} (${m.userId}) - ${m.role || 'Player'}`;
                        groupB.appendChild(opt);
                    });
                    scorerSelectDropdown.appendChild(groupB);
                }
            }).catch(err => console.warn('Squad load error:', err));
        }
    };

    const triggerBtns = document.querySelectorAll('#btn-open-assign-scorer, #btn-info-assign-scorer, .btn-open-scorer-modal');
    triggerBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            openModalFunc();
        });
    });

    // Live search listener
    if (scorerSearchInput && scorerSearchResults) {
        let searchTimeout = null;
        scorerSearchInput.addEventListener('input', () => {
            clearTimeout(searchTimeout);
            const query = scorerSearchInput.value.trim();
            if (query.length < 2) {
                scorerSearchResults.innerHTML = '';
                return;
            }

            searchTimeout = setTimeout(async () => {
                try {
                    const players = await ApiService.get('/players/search?query=' + encodeURIComponent(query)).catch(() => []);
                    scorerSearchResults.innerHTML = '';

                    if (!players || players.length === 0) {
                        scorerSearchResults.innerHTML = '<div style="font-size:0.8rem; color:#94a3b8; padding:0.5rem; text-align:center;">No matching registered players found</div>';
                        return;
                    }

                    players.forEach(p => {
                        const card = document.createElement('div');
                        card.style.cssText = 'display:flex; align-items:center; justify-space-between; gap:0.75rem; background:rgba(15,23,42,0.8); border:1px solid rgba(255,255,255,0.1); border-radius:8px; padding:0.5rem 0.75rem;';
                        
                        const photoUrl = p.profilePhotoUrl ? ApiService.getImageUrl(p.profilePhotoUrl) : null;
                        const avatarHtml = photoUrl 
                            ? `<img src="${photoUrl}" style="width:32px; height:32px; border-radius:50%; object-fit:cover;">`
                            : `<div style="width:32px; height:32px; border-radius:50%; background:#0d5c3a; color:#fff; display:flex; align-items:center; justify-content:center; font-size:0.8rem;">👤</div>`;

                        card.innerHTML = `
                            <div style="display:flex; align-items:center; gap:0.6rem; flex:1; overflow:hidden;">
                                ${avatarHtml}
                                <div style="overflow:hidden;">
                                    <div style="font-weight:700; color:#fff; font-size:0.88rem; white-space:nowrap; text-overflow:ellipsis; overflow:hidden;">${p.displayName || p.name || 'Player'}</div>
                                    <div style="font-size:0.75rem; color:#10b981; font-weight:600;">${p.userId} &bull; ${p.playingRole || 'Player'}</div>
                                </div>
                            </div>
                            <button type="button" class="btn-select-searched-scorer" data-userid="${p.userId}" style="background:#10b981; color:#000; font-weight:700; border:none; border-radius:6px; padding:0.35rem 0.75rem; font-size:0.78rem; cursor:pointer; flex-shrink:0;">
                                Select
                            </button>
                        `;

                        const selectBtn = card.querySelector('.btn-select-searched-scorer');
                        selectBtn.addEventListener('click', () => {
                            selectedScorerUserId = p.userId;
                            if (scorerCustomInput) scorerCustomInput.value = p.userId;
                            ApiService.showToast(`Selected ${p.displayName || p.name} (${p.userId}) as scorer`, 'info');
                        });

                        scorerSearchResults.appendChild(card);
                    });

                } catch (e) {
                    console.error('Player search error:', e);
                }
            }, 300);
        });
    }

    const closeScorerModalFunc = () => {
        if (scorerModal) scorerModal.style.display = 'none';
        hideAlert('scorer-modal-alert');
    };

    if (closeScorerBtn) closeScorerBtn.addEventListener('click', closeScorerModalFunc);
    if (cancelScorerBtn) cancelScorerBtn.addEventListener('click', closeScorerModalFunc);
    if (scorerModal) {
        scorerModal.addEventListener('click', (e) => {
            if (e.target === scorerModal) closeScorerModalFunc();
        });
    }

    if (saveScorerBtn) {
        saveScorerBtn.addEventListener('click', async () => {
            if (!currentMatchData) return;
            hideAlert('scorer-modal-alert');

            const dropdownVal = scorerSelectDropdown ? scorerSelectDropdown.value : '';
            const customVal = scorerCustomInput ? scorerCustomInput.value.trim() : '';
            const targetScorerId = selectedScorerUserId || dropdownVal || customVal;

            if (!targetScorerId) {
                showAlert('scorer-modal-alert', 'Please select a player or enter a valid CRK ID / Email.');
                return;
            }

            try {
                saveScorerBtn.disabled = true;
                saveScorerBtn.textContent = 'Saving...';

                await ApiService.post(`/matches/${encodeURIComponent(currentMatchData.matchId)}/scorer`, {
                    scorerUserId: targetScorerId
                });

                ApiService.showToast('Match Scorer assigned successfully!', 'success');
                closeScorerModalFunc();
                await loadMatchDetails();

            } catch (err) {
                console.error('Failed to assign scorer:', err);
                showAlert('scorer-modal-alert', err.message || 'Failed to assign scorer.');
            } finally {
                saveScorerBtn.disabled = false;
                saveScorerBtn.textContent = 'Assign Scorer';
            }
        });
    }
}

function populateEditModal(match) {
    if (document.getElementById('edit-matchName')) document.getElementById('edit-matchName').value = match.matchName || '';
    if (document.getElementById('edit-matchDate')) document.getElementById('edit-matchDate').value = match.matchDate || '';
    if (document.getElementById('edit-matchTime')) document.getElementById('edit-matchTime').value = match.matchTime || '';
    if (document.getElementById('edit-venue')) document.getElementById('edit-venue').value = match.venue || '';
    if (document.getElementById('edit-format')) document.getElementById('edit-format').value = match.format || 'T20';
    if (document.getElementById('edit-overs')) {
        const oversInput = document.getElementById('edit-overs');
        oversInput.value = match.overs || 20;
        oversInput.readOnly = match.format !== 'CUSTOM';
    }
    if (document.getElementById('edit-description')) document.getElementById('edit-description').value = match.description || '';
}

function initFormatChangeListener() {
    const formatSelect = document.getElementById('edit-format');
    const oversInput = document.getElementById('edit-overs');

    if (!formatSelect || !oversInput) return;

    formatSelect.addEventListener('change', () => {
        const val = formatSelect.value;
        if (val === 'T10') {
            oversInput.value = 10;
            oversInput.readOnly = true;
        } else if (val === 'T20') {
            oversInput.value = 20;
            oversInput.readOnly = true;
        } else if (val === 'ODI') {
            oversInput.value = 50;
            oversInput.readOnly = true;
        } else if (val === 'CUSTOM') {
            oversInput.readOnly = false;
            oversInput.focus();
        }
    });
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
