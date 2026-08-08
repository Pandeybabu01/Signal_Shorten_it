/* Thin fetch wrapper: attaches JWT, refreshes on 401 once, throws readable errors. */
const API = (() => {
    const TOKEN_KEY = 'us_access_token';
    const REFRESH_KEY = 'us_refresh_token';
    const USER_KEY = 'us_username';

    function getAccessToken() { return localStorage.getItem(TOKEN_KEY); }
    function getRefreshToken() { return localStorage.getItem(REFRESH_KEY); }
    function getUsername() { return localStorage.getItem(USER_KEY); }

    function setSession(auth) {
        localStorage.setItem(TOKEN_KEY, auth.accessToken);
        localStorage.setItem(REFRESH_KEY, auth.refreshToken);
        localStorage.setItem(USER_KEY, auth.username);
    }

    function clearSession() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(REFRESH_KEY);
        localStorage.removeItem(USER_KEY);
    }

    function isLoggedIn() { return !!getAccessToken(); }

    async function raw(path, options = {}, retry = true) {
        const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
        const token = getAccessToken();
        if (token) headers['Authorization'] = 'Bearer ' + token;

        const res = await fetch(path, Object.assign({}, options, { headers }));

        if (res.status === 401 && retry && getRefreshToken()) {
            const refreshed = await tryRefresh();
            if (refreshed) return raw(path, options, false);
            clearSession();
        }

        if (!res.ok) {
            let message = 'Request failed (' + res.status + ')';
            try {
                const body = await res.json();
                if (body.message) message = body.message;
                if (body.details && body.details.length) message += ': ' + body.details.join(', ');
            } catch (e) { /* non-JSON error body */ }
            const err = new Error(message);
            err.status = res.status;
            throw err;
        }

        if (res.status === 204) return null;
        const contentType = res.headers.get('content-type') || '';
        if (contentType.includes('application/json')) return res.json();
        return res.blob();
    }

    async function tryRefresh() {
        try {
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken: getRefreshToken() })
            });
            if (!res.ok) return false;
            const auth = await res.json();
            setSession(auth);
            return true;
        } catch (e) {
            return false;
        }
    }

    return {
        get: (path) => raw(path, { method: 'GET' }),
        post: (path, body) => raw(path, { method: 'POST', body: JSON.stringify(body) }),
        patch: (path, body) => raw(path, { method: 'PATCH', body: JSON.stringify(body) }),
        del: (path) => raw(path, { method: 'DELETE' }),
        setSession, clearSession, isLoggedIn, getUsername
    };
})();
