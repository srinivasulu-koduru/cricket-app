/**
 * Cricket App - Team Details JavaScript
 */

let currentTeam = null;
let currentTeamId = null;

document.addEventListener('DOMContentLoaded', () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    const urlParams = new URLSearchParams(window.location.search);
    currentTeamId = urlParams.get('id');

    if (!currentTeamId) {
        window.location.href = 'teams.html';
        return;
    }

    initLogout();
    loadTeamData();
    initCopyJoinLink();
    initInviteModal();
    initEditTeamModal();
    initLogoUpload();
    initLeaveTeam();
    initDeleteTeam();
});

function initLogout() {
    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.addEventListener('click', async () => {
            try {
                await ApiService.post('/auth/logout', {});
            } catch (ignored) {}
            ApiService.clearToken();
            ApiService.showToast('Logged out successfully.', 'info');
            window.location.href = 'login.html';
        });
    }
}

/* 1. Load Team & Members Data */
async function loadTeamData() {
    hideAlert('team-details-alert');

    try {
        currentTeam = await ApiService.get(`/teams/${currentTeamId}`);
        const members = await ApiService.get(`/teams/${currentTeamId}/members`);

        renderTeamHeader(currentTeam);
        renderMembersTable(members, currentTeam);
    } catch (err) {
        showAlert('team-details-alert', err.message || 'Failed to load team details.');
    }
}

function renderTeamHeader(team) {
    if (!team) return;

    if (document.getElementById('team-id-badge')) document.getElementById('team-id-badge').textContent = team.teamId || 'TEAM-------';
    if (document.getElementById('team-name')) document.getElementById('team-name').textContent = team.name || 'Unnamed Team';
    if (document.getElementById('team-description')) document.getElementById('team-description').textContent = team.description || 'No description provided.';
    if (document.getElementById('team-creator-name')) document.getElementById('team-creator-name').textContent = team.createdByName || 'Unknown';
    if (document.getElementById('my-role-badge')) document.getElementById('my-role-badge').textContent = team.currentUserRole || 'PLAYER';

    // Logo Display
    const logoImg = document.getElementById('team-logo-img');
    const logoFallback = document.getElementById('team-logo-fallback');

    if (team.logoUrl) {
        const fullLogoUrl = ApiService.getImageUrl(team.logoUrl);
        if (logoImg) {
            logoImg.src = fullLogoUrl;
            logoImg.style.display = 'block';
        }
        if (logoFallback) logoFallback.style.display = 'none';
    } else {
        if (logoImg) logoImg.style.display = 'none';
        if (logoFallback) logoFallback.style.display = 'flex';
    }

    // Captain/Owner Permissions Toolbar
    const isOwner = team.currentUserRole === 'OWNER';
    const isOwnerOrCaptain = isOwner || team.currentUserRole === 'CAPTAIN';
    const logoActionBox = document.getElementById('logo-action-box');
    const btnInvite = document.getElementById('btn-open-invite-modal');
    const btnEdit = document.getElementById('btn-open-edit-team-modal');
    const btnDelete = document.getElementById('btn-delete-team');

    if (logoActionBox) logoActionBox.style.display = isOwnerOrCaptain ? 'block' : 'none';
    if (btnInvite) btnInvite.style.display = isOwnerOrCaptain ? 'inline-flex' : 'none';
    if (btnEdit) btnEdit.style.display = isOwnerOrCaptain ? 'inline-flex' : 'none';
    if (btnDelete) btnDelete.style.display = isOwner ? 'inline-flex' : 'none';
}

function renderMembersTable(members, team) {
    const tbody = document.getElementById('members-table-body');
    const countSpan = document.getElementById('member-count');

    if (countSpan) countSpan.textContent = members ? members.length : 0;
    if (!tbody) return;

    if (!members || members.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 2rem;">No active members in this team.</td></tr>';
        return;
    }

    const isOwner = team.currentUserRole === 'OWNER';
    const isOwnerOrCaptain = isOwner || team.currentUserRole === 'CAPTAIN';

    tbody.innerHTML = members.map(m => {
        const photoUrl = ApiService.getImageUrl(m.profilePhotoUrl);
        const rolePillClass = m.role === 'OWNER' ? 'role-badge owner' : (m.role === 'CAPTAIN' ? 'role-badge owner' : 'role-badge');

        let actionBtnHtml = '-';
        if (isOwnerOrCaptain && m.role !== 'OWNER') {
            let roleToggleBtn = '';
            if (m.role === 'PLAYER') {
                roleToggleBtn = `<button class="btn btn-primary btn-sm" style="padding: 0.25rem 0.6rem; font-size: 0.78rem; background: #0d5c3a;" onclick="handleChangeRole('${escapeHtml(m.userId)}', 'CAPTAIN')">👑 Make Captain</button>`;
            } else if (m.role === 'CAPTAIN') {
                roleToggleBtn = `<button class="btn btn-secondary btn-sm" style="padding: 0.25rem 0.6rem; font-size: 0.78rem;" onclick="handleChangeRole('${escapeHtml(m.userId)}', 'PLAYER')">Remove Captain Role</button>`;
            }

            actionBtnHtml = `
                <div style="display: flex; gap: 0.4rem; justify-content: flex-end; align-items: center;">
                    ${roleToggleBtn}
                    <button class="btn btn-danger btn-sm" style="padding: 0.25rem 0.6rem; font-size: 0.78rem;" onclick="handleRemovePlayer('${escapeHtml(m.userId)}')">Remove</button>
                </div>
            `;
        }

        return `
            <tr>
                <td>
                    <div style="display: flex; gap: 0.75rem; align-items: center;">
                        <div style="width: 32px; height: 32px; border-radius: 50%; background: rgba(22, 35, 71, 0.9); border: 1px solid var(--color-pitch-emerald); display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0;">
                            ${photoUrl ? `<img src="${photoUrl}" alt="${escapeHtml(m.name)}" style="width:100%; height:100%; object-fit:cover;">` : `<span style="font-size: 0.85rem;">👤</span>`}
                        </div>
                        <strong style="color: #fff;">${escapeHtml(m.name)}</strong>
                    </div>
                </td>
                <td><span class="permanent-id-badge small">${escapeHtml(m.userId)}</span></td>
                <td><span class="${rolePillClass}">${escapeHtml(m.role)}</span></td>
                <td style="color: var(--text-secondary);">${formatDate(m.joinedAt)}</td>
                <td style="text-align: right;">${actionBtnHtml}</td>
            </tr>
        `;
    }).join('');
}

/* 2. Copy Shareable Join Link */
function initCopyJoinLink() {
    const btn = document.getElementById('btn-copy-join-link');
    if (!btn) return;

    btn.addEventListener('click', () => {
        if (!currentTeam || !currentTeam.joinToken) return;

        const origin = window.location.origin;
        const joinUrl = `${origin}/pages/join-team.html?token=${currentTeam.joinToken}`;

        navigator.clipboard.writeText(joinUrl).then(() => {
            ApiService.showToast('Shareable join link copied to clipboard!', 'success');
            const originalText = btn.textContent;
            btn.textContent = '✅ Link Copied!';
            setTimeout(() => btn.textContent = originalText, 2500);
        }).catch(() => {
            prompt('Copy this shareable team join link:', joinUrl);
        });
    });
}

/* 3. Invite Player Modal */
function initInviteModal() {
    const modal = document.getElementById('invite-player-modal');
    const btnOpen = document.getElementById('btn-open-invite-modal');
    const btnClose = document.getElementById('btn-close-invite-modal');
    const btnCancel = document.getElementById('btn-cancel-invite-modal');
    const form = document.getElementById('invite-player-form');

    if (!modal) return;

    if (btnOpen) {
        btnOpen.addEventListener('click', () => {
            if (form) form.reset();
            hideAlert('invite-modal-alert');
            modal.style.display = 'flex';
        });
    }

    const closeModal = () => modal.style.display = 'none';
    if (btnClose) btnClose.addEventListener('click', closeModal);
    if (btnCancel) btnCancel.addEventListener('click', closeModal);

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            hideAlert('invite-modal-alert');

            const cricketUserId = document.getElementById('invite-crk-id').value.trim();
            const btnSend = document.getElementById('btn-send-invitation');
            btnSend.disabled = true;
            btnSend.textContent = 'Sending...';

            try {
                const res = await ApiService.post(`/teams/${currentTeamId}/invitations`, { cricketUserId });
                closeModal();
                ApiService.showToast(`Invitation sent to player ${res.invitedUserName} (${res.invitedUserId})!`, 'success');
            } catch (err) {
                showAlert('invite-modal-alert', err.message || 'Failed to send invitation.');
            } finally {
                btnSend.disabled = false;
                btnSend.textContent = 'Send Invitation';
            }
        });
    }
}

/* 4. Edit Team Modal */
function initEditTeamModal() {
    const modal = document.getElementById('edit-team-modal');
    const btnOpen = document.getElementById('btn-open-edit-team-modal');
    const btnClose = document.getElementById('btn-close-edit-team-modal');
    const btnCancel = document.getElementById('btn-cancel-edit-team-modal');
    const form = document.getElementById('edit-team-form');

    if (!modal) return;

    if (btnOpen) {
        btnOpen.addEventListener('click', () => {
            if (currentTeam) {
                document.getElementById('edit-team-name').value = currentTeam.name || '';
                document.getElementById('edit-team-desc').value = currentTeam.description || '';
            }
            hideAlert('edit-team-modal-alert');
            modal.style.display = 'flex';
        });
    }

    const closeModal = () => modal.style.display = 'none';
    if (btnClose) btnClose.addEventListener('click', closeModal);
    if (btnCancel) btnCancel.addEventListener('click', closeModal);

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            hideAlert('edit-team-modal-alert');

            const name = document.getElementById('edit-team-name').value.trim();
            const description = document.getElementById('edit-team-desc').value.trim();

            const btnSave = document.getElementById('btn-save-team');
            btnSave.disabled = true;
            btnSave.textContent = 'Saving...';

            try {
                const updated = await ApiService.put(`/teams/${currentTeamId}`, { name, description });
                currentTeam = updated;
                renderTeamHeader(updated);
                closeModal();
                ApiService.showToast('Team information updated successfully!', 'success');
            } catch (err) {
                showAlert('edit-team-modal-alert', err.message || 'Failed to update team.');
            } finally {
                btnSave.disabled = false;
                btnSave.textContent = 'Save Changes';
            }
        });
    }
}

/* 5. Team Logo Upload */
function initLogoUpload() {
    const fileInput = document.getElementById('logo-file-input');
    if (!fileInput) return;

    fileInput.addEventListener('change', async () => {
        if (!fileInput.files || fileInput.files.length === 0) return;

        const file = fileInput.files[0];
        hideAlert('team-details-alert');

        if (file.size > 5 * 1024 * 1024) {
            showAlert('team-details-alert', 'Team logo size exceeds maximum limit of 5 MB.');
            fileInput.value = '';
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        try {
            const updated = await ApiService.postMultipart(`/teams/${currentTeamId}/logo`, formData);
            currentTeam = updated;
            renderTeamHeader(updated);
            ApiService.showToast('Team logo updated!', 'success');
        } catch (err) {
            showAlert('team-details-alert', err.message || 'Failed to upload team logo.');
        } finally {
            fileInput.value = '';
        }
    });
}

/* 6. Leave Team & Remove Player */
function initLeaveTeam() {
    const btnLeave = document.getElementById('btn-leave-team');
    if (!btnLeave) return;

    btnLeave.addEventListener('click', async () => {
        if (!confirm('Are you sure you want to leave this team?')) return;
        hideAlert('team-details-alert');

        try {
            const res = await ApiService.post(`/teams/${currentTeamId}/leave`, {});
            ApiService.showToast(res.message || 'You have left the team.', 'info');
            setTimeout(() => window.location.href = 'teams.html', 1000);
        } catch (err) {
            showAlert('team-details-alert', err.message || 'Failed to leave team.');
        }
    });
}

async function handleRemovePlayer(targetUserId) {
    if (!confirm(`Are you sure you want to remove player (${targetUserId}) from the team?`)) return;
    hideAlert('team-details-alert');

    try {
        const res = await ApiService.delete(`/teams/${currentTeamId}/members/${targetUserId}`);
        ApiService.showToast(res.message || 'Player removed.', 'info');
        loadTeamData();
    } catch (err) {
        showAlert('team-details-alert', err.message || 'Failed to remove player.');
    }
}

function initDeleteTeam() {
    const btnDelete = document.getElementById('btn-delete-team');
    if (!btnDelete) return;

    btnDelete.addEventListener('click', async () => {
        if (!currentTeam) return;
        if (!confirm(`Are you sure you want to permanently delete team "${currentTeam.name}" (${currentTeam.teamId})?\n\nThis will remove all team members and invitations. This action CANNOT be undone.`)) {
            return;
        }

        hideAlert('team-details-alert');

        try {
            btnDelete.disabled = true;
            const res = await ApiService.delete(`/teams/${currentTeamId}`);
            ApiService.showToast(res.message || 'Team deleted successfully.', 'success');
            setTimeout(() => window.location.href = 'teams.html', 1000);
        } catch (err) {
            console.error('Failed to delete team:', err);
            showAlert('team-details-alert', err.message || 'Failed to delete team.');
            btnDelete.disabled = false;
        }
    });
}

async function handleChangeRole(targetUserId, newRole) {
    if (!currentTeamId || !targetUserId || !newRole) return;
    const roleTitle = newRole === 'CAPTAIN' ? 'Captain' : 'Player';
    if (!confirm(`Are you sure you want to set this member's role to ${roleTitle}?`)) return;

    hideAlert('team-details-alert');

    try {
        const updatedMember = await ApiService.put(`/teams/${currentTeamId}/members/${targetUserId}/role`, { role: newRole });
        ApiService.showToast(`Role updated! ${updatedMember.name} is now a ${updatedMember.role}.`, 'success');
        loadTeamData();
    } catch (err) {
        console.error('Failed to update member role:', err);
        showAlert('team-details-alert', err.message || 'Failed to update member role.');
    }
}

/* Helpers */
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

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, match => {
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return map[match];
    });
}

function formatDate(isoStr) {
    if (!isoStr) return '-';
    try {
        const d = new Date(isoStr);
        return d.toLocaleDateString();
    } catch (e) {
        return isoStr;
    }
}
