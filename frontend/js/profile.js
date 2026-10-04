/**
 * Cricket App - Player Profile JavaScript
 * Fully integrated with backend APIs (/profile/me, /teams/my)
 */

let currentProfile = null;

document.addEventListener('DOMContentLoaded', () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    initLogout();
    initStatsTabs();
    initShareButton();
    loadProfile();
    loadUserTeams();
    initModalEvents();
    initPhotoUpload();
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

function initStatsTabs() {
    const tabBtns = document.querySelectorAll('.stats-tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const tabTarget = btn.getAttribute('data-tab');
            const tabPanes = document.querySelectorAll('.stats-tab-pane');
            tabPanes.forEach(pane => {
                if (pane.id === `stats-tab-${tabTarget}`) {
                    pane.style.display = 'block';
                    pane.classList.add('active');
                } else {
                    pane.style.display = 'none';
                    pane.classList.remove('active');
                }
            });
        });
    });
}

function initShareButton() {
    const shareBtn = document.getElementById('btn-share-profile');
    if (shareBtn) {
        shareBtn.addEventListener('click', () => {
            if (!currentProfile) return;
            const textToCopy = `Cricket Player Profile: ${currentProfile.name || 'Player'} (${currentProfile.userId || 'CRK'}) - ${formatEnum(currentProfile.playingRole) || 'Player'}`;
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(textToCopy)
                    .then(() => ApiService.showToast('Profile info copied to clipboard!', 'success'))
                    .catch(() => ApiService.showToast(textToCopy, 'info'));
            } else {
                ApiService.showToast(textToCopy, 'info');
            }
        });
    }
}

async function loadProfile() {
    hideAlert('profile-alert');

    try {
        currentProfile = await ApiService.get('/profile/me');
        renderProfile(currentProfile);
    } catch (err) {
        showAlert('profile-alert', err.message || 'Failed to load profile. Session may have expired.');
        if (err.message && err.message.includes('401')) {
            ApiService.clearToken();
            window.location.href = 'login.html';
        }
    }
}

function renderProfile(profile) {
    if (!profile) return;

    const name = profile.name || 'Unnamed Player';
    const userId = profile.userId || 'CRK-------';
    const email = profile.email || '';

    // Main Identity Hero
    if (document.getElementById('display-user-id')) document.getElementById('display-user-id').textContent = userId;
    if (document.getElementById('display-info-user-id')) document.getElementById('display-info-user-id').textContent = userId;
    if (document.getElementById('display-name')) document.getElementById('display-name').textContent = name;
    if (document.getElementById('display-email')) document.getElementById('display-email').textContent = email;
    if (document.getElementById('sidebar-user-name')) document.getElementById('sidebar-user-name').textContent = name;

    // Profile Photo
    const photoImg = document.getElementById('profile-photo-img');
    const avatarFallback = document.getElementById('profile-avatar-fallback');
    const btnDeletePhoto = document.getElementById('btn-delete-photo');
    const sidebarAvatarImgs = document.querySelectorAll('.user-avatar-img');
    const sidebarAvatarFallbacks = document.querySelectorAll('.user-avatar-fallback');

    if (profile.profilePhotoUrl) {
        const fullPhotoUrl = ApiService.getImageUrl(profile.profilePhotoUrl);
        if (photoImg) {
            photoImg.src = fullPhotoUrl;
            photoImg.style.display = 'block';
        }
        if (avatarFallback) avatarFallback.style.display = 'none';
        if (btnDeletePhoto) btnDeletePhoto.style.display = 'inline-block';

        sidebarAvatarImgs.forEach(img => {
            img.src = fullPhotoUrl;
            img.style.display = 'block';
        });
        sidebarAvatarFallbacks.forEach(fb => fb.style.display = 'none');
    } else {
        if (photoImg) photoImg.style.display = 'none';
        if (avatarFallback) avatarFallback.style.display = 'flex';
        if (btnDeletePhoto) btnDeletePhoto.style.display = 'none';

        sidebarAvatarImgs.forEach(img => img.style.display = 'none');
        sidebarAvatarFallbacks.forEach(fb => fb.style.display = 'flex');
    }

    // Role & Attributes
    const formattedRole = formatEnum(profile.playingRole) || 'All-Rounder';
    if (document.getElementById('display-playing-role-badge')) {
        document.getElementById('display-playing-role-badge').textContent = formattedRole.toUpperCase();
    }
    if (document.getElementById('display-playing-role')) {
        document.getElementById('display-playing-role').textContent = formattedRole;
    }
    if (document.getElementById('display-batting-style')) {
        document.getElementById('display-batting-style').textContent = formatEnum(profile.battingStyle) || 'Right-Handed';
    }
    if (document.getElementById('display-bowling-style')) {
        document.getElementById('display-bowling-style').textContent = formatEnum(profile.bowlingStyle) || 'Right-Arm Medium';
    }

    // Personal Details
    if (document.getElementById('display-dob')) document.getElementById('display-dob').textContent = profile.dateOfBirth || 'Not Specified';
    if (document.getElementById('display-gender')) document.getElementById('display-gender').textContent = formatEnum(profile.gender) || 'Not Specified';
    if (document.getElementById('display-location')) document.getElementById('display-location').textContent = profile.location || 'Location Not Set';
    if (document.getElementById('display-bio')) {
        document.getElementById('display-bio').textContent = profile.bio || 'No bio provided yet. Click "Edit Profile" to add your cricket journey bio.';
    }

    // Calculate & render completion bar
    updateCompletionPercentage(profile);

    // Render Dynamic Performance Stats & Match History & Achievements
    renderBattingStats(profile.batting);
    renderBowlingStats(profile.bowling);
    renderFieldingStats(profile.fielding);
    renderMatchHistory(profile.matchHistory);
    renderAchievements(profile.achievements);
}

function renderAchievements(achievements) {
    const container = document.getElementById('prof-achievements-container');
    if (!container) return;

    if (!achievements || achievements.length === 0) {
        container.innerHTML = `
            <div style="padding: 1.5rem 1rem; text-align: center; color: #64748b; font-size: 0.88rem;">
                No achievements unlocked yet. Key career milestones (50s, 100s, 5-wicket hauls, debut, match wins) will unlock automatically as completed match scores are recorded.
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="prof-achievements-grid">
            ${achievements.map(a => {
                const colorClass = (a.type || 'GOLD').toLowerCase();
                return `
                    <div class="achievement-badge">
                        <div class="badge-icon-box ${colorClass}">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                        </div>
                        <div class="badge-content">
                            <h4 class="badge-name">${escapeHtml(a.title)}</h4>
                            <span class="badge-count-tag">${escapeHtml(a.description || a.type)}</span>
                        </div>
                    </div>
                `;
            }).join('')}
        </div>
    `;
}

function renderBattingStats(b) {
    const grid = document.getElementById('prof-batting-grid');
    if (!grid) return;

    if (!b || b.matches === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1 / -1; padding: 1.5rem; text-align: center; color: #64748b;">
                No batting statistics yet from completed matches.
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
    const grid = document.getElementById('prof-bowling-grid');
    if (!grid) return;

    if (!bw || bw.matches === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1 / -1; padding: 1.5rem; text-align: center; color: #64748b;">
                No bowling statistics yet from completed matches.
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
    const grid = document.getElementById('prof-fielding-grid');
    if (!grid) return;

    if (!f) {
        grid.innerHTML = `
            <div style="grid-column: 1 / -1; padding: 1.5rem; text-align: center; color: #64748b;">
                No fielding statistics yet.
            </div>
        `;
        return;
    }

    grid.innerHTML = `
        <div class="stat-box"><span class="stat-val">${f.catches || 0}</span><span class="stat-lbl">Catches</span></div>
        <div class="stat-box"><span class="stat-val">${f.runOuts || 0}</span><span class="stat-lbl">Run Outs</span></div>
        <div class="stat-box"><span class="stat-val">${f.stumpings || 0}</span><span class="stat-lbl">Stumpings</span></div>
    `;
}

function renderMatchHistory(history) {
    const tbody = document.getElementById('prof-history-tbody');
    if (!tbody) return;

    if (!history || history.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" style="text-align: center; padding: 1.5rem; color: #64748b;">
                    No completed matches played yet.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = history.map(m => {
        let perfText = '';
        if (m.runs !== null && m.runs !== undefined) {
            perfText += `${m.runs}${m.balls != null ? ' (' + m.balls + ')' : ''}`;
        }
        if (m.wickets !== null && m.wickets > 0) {
            perfText += (perfText ? ' & ' : '') + `${m.wickets} wkts`;
        }
        if (m.catches !== null && m.catches > 0) {
            perfText += (perfText ? ' & ' : '') + `${m.catches} c`;
        }
        if (!perfText) perfText = 'DNB';

        const dateStr = m.date ? new Date(m.date).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '-';
        const isWin = m.result && m.result.toLowerCase().includes('won') && m.result.includes(m.teamName);
        const resultClass = isWin ? 'win' : 'loss';

        return `
            <tr>
                <td><strong style="color: #0f172a;">${escapeHtml(m.opponentName || 'Opponent')}</strong></td>
                <td><span class="format-badge">${escapeHtml(m.format || 'T20')}</span></td>
                <td>${dateStr}</td>
                <td><span class="perf-highlight">${escapeHtml(perfText)}</span></td>
                <td><span class="result-badge ${resultClass}">${escapeHtml(m.result || 'Completed')}</span></td>
            </tr>
        `;
    }).join('');
}

function updateCompletionPercentage(profile) {
    const fields = [
        profile.name,
        profile.email,
        profile.profilePhotoUrl,
        profile.playingRole,
        profile.battingStyle,
        profile.bowlingStyle,
        profile.location,
        profile.dateOfBirth,
        profile.gender,
        profile.bio
    ];

    const filled = fields.filter(f => f !== null && f !== undefined && String(f).trim() !== '').length;
    const pct = Math.min(100, Math.max(20, Math.round((filled / fields.length) * 100)));

    const pctText = document.getElementById('profile-completion-pct');
    const pctBar = document.getElementById('profile-completion-bar');

    if (pctText) pctText.textContent = `${pct}%`;
    if (pctBar) pctBar.style.width = `${pct}%`;
}

async function loadUserTeams() {
    const teamsListEl = document.getElementById('prof-user-teams-list');
    if (!teamsListEl) return;

    try {
        const teams = await ApiService.get('/teams/my');
        if (!teams || teams.length === 0) {
            teamsListEl.innerHTML = `
                <div style="background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 12px; padding: 1.25rem; text-align: center; width: 100%; box-sizing: border-box;">
                    <p style="color: #64748b; font-size: 0.85rem; margin: 0 0 0.5rem 0;">You haven't joined any teams yet.</p>
                    <a href="teams.html" style="color: #0d5c3a; font-weight: 600; font-size: 0.85rem; text-decoration: none;">Create or Join Team &rarr;</a>
                </div>
            `;
            return;
        }

        teamsListEl.innerHTML = teams.map(team => {
            const logoUrl = ApiService.getImageUrl(team.logoUrl);
            const role = team.currentUserRole || 'PLAYER';
            const isOwner = role === 'OWNER' || role === 'CAPTAIN';

            return `
                <div style="display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; padding: 0.85rem 0.75rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; margin-bottom: 0.65rem; width: 100%; max-width: 100%; box-sizing: border-box; min-width: 0; flex-wrap: wrap;">
                    <div style="display: flex; align-items: center; gap: 0.75rem; min-width: 0; flex: 1;">
                        <div style="width: 36px; height: 36px; border-radius: 50%; background: #0b1727; display: flex; align-items: center; justify-content: center; overflow: hidden; border: 1px solid #10b981; color: #10b981; flex-shrink: 0;">
                            ${logoUrl 
                                ? `<img src="${logoUrl}" alt="${escapeHtml(team.name)}" style="width: 100%; height: 100%; object-fit: cover;">` 
                                : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>`}
                        </div>
                        <div style="min-width: 0; flex: 1; word-break: break-word;">
                            <h4 style="font-family: 'Outfit', sans-serif; font-size: 0.92rem; font-weight: 700; color: #0f172a; margin: 0 0 0.1rem 0; word-break: break-word;">${escapeHtml(team.name)}</h4>
                            <span style="font-size: 0.75rem; color: #64748b; font-weight: 600; word-break: break-word;">${escapeHtml(team.teamId)} &bull; ${team.memberCount || 1} Members</span>
                        </div>
                    </div>
                    <span style="padding: 0.2rem 0.55rem; background: ${isOwner ? '#fef3c7' : '#e6f4ed'}; color: ${isOwner ? '#d97706' : '#0d5c3a'}; font-size: 0.7rem; font-weight: 700; border-radius: 4px; flex-shrink: 0;">${escapeHtml(role)}</span>
                </div>
            `;
        }).join('');
    } catch (err) {
        console.error('Failed to load user teams for profile:', err);
        teamsListEl.innerHTML = `<div style="color: #ef4444; font-size: 0.85rem; padding: 1rem; text-align: center;">Unable to load teams.</div>`;
    }
}

function formatEnum(val) {
    if (!val) return null;
    return val
        .split('_')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, match => {
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return map[match];
    });
}

function initModalEvents() {
    const modal = document.getElementById('edit-profile-modal');
    const btnOpen = document.getElementById('btn-open-edit-modal');
    const btnClose = document.getElementById('btn-close-modal');
    const btnCancel = document.getElementById('btn-cancel-modal');
    const form = document.getElementById('edit-profile-form');

    if (!modal) return;

    if (btnOpen) {
        btnOpen.addEventListener('click', () => {
            populateModalForm();
            hideAlert('modal-alert');
            modal.style.display = 'flex';
        });
    }

    const closeModal = () => {
        modal.style.display = 'none';
    };

    if (btnClose) btnClose.addEventListener('click', closeModal);
    if (btnCancel) btnCancel.addEventListener('click', closeModal);

    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            hideAlert('modal-alert');

            const btnSave = document.getElementById('btn-save-profile');
            btnSave.disabled = true;
            btnSave.textContent = 'Saving...';

            const payload = {
                name: document.getElementById('input-name').value.trim(),
                dateOfBirth: document.getElementById('input-dob').value || null,
                gender: document.getElementById('input-gender').value || null,
                location: document.getElementById('input-location').value.trim() || null,
                playingRole: document.getElementById('input-playing-role').value || null,
                battingStyle: document.getElementById('input-batting-style').value || null,
                bowlingStyle: document.getElementById('input-bowling-style').value || null,
                bio: document.getElementById('input-bio').value.trim() || null
            };

            try {
                const updated = await ApiService.put('/profile/me', payload);
                currentProfile = updated;
                renderProfile(updated);
                closeModal();
                ApiService.showToast('Player profile updated successfully!', 'success');
            } catch (err) {
                showAlert('modal-alert', err.message || 'Failed to update profile.');
            } finally {
                btnSave.disabled = false;
                btnSave.textContent = 'Save Player Profile';
            }
        });
    }
}

function populateModalForm() {
    if (!currentProfile) return;

    if (document.getElementById('modal-read-user-id')) document.getElementById('modal-read-user-id').textContent = currentProfile.userId || 'CRK-------';
    if (document.getElementById('modal-read-email')) document.getElementById('modal-read-email').textContent = currentProfile.email || '';

    if (document.getElementById('input-name')) document.getElementById('input-name').value = currentProfile.name || '';
    if (document.getElementById('input-dob')) document.getElementById('input-dob').value = currentProfile.dateOfBirth || '';
    if (document.getElementById('input-gender')) document.getElementById('input-gender').value = currentProfile.gender || '';
    if (document.getElementById('input-location')) document.getElementById('input-location').value = currentProfile.location || '';
    if (document.getElementById('input-playing-role')) document.getElementById('input-playing-role').value = currentProfile.playingRole || '';
    if (document.getElementById('input-batting-style')) document.getElementById('input-batting-style').value = currentProfile.battingStyle || '';
    if (document.getElementById('input-bowling-style')) document.getElementById('input-bowling-style').value = currentProfile.bowlingStyle || '';
    if (document.getElementById('input-bio')) document.getElementById('input-bio').value = currentProfile.bio || '';
}

function initPhotoUpload() {
    const fileInput = document.getElementById('photo-file-input');
    const btnDelete = document.getElementById('btn-delete-photo');

    if (fileInput) {
        fileInput.addEventListener('change', async () => {
            if (!fileInput.files || fileInput.files.length === 0) return;

            const file = fileInput.files[0];
            hideAlert('profile-alert');

            if (file.size > 5 * 1024 * 1024) {
                showAlert('profile-alert', 'Selected image exceeds maximum limit of 5 MB.');
                fileInput.value = '';
                return;
            }

            const formData = new FormData();
            formData.append('file', file);

            try {
                const updated = await ApiService.postMultipart('/profile/me/photo', formData);
                currentProfile = updated;
                renderProfile(updated);
                ApiService.showToast('Profile photo updated!', 'success');
            } catch (err) {
                showAlert('profile-alert', err.message || 'Failed to upload photo.');
            } finally {
                fileInput.value = '';
            }
        });
    }

    if (btnDelete) {
        btnDelete.addEventListener('click', async () => {
            if (!confirm('Are you sure you want to remove your profile photo?')) return;
            hideAlert('profile-alert');

            try {
                const updated = await ApiService.delete('/profile/me/photo');
                currentProfile = updated;
                renderProfile(updated);
                ApiService.showToast('Profile photo removed.', 'info');
            } catch (err) {
                showAlert('profile-alert', err.message || 'Failed to remove photo.');
            }
        });
    }
}

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
