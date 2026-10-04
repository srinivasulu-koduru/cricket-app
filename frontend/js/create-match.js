/**
 * Cricket App - Create Match JavaScript
 * Stage 5 - Match Creation
 */

let myTeamsList = [];
let allTeamsList = [];

document.addEventListener('DOMContentLoaded', () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    initLogout();
    loadSidebarUser();
    setDefaultDateAndTime();
    initTeamsDropdowns();
    initFormatChangeListener();
    initFormSubmit();
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

function setDefaultDateAndTime() {
    const dateInput = document.getElementById('matchDate');
    const timeInput = document.getElementById('matchTime');

    if (dateInput && !dateInput.value) {
        const today = new Date();
        const yyyy = today.getFullYear();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const dd = String(today.getDate()).padStart(2, '0');
        dateInput.value = `${yyyy}-${mm}-${dd}`;
    }

    if (timeInput && !timeInput.value) {
        timeInput.value = '09:00';
    }
}

async function initTeamsDropdowns() {
    const selectA = document.getElementById('teamAId');
    const selectB = document.getElementById('teamBId');

    try {
        const [myRes, allRes] = await Promise.all([
            ApiService.get('/teams/my'),
            ApiService.get('/teams/all').catch(() => ApiService.get('/teams/my'))
        ]);

        myTeamsList = myRes || [];
        allTeamsList = allRes || myTeamsList;

        populateTeamASelect();
        populateTeamBSelect();

        selectA.addEventListener('change', () => {
            populateTeamBSelect();
        });

    } catch (err) {
        console.error('Failed to load teams:', err);
        showAlert('create-match-alert', 'Failed to load teams. Please ensure you have created or joined a team first.');
    }
}

function populateTeamASelect() {
    const selectA = document.getElementById('teamAId');
    if (!selectA) return;

    // Only teams where user is CAPTAIN or OWNER can be selected as Team A
    const captainTeams = myTeamsList.filter(t => {
        const role = t.currentUserRole || t.role;
        return role === 'CAPTAIN' || role === 'OWNER';
    });

    if (captainTeams.length === 0) {
        selectA.innerHTML = '<option value="">-- You must be a Team Captain or Owner to schedule a match --</option>';
        return;
    }

    selectA.innerHTML = '<option value="">-- Select Your Team (Team A) --</option>' +
        captainTeams.map(t => {
            const role = t.currentUserRole || t.role || 'CAPTAIN';
            return `<option value="${escapeHtml(t.teamId)}">${escapeHtml(t.name)} (${escapeHtml(t.teamId)}) [${escapeHtml(role)}]</option>`;
        }).join('');
}

function populateTeamBSelect() {
    const selectA = document.getElementById('teamAId');
    const selectB = document.getElementById('teamBId');
    if (!selectB) return;

    const selectedAVal = selectA ? selectA.value : '';

    // Team B can be any team in the application except Team A
    const availableOpponents = allTeamsList.filter(t => t.teamId !== selectedAVal);

    if (availableOpponents.length === 0) {
        selectB.innerHTML = '<option value="">-- No eligible opponent teams available --</option>';
        return;
    }

    selectB.innerHTML = '<option value="">-- Select Opponent Team (Team B) --</option>' +
        availableOpponents.map(t => `<option value="${escapeHtml(t.teamId)}">${escapeHtml(t.name)} (${escapeHtml(t.teamId)})</option>`).join('');
}

function initFormatChangeListener() {
    const formatSelect = document.getElementById('format');
    const oversInput = document.getElementById('overs');

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

function initFormSubmit() {
    const form = document.getElementById('form-create-match');
    const btnSubmit = document.getElementById('btn-submit-match');

    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideAlert('create-match-alert');

        const matchName = document.getElementById('matchName').value.trim();
        const teamAId = document.getElementById('teamAId').value.trim();
        const teamBId = document.getElementById('teamBId').value.trim();
        const matchDate = document.getElementById('matchDate').value;
        const matchTime = document.getElementById('matchTime').value;
        const venue = document.getElementById('venue').value.trim();
        const format = document.getElementById('format').value;
        const overs = parseInt(document.getElementById('overs').value, 10);
        const description = document.getElementById('description').value.trim();

        if (!matchName) {
            showAlert('create-match-alert', 'Please enter a match name.');
            return;
        }

        if (!teamAId || !teamBId) {
            showAlert('create-match-alert', 'Please select both Team A and Team B.');
            return;
        }

        if (teamAId === teamBId) {
            showAlert('create-match-alert', 'Please select two different teams.');
            return;
        }

        if (!matchDate || !matchTime) {
            showAlert('create-match-alert', 'Please select a valid match date and time.');
            return;
        }

        if (!venue) {
            showAlert('create-match-alert', 'Please enter a venue.');
            return;
        }

        if (format === 'CUSTOM' && (isNaN(overs) || overs <= 0 || overs > 100)) {
            showAlert('create-match-alert', 'Please enter a valid number of overs (1 to 100) for custom format.');
            return;
        }

        const payload = {
            matchName,
            teamAId,
            teamBId,
            matchDate,
            matchTime,
            venue,
            format,
            overs,
            description
        };

        try {
            btnSubmit.disabled = true;
            btnSubmit.innerHTML = '<span>Creating Match...</span>';

            const response = await ApiService.post('/matches', payload);
            ApiService.showToast(`Match created successfully! ID: ${response.matchId}`, 'success');
            window.location.href = `match-details.html?id=${encodeURIComponent(response.matchId)}`;
        } catch (err) {
            console.error('Match creation error:', err);
            showAlert('create-match-alert', err.message || 'Unable to create match. Please try again.');
            btnSubmit.disabled = false;
            btnSubmit.innerHTML = `
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                <span>Schedule Match</span>
            `;
        }
    });
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
