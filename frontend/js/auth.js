/**
 * Cricket App - Authentication & Account Management Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    initRegisterForm();
    initVerifyOtpForm();
    initLoginForm();
    initDashboard();
    initForgotPasswordForm();
    initResetPasswordForm();
});

/* Helper: Display Alert Messages & Toasts */
function showAlert(elementId, message, type = 'error') {
    const alertEl = document.getElementById(elementId);
    if (alertEl) {
        alertEl.className = `alert alert-${type}`;
        alertEl.textContent = message;
        alertEl.style.display = 'block';
    }
    if (typeof ApiService !== 'undefined' && ApiService.showToast) {
        ApiService.showToast(message, type);
    }
}

function hideAlert(elementId) {
    const alertEl = document.getElementById(elementId);
    if (alertEl) alertEl.style.display = 'none';
}

/* 1. Register Page */
function initRegisterForm() {
    const form = document.getElementById('register-form');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideAlert('register-alert');

        const name = document.getElementById('name').value.trim();
        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (password !== confirmPassword) {
            showAlert('register-alert', 'Passwords do not match');
            return;
        }

        const btnSubmit = document.getElementById('btn-register');
        btnSubmit.disabled = true;
        btnSubmit.textContent = 'Sending OTP...';

        try {
            await ApiService.post('/auth/register/request-otp', {
                name, email, password, confirmPassword
            });

            sessionStorage.setItem('pendingEmail', email);
            sessionStorage.setItem('otpPurpose', 'REGISTRATION');
            ApiService.showToast('OTP sent successfully to your email!', 'success');
            window.location.href = 'verify-otp.html';
        } catch (err) {
            showAlert('register-alert', err.message);
            btnSubmit.disabled = false;
            btnSubmit.textContent = 'Create Account & Send OTP';
        }
    });
}

/* 2. OTP Verification Page */
function initVerifyOtpForm() {
    const form = document.getElementById('verify-otp-form');
    if (!form) return;

    const email = sessionStorage.getItem('pendingEmail');
    const purpose = sessionStorage.getItem('otpPurpose') || 'REGISTRATION';

    const displayEmailEl = document.getElementById('display-email');
    if (displayEmailEl && email) displayEmailEl.textContent = email;

    if (!email) {
        window.location.href = purpose === 'PASSWORD_RESET' ? 'forgot-password.html' : 'register.html';
        return;
    }

    // Initialize 6-digit box auto-advance logic
    setupOtpDigitInputs();

    startOtpCountdown(60);

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideAlert('otp-alert');

        // Combine inputs from 6 digit boxes or single hidden input
        let otp = combineOtpDigits();
        if (!otp || otp.length < 6) {
            showAlert('otp-alert', 'Please enter all 6 digits of the OTP code.');
            return;
        }

        const btnVerify = document.getElementById('btn-verify-otp');
        btnVerify.disabled = true;
        btnVerify.textContent = 'Verifying...';

        try {
            const endpoint = purpose === 'PASSWORD_RESET' 
                ? '/auth/forgot-password/verify-otp' 
                : '/auth/register/verify-otp';

            const data = await ApiService.post(endpoint, { email, otp });

            if (purpose === 'REGISTRATION') {
                document.getElementById('otp-input-section').style.display = 'none';
                document.getElementById('success-user-id-section').style.display = 'block';
                document.getElementById('created-user-id').textContent = data.userId || 'CRKXXXXXX';
                sessionStorage.removeItem('pendingEmail');
                ApiService.showToast('Account activated successfully!', 'success');
            } else {
                sessionStorage.setItem('resetOtp', otp);
                ApiService.showToast('OTP verified! Set your new password.', 'success');
                window.location.href = 'reset-password.html';
            }
        } catch (err) {
            showAlert('otp-alert', err.message);
            btnVerify.disabled = false;
            btnVerify.textContent = 'Verify OTP & Activate';
        }
    });

    const btnResend = document.getElementById('btn-resend-otp');
    if (btnResend) {
        btnResend.addEventListener('click', async () => {
            hideAlert('otp-alert');
            btnResend.disabled = true;

            try {
                const endpoint = purpose === 'PASSWORD_RESET'
                    ? '/auth/forgot-password/request-otp'
                    : '/auth/register/resend-otp';

                await ApiService.post(endpoint, { email });
                showAlert('otp-alert', 'A new OTP has been sent to your email.', 'success');
                startOtpCountdown(60);
            } catch (err) {
                showAlert('otp-alert', err.message);
                btnResend.disabled = false;
            }
        });
    }
}

function setupOtpDigitInputs() {
    const digitInputs = document.querySelectorAll('.otp-digit-input');
    if (!digitInputs || digitInputs.length === 0) return;

    digitInputs.forEach((input, index) => {
        input.addEventListener('input', (e) => {
            const val = e.target.value;
            if (val.length === 1 && index < digitInputs.length - 1) {
                digitInputs[index + 1].focus();
            }
        });

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Backspace' && !e.target.value && index > 0) {
                digitInputs[index - 1].focus();
            }
        });

        input.addEventListener('paste', (e) => {
            e.preventDefault();
            const pasteData = (e.clipboardData || window.clipboardData).getData('text').trim();
            if (/^\d{6}$/.test(pasteData)) {
                pasteData.split('').forEach((char, i) => {
                    if (digitInputs[i]) digitInputs[i].value = char;
                });
                digitInputs[5].focus();
            }
        });
    });
}

function combineOtpDigits() {
    const hiddenOtpInput = document.getElementById('otp');
    const digitInputs = document.querySelectorAll('.otp-digit-input');
    
    if (digitInputs && digitInputs.length === 6) {
        let code = '';
        digitInputs.forEach(inp => code += inp.value.trim());
        if (hiddenOtpInput) hiddenOtpInput.value = code;
        return code;
    }
    
    return hiddenOtpInput ? hiddenOtpInput.value.trim() : '';
}

function startOtpCountdown(seconds) {
    const countdownEl = document.getElementById('cooldown-timer');
    const btnResend = document.getElementById('btn-resend-otp');
    if (!countdownEl || !btnResend) return;

    btnResend.disabled = true;
    let remaining = seconds;

    const timer = setInterval(() => {
        remaining--;
        countdownEl.textContent = `(Resend available in ${remaining}s)`;
        if (remaining <= 0) {
            clearInterval(timer);
            countdownEl.textContent = '';
            btnResend.disabled = false;
        }
    }, 1000);
}

/* 3. Login Page */
function initLoginForm() {
    const form = document.getElementById('login-form');
    if (!form) return;

    if (typeof ApiService !== 'undefined' && ApiService.getToken()) {
        window.location.replace('dashboard.html');
        return;
    }

    // Password visibility toggle handler
    const toggleBtn = document.getElementById('toggle-password-btn');
    const passwordInput = document.getElementById('password');
    const iconEyeOff = document.getElementById('icon-eye-off');
    const iconEye = document.getElementById('icon-eye');

    if (toggleBtn && passwordInput) {
        const togglePassword = (e) => {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }
            const isPassword = passwordInput.getAttribute('type') === 'password';
            passwordInput.setAttribute('type', isPassword ? 'text' : 'password');
            if (iconEyeOff && iconEye) {
                iconEyeOff.style.display = isPassword ? 'none' : 'block';
                iconEye.style.display = isPassword ? 'block' : 'none';
            }
        };
        toggleBtn.addEventListener('click', togglePassword);
    }

    const executeLogin = async () => {
        hideAlert('login-alert');

        const emailEl = document.getElementById('email');
        const passwordEl = document.getElementById('password');
        if (!emailEl || !passwordEl) return;

        const email = emailEl.value.trim();
        const password = passwordEl.value;

        if (!email || !password) {
            showAlert('login-alert', 'Please enter your email and password.');
            return;
        }

        const btnLogin = document.getElementById('btn-login');
        if (btnLogin) {
            btnLogin.disabled = true;
            btnLogin.textContent = 'Signing in...';
        }

        try {
            const data = await ApiService.post('/auth/login', { email, password });
            ApiService.setToken(data.token);
            ApiService.showToast('Login successful! Welcome back.', 'success');
            window.location.href = 'dashboard.html';
        } catch (err) {
            showAlert('login-alert', err.message);
            if (btnLogin) {
                btnLogin.disabled = false;
                btnLogin.textContent = 'Sign In';
            }
        }
    };

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        executeLogin();
    });

    const btnLogin = document.getElementById('btn-login');
    if (btnLogin) {
        btnLogin.addEventListener('click', (e) => {
            if (!form.checkValidity || form.checkValidity()) {
                e.preventDefault();
                executeLogin();
            }
        });
    }
}

/* 4. Dashboard Page */
async function initDashboard() {
    const dashboardSection = document.getElementById('dashboard-content');
    if (!dashboardSection) return;

    const token = ApiService.getToken();
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    try {
        const user = await ApiService.get('/auth/me');
        if (document.getElementById('user-name')) document.getElementById('user-name').textContent = user.name;
        if (document.getElementById('user-id')) document.getElementById('user-id').textContent = user.userId;
        if (document.getElementById('user-email')) document.getElementById('user-email').textContent = user.email;
        if (document.getElementById('user-verified')) document.getElementById('user-verified').textContent = user.emailVerified ? 'Yes (Verified)' : 'No';
        if (document.getElementById('header-user-name')) document.getElementById('header-user-name').textContent = user.name;
        if (document.getElementById('header-user-id')) document.getElementById('header-user-id').textContent = user.userId;
    } catch (err) {
        console.error('Failed to load dashboard data:', err.message);
        if (err.status === 401) {
            ApiService.clearToken();
            window.location.href = 'login.html';
        }
    }

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

/* 5. Forgot Password Request Page */
function initForgotPasswordForm() {
    const form = document.getElementById('forgot-password-form');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideAlert('forgot-alert');

        const email = document.getElementById('email').value.trim();
        const btnSubmit = document.getElementById('btn-forgot');
        btnSubmit.disabled = true;
        btnSubmit.textContent = 'Sending...';

        try {
            await ApiService.post('/auth/forgot-password/request-otp', { email });
            sessionStorage.setItem('pendingEmail', email);
            sessionStorage.setItem('otpPurpose', 'PASSWORD_RESET');
            ApiService.showToast('Reset OTP sent to your email.', 'success');
            window.location.href = 'verify-otp.html';
        } catch (err) {
            showAlert('forgot-alert', err.message);
            btnSubmit.disabled = false;
            btnSubmit.textContent = 'Send Reset OTP';
        }
    });
}

/* 6. Reset Password Page */
function initResetPasswordForm() {
    const form = document.getElementById('reset-password-form');
    if (!form) return;

    const email = sessionStorage.getItem('pendingEmail');
    const otp = sessionStorage.getItem('resetOtp');

    if (!email || !otp) {
        window.location.href = 'forgot-password.html';
        return;
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideAlert('reset-alert');

        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (newPassword !== confirmPassword) {
            showAlert('reset-alert', 'Passwords do not match');
            return;
        }

        const btnReset = document.getElementById('btn-reset');
        btnReset.disabled = true;
        btnReset.textContent = 'Updating...';

        try {
            await ApiService.post('/auth/reset-password', {
                email, otp, newPassword, confirmPassword
            });
            sessionStorage.removeItem('pendingEmail');
            sessionStorage.removeItem('resetOtp');
            sessionStorage.removeItem('otpPurpose');
            
            document.getElementById('reset-form-container').style.display = 'none';
            document.getElementById('reset-success-container').style.display = 'block';
            ApiService.showToast('Password reset successful! Please log in.', 'success');
        } catch (err) {
            showAlert('reset-alert', err.message);
            btnReset.disabled = false;
            btnReset.textContent = 'Update Password';
        }
    });
}
