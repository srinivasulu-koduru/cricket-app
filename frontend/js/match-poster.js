/**
 * Cricket App - Match Poster JavaScript
 * Stage 5 UI/UX Correction
 */

document.addEventListener('DOMContentLoaded', () => {
    loadPosterData();
});

async function loadPosterData() {
    const urlParams = new URLSearchParams(window.location.search);
    const matchId = urlParams.get('matchId') || urlParams.get('id');

    if (!matchId) {
        showAlert('mp-alert', 'No match ID specified in URL parameters.');
        return;
    }

    const backBtn = document.getElementById('poster-back-btn');
    if (backBtn) {
        backBtn.href = `match-details.html?id=${encodeURIComponent(matchId)}`;
    }

    try {
        const match = await ApiService.get('/matches/' + encodeURIComponent(matchId));
        renderMatchPoster(match);
    } catch (err) {
        console.error('Failed to load Match Poster data:', err);
        showAlert('mp-alert', err.message || 'Failed to load match details for poster.');
    }
}

function renderMatchPoster(match) {
    if (!match) return;

    const matchId = match.matchId || 'MATCH------';
    const title = match.matchName || 'Cricket Match Fixture';
    const format = match.format || 'T20';
    const overs = match.overs || 20;
    const venue = match.venue || 'Venue Ground';
    const dateStr = formatDate(match.matchDate);
    const timeStr = formatTime(match.matchTime);

    const teamA = match.teamA || {};
    const teamB = match.teamB || {};

    if (document.getElementById('mp-match-title')) document.getElementById('mp-match-title').textContent = title;
    if (document.getElementById('mp-format-spec')) document.getElementById('mp-format-spec').textContent = `${format} MATCH \u2022 ${overs} OVERS`;
    if (document.getElementById('mp-match-id')) document.getElementById('mp-match-id').textContent = matchId;
    if (document.getElementById('mp-date')) document.getElementById('mp-date').textContent = dateStr;
    if (document.getElementById('mp-time')) document.getElementById('mp-time').textContent = timeStr;
    if (document.getElementById('mp-venue')) document.getElementById('mp-venue').textContent = venue;

    // Team A
    if (document.getElementById('mp-team-a-name')) document.getElementById('mp-team-a-name').textContent = teamA.name || 'Team A';
    if (document.getElementById('mp-team-a-id')) document.getElementById('mp-team-a-id').textContent = teamA.teamId || '';
    const logoA = ApiService.getImageUrl(teamA.logoUrl);
    const imgA = document.getElementById('mp-team-a-logo');
    const fbA = document.getElementById('mp-team-a-fallback');
    if (logoA && imgA) {
        imgA.src = logoA;
        imgA.style.display = 'block';
        if (fbA) fbA.style.display = 'none';
    }

    // Team B
    if (document.getElementById('mp-team-b-name')) document.getElementById('mp-team-b-name').textContent = teamB.name || 'Team B';
    if (document.getElementById('mp-team-b-id')) document.getElementById('mp-team-b-id').textContent = teamB.teamId || '';
    const logoB = ApiService.getImageUrl(teamB.logoUrl);
    const imgB = document.getElementById('mp-team-b-logo');
    const fbB = document.getElementById('mp-team-b-fallback');
    if (logoB && imgB) {
        imgB.src = logoB;
        imgB.style.display = 'block';
        if (fbB) fbB.style.display = 'none';
    }
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

function showAlert(elementId, message) {
    const el = document.getElementById(elementId);
    if (el) {
        el.className = 'alert alert-error';
        el.textContent = message;
        el.style.display = 'block';
    }
}
