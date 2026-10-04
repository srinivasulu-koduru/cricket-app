/**
 * Cricket App - Player Search JavaScript
 * Search players by Name or CRK ID (/api/players/search?q={query})
 */

let searchDebounceTimer = null;

document.addEventListener('DOMContentLoaded', () => {
    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    initLogout();
    loadSidebarUser();
    initSearchEvents();
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

function initSearchEvents() {
    const input = document.getElementById('player-search-input');
    const btnDoSearch = document.getElementById('btn-do-search');
    const btnClear = document.getElementById('btn-clear-search');

    if (!input) return;

    // Check query param in URL if opened like players.html?q=Sri
    const urlParams = new URLSearchParams(window.location.search);
    const initialQuery = urlParams.get('q');
    if (initialQuery) {
        input.value = initialQuery;
        if (btnClear) btnClear.style.display = 'block';
        performSearch(initialQuery);
    }

    input.addEventListener('input', () => {
        const val = input.value.trim();
        if (btnClear) {
            btnClear.style.display = val.length > 0 ? 'block' : 'none';
        }

        clearTimeout(searchDebounceTimer);
        if (val.length === 0) {
            renderInitialState();
            return;
        }

        searchDebounceTimer = setTimeout(() => {
            performSearch(val);
        }, 350);
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            clearTimeout(searchDebounceTimer);
            const val = input.value.trim();
            if (val.length > 0) {
                performSearch(val);
            }
        }
    });

    if (btnDoSearch) {
        btnDoSearch.addEventListener('click', () => {
            clearTimeout(searchDebounceTimer);
            const val = input.value.trim();
            if (val.length > 0) {
                performSearch(val);
            }
        });
    }

    if (btnClear) {
        btnClear.addEventListener('click', () => {
            input.value = '';
            btnClear.style.display = 'none';
            renderInitialState();
            input.focus();
        });
    }
}

async function performSearch(query) {
    const grid = document.getElementById('players-results-grid');
    const countTitle = document.getElementById('results-count-title');
    if (!grid) return;

    hideAlert('players-alert');
    renderSkeletonState();

    try {
        const results = await ApiService.get('/players/search?q=' + encodeURIComponent(query));
        renderSearchResults(results, query);
    } catch (err) {
        console.error('Player search error:', err);
        renderErrorState();
    }
}

function renderInitialState() {
    const grid = document.getElementById('players-results-grid');
    const countTitle = document.getElementById('results-count-title');
    if (countTitle) countTitle.textContent = 'Player Search Results';
    if (!grid) return;

    grid.innerHTML = `
        <div class="search-state-box">
            <div class="state-icon-circle emerald">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
            </div>
            <h3 class="state-title">Find your next teammate</h3>
            <p class="state-subtext">Search using a player name or CRK ID (e.g. Srinivasulu, CRK102847).</p>
        </div>
    `;
}

function renderSkeletonState() {
    const grid = document.getElementById('players-results-grid');
    if (!grid) return;

    grid.innerHTML = `
        <div class="player-card-skeleton"></div>
        <div class="player-card-skeleton"></div>
        <div class="player-card-skeleton"></div>
    `;
}

function renderSearchResults(results, query) {
    const grid = document.getElementById('players-results-grid');
    const countTitle = document.getElementById('results-count-title');
    if (!grid) return;

    if (countTitle) {
        countTitle.textContent = results && results.length > 0 
            ? `Search Results (${results.length} player${results.length > 1 ? 's' : ''} found for "${escapeHtml(query)}")`
            : `Search Results for "${escapeHtml(query)}"`;
    }

    if (!results || results.length === 0) {
        grid.innerHTML = `
            <div class="search-state-box">
                <div class="state-icon-circle gold">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line><line x1="8" y1="11" x2="14" y2="11"></line></svg>
                </div>
                <h3 class="state-title">No players found</h3>
                <p class="state-subtext">Try searching with a different name or CRK ID.</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = results.map(player => {
        const photoUrl = ApiService.getImageUrl(player.profilePhotoUrl);
        const name = escapeHtml(player.name || 'Player');
        const crkId = escapeHtml(player.userId || 'CRK-------');
        const role = formatEnum(player.playingRole) || 'Player';
        const batting = formatEnum(player.battingStyle) || 'Not Specified';
        const bowling = formatEnum(player.bowlingStyle) || 'Not Specified';
        const location = escapeHtml(player.location || 'Location Not Set');

        return `
            <div class="player-result-card">
                <div class="player-card-header-row">
                    <div class="player-card-avatar">
                        ${photoUrl 
                            ? `<img src="${photoUrl}" alt="${name}">` 
                            : `<span class="avatar-fallback-emoji">👤</span>`}
                    </div>
                    <div style="min-width: 0; flex: 1;">
                        <span class="player-crk-badge">${crkId}</span>
                        <h4 class="player-card-name">${name}</h4>
                        <span class="player-role-tag">${role}</span>
                    </div>
                </div>

                <div class="player-card-specs">
                    <div class="spec-row">
                        <span class="spec-lbl">Batting Style</span>
                        <span class="spec-val">${batting}</span>
                    </div>
                    <div class="spec-row">
                        <span class="spec-lbl">Bowling Style</span>
                        <span class="spec-val">${bowling}</span>
                    </div>
                    <div class="spec-row">
                        <span class="spec-lbl">Location</span>
                        <span class="spec-val">${location}</span>
                    </div>
                </div>

                <div class="player-card-footer">
                    <a href="public-profile.html?id=${encodeURIComponent(crkId)}" class="player-view-btn">
                        <span>View Profile</span>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
                    </a>
                </div>
            </div>
        `;
    }).join('');
}

function renderErrorState() {
    const grid = document.getElementById('players-results-grid');
    if (!grid) return;

    grid.innerHTML = `
        <div class="search-state-box">
            <div class="state-icon-circle danger">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
            </div>
            <h3 class="state-title">Something went wrong</h3>
            <p class="state-subtext">Please try again.</p>
        </div>
    `;
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

function hideAlert(elementId) {
    const el = document.getElementById(elementId);
    if (el) el.style.display = 'none';
}
