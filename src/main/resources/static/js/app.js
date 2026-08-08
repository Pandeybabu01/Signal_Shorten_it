// --- Auth link / panel wiring -----------------------------------------
const authLink = document.getElementById('authLink');
const authPanel = document.getElementById('authPanel');
const authForm = document.getElementById('authForm');
const authTitle = document.getElementById('authTitle');
const authSwitch = document.getElementById('authSwitch');
const emailField = document.getElementById('emailField');
const authError = document.getElementById('authError');
let authMode = 'login';

function refreshAuthLink() {
    if (API.isLoggedIn()) {
        authLink.textContent = 'Log out (' + API.getUsername() + ')';
    } else {
        authLink.textContent = 'Log in';
    }
}
refreshAuthLink();

authLink.addEventListener('click', (e) => {
    e.preventDefault();
    if (API.isLoggedIn()) {
        API.clearSession();
        refreshAuthLink();
        return;
    }
    authPanel.style.display = authPanel.style.display === 'none' ? 'block' : 'none';
});

authSwitch.addEventListener('click', () => {
    authMode = authMode === 'login' ? 'register' : 'login';
    authTitle.textContent = authMode === 'login' ? 'Log in' : 'Create an account';
    authSwitch.textContent = authMode === 'login' ? 'Need an account? Register' : 'Already have an account? Log in';
    emailField.style.display = authMode === 'register' ? 'block' : 'none';
});

authForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    authError.classList.remove('show');
    const username = document.getElementById('authUsername').value.trim();
    const password = document.getElementById('authPassword').value;
    try {
        let auth;
        if (authMode === 'login') {
            auth = await API.post('/api/auth/login', { usernameOrEmail: username, password });
        } else {
            const email = document.getElementById('authEmail').value.trim();
            auth = await API.post('/api/auth/register', { username, email, password });
        }
        API.setSession(auth);
        refreshAuthLink();
        authPanel.style.display = 'none';
    } catch (err) {
        authError.textContent = err.message;
        authError.classList.add('show');
    }
});

// --- Advanced options toggle --------------------------------------------
const advToggle = document.getElementById('advToggle');
const advPanel = document.getElementById('advPanel');
advToggle.addEventListener('click', () => {
    advPanel.classList.toggle('open');
    advToggle.textContent = advPanel.classList.contains('open')
        ? '– advanced options'
        : '+ advanced options (custom alias, expiry, password, click limit)';
});

// --- Shorten form ---------------------------------------------------------
const form = document.getElementById('shortenForm');
const result = document.getElementById('result');
const resultLink = document.getElementById('resultLink');
const errorBanner = document.getElementById('errorBanner');
const submitBtn = document.getElementById('submitBtn');
let lastShortCode = null;

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorBanner.classList.remove('show');
    result.classList.remove('show');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Shortening…';

    const payload = { originalUrl: document.getElementById('originalUrl').value.trim() };
    const alias = document.getElementById('customAlias').value.trim();
    const title = document.getElementById('title').value.trim();
    const expiresAt = document.getElementById('expiresAt').value;
    const maxClicks = document.getElementById('maxClicks').value;
    const password = document.getElementById('password').value;

    if (alias) payload.customAlias = alias;
    if (title) payload.title = title;
    if (expiresAt) payload.expiresAt = expiresAt;
    if (maxClicks) payload.maxClicks = parseInt(maxClicks, 10);
    if (password) payload.password = password;

    try {
        const created = await API.post('/api/urls', payload);
        lastShortCode = created.shortCode;
        resultLink.textContent = created.shortUrl;
        result.classList.add('show');
        form.reset();
    } catch (err) {
        errorBanner.textContent = err.message;
        errorBanner.classList.add('show');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Shorten URL';
    }
});

document.getElementById('copyBtn').addEventListener('click', () => {
    navigator.clipboard.writeText(resultLink.textContent);
    const btn = document.getElementById('copyBtn');
    const original = btn.textContent;
    btn.textContent = 'Copied!';
    setTimeout(() => btn.textContent = original, 1200);
});

document.getElementById('qrBtn').addEventListener('click', () => {
    if (lastShortCode) window.open('/api/urls/' + lastShortCode + '/qrcode', '_blank');
});
