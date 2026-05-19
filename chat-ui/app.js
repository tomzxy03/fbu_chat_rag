// ─── Config ──────────────────────────────────────────────────
const API = window.location.origin;

// ─── State ───────────────────────────────────────────────────
let token = localStorage.getItem('token');
let user = JSON.parse(localStorage.getItem('user') || 'null');
let currentConvId = null;

// ─── DOM Elements ────────────────────────────────────────────
const loginModal = document.getElementById('login-modal');
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const authError = document.getElementById('auth-error');
const messagesEl = document.getElementById('messages');
const chatForm = document.getElementById('chat-form');
const chatInput = document.getElementById('chat-input');
const sendBtn = document.getElementById('send-btn');
const convList = document.getElementById('conversation-list');
const adminSection = document.getElementById('admin-section');
const uploadForm = document.getElementById('upload-form');
const uploadStatus = document.getElementById('upload-status');
const chatTitle = document.getElementById('chat-title');
const userInfo = document.getElementById('user-info');
const authBtn = document.getElementById('auth-btn');
const loginHint = document.getElementById('login-hint');

// ─── Login Modal ─────────────────────────────────────────────
function openLoginModal() { loginModal.classList.remove('hidden'); }
function closeLoginModal() { loginModal.classList.add('hidden'); authError.classList.add('hidden'); }

authBtn.addEventListener('click', () => {
    if (token) {
        // Logout
        token = null; user = null; currentConvId = null;
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        updateAuthUI();
        resetChat();
    } else {
        openLoginModal();
    }
});

document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        loginForm.classList.toggle('hidden', tab.dataset.tab !== 'login');
        registerForm.classList.toggle('hidden', tab.dataset.tab !== 'register');
        authError.classList.add('hidden');
    });
});

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;
    try {
        const res = await fetch(`${API}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Sai tên đăng nhập hoặc mật khẩu');
        token = data.token;
        user = { username: data.username, role: data.role };
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(user));
        closeLoginModal();
        updateAuthUI();
        loadConversations();
    } catch (err) {
        showAuthError(err.message);
    }
});

registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('reg-username').value;
    const password = document.getElementById('reg-password').value;
    try {
        const res = await fetch(`${API}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Đăng ký thất bại');
        // Auto login
        document.getElementById('login-username').value = username;
        document.getElementById('login-password').value = password;
        document.querySelector('.tab[data-tab="login"]').click();
        loginForm.dispatchEvent(new Event('submit'));
    } catch (err) {
        showAuthError(err.message);
    }
});

function showAuthError(msg) {
    authError.textContent = msg;
    authError.classList.toggle('hidden', !msg);
}

// ─── UI State ────────────────────────────────────────────────
function updateAuthUI() {
    if (token && user) {
        userInfo.textContent = `${user.username} (${user.role})`;
        authBtn.textContent = 'Đăng xuất';
        authBtn.className = 'btn-logout';
        loginHint.classList.add('hidden');
        adminSection.classList.toggle('hidden', user.role !== 'ADMIN');
        if (user.role === 'ADMIN') loadDocuments(); // Load danh sách tài liệu khi ADMIN login
    } else {
        userInfo.textContent = 'Khách';
        authBtn.textContent = 'Đăng nhập';
        authBtn.className = 'btn-login';
        loginHint.classList.remove('hidden');
        adminSection.classList.add('hidden');
        convList.innerHTML = '<p class="conv-empty">Đăng nhập để xem lịch sử</p>';
    }
}

// ─── Chat ────────────────────────────────────────────────────
async function loadConversations() {
    if (!token) return;
    try {
        const res = await apiFetch('/api/chat/conversations');
        const convs = await res.json();
        convList.innerHTML = '';
        if (convs.length === 0) {
            convList.innerHTML = '<p class="conv-empty">Chưa có cuộc trò chuyện</p>';
            return;
        }
        convs.forEach(c => {
            const div = document.createElement('div');
            div.className = 'conv-item' + (c.id === currentConvId ? ' active' : '');
            div.textContent = c.title || 'Cuộc trò chuyện';
            div.onclick = () => loadConversation(c.id, c.title);
            convList.appendChild(div);
        });
    } catch (err) {
        console.error('Failed to load conversations', err);
    }
}

async function loadConversation(convId, title) {
    currentConvId = convId;
    chatTitle.textContent = title || 'Cuộc trò chuyện';
    try {
        const res = await apiFetch(`/api/chat/conversations/${convId}/messages`);
        const msgs = await res.json();
        messagesEl.innerHTML = '';
        msgs.forEach(m => addMessage(m.role, m.content));
        messagesEl.scrollTop = messagesEl.scrollHeight;
        loadConversations();
    } catch (err) {
        console.error('Failed to load history', err);
    }
}

function resetChat() {
    currentConvId = null;
    chatTitle.textContent = 'Cuộc trò chuyện mới';
    messagesEl.innerHTML = `
        <div class="welcome-msg">
            <div class="welcome-icon">🤖</div>
            <h2>Xin chào! Tôi là trợ lý AI của FBU</h2>
            <p>Hãy hỏi tôi về quy chế, quy trình, hoặc thông tin của trường.</p>
            <div class="suggestions">
                <button class="suggestion" onclick="askSuggestion(this)">Điều kiện tốt nghiệp?</button>
                <button class="suggestion" onclick="askSuggestion(this)">Học bổng toàn phần?</button>
                <button class="suggestion" onclick="askSuggestion(this)">Điểm rèn luyện?</button>
            </div>
        </div>`;
}

document.getElementById('new-chat-btn').addEventListener('click', () => {
    resetChat();
    if (token) loadConversations();
});

chatForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const query = chatInput.value.trim();
    if (!query) return;

    const welcome = messagesEl.querySelector('.welcome-msg');
    if (welcome) welcome.remove();

    addMessage('user', query);
    chatInput.value = '';
    sendBtn.disabled = true;

    const typing = document.createElement('div');
    typing.className = 'typing-indicator';
    typing.innerHTML = '<div class="typing-dots"><span></span><span></span><span></span></div>';
    messagesEl.appendChild(typing);
    messagesEl.scrollTop = messagesEl.scrollHeight;

    try {
        const body = { query };
        if (currentConvId) body.conversationId = currentConvId;

        // Anonymous: gửi không có token, server vẫn trả lời
        const headers = { 'Content-Type': 'application/json' };
        if (token) headers['Authorization'] = `Bearer ${token}`;

        const res = await fetch(`${API}/api/chat`, {
            method: 'POST',
            headers,
            body: JSON.stringify(body)
        });
        const data = await res.json();
        typing.remove();

        if (!res.ok) throw new Error(data.message || 'Lỗi hệ thống');

        // Cập nhật conversationId nếu server trả về (user logged in)
        if (data.conversationId) {
            currentConvId = data.conversationId;
        }
        chatTitle.textContent = query.length > 40 ? query.substring(0, 37) + '...' : query;

        let content = data.answer || '';
        if (data.sources && data.sources.length > 0) {
            const uniqueFiles = [...new Set(data.sources.map(s => s.file))];
            content += '\n\n📎 Nguồn: ' + uniqueFiles.join(', ');
        }
        addMessage('assistant', content);
        if (token) loadConversations();
    } catch (err) {
        typing.remove();
        addMessage('assistant', '❌ ' + err.message);
    } finally {
        sendBtn.disabled = false;
        chatInput.focus();
    }
});

function addMessage(role, content) {
    const div = document.createElement('div');
    div.className = `message ${role}`;
    if (role === 'assistant' && typeof marked !== 'undefined') {
        div.innerHTML = marked.parse(content);
    } else {
        div.textContent = content;
    }
    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;
}

function askSuggestion(btn) {
    chatInput.value = btn.textContent;
    chatForm.dispatchEvent(new Event('submit'));
}

// ─── Upload (Admin) ──────────────────────────────────────────
uploadForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fileInput = document.getElementById('upload-file');
    if (!fileInput.files.length) return;

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('year', document.getElementById('upload-year').value);
    formData.append('docType', document.getElementById('upload-doctype').value);

    uploadStatus.textContent = 'Đang upload...';
    uploadStatus.style.color = 'var(--text-secondary)';

    try {
        const res = await fetch(`${API}/api/documents/ingest`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Upload thất bại');
        uploadStatus.textContent = '✅ ' + data.message;
        uploadStatus.style.color = 'var(--success)';
        fileInput.value = '';
        loadDocuments(); // Refresh danh sách sau khi upload thành công
    } catch (err) {
        uploadStatus.textContent = '❌ ' + err.message;
        uploadStatus.style.color = 'var(--error)';
    }
});

// ─── Document Management (Admin) ─────────────────────────────
async function loadDocuments() {
    if (!token || !user || user.role !== 'ADMIN') return;
    const container = document.getElementById('doc-list-container');
    if (!container) return;

    try {
        const res = await apiFetch('/api/documents');
        const docs = await res.json();

        if (!Array.isArray(docs) || docs.length === 0) {
            container.innerHTML = '<p style="font-size:12px;color:var(--text-secondary);">Chưa có tài liệu nào</p>';
            return;
        }

        const rows = docs.map(d => `
            <tr>
                <td title="${d.filename}" style="max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:11px;">${d.filename}</td>
                <td style="font-size:11px;text-align:center;">${d.year ?? '—'}</td>
                <td style="font-size:11px;text-align:center;">${d.docType ?? '—'}</td>
                <td style="font-size:11px;text-align:center;">${d.chunkCount}</td>
                <td style="text-align:center;">
                    <button onclick="deleteDocument('${d.filename.replace(/'/g, "\\'")}')"
                        style="font-size:10px;padding:2px 6px;background:var(--error,#e53e3e);color:#fff;border:none;border-radius:4px;cursor:pointer;">
                        Xóa
                    </button>
                </td>
            </tr>`).join('');

        container.innerHTML = `
            <table style="width:100%;border-collapse:collapse;font-size:11px;">
                <thead>
                    <tr style="border-bottom:1px solid var(--border,#e2e8f0);">
                        <th style="text-align:left;padding:4px 2px;font-size:11px;">Tên file</th>
                        <th style="padding:4px 2px;font-size:11px;">Năm</th>
                        <th style="padding:4px 2px;font-size:11px;">Loại</th>
                        <th style="padding:4px 2px;font-size:11px;">Chunks</th>
                        <th style="padding:4px 2px;font-size:11px;"></th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>`;
    } catch (err) {
        if (container) {
            container.innerHTML = '<p style="font-size:12px;color:var(--error,#e53e3e);">❌ Không thể tải danh sách tài liệu</p>';
        }
    }
}

async function deleteDocument(filename) {
    if (!confirm(`Xóa tài liệu "${filename}"?`)) return;
    try {
        const res = await apiFetch(`/api/documents/${encodeURIComponent(filename)}`, { method: 'DELETE' });
        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            throw new Error(data.message || `HTTP ${res.status}`);
        }
        uploadStatus.textContent = `✅ Đã xóa: ${filename}`;
        uploadStatus.style.color = 'var(--success)';
        loadDocuments(); // Refresh list
    } catch (err) {
        uploadStatus.textContent = `❌ Xóa thất bại: ${err.message}`;
        uploadStatus.style.color = 'var(--error)';
    }
}

// ─── API Helper ──────────────────────────────────────────────
async function apiFetch(path, opts = {}) {
    opts.headers = opts.headers || {};
    if (token) opts.headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`${API}${path}`, opts);
    if (res.status === 401 || res.status === 403) {
        token = null; user = null;
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        updateAuthUI();
        throw new Error('Phiên đăng nhập hết hạn');
    }
    return res;
}

// ─── Init ────────────────────────────────────────────────────
updateAuthUI();
if (token) loadConversations();
