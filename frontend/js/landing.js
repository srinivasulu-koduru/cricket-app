/**
 * Cricket App - Central Application Version Definition
 * Single source of truth for the application version.
 */
const APP_VERSION = "1.0.2";
window.APP_VERSION = APP_VERSION;

function init() {
    initLandingNav();
    initSmoothScroll();
    initScrollSpy();
    initAppVersion();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}

function initAppVersion() {
    const formattedVersion = `Cricket App · v${APP_VERSION}`;
    const versionElements = document.querySelectorAll('.app-version-display');
    versionElements.forEach(el => {
        el.textContent = formattedVersion;
    });
}

function initLandingNav() {
    const toggleBtn = document.getElementById('landing-nav-toggle');
    const drawer = document.getElementById('landing-nav-drawer');

    if (!toggleBtn || !drawer) return;

    toggleBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        const isOpen = drawer.classList.toggle('open');
        toggleBtn.setAttribute('aria-expanded', isOpen);
    });

    // Close on any link click inside drawer
    drawer.querySelectorAll('a').forEach(link => {
        link.addEventListener('click', () => {
            drawer.classList.remove('open');
            toggleBtn.setAttribute('aria-expanded', 'false');
        });
    });

    // Close when clicking outside
    document.addEventListener('click', (e) => {
        if (drawer.classList.contains('open') && !drawer.contains(e.target) && !toggleBtn.contains(e.target)) {
            drawer.classList.remove('open');
            toggleBtn.setAttribute('aria-expanded', 'false');
        }
    });
}

function initSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const targetId = this.getAttribute('href');
            if (!targetId || targetId === '#') return;
            
            const targetEl = document.querySelector(targetId);
            if (targetEl) {
                e.preventDefault();
                targetEl.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
}

function initScrollSpy() {
    const sections = document.querySelectorAll('section[id]');
    const navLinks = document.querySelectorAll('.nav-link-item[href^="#"]');

    if (!sections.length || !navLinks.length) return;

    window.addEventListener('scroll', () => {
        let current = '';
        const scrollY = window.pageYOffset;

        sections.forEach(section => {
            const sectionTop = section.offsetTop - 120;
            const sectionHeight = section.offsetHeight;
            if (scrollY >= sectionTop && scrollY < sectionTop + sectionHeight) {
                current = '#' + section.getAttribute('id');
            }
        });

        navLinks.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === current) {
                link.classList.add('active');
            }
        });
    }, { passive: true });
}
