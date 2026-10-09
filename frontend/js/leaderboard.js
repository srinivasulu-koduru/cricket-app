// js/leaderboard.js

document.addEventListener('DOMContentLoaded', () => {
    // 1. Handle Navigation & Profile
    setupNavigation();

    // 2. State & Elements
    let currentCategory = 'runs';
    let currentRankings = []; // Original data from API
    let filteredRankings = []; // Data after search filter

    const podiumContainer = document.getElementById('lb-podium');
    const tableBody = document.getElementById('lb-table-body');
    const tableHeadRow = document.getElementById('lb-table-head-row');
    const searchInput = document.getElementById('lb-search-input');
    const tableTitle = document.getElementById('lb-table-title');
    const categoryBtns = document.querySelectorAll('.lb-cat-btn');

    // Category configurations (titles and table headers)
    const categoryConfigs = {
        'runs': {
            title: 'Top Run Scorers',
            columns: [
                { id: 'rank', label: 'RANK', className: 'td-rank' },
                { id: 'player', label: 'PLAYER', className: '' },
                { id: 'team', label: 'TEAM', className: 'td-team' },
                { id: 'matches', label: 'MAT', className: 'td-stat' },
                { id: 'strikeRate', label: 'SR', className: 'td-stat' },
                { id: 'runs', label: 'RUNS', className: 'td-stat-primary' }
            ]
        },
        'sixes': {
            title: 'Most Sixes',
            columns: [
                { id: 'rank', label: 'RANK', className: 'td-rank' },
                { id: 'player', label: 'PLAYER', className: '' },
                { id: 'team', label: 'TEAM', className: 'td-team' },
                { id: 'matches', label: 'MAT', className: 'td-stat' },
                { id: 'runs', label: 'RUNS', className: 'td-stat' },
                { id: 'sixes', label: 'SIXES', className: 'td-stat-primary' }
            ]
        },
        'fours': {
            title: 'Most Fours',
            columns: [
                { id: 'rank', label: 'RANK', className: 'td-rank' },
                { id: 'player', label: 'PLAYER', className: '' },
                { id: 'team', label: 'TEAM', className: 'td-team' },
                { id: 'matches', label: 'MAT', className: 'td-stat' },
                { id: 'runs', label: 'RUNS', className: 'td-stat' },
                { id: 'fours', label: 'FOURS', className: 'td-stat-primary' }
            ]
        },
        'strike_rate': {
            title: 'Best Batting Strike Rate',
            columns: [
                { id: 'rank', label: 'RANK', className: 'td-rank' },
                { id: 'player', label: 'PLAYER', className: '' },
                { id: 'team', label: 'TEAM', className: 'td-team' },
                { id: 'matches', label: 'MAT', className: 'td-stat' },
                { id: 'runs', label: 'RUNS', className: 'td-stat' },
                { id: 'strikeRate', label: 'SR', className: 'td-stat-primary' }
            ]
        },
        'wickets': {
            title: 'Top Wicket Takers',
            columns: [
                { id: 'rank', label: 'RANK', className: 'td-rank' },
                { id: 'player', label: 'PLAYER', className: '' },
                { id: 'team', label: 'TEAM', className: 'td-team' },
                { id: 'matches', label: 'MAT', className: 'td-stat' },
                { id: 'economy', label: 'ECON', className: 'td-stat' },
                { id: 'wickets', label: 'WKTS', className: 'td-stat-primary' }
            ]
        },
        'economy': {
            title: 'Best Bowling Economy',
            columns: [
                { id: 'rank', label: 'RANK', className: 'td-rank' },
                { id: 'player', label: 'PLAYER', className: '' },
                { id: 'team', label: 'TEAM', className: 'td-team' },
                { id: 'matches', label: 'MAT', className: 'td-stat' },
                { id: 'wickets', label: 'WKTS', className: 'td-stat' },
                { id: 'economy', label: 'ECON', className: 'td-stat-primary' }
            ]
        },
        'maidens': {
            title: 'Most Maidens',
            columns: [
                { id: 'rank', label: 'RANK', className: 'td-rank' },
                { id: 'player', label: 'PLAYER', className: '' },
                { id: 'team', label: 'TEAM', className: 'td-team' },
                { id: 'matches', label: 'MAT', className: 'td-stat' },
                { id: 'wickets', label: 'WKTS', className: 'td-stat' },
                { id: 'maidens', label: 'MAIDENS', className: 'td-stat-primary' }
            ]
        }
    };

    // 3. Setup Event Listeners
    categoryBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            categoryBtns.forEach(b => b.classList.remove('active'));
            e.currentTarget.classList.add('active');
            currentCategory = e.currentTarget.dataset.category;
            fetchLeaderboard();
        });
    });

    searchInput.addEventListener('input', (e) => {
        const term = e.target.value.toLowerCase();
        filteredRankings = currentRankings.filter(p => p.name && p.name.toLowerCase().includes(term));
        renderTable(); // We usually don't filter podium based on search, only the table
    });

    // 4. Initial Load
    fetchLeaderboard();

    // ----------------------------------------------------
    // Functions
    // ----------------------------------------------------

    async function fetchLeaderboard() {
        showLoading();
        try {
            // Unauthenticated endpoint using ApiService
            const data = await ApiService.get(`/public/leaderboard?category=${currentCategory}&timeframe=All Time`, { skipAuth: true });
            
            currentRankings = data.rankings || [];
            filteredRankings = [...currentRankings];
            
            renderUI();
        } catch (error) {
            console.error('Leaderboard error:', error);
            showError();
        }
    }

    function showLoading() {
        podiumContainer.innerHTML = '<div class="lb-podium-placeholder">Loading Top Players...</div>';
        tableBody.innerHTML = '<tr><td colspan="10" style="text-align:center; padding: 3rem;">Loading rankings...</td></tr>';
    }

    function showError() {
        podiumContainer.innerHTML = '<div class="lb-podium-placeholder" style="color:#ef4444;">Failed to load data.</div>';
        tableBody.innerHTML = '<tr><td colspan="10" style="text-align:center; padding: 3rem; color:#ef4444;">Could not load leaderboard. Please try again.</td></tr>';
    }

    function renderUI() {
        tableTitle.innerText = categoryConfigs[currentCategory].title;
        renderPodium();
        renderTableHeader();
        renderTable();
    }

    function renderPodium() {
        podiumContainer.innerHTML = '';
        
        if (currentRankings.length === 0) {
            podiumContainer.innerHTML = '<div class="lb-podium-placeholder" style="color:#f8fafc; font-size:1.1rem; font-weight:600;">No one has the best performance yet.</div>';
            return;
        }

        // Podium wants Rank 2, Rank 1, Rank 3
        const top3 = [
            currentRankings[1], // Rank 2
            currentRankings[0], // Rank 1
            currentRankings[2]  // Rank 3
        ];

        top3.forEach((player, index) => {
            if (!player) return;
            
            // Map index to visual position
            let positionClass = '';
            let rankNum = 0;
            if (index === 0) { positionClass = 'lb-rank-2'; rankNum = 2; }
            if (index === 1) { positionClass = 'lb-rank-1'; rankNum = 1; }
            if (index === 2) { positionClass = 'lb-rank-3'; rankNum = 3; }

            const avatar = player.profilePhotoUrl ? `http://localhost:8080${player.profilePhotoUrl}` : '../assets/images/default-avatar.png';
            const team = player.teamName || 'Independent';
            
            // Determine primary stat to show on podium based on category
            let statLabel = categoryConfigs[currentCategory].columns[categoryConfigs[currentCategory].columns.length - 1].label;
            let statValue = player.value;
            // Format stat if economy/sr
            if (statValue % 1 !== 0) statValue = statValue.toFixed(2);
            
            const cardHtml = `
                <div class="lb-podium-card ${positionClass}">
                    <div class="lb-podium-rank-badge">${rankNum}</div>
                    <img src="${avatar}" alt="${player.name}" class="lb-podium-avatar" onerror="this.src='../assets/images/default-avatar.png'">
                    <h3 class="lb-podium-name" title="${player.name}">${player.name}</h3>
                    <div class="lb-podium-team">${team}</div>
                    <div class="lb-podium-stat">${statValue}</div>
                    <div class="lb-podium-label">${statLabel}</div>
                </div>
            `;
            podiumContainer.insertAdjacentHTML('beforeend', cardHtml);
        });
    }

    function renderTableHeader() {
        const cols = categoryConfigs[currentCategory].columns;
        let html = '';
        cols.forEach(c => {
            html += `<th>${c.label}</th>`;
        });
        tableHeadRow.innerHTML = html;
    }

    function renderTable() {
        tableBody.innerHTML = '';
        
        if (filteredRankings.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="10" style="text-align:center; padding: 3rem; color:#f8fafc; font-weight:600; font-size:1.1rem;">No one has the best performance yet.</td></tr>';
            return;
        }

        const cols = categoryConfigs[currentCategory].columns;
        
        filteredRankings.forEach(player => {
            const tr = document.createElement('tr');
            
            // Assign rank class if top 3
            if (player.rank === 1) tr.classList.add('rank-1');
            if (player.rank === 2) tr.classList.add('rank-2');
            if (player.rank === 3) tr.classList.add('rank-3');

            let rowHtml = '';
            
            cols.forEach(col => {
                let cellValue = player[col.id];
                if (col.id === 'player') {
                    const avatar = player.profilePhotoUrl ? `http://localhost:8080${player.profilePhotoUrl}` : '../assets/images/default-avatar.png';
                    rowHtml += `
                        <td>
                            <div class="td-player">
                                <img src="${avatar}" alt="${player.name}" onerror="this.src='../assets/images/default-avatar.png'">
                                <span class="td-player-name">${player.name}</span>
                            </div>
                        </td>
                    `;
                } else if (col.id === 'team') {
                    rowHtml += `<td class="${col.className}">${player.teamName || '-'}</td>`;
                } else if (col.id === 'strikeRate' || col.id === 'economy') {
                    rowHtml += `<td class="${col.className}">${player[col.id].toFixed(2)}</td>`;
                } else {
                    rowHtml += `<td class="${col.className}">${cellValue}</td>`;
                }
            });
            
            tr.innerHTML = rowHtml;
            tableBody.appendChild(tr);
        });
    }

    function setupNavigation() {
        // Mobile toggle
        const toggleBtn = document.getElementById('lb-mobile-toggle');
        const drawer = document.getElementById('lb-mobile-drawer');
        toggleBtn.addEventListener('click', () => {
            drawer.classList.toggle('open');
        });

        // Profile handling
        if (typeof ApiService !== 'undefined') {
            const token = ApiService.getToken();
            const profileBtn = document.getElementById('lb-profile-btn');
            
            if (token) {
                // Assuming we can fetch profile easily or rely on localstorage, but we'll try API
                fetch('http://localhost:8080/api/auth/me', {
                    headers: { 'Authorization': 'Bearer ' + token }
                }).then(r => {
                    if(r.ok) return r.json();
                    throw new Error('Not logged in');
                }).then(u => {
                    if (u.profile && u.profile.profilePhotoUrl) {
                        const img = document.getElementById('lb-profile-img');
                        img.src = `http://localhost:8080${u.profile.profilePhotoUrl}`;
                        img.style.display = 'block';
                        document.getElementById('lb-profile-fallback').style.display = 'none';
                    }
                }).catch(e => {
                    // Profile button remains fallback, but points to login if not valid
                    profileBtn.href = 'login.html';
                });
            } else {
                profileBtn.href = 'login.html';
            }
        }
    }
});
