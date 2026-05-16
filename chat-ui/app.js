// ─── Config ──────────────────────────────────────────────────
const API = window.location.origin; // Spring API on same host

// ─── State ───────────────────────────────────────────────────
let token = localStorage.getItem('token');
let user = JSON.parse(localStorage.getItem('user') || 'null');
let currentConvId = null;

// ─── DOM Elements ────────────────────────────────────────────
const loginScreen = document.getElementById('login-screen');
const chatScreen = document.getElementById('chat-screen');
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

// ─── Auth ────────────────────────────────────────────────────
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
        showChat();
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
        showAuthError('');
        // Auto login after register
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

document.getElementById('logout-btn').addEventListener('click', () => {
    token = null; user = null; currentConvId = null;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    loginScreen.classList.remove('hidden');
    chatScreen.classList.add('hidden');
});

// ─── Chat ────────────────────────────────────────────────────
function showChat() {
    loginScreen.classList.add('hidden');
    chatScreen.classList.remove('hidden');
    userInfo.textContent = `${user.username} (${user.role})`;
    adminSection.classList.toggle('hidden', user.role !== 'ADMIN');
    loadConversations();
}

async function loadConversations() {
    try {
        const res = await apiFetch('/api/chat/conversations');
        const convs = await res.json();
        convList.innerHTML = '';
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
        loadConversations(); // refresh active state
    } catch (err) {
        console.error('Failed to load history', err);
    }
}

document.getElementById('new-chat-btn').addEventListener('click', () => {
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
    loadConversations();
});

chatForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const query = chatInput.value.trim();
    if (!query) return;

    // Clear welcome message
    const welcome = messagesEl.querySelector('.welcome-msg');
    if (welcome) welcome.remove();

    addMessage('user', query);
    chatInput.value = '';
    sendBtn.disabled = true;

    // Show typing indicator
    const typing = document.createElement('div');
    typing.className = 'typing-indicator';
    typing.innerHTML = '<div class="typing-dots"><span></span><span></span><span></span></div>';
    messagesEl.appendChild(typing);
    messagesEl.scrollTop = messagesEl.scrollHeight;

    try {
        const body = { query };
        if (currentConvId) body.conversationId = currentConvId;

        const res = await apiFetch('/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();

        typing.remove();

        if (!res.ok) throw new Error(data.message || 'Lỗi hệ thống');

        currentConvId = data.conversationId;
        chatTitle.textContent = query.length > 40 ? query.substring(0, 37) + '...' : query;

        let content = data.answer || '';
        if (data.sources && data.sources.length > 0) {
            content += '\n\n📎 Nguồn: ' + data.sources.map(s => s.file).join(', ');
        }
        addMessage('assistant', content);
        loadConversations();
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
    } catch (err) {
        uploadStatus.textContent = '❌ ' + err.message;
        uploadStatus.style.color = 'var(--error)';
    }
});

// ─── API Helper ──────────────────────────────────────────────
async function apiFetch(path, opts = {}) {
    opts.headers = opts.headers || {};
    if (token) opts.headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`${API}${path}`, opts);
    if (res.status === 401) {
        // Token expired
        document.getElementById('logout-btn').click();
        throw new Error('Phiên đăng nhập hết hạn');
    }
    return res;
}

// ─── Init ────────────────────────────────────────────────────
if (token && user) {
    showChat();
} else {
    loginScreen.classList.remove('hidden');
    chatScreen.classList.add('hidden');
}
