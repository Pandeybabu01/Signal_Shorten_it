const authLink = document.getElementById('authLink');
function refreshAuthLink() {
    authLink.textContent = API.isLoggedIn() ? 'Log out (' + API.getUsername() + ')' : 'Log in';
}
authLink.addEventListener('click', (e) => {
    e.preventDefault();
    if (API.isLoggedIn()) { API.clearSession(); location.reload(); }
    else { location.href = '/index.html'; }
});
refreshAuthLink();

const ACCENT = '#4fd1c5';
const ACCENT2 = '#f5a623';
const PALETTE = ['#4fd1c5', '#f5a623', '#7c9cff', '#ef5a5a', '#8a97b3', '#5ad1a0'];

let timeChart, deviceChart, browserChart;

async function init() {
    if (!API.isLoggedIn()) {
        document.getElementById('loggedOutNotice').style.display = 'block';
        return;
    }
    document.getElementById('dashboardContent').style.display = 'block';
    await loadLinks();
}

async function loadLinks() {
    try {
        const paged = await API.get('/api/urls?page=0&size=50');
        const tbody = document.getElementById('linksBody');
        tbody.innerHTML = '';
        if (!paged.content.length) {
            document.getElementById('emptyState').style.display = 'block';
            document.getElementById('linksTable').style.display = 'none';
            return;
        }
        document.getElementById('emptyState').style.display = 'none';
        document.getElementById('linksTable').style.display = 'table';

        paged.content.forEach(link => {
            const tr = document.createElement('tr');
            const created = new Date(link.createdAt).toLocaleDateString();
            tr.innerHTML = `
                <td class="code-cell">${link.shortCode}</td>
                <td class="muted" title="${escapeHtml(link.originalUrl)}">${truncate(link.originalUrl, 42)}</td>
                <td>${link.clickCount}</td>
                <td><span class="pill ${link.active && !link.expired ? 'active' : 'inactive'}">${link.active && !link.expired ? 'active' : 'inactive'}</span></td>
                <td class="muted">${created}</td>
                <td>
                  <button class="ghost" data-code="${link.shortCode}" onclick="showAnalytics('${link.shortCode}')">Analytics</button>
                  <button class="danger-ghost" onclick="deleteLink('${link.shortCode}')">Delete</button>
                </td>`;
            tbody.appendChild(tr);
        });
    } catch (err) {
        alert('Failed to load links: ' + err.message);
    }
}

async function deleteLink(code) {
    if (!confirm('Delete short link "' + code + '"? This cannot be undone.')) return;
    try {
        await API.del('/api/urls/' + code);
        await loadLinks();
    } catch (err) {
        alert('Failed to delete: ' + err.message);
    }
}

async function showAnalytics(code) {
    document.getElementById('analyticsPanel').style.display = 'block';
    document.getElementById('selectedCode').textContent = code;
    document.getElementById('analyticsPanel').scrollIntoView({ behavior: 'smooth' });

    try {
        const data = await API.get('/api/analytics/' + code + '?days=30');
        document.getElementById('statTotal').textContent = data.totalClicks;
        document.getElementById('statUnique').textContent = data.uniqueVisitors;
        document.getElementById('statTop').textContent = data.topReferrers.length ? data.topReferrers[0].label : '—';

        renderTimeChart(data.clicksOverTime);
        renderPieChart('deviceChart', data.deviceBreakdown, (c) => deviceChart = c);
        renderPieChart('browserChart', data.browserBreakdown, (c) => browserChart = c);

        const refList = document.getElementById('referrerList');
        refList.innerHTML = '';
        if (!data.topReferrers.length) {
            refList.innerHTML = '<div class="muted">No referrer data yet.</div>';
        } else {
            data.topReferrers.slice(0, 6).forEach(r => {
                const row = document.createElement('div');
                row.style.cssText = 'display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--border);font-size:0.85rem;';
                row.innerHTML = `<span class="muted">${escapeHtml(truncate(r.label, 34))}</span><span class="code-cell">${r.count}</span>`;
                refList.appendChild(row);
            });
        }
    } catch (err) {
        alert('Failed to load analytics: ' + err.message);
    }
}

function renderTimeChart(points) {
    const ctx = document.getElementById('timeChart').getContext('2d');
    if (timeChart) timeChart.destroy();
    timeChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: points.map(p => p.date),
            datasets: [{
                label: 'Clicks',
                data: points.map(p => p.count),
                borderColor: ACCENT,
                backgroundColor: 'rgba(79,209,197,0.12)',
                fill: true,
                tension: 0.3,
                pointRadius: 2
            }]
        },
        options: {
            plugins: { legend: { display: false } },
            scales: {
                x: { ticks: { color: '#8a97b3' }, grid: { color: '#1f2c42' } },
                y: { beginAtZero: true, ticks: { color: '#8a97b3' }, grid: { color: '#1f2c42' } }
            }
        }
    });
}

function renderPieChart(canvasId, entries, assign) {
    const ctx = document.getElementById(canvasId).getContext('2d');
    const chart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: entries.map(e => e.label || 'Unknown'),
            datasets: [{ data: entries.map(e => e.count), backgroundColor: PALETTE, borderWidth: 0 }]
        },
        options: { plugins: { legend: { position: 'bottom', labels: { color: '#8a97b3', boxWidth: 10, font: { size: 11 } } } } }
    });
    assign(chart);
}

document.getElementById('closeAnalytics').addEventListener('click', () => {
    document.getElementById('analyticsPanel').style.display = 'none';
});
document.getElementById('refreshBtn').addEventListener('click', loadLinks);

function truncate(str, n) { return str.length > n ? str.slice(0, n - 1) + '…' : str; }
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

init();
