/**
 * CRICKET APP — CELEBRATION ANIMATION MANAGER
 * Manages full-screen celebration animations for:
 *   - TYPE 1: SIX
 *   - TYPE 2: FOUR
 *   - TYPE 3: WICKET
 *
 * Adheres strictly to the Visual Reference:
 *   Step 1: Launch from viewport bottom
 *   Step 2: Curved upward rise toward score/current-over target
 *   Step 3: Radiant blast & orbital halo reveal
 *
 * Ensures:
 *   - Deduplication (in-memory Set)
 *   - Scorer settings (ON/OFF, Styles, Previews)
 *   - Accessibility (prefers-reduced-motion)
 *   - Zero DOM alteration to existing dashboards
 *   - Clean automatic disposal
 */

(function (window) {
    'use strict';

    const processedEvents = new Set();
    let currentOverlayEl = null;
    let autoDismissTimer = null;

    const RocketSVG = (typeClass) => `
        <svg class="crk-rocket-svg" viewBox="0 0 100 160" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
                <linearGradient id="rocketHull_${typeClass}" x1="20" y1="20" x2="80" y2="120" gradientUnits="userSpaceOnUse">
                    <stop offset="0%" stop-color="#ffffff" stop-opacity="0.9"/>
                    <stop offset="30%" stop-color="${typeClass === 'four' ? '#38bdf8' : (typeClass === 'wicket' ? '#ef4444' : '#10b981')}"/>
                    <stop offset="85%" stop-color="${typeClass === 'four' ? '#0369a1' : (typeClass === 'wicket' ? '#991b1b' : '#047857')}"/>
                    <stop offset="100%" stop-color="#0f172a"/>
                </linearGradient>
                <linearGradient id="finGrad_${typeClass}" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0%" stop-color="#f97316"/>
                    <stop offset="100%" stop-color="#b91c1c"/>
                </linearGradient>
                <radialGradient id="portholeGlass" cx="50%" cy="50%" r="50%">
                    <stop offset="0%" stop-color="#e0f2fe"/>
                    <stop offset="70%" stop-color="#38bdf8"/>
                    <stop offset="100%" stop-color="#0284c7"/>
                </radialGradient>
            </defs>

            <!-- Delta Fins Left & Right -->
            <path d="M22 85 C14 100 2 120 4 135 C14 133 28 126 30 115 Z" fill="url(#finGrad_${typeClass})"/>
            <path d="M78 85 C86 100 98 120 96 135 C86 133 72 126 70 115 Z" fill="url(#finGrad_${typeClass})"/>

            <!-- Center Vertical Stabilizer -->
            <path d="M47 80 L53 80 L52 125 L48 125 Z" fill="url(#finGrad_${typeClass})"/>

            <!-- Aerodynamic Rocket Hull -->
            <path d="M50 8 C36 32 26 72 26 118 C38 123 62 123 74 118 C74 72 64 32 50 8 Z" fill="url(#rocketHull_${typeClass})" stroke="rgba(255,255,255,0.4)" stroke-width="1.5"/>

            <!-- Nosecone Cap Accent -->
            <path d="M50 8 C44 22 40 34 38 42 C46 45 54 45 62 42 C60 34 56 22 50 8 Z" fill="url(#finGrad_${typeClass})"/>

            <!-- Porthole Window with Riveted Metallic Rim -->
            <circle cx="50" cy="62" r="14" fill="#fb923c" stroke="#fed7aa" stroke-width="2"/>
            <circle cx="50" cy="62" r="11" fill="url(#portholeGlass)"/>
            <circle cx="47" cy="59" r="4" fill="#ffffff" opacity="0.6"/>

            <!-- Exhaust Nozzle -->
            <path d="M40 120 L60 120 L57 128 L43 128 Z" fill="#475569"/>
        </svg>
    `;

    const WicketStumpsSVG = `
        <svg class="crk-stumps-svg-wrap" viewBox="0 0 160 160" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
                <linearGradient id="stumpWood" x1="0" y1="0" x2="1" y2="0">
                    <stop offset="0%" stop-color="#fed7aa"/>
                    <stop offset="45%" stop-color="#f59e0b"/>
                    <stop offset="100%" stop-color="#b45309"/>
                </linearGradient>
                <linearGradient id="bailWood" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#fef3c7"/>
                    <stop offset="100%" stop-color="#d97706"/>
                </linearGradient>
            </defs>

            <!-- 3 Stumps with Dynamic Pivot Centers for Shatter -->
            <g class="crk-stump-leg">
                <rect x="42" y="44" width="10" height="92" rx="4" fill="url(#stumpWood)" stroke="#78350f" stroke-width="1.5"/>
            </g>
            <g class="crk-stump-middle">
                <rect x="75" y="44" width="10" height="92" rx="4" fill="url(#stumpWood)" stroke="#78350f" stroke-width="1.5"/>
            </g>
            <g class="crk-stump-off">
                <rect x="108" y="44" width="10" height="92" rx="4" fill="url(#stumpWood)" stroke="#78350f" stroke-width="1.5"/>
            </g>

            <!-- 2 Bails Flying Off -->
            <g class="crk-bail-left">
                <rect x="38" y="38" width="40" height="6" rx="2" fill="url(#bailWood)" stroke="#78350f" stroke-width="1"/>
            </g>
            <g class="crk-bail-right">
                <rect x="82" y="38" width="40" height="6" rx="2" fill="url(#bailWood)" stroke="#78350f" stroke-width="1"/>
            </g>
        </svg>
    `;

    const CelebrationManager = {
        isEnabled() {
            return localStorage.getItem('cricket_celebrations_enabled') !== 'false';
        },

        setEnabled(enabled) {
            localStorage.setItem('cricket_celebrations_enabled', enabled ? 'true' : 'false');
        },

        hasProcessed(celebrationId) {
            if (!celebrationId) return false;
            return processedEvents.has(String(celebrationId));
        },

        markProcessed(celebrationId) {
            if (!celebrationId) return;
            processedEvents.add(String(celebrationId));
            // Keep set bound to reasonable size
            if (processedEvents.size > 200) {
                const oldest = processedEvents.values().next().value;
                processedEvents.delete(oldest);
            }
        },

        trigger(eventType, celebrationId, matchId) {
            if (!this.isEnabled()) return;
            if (!eventType) return;

            const normalizedType = String(eventType).toUpperCase().trim();
            if (normalizedType !== 'SIX' && normalizedType !== 'FOUR' && normalizedType !== 'WICKET') {
                return;
            }

            if (celebrationId) {
                if (this.hasProcessed(celebrationId)) {
                    return; // Avoid duplicate animation!
                }
                this.markProcessed(celebrationId);
            }

            this.play(normalizedType);
        },

        preview(eventType) {
            const normalizedType = String(eventType).toUpperCase().trim();
            this.play(normalizedType);
        },

        play(type) {
            // Clean up existing overlay if any is currently playing
            this.dismiss();

            const isReducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
            const overlay = document.createElement('div');
            overlay.id = 'cricket-celebration-overlay';
            currentOverlayEl = overlay;

            const typeLower = type.toLowerCase();

            // 1. Ambient Glow
            const ambientGlow = document.createElement('div');
            ambientGlow.className = `crk-ambient-glow ${typeLower}`;
            overlay.appendChild(ambientGlow);

            if (!isReducedMotion) {
                // 2. Launch Smoke & Rocket
                const smoke = document.createElement('div');
                smoke.className = 'crk-launch-smoke';
                overlay.appendChild(smoke);

                const rocketWrap = document.createElement('div');
                rocketWrap.className = `crk-rocket-container ${typeLower} launching`;
                rocketWrap.innerHTML = `
                    ${RocketSVG(typeLower)}
                    <div class="crk-rocket-flame"></div>
                    <div class="crk-rocket-exhaust-tail"></div>
                `;
                overlay.appendChild(rocketWrap);
            }

            // 3. Burst Container at target area
            const burst = document.createElement('div');
            burst.className = `crk-burst-container ${typeLower}`;

            if (!isReducedMotion) {
                // Flash
                const flash = document.createElement('div');
                flash.className = 'crk-burst-flash';
                burst.appendChild(flash);

                // Halo Orbit Ring (Reference Image key visual)
                const halo = document.createElement('div');
                halo.className = 'crk-halo-ring';
                burst.appendChild(halo);

                const dustRing = document.createElement('div');
                dustRing.className = 'crk-halo-dust-ring';
                burst.appendChild(dustRing);

                // Radial Explosive Particles
                const particleCount = type === 'WICKET' ? 28 : 24;
                const colors = type === 'SIX'
                    ? ['#34d399', '#10b981', '#fef08a', '#fbbf24', '#ffffff']
                    : (type === 'FOUR'
                        ? ['#38bdf8', '#0284c7', '#a7f3d0', '#e0f2fe', '#ffffff']
                        : ['#ef4444', '#f87171', '#fbbf24', '#dc2626', '#ffffff']);

                for (let i = 0; i < particleCount; i++) {
                    const p = document.createElement('div');
                    p.className = 'crk-particle';
                    const angle = (i / particleCount) * (Math.PI * 2) + (Math.random() * 0.2 - 0.1);
                    const distance = 80 + Math.random() * 110;
                    const tx = Math.cos(angle) * distance;
                    const ty = Math.sin(angle) * distance;
                    const color = colors[i % colors.length];
                    const size = 5 + Math.random() * 7;
                    const delay = 1.05 + Math.random() * 0.15;

                    p.style.setProperty('--tx', `${tx}px`);
                    p.style.setProperty('--ty', `${ty}px`);
                    p.style.background = color;
                    p.style.boxShadow = `0 0 10px ${color}`;
                    p.style.width = `${size}px`;
                    p.style.height = `${size}px`;
                    p.style.animation = `crkParticleFly 1.2s ${delay}s cubic-bezier(0.12, 0.9, 0.2, 1) forwards`;

                    burst.appendChild(p);
                }
            }

            // 4. Reveal Content
            const reveal = document.createElement('div');
            reveal.className = 'crk-reveal-box';

            if (type === 'SIX') {
                reveal.innerHTML = `
                    <div class="crk-big-number six">6</div>
                    <div class="crk-celeb-pill-badge six">
                        <span>✨ 6 RUNS!</span>
                        <span class="crk-badge-subtext">MAXIMUM</span>
                    </div>
                `;
            } else if (type === 'FOUR') {
                reveal.innerHTML = `
                    <div class="crk-big-number four">4</div>
                    <div class="crk-celeb-pill-badge four">
                        <span>⚡ 4 RUNS!</span>
                        <span class="crk-badge-subtext">BOUNDARY</span>
                    </div>
                `;
            } else if (type === 'WICKET') {
                reveal.innerHTML = `
                    <div class="crk-wicket-stage">
                        <div class="crk-wicket-shockwave"></div>
                        ${WicketStumpsSVG}
                    </div>
                    <h2 class="crk-big-wicket-title">WICKET!</h2>
                    <div class="crk-celeb-pill-badge wicket">
                        <span>💥 OUT!</span>
                        <span class="crk-badge-subtext">WICKET FALLEN</span>
                    </div>
                `;
            }

            burst.appendChild(reveal);
            overlay.appendChild(burst);

            // Mount overlay to document
            document.body.appendChild(overlay);

            // Auto fade-out and cleanup timing
            const totalDuration = type === 'WICKET' ? 2750 : 2550;
            autoDismissTimer = setTimeout(() => {
                if (overlay) {
                    overlay.classList.add('fading-out');
                    setTimeout(() => {
                        CelebrationManager.dismiss();
                    }, 350);
                }
            }, totalDuration);
        },

        dismiss() {
            if (autoDismissTimer) {
                clearTimeout(autoDismissTimer);
                autoDismissTimer = null;
            }
            if (currentOverlayEl && currentOverlayEl.parentNode) {
                currentOverlayEl.parentNode.removeChild(currentOverlayEl);
            }
            currentOverlayEl = null;
        }
    };

    window.CelebrationManager = CelebrationManager;

})(window);
