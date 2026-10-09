/**
 * Share logic for completed match scorecards
 */

function openShareOptions() {
    const matchId = typeof currentMatchId !== 'undefined' ? currentMatchId : new URLSearchParams(window.location.search).get('matchId');
    if (!matchId) {
        alert("Match ID not found");
        return;
    }
    const url = window.location.origin + '/scorecard.html?matchId=' + matchId;
    
    // Attempt to use native Web Share API
    if (navigator.share) {
        const shareTitle = 'Cricket Match Scorecard';
        let matchName = 'Cricket Match';
        
        // Try to get match name from state if available
        if (typeof state !== 'undefined' && state) {
            matchName = state.matchName || `${state.teamA?.name || 'Team A'} vs ${state.teamB?.name || 'Team B'}`;
        }
        
        const shareText = `Check out the completed match scorecard for ${matchName} on Cricket App!`;
        
        navigator.share({
            title: shareTitle,
            text: shareText,
            url: url
        }).catch(err => {
            console.log("Error using native share:", err);
            // Fallback
            document.getElementById('share-modal').style.display = 'flex';
        });
    } else {
        // Use custom modal
        document.getElementById('share-modal').style.display = 'flex';
    }
}

function closeShareOptions() {
    document.getElementById('share-modal').style.display = 'none';
}

function getShareUrl() {
    const matchId = typeof currentMatchId !== 'undefined' ? currentMatchId : new URLSearchParams(window.location.search).get('matchId');
    return window.location.origin + '/scorecard.html?matchId=' + matchId;
}

function shareToWhatsApp() {
    const text = `Check out this match scorecard: ${getShareUrl()}`;
    window.open(`https://api.whatsapp.com/send?text=${encodeURIComponent(text)}`, '_blank');
}

function shareToTelegram() {
    const url = getShareUrl();
    const text = `Check out this match scorecard!`;
    window.open(`https://t.me/share/url?url=${encodeURIComponent(url)}&text=${encodeURIComponent(text)}`, '_blank');
}

function shareToFacebook() {
    const url = getShareUrl();
    window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`, '_blank');
}

function copyShareLink() {
    const url = getShareUrl();
    navigator.clipboard.writeText(url).then(() => {
        const btn = document.getElementById('btn-copy-link');
        if (btn) {
            const originalHTML = btn.innerHTML;
            btn.innerHTML = '✅ Copied!';
            setTimeout(() => {
                btn.innerHTML = originalHTML;
            }, 2000);
        } else {
            alert('Link copied to clipboard!');
        }
    }).catch(err => {
        console.error('Could not copy text: ', err);
        prompt("Copy this link:", url);
    });
}
