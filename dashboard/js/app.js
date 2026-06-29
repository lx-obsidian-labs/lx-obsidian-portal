var API = '/api';
var TOKEN = localStorage.getItem('lx_token');
var currentUser = null;

function api(path, opts) {
  opts = opts || {};
  var headers = { 'Content-Type': 'application/json' };
  if (TOKEN) headers['Authorization'] = 'Bearer ' + TOKEN;
  return fetch(API + path, {
    method: opts.method || 'GET',
    headers: headers,
    body: opts.body ? JSON.stringify(opts.body) : undefined
  }).then(function (r) { return r.json(); });
}

function showAuthError(msg) {
  var el = document.getElementById('authError');
  if (el) { el.textContent = msg; }
}

function renderPage(name) {
  var pages = {
    dashboard: renderDashboard,
    store: renderStore,
    'ai-hub': renderAiHub,
    projects: renderProjects,
    billing: renderBilling,
    licenses: renderLicenses,
    academy: renderAcademy,
    hosting: renderHosting,
    insights: renderInsights,
    teams: renderTeams,
    support: renderSupport,
    bookings: renderBookings,
    affiliates: renderAffiliates,
    vendors: renderVendors,
    notifications: renderNotifications
  };
  var fn = pages[name] || renderDashboard;
  document.getElementById('pageTitle').textContent = name.replace('-', ' ').replace(/\b\w/g, function (l) { return l.toUpperCase(); });
  document.getElementById('pageContent').innerHTML = '<div class="loading">Loading</div>';
  fn();
}

// AUTH
document.addEventListener('DOMContentLoaded', function () {
  if (TOKEN) { loadUser(); return; }
  document.getElementById('authScreen').style.display = 'flex';
  document.getElementById('mainLayout').style.display = 'none';

  var tabs = document.querySelectorAll('.auth-tab');
  tabs.forEach(function (t) {
    t.addEventListener('click', function () {
      tabs.forEach(function (x) { x.classList.remove('active'); });
      t.classList.add('active');
      document.querySelectorAll('.auth-form').forEach(function (f) { f.classList.remove('active'); });
      document.getElementById(t.dataset.tab + 'Form').classList.add('active');
      var err = document.getElementById('authError');
      if (err) err.textContent = '';
    });
  });

  document.getElementById('loginForm').addEventListener('submit', function (e) {
    e.preventDefault();
    var email = document.getElementById('loginEmail').value;
    var pass = document.getElementById('loginPassword').value;
    if (!email || !pass) { showAuthError('Please fill in all fields'); return; }
    api('/auth/login', { method: 'POST', body: { email: email, password: pass } }).then(function (d) {
      if (d.error) { showAuthError(d.error); return; }
      TOKEN = d.token;
      localStorage.setItem('lx_token', TOKEN);
      currentUser = d.user;
      showApp();
    });
  });

  document.getElementById('registerForm').addEventListener('submit', function (e) {
    e.preventDefault();
    var name = document.getElementById('regName').value;
    var email = document.getElementById('regEmail').value;
    var pass = document.getElementById('regPassword').value;
    var company = document.getElementById('regCompany').value;
    if (!name || !email || !pass) { showAuthError('Name, email, and password are required'); return; }
    if (pass.length < 6) { showAuthError('Password must be at least 6 characters'); return; }
    api('/auth/register', { method: 'POST', body: { name: name, email: email, password: pass, company: company } }).then(function (d) {
      if (d.error) { showAuthError(d.error); return; }
      TOKEN = d.token;
      localStorage.setItem('lx_token', TOKEN);
      currentUser = d.user;
      showApp();
    });
  });

  document.getElementById('logoutBtn').addEventListener('click', function () {
    TOKEN = null;
    localStorage.removeItem('lx_token');
    currentUser = null;
    document.getElementById('authScreen').style.display = 'flex';
    document.getElementById('mainLayout').style.display = 'none';
  });

  // Sidebar nav
  document.querySelectorAll('.sidebar__item[data-page]').forEach(function (item) {
    item.addEventListener('click', function () {
      document.querySelectorAll('.sidebar__item').forEach(function (x) { x.classList.remove('active'); });
      item.classList.add('active');
      renderPage(item.dataset.page);
    });
  });

  // Menu toggle for mobile
  document.getElementById('menuToggle').addEventListener('click', function () {
    document.getElementById('sidebar').classList.toggle('open');
  });

  // Notif button
  document.getElementById('notifBtn').addEventListener('click', function () {
    renderPage('notifications');
    document.querySelectorAll('.sidebar__item').forEach(function (x) { x.classList.remove('active'); });
    document.querySelector('.sidebar__item[data-page="notifications"]').classList.add('active');
  });
});

function loadUser() {
  api('/auth/me').then(function (d) {
    if (d.error) {
      TOKEN = null;
      localStorage.removeItem('lx_token');
      document.getElementById('authScreen').style.display = 'flex';
      document.getElementById('mainLayout').style.display = 'none';
      return;
    }
    currentUser = d.user;
    showApp();
  });
}

function showApp() {
  document.getElementById('authScreen').style.display = 'none';
  document.getElementById('mainLayout').style.display = 'flex';
  var avatar = document.getElementById('userAvatar');
  avatar.textContent = currentUser.name ? currentUser.name.charAt(0).toUpperCase() : 'U';
  document.getElementById('userName').textContent = currentUser.name || 'User';
  document.getElementById('userEmail').textContent = currentUser.email || '';
  renderPage('dashboard');
  loadCredits();
  loadNotifCount();
}

function loadCredits() {
  if (!TOKEN) return;
  api('/ai/credits').then(function (d) {
    if (d.credits !== undefined) {
      document.getElementById('creditsDisplay').textContent = '\u26A1 ' + d.credits + ' credits';
    }
  });
}

function loadNotifCount() {
  if (!TOKEN) return;
  api('/notifications').then(function (d) {
    var badge = document.getElementById('notifBadge');
    if (d.unread) { badge.textContent = d.unread; badge.style.display = 'flex'; }
    else { badge.style.display = 'none'; }
  });
}

// ===== DASHBOARD =====
function renderDashboard() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading dashboard</div>';
  api('/dashboard/dashboard').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="grid grid-4" style="margin-bottom:1.5rem">' +
      statCard('&#128194;', d.projects.count, 'Projects') +
      statCard('&#128722;', d.orders.count, 'Orders') +
      statCard('&#128274;', d.licenses.count, 'Active Licenses') +
      statCard('&#9733;', d.subscriptions.count, 'Subscriptions') +
      statCard('&#10067;', d.tickets.count, 'Open Tickets') +
      statCard('&#128196;', d.invoices.length, 'Invoices') +
      statCard('&#36;', '$' + (d.totalSpent || 0).toFixed(0), 'Total Spent') +
      statCard('&#128230;', d.uploadedFiles.count, 'Files Uploaded') +
    '</div>';
    if (d.notifications && d.notifications.length) {
      html += '<div class="section-title" style="margin-bottom:0.75rem">Recent Alerts</div>';
      d.notifications.forEach(function (n) {
        html += '<div class="card" style="margin-bottom:0.5rem;font-size:0.85rem">' +
          '<span class="badge badge--open">' + n.type + '</span> ' + n.title +
          '<span style="float:right;color:var(--muted);font-size:0.75rem">' + new Date(n.created_at).toLocaleDateString() + '</span></div>';
      });
    }
    cont.innerHTML = html;
  });
}

function statCard(icon, value, label) {
  return '<div class="card card--stat"><div class="stat-icon">' + icon + '</div><div><div class="stat-value">' + value + '</div><div class="stat-label">' + label + '</div></div></div>';
}

// ===== STORE =====
function renderStore() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading marketplace</div>';
  api('/store/products').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-header"><div class="section-title">Digital Marketplace</div>' +
      '<div class="form-row"><input type="text" id="storeSearch" placeholder="Search products..." style="width:200px">' +
      '<select id="storeCategory"><option value="">All Categories</option>';
    (d.categories || []).forEach(function (c) { html += '<option value="' + c + '">' + c + '</option>'; });
    html += '</select></div></div><div class="product-grid" id="productGrid">';
    (d.products || []).forEach(function (p) {
      html += '<div class="product-card" data-category="' + (p.category || '') + '" data-name="' + p.name.toLowerCase() + '">' +
        '<div class="product-card__title">' + p.name + '</div>' +
        '<div class="product-card__desc">' + (p.description || '') + '</div>' +
        '<div class="product-card__footer"><span class="product-card__price">$' + (p.price || 0).toFixed(2) + '</span>' +
        '<span class="product-card__category">' + (p.category || '') + '</span></div>' +
        '<button class="btn btn--primary btn--sm btn--full" style="margin-top:0.75rem" onclick="buyProduct(\'' + p.id + '\')">Buy Now</button>' +
      '</div>';
    });
    html += '</div>';
    cont.innerHTML = html;
    setTimeout(function () {
      var search = document.getElementById('storeSearch');
      var cat = document.getElementById('storeCategory');
      if (search) search.addEventListener('input', filterProducts);
      if (cat) cat.addEventListener('change', filterProducts);
    }, 50);
  });
}

function filterProducts() {
  var search = (document.getElementById('storeSearch').value || '').toLowerCase();
  var cat = document.getElementById('storeCategory').value;
  document.querySelectorAll('.product-card').forEach(function (c) {
    var match = true;
    if (search && !c.dataset.name.includes(search)) match = false;
    if (cat && c.dataset.category !== cat) match = false;
    c.style.display = match ? '' : 'none';
  });
}

function buyProduct(id) {
  if (!TOKEN) { alert('Please sign in first'); return; }
  api('/store/checkout', { method: 'POST', body: { items: [{ product_id: id }] } }).then(function (d) {
    if (d.error) { alert(d.error); return; }
    alert('Purchase complete! License key: ' + (d.items && d.items[0] ? d.items[0].license_key : 'check your orders'));
    renderPage('store');
  });
}

// ===== AI HUB =====
function renderAiHub() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading AI Hub</div>';
  api('/ai/agents').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-header"><div class="section-title">AI Hub</div>' +
      '<button class="btn btn--secondary btn--sm" onclick="purchaseCredits()">+ Buy Credits</button></div>' +
      '<div class="section-subtitle">Choose a specialized AI assistant to help with your work</div>' +
      '<div class="agent-grid">';
    (d.agents || []).forEach(function (a) {
      html += '<div class="agent-card" onclick="startAiChat(\'' + a.id + '\',\'' + a.name.replace(/'/g, "\\'") + '\')">' +
        '<div class="agent-card__icon">' + getAgentIcon(a.icon) + '</div>' +
        '<div class="agent-card__name">' + a.name + '</div>' +
        '<div class="agent-card__desc">' + (a.description || '') + '</div>' +
      '</div>';
    });
    html += '</div><div style="margin-top:1.5rem"><div class="section-title">Recent Chats</div><div id="recentChats"></div></div>';
    cont.innerHTML = html;
    api('/ai/chats').then(function (c) {
      var el = document.getElementById('recentChats');
      if (!el) return;
      if (!c.chats || !c.chats.length) { el.innerHTML = '<div class="empty-state">No previous chats</div>'; return; }
      var html2 = '<div class="table-wrap"><table><tr><th>Agent</th><th>Title</th><th>Date</th></tr>';
      c.chats.forEach(function (ch) {
        html2 += '<tr onclick="openAiChat(\'' + ch.id + '\')" style="cursor:pointer"><td>' + getAgentIcon(ch.agent_icon) + ' ' + ch.agent_name + '</td><td>' + (ch.title || 'Untitled') + '</td><td>' + new Date(ch.created_at).toLocaleDateString() + '</td></tr>';
      });
      html2 += '</table></div>';
      el.innerHTML = html2;
    });
  });
}

function getAgentIcon(icon) {
  var icons = { briefcase: '\uD83D\uDCBC', code: '\uD83D\uDCBB', palette: '\uD83C\uDFA8', 'trending-up': '\uD83D\uDCC8', edit: '\u270F\uFE0F', shield: '\uD83D\uDEE1\uFE0F', 'file-text': '\uD83D\uDCC4', search: '\uD83D\uDD0D', terminal: '\u2328\uFE0F', 'book-open': '\uD83D\uDCD6' };
  return icons[icon] || '\uD83E\uDD16';
}

var currentChatId = null;

function startAiChat(agentId, agentName) {
  if (!TOKEN) { alert('Please sign in'); return; }
  api('/ai/chats', { method: 'POST', body: { agent_id: agentId, title: 'Chat with ' + agentName } }).then(function (d) {
    if (d.error) { alert(d.error); return; }
    openAiChat(d.chat.id);
  });
}

function openAiChat(chatId) {
  currentChatId = chatId;
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading chat</div>';
  api('/ai/chats/' + chatId + '/messages').then(function (d) {
    var html = '<div class="section-header"><div class="section-title">Chat</div>' +
      '<button class="btn btn--ghost btn--sm" onclick="renderPage(\'ai-hub\')">Back to AI Hub</button></div>' +
      '<div class="chat-area"><div class="chat-area__messages" id="chatMessages">';
    (d.messages || []).forEach(function (m) {
      html += '<div class="message message--' + m.role + '">' + m.content + '</div>';
    });
    html += '</div><div class="chat-area__input"><input type="text" id="chatInputMsg" placeholder="Type your message..." onkeydown="if(event.key===\'Enter\')sendChatMsg()">' +
      '<button onclick="sendChatMsg()">Send</button></div></div>';
    cont.innerHTML = html;
    var msgs = document.getElementById('chatMessages');
    if (msgs) msgs.scrollTop = msgs.scrollHeight;
  });
}

function sendChatMsg() {
  var input = document.getElementById('chatInputMsg');
  var msg = input ? input.value : '';
  if (!msg || !currentChatId || !TOKEN) return;
  input.value = '';
  var msgs = document.getElementById('chatMessages');
  if (msgs) msgs.innerHTML += '<div class="message message--user">' + msg + '</div>';
  api('/ai/chats/' + currentChatId + '/messages', { method: 'POST', body: { content: msg } }).then(function (d) {
    if (msgs && d.message) {
      msgs.innerHTML += '<div class="message message--bot">' + d.message.content + '</div>';
      msgs.scrollTop = msgs.scrollHeight;
      loadCredits();
    }
  });
}

function purchaseCredits() {
  var amt = prompt('How many credits to purchase? (min 10)', '50');
  if (!amt) return;
  amt = parseInt(amt);
  if (amt < 10) { alert('Minimum 10 credits'); return; }
  api('/ai/credits/purchase', { method: 'POST', body: { amount: amt } }).then(function (d) {
    if (d.credits !== undefined) {
      document.getElementById('creditsDisplay').textContent = '\u26A1 ' + d.credits + ' credits';
      alert('Purchased ' + amt + ' credits!');
    }
  });
}

// ===== PROJECTS =====
function renderProjects() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading projects</div>';
  api('/dashboard/projects').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-header"><div class="section-title">My Projects</div>' +
      '<button class="btn btn--primary btn--sm" onclick="showNewProjectModal()">+ New Project</button></div>';
    if (!d.projects || !d.projects.length) {
      html += '<div class="empty-state"><div class="empty-state__icon">\uD83D\uDCCB</div><div class="empty-state__text">No projects yet. Create your first project!</div></div>';
    } else {
      html += '<div class="table-wrap"><table><tr><th>Title</th><th>Status</th><th>Budget</th><th>Deadline</th><th>Created</th></tr>';
      d.projects.forEach(function (p) {
        html += '<tr onclick="openProject(\'' + p.id + '\')" style="cursor:pointer">' +
          '<td>' + p.title + '</td><td><span class="badge badge--' + (p.status === 'active' ? 'active' : 'pending') + '">' + p.status + '</span></td>' +
          '<td>' + (p.budget ? '$' + p.budget : '-') + '</td><td>' + (p.deadline ? new Date(p.deadline).toLocaleDateString() : '-') + '</td>' +
          '<td>' + new Date(p.created_at).toLocaleDateString() + '</td></tr>';
      });
      html += '</table></div>';
    }
    cont.innerHTML = html;
  });
}

function showNewProjectModal() {
  var html = '<div class="modal-overlay active" id="projectModal"><div class="modal"><div class="modal__header"><div class="modal__title">New Project</div><button class="modal__close" onclick="closeModal(\'projectModal\')">&times;</button></div>' +
    '<div class="modal__body"><input type="text" id="projTitle" placeholder="Project Title" required><textarea id="projDesc" placeholder="Description"></textarea>' +
    '<div class="form-row"><input type="number" id="projBudget" placeholder="Budget (optional)" step="0.01"><input type="date" id="projDeadline"></div></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'projectModal\')">Cancel</button><button class="btn btn--primary" onclick="createProject()">Create</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function createProject() {
  var title = document.getElementById('projTitle').value;
  if (!title) { alert('Title is required'); return; }
  api('/dashboard/projects', { method: 'POST', body: { title: title, description: document.getElementById('projDesc').value, budget: document.getElementById('projBudget').value || null, deadline: document.getElementById('projDeadline').value || null } }).then(function (d) {
    if (d.error) { alert(d.error); return; }
    closeModal('projectModal');
    renderPage('projects');
  });
}

function openProject(id) {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading project</div>';
  api('/dashboard/projects/' + id).then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var p = d.project;
    var html = '<div class="section-header"><div class="section-title">' + p.title + '</div>' +
      '<button class="btn btn--ghost btn--sm" onclick="renderPage(\'projects\')">Back</button></div>' +
      '<div class="grid grid-4" style="margin-bottom:1rem">' +
      '<div class="card"><div>Status</div><div style="font-size:1.25rem;font-weight:600">' + p.status + '</div></div>' +
      '<div class="card"><div>Budget</div><div style="font-size:1.25rem;font-weight:600">' + (p.budget ? '$' + p.budget : 'N/A') + '</div></div>' +
      '<div class="card"><div>Tasks</div><div style="font-size:1.25rem;font-weight:600">' + (d.tasks || []).length + '</div></div>' +
      '<div class="card"><div>Files</div><div style="font-size:1.25rem;font-weight:600">' + (d.files || []).length + '</div></div>' +
      '</div>';
    // Tabs
    html += '<div style="margin-bottom:1rem;display:flex;gap:0.5rem">' +
      '<button class="btn btn--sm ' + (true ? 'btn--primary' : 'btn--secondary') + '" onclick="showProjectTab(\'' + id + '\',\'tasks\')">Tasks</button>' +
      '<button class="btn btn--sm btn--secondary" onclick="showProjectTab(\'' + id + '\',\'milestones\')">Milestones</button>' +
      '<button class="btn btn--sm btn--secondary" onclick="showProjectTab(\'' + id + '\',\'files\')">Files</button>' +
      '<button class="btn btn--sm btn--secondary" onclick="showProjectTab(\'' + id + '\',\'messages\')">Messages</button>' +
      '<button class="btn btn--sm btn--secondary" onclick="showProjectTab(\'' + id + '\',\'deploy\')">Deploy</button>' +
      '</div><div id="projectTabContent">';
    // Default show tasks
    html += '<div class="section-header"><div class="section-title">Tasks</div><button class="btn btn--primary btn--sm" onclick="showAddTask(\'' + id + '\')">+ Add Task</button></div>';
    if (!d.tasks || !d.tasks.length) {
      html += '<div class="empty-state">No tasks</div>';
    } else {
      html += '<div class="table-wrap"><table><tr><th>Title</th><th>Priority</th><th>Status</th><th>Due</th></tr>';
      d.tasks.forEach(function (t) {
        html += '<tr><td>' + t.title + '</td><td>' + (t.priority || 'medium') + '</td>' +
          '<td><span class="badge badge--' + (t.status === 'done' ? 'active' : 'pending') + '">' + t.status + '</span></td>' +
          '<td>' + (t.due_date ? new Date(t.due_date).toLocaleDateString() : '-') + '</td></tr>';
      });
      html += '</table></div>';
    }
    html += '</div>';
    cont.innerHTML = html;
  });
}

function showProjectTab(id, tab) {
  api('/dashboard/projects/' + id).then(function (d) {
    var el = document.getElementById('projectTabContent');
    if (!el) return;
    if (tab === 'tasks') {
      el.innerHTML = '<div class="section-header"><div class="section-title">Tasks</div><button class="btn btn--primary btn--sm" onclick="showAddTask(\'' + id + '\')">+ Add Task</button></div>';
      if (!d.tasks || !d.tasks.length) { el.innerHTML += '<div class="empty-state">No tasks</div>'; return; }
      var html = '<div class="table-wrap"><table><tr><th>Title</th><th>Priority</th><th>Status</th><th>Due</th></tr>';
      d.tasks.forEach(function (t) {
        html += '<tr><td>' + t.title + '</td><td>' + (t.priority || 'medium') + '</td><td><span class="badge badge--' + (t.status === 'done' ? 'active' : 'pending') + '">' + t.status + '</span></td><td>' + (t.due_date ? new Date(t.due_date).toLocaleDateString() : '-') + '</td></tr>';
      });
      el.innerHTML = html + '</table></div>';
    } else if (tab === 'milestones') {
      el.innerHTML = '<div class="section-header"><div class="section-title">Milestones</div><button class="btn btn--primary btn--sm" onclick="showAddMilestone(\'' + id + '\')">+ Add Milestone</button></div>';
      if (!d.milestones || !d.milestones.length) { el.innerHTML += '<div class="empty-state">No milestones</div>'; return; }
      var html = '<div class="table-wrap"><table><tr><th>Title</th><th>Due</th><th>Amount</th><th>Status</th></tr>';
      d.milestones.forEach(function (m) {
        html += '<tr><td>' + m.title + '</td><td>' + (m.due_date ? new Date(m.due_date).toLocaleDateString() : '-') + '</td><td>' + (m.amount ? '$' + m.amount : '-') + '</td><td><span class="badge badge--' + (m.status === 'completed' ? 'active' : 'pending') + '">' + m.status + '</span></td></tr>';
      });
      el.innerHTML = html + '</table></div>';
    } else if (tab === 'files') {
      el.innerHTML = '<div class="section-header"><div class="section-title">Files</div><button class="btn btn--primary btn--sm" onclick="showAddFile(\'' + id + '\')">+ Upload File</button></div>';
      if (!d.files || !d.files.length) { el.innerHTML += '<div class="empty-state">No files uploaded</div>'; return; }
      var html = '<div class="table-wrap"><table><tr><th>Name</th><th>Type</th><th>Size</th><th>Date</th></tr>';
      d.files.forEach(function (f) {
        html += '<tr><td>' + f.name + '</td><td>' + (f.type || '-') + '</td><td>' + (f.size ? (f.size / 1024).toFixed(1) + ' KB' : '-') + '</td><td>' + new Date(f.created_at).toLocaleDateString() + '</td></tr>';
      });
      el.innerHTML = html + '</table></div>';
    } else if (tab === 'messages') {
      el.innerHTML = '<div class="chat-area" style="height:400px"><div class="chat-area__messages" id="projMessages">';
      (d.messages || []).forEach(function (m) {
        el.innerHTML += '<div class="message message--' + (m.user_id === currentUser.id ? 'user' : 'bot') + '"><strong>' + (m.user_name || 'User') + ':</strong> ' + m.content + '</div>';
      });
      el.innerHTML += '</div><div class="chat-area__input"><input type="text" id="projMsgInput" placeholder="Type a message..." onkeydown="if(event.key===\'Enter\')sendProjMsg(\'' + id + '\')"><button onclick="sendProjMsg(\'' + id + '\')">Send</button></div></div>';
      var pm = document.getElementById('projMessages');
      if (pm) pm.scrollTop = pm.scrollHeight;
    } else if (tab === 'deploy') {
      el.innerHTML = '<div class="section-header"><div class="section-title">Deployments</div><button class="btn btn--primary btn--sm" onclick="deployProject(\'' + id + '\')">Deploy Now</button></div>';
      var html2 = '<div class="table-wrap"><table><tr><th>Version</th><th>Status</th><th>URL</th><th>Date</th></tr>';
      if (d.deployments && d.deployments.length) {
        d.deployments.forEach(function (dep) {
          html2 += '<tr><td>' + (dep.version || '1.0.0') + '</td><td><span class="badge badge--' + (dep.status === 'live' ? 'active' : 'pending') + '">' + dep.status + '</span></td><td>' + (dep.url || '-') + '</td><td>' + new Date(dep.created_at).toLocaleDateString() + '</td></tr>';
        });
      } else { html2 += '<tr><td colspan="4" style="text-align:center;color:var(--muted)">No deployments yet</td></tr>'; }
      html2 += '</table></div>';
      el.innerHTML += html2;
    }
  });
}

function showAddTask(projectId) {
  var html = '<div class="modal-overlay active" id="taskModal"><div class="modal"><div class="modal__header"><div class="modal__title">Add Task</div><button class="modal__close" onclick="closeModal(\'taskModal\')">&times;</button></div>' +
    '<div class="modal__body"><input type="text" id="taskTitle" placeholder="Task title" required><textarea id="taskDesc" placeholder="Description"></textarea>' +
    '<div class="form-row"><select id="taskPriority"><option value="low">Low</option><option value="medium" selected>Medium</option><option value="high">High</option></select>' +
    '<input type="date" id="taskDue"></div></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'taskModal\')">Cancel</button><button class="btn btn--primary" onclick="addTask(\'' + projectId + '\')">Add</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function addTask(projectId) {
  var title = document.getElementById('taskTitle').value;
  if (!title) { alert('Title required'); return; }
  api('/dashboard/projects/' + projectId + '/tasks', { method: 'POST', body: { title: title, description: document.getElementById('taskDesc').value, priority: document.getElementById('taskPriority').value, due_date: document.getElementById('taskDue').value } }).then(function () {
    closeModal('taskModal');
    showProjectTab(projectId, 'tasks');
  });
}

function showAddMilestone(projectId) {
  var html = '<div class="modal-overlay active" id="milestoneModal"><div class="modal"><div class="modal__header"><div class="modal__title">Add Milestone</div><button class="modal__close" onclick="closeModal(\'milestoneModal\')">&times;</button></div>' +
    '<div class="modal__body"><input type="text" id="msTitle" placeholder="Milestone title" required><textarea id="msDesc" placeholder="Description"></textarea>' +
    '<div class="form-row"><input type="number" id="msAmount" placeholder="Amount (optional)" step="0.01"><input type="date" id="msDue"></div></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'milestoneModal\')">Cancel</button><button class="btn btn--primary" onclick="addMilestone(\'' + projectId + '\')">Add</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function addMilestone(projectId) {
  var title = document.getElementById('msTitle').value;
  if (!title) { alert('Title required'); return; }
  api('/dashboard/projects/' + projectId + '/milestones', { method: 'POST', body: { title: title, description: document.getElementById('msDesc').value, amount: document.getElementById('msAmount').value || null, due_date: document.getElementById('msDue').value || null } }).then(function () {
    closeModal('milestoneModal');
    showProjectTab(projectId, 'milestones');
  });
}

function showAddFile(projectId) {
  var html = '<div class="modal-overlay active" id="fileModal"><div class="modal"><div class="modal__header"><div class="modal__title">Upload File</div><button class="modal__close" onclick="closeModal(\'fileModal\')">&times;</button></div>' +
    '<div class="modal__body"><input type="text" id="fileName" placeholder="File name" required><input type="text" id="fileUrl" placeholder="File URL or path"><div class="form-row"><input type="number" id="fileSize" placeholder="Size (bytes)"><input type="text" id="fileType" placeholder="Type (e.g. pdf, image)"></div></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'fileModal\')">Cancel</button><button class="btn btn--primary" onclick="addFile(\'' + projectId + '\')">Upload</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function addFile(projectId) {
  var name = document.getElementById('fileName').value;
  if (!name) { alert('Name required'); return; }
  api('/dashboard/projects/' + projectId + '/files', { method: 'POST', body: { name: name, path: document.getElementById('fileUrl').value, size: parseInt(document.getElementById('fileSize').value) || 0, type: document.getElementById('fileType').value } }).then(function () {
    closeModal('fileModal');
    showProjectTab(projectId, 'files');
  });
}

function sendProjMsg(projectId) {
  var input = document.getElementById('projMsgInput');
  var msg = input ? input.value : '';
  if (!msg) return;
  input.value = '';
  api('/dashboard/projects/' + projectId + '/messages', { method: 'POST', body: { content: msg } }).then(function () {
    showProjectTab(projectId, 'messages');
  });
}

function deployProject(projectId) {
  api('/hosting/deployments', { method: 'POST', body: { project_id: projectId, version: '1.0.' + Date.now() % 1000 } }).then(function (d) {
    if (d.deployment) { alert('Deployment started: ' + d.deployment.url); showProjectTab(projectId, 'deploy'); }
  });
}

// ===== BILLING =====
function renderBilling() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading billing</div>';
  Promise.all([api('/billing/invoices'), api('/billing/payments'), api('/billing/cards'), api('/dashboard/subscriptions')]).then(function (results) {
    var inv = results[0], pay = results[1], cards = results[2], subs = results[3];
    var html = '<div class="section-title" style="margin-bottom:1rem">Billing Center</div>' +
      '<div class="grid grid-3" style="margin-bottom:1.5rem">' +
      '<div class="card"><div style="font-weight:600">Subscriptions</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + (subs.subscriptions ? subs.subscriptions.length : 0) + '</div></div>' +
      '<div class="card"><div style="font-weight:600">Invoices</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + (inv.invoices ? inv.invoices.length : 0) + '</div></div>' +
      '<div class="card"><div style="font-weight:600">Saved Cards</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + (cards.cards ? cards.cards.length : 0) + '</div></div>' +
      '</div>';
    // Subscriptions
    html += '<div class="section-header"><div class="section-title">Subscriptions</div></div>';
    if (subs.subscriptions && subs.subscriptions.length) {
      html += '<div class="table-wrap"><table><tr><th>Plan</th><th>Amount</th><th>Interval</th><th>Status</th><th>Next Billing</th></tr>';
      subs.subscriptions.forEach(function (s) {
        html += '<tr><td>' + s.plan + '</td><td>$' + s.amount + '</td><td>' + s.interval + '</td><td><span class="badge badge--active">' + s.status + '</span></td><td>' + (s.next_billing ? new Date(s.next_billing).toLocaleDateString() : '-') + '</td></tr>';
      });
      html += '</table></div>';
    } else { html += '<div class="empty-state">No active subscriptions</div>'; }
    // Invoices
    html += '<div style="margin-top:1.5rem" class="section-header"><div class="section-title">Invoices</div></div>';
    if (inv.invoices && inv.invoices.length) {
      html += '<div class="table-wrap"><table><tr><th>Invoice #</th><th>Amount</th><th>Status</th><th>Due Date</th></tr>';
      inv.invoices.forEach(function (i) {
        html += '<tr><td>' + (i.invoice_number || i.id.slice(0, 8)) + '</td><td>$' + i.amount.toFixed(2) + '</td><td><span class="badge badge--' + (i.status === 'paid' ? 'active' : 'pending') + '">' + i.status + '</span></td><td>' + (i.due_date ? new Date(i.due_date).toLocaleDateString() : '-') + '</td></tr>';
      });
      html += '</table></div>';
    } else { html += '<div class="empty-state">No invoices yet</div>'; }
    // Cards
    html += '<div style="margin-top:1.5rem" class="section-header"><div class="section-title">Saved Cards</div><button class="btn btn--primary btn--sm" onclick="showAddCard()">+ Add Card</button></div>';
    if (cards.cards && cards.cards.length) {
      html += '<div class="table-wrap"><table><tr><th>Card</th><th>Expiry</th><th>Default</th></tr>';
      cards.cards.forEach(function (c) {
        html += '<tr><td>' + (c.brand || 'Card') + ' **** ' + c.last_four + '</td><td>' + (c.expiry_month || '??') + '/' + (c.expiry_year || '??') + '</td><td>' + (c.is_default ? '&#10003;' : '') + '</td></tr>';
      });
      html += '</table></div>';
    } else { html += '<div class="empty-state">No saved cards</div>'; }
    cont.innerHTML = html;
  });
}

function showAddCard() {
  var html = '<div class="modal-overlay active" id="cardModal"><div class="modal"><div class="modal__header"><div class="modal__title">Add Payment Method</div><button class="modal__close" onclick="closeModal(\'cardModal\')">&times;</button></div>' +
    '<div class="modal__body"><input type="text" id="cardLast4" placeholder="Last 4 digits" maxlength="4"><input type="text" id="cardBrand" placeholder="Brand (Visa, MC, etc)"><div class="form-row"><input type="number" id="cardExpMonth" placeholder="Exp month (1-12)" min="1" max="12"><input type="number" id="cardExpYear" placeholder="Exp year (e.g. 28)"></div></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'cardModal\')">Cancel</button><button class="btn btn--primary" onclick="addCard()">Save</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function addCard() {
  var last4 = document.getElementById('cardLast4').value;
  if (!last4 || last4.length !== 4) { alert('Enter last 4 digits'); return; }
  api('/billing/cards', { method: 'POST', body: { last_four: last4, brand: document.getElementById('cardBrand').value, expiry_month: parseInt(document.getElementById('cardExpMonth').value), expiry_year: parseInt(document.getElementById('cardExpYear').value) } }).then(function () {
    closeModal('cardModal');
    renderPage('billing');
  });
}

// ===== LICENSES =====
function renderLicenses() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading licenses</div>';
  api('/dashboard/licenses').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-title" style="margin-bottom:1rem">Software Licenses</div>';
    if (!d.licenses || !d.licenses.length) {
      html += '<div class="empty-state"><div class="empty-state__icon">\uD83D\uDD11</div><div class="empty-state__text">No licenses yet. Purchase products from the Marketplace to get licenses.</div></div>';
    } else {
      html += '<div class="table-wrap"><table><tr><th>Product</th><th>License Key</th><th>Status</th><th>Expires</th><th>Download</th></tr>';
      d.licenses.forEach(function (l) {
        html += '<tr><td>' + l.product_name + '</td><td style="font-family:monospace;font-size:0.8rem">' + l.license_key + '</td>' +
          '<td><span class="badge badge--active">' + l.status + '</span></td>' +
          '<td>' + (l.expires_at ? new Date(l.expires_at).toLocaleDateString() : 'Never') + '</td>' +
          '<td><button class="btn btn--sm btn--secondary" onclick="alert(\'Download started\')">Download</button></td></tr>';
      });
      html += '</table></div>';
    }
    cont.innerHTML = html;
  });
}

// ===== ACADEMY =====
function renderAcademy() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading academy</div>';
  api('/academy/courses').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-title" style="margin-bottom:1rem">LX Academy</div>' +
      '<div class="section-subtitle">Learn programming, AI, design, and more</div>' +
      '<div class="product-grid">';
    (d.courses || []).forEach(function (c) {
      html += '<div class="product-card" onclick="openCourse(\'' + c.id + '\')" style="cursor:pointer">' +
        '<div class="product-card__title">' + c.title + '</div>' +
        '<div class="product-card__desc">' + (c.description || '') + '</div>' +
        '<div class="product-card__footer"><span style="color:var(--muted);font-size:0.8rem">' + (c.category || '') + '</span>' +
        '<span style="font-weight:600">' + (c.duration_hours || 0) + 'h</span></div></div>';
    });
    html += '</div><div style="margin-top:1.5rem"><div class="section-title">My Enrolled Courses</div><div id="myCourses"></div></div>';
    cont.innerHTML = html;
    api('/academy/my-courses').then(function (e) {
      var el = document.getElementById('myCourses');
      if (!el) return;
      if (!e.enrollments || !e.enrollments.length) { el.innerHTML = '<div class="empty-state">Not enrolled in any courses</div>'; return; }
      var h = '<div class="table-wrap"><table><tr><th>Course</th><th>Progress</th><th>Status</th></tr>';
      e.enrollments.forEach(function (en) {
        h += '<tr><td>' + en.title + '</td><td><div style="background:var(--border);border-radius:4px;height:8px;width:100px;display:inline-block;overflow:hidden"><div style="background:var(--accent);height:100%;width:' + (en.progress || 0) + '%"></div></div> ' + Math.round(en.progress || 0) + '%</td><td>' + (en.completed ? '\u2705 Completed' : '\uD83D\uDCD6 In Progress') + '</td></tr>';
      });
      el.innerHTML = h + '</table></div>';
    });
  });
}

function openCourse(courseId) {
  api('/academy/courses/').then(function (d) {
    var course = d.courses ? d.courses.find(function (c) { return c.id === courseId; }) : null;
    if (!course) return;
    api('/academy/courses/' + courseId + '/enroll', { method: 'POST' }).then(function () {
      var cont = document.getElementById('pageContent');
      var html = '<div class="section-header"><div class="section-title">' + course.title + '</div><button class="btn btn--ghost btn--sm" onclick="renderPage(\'academy\')">Back</button></div>';
      html += '<div class="section-subtitle">' + (course.description || '') + '</div>';
      html += '<div class="section-title" style="margin-top:1rem">Lessons</div>';
      // Fetch lessons
      api('/academy/courses/' + courseId).then(function (c2) {
        if (c2.lessons && c2.lessons.length) {
          html += '<div class="table-wrap"><table><tr><th>Lesson</th><th>Duration</th><th>Complete</th></tr>';
          c2.lessons.forEach(function (l) {
            html += '<tr><td>' + l.title + '</td><td>' + (l.duration_minutes || '-') + ' min</td><td><button class="btn btn--sm btn--secondary" onclick="completeLesson(\'' + l.id + '\',\'' + courseId + '\')">Mark Done</button></td></tr>';
          });
          html += '</table></div>';
        } else {
          html += '<div class="empty-state">No lessons yet</div>';
        }
        cont.innerHTML = html;
      });
    });
  });
}

function completeLesson(lessonId, courseId) {
  api('/academy/lessons/' + lessonId + '/complete', { method: 'POST' }).then(function () {
    openCourse(courseId);
  });
}

// ===== HOSTING =====
function renderHosting() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading hosting</div>';
  api('/hosting/accounts').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-header"><div class="section-title">Hosting Center</div>' +
      '<button class="btn btn--primary btn--sm" onclick="showAddHosting()">+ Add Site</button></div>' +
      '<div class="grid grid-4" style="margin-bottom:1rem">';
    if (d.accounts && d.accounts.length) {
      d.accounts.forEach(function (a) {
        var used = a.storage_used || 0, limit = a.storage_limit || 5120;
        var pct = Math.round((used / limit) * 100);
        html += '<div class="card"><div style="font-weight:600">' + (a.domain || 'No domain') + '</div>' +
          '<div style="font-size:0.8rem;color:var(--muted)">' + a.plan + ' plan</div>' +
          '<div style="margin-top:0.5rem;font-size:0.75rem">Storage: ' + (used / 1024).toFixed(1) + ' GB / ' + (limit / 1024).toFixed(1) + ' GB</div>' +
          '<div style="background:var(--border);border-radius:4px;height:6px;margin-top:0.25rem;overflow:hidden"><div style="background:var(--accent);height:100%;width:' + pct + '%"></div></div></div>';
      });
    } else {
      html += '<div class="card" style="grid-column:1/-1;text-align:center;color:var(--muted)">No hosting accounts</div>';
    }
    html += '</div>';
    // SSL
    if (d.ssl && d.ssl.length) {
      html += '<div class="section-title" style="margin:1rem 0 0.5rem">SSL Certificates</div>';
      html += '<div class="table-wrap"><table><tr><th>Domain</th><th>Issuer</th><th>Expires</th><th>Status</th></tr>';
      d.ssl.forEach(function (s) {
        html += '<tr><td>' + s.domain + '</td><td>' + (s.issuer || 'Auto') + '</td><td>' + (s.expires_at ? new Date(s.expires_at).toLocaleDateString() : '-') + '</td><td><span class="badge badge--active">' + s.status + '</span></td></tr>';
      });
      html += '</table></div>';
    }
    cont.innerHTML = html;
  });
}

function showAddHosting() {
  var html = '<div class="modal-overlay active" id="hostingModal"><div class="modal"><div class="modal__header"><div class="modal__title">New Hosting Account</div><button class="modal__close" onclick="closeModal(\'hostingModal\')">&times;</button></div>' +
    '<div class="modal__body"><input type="text" id="hostDomain" placeholder="Domain (e.g. example.com)"><select id="hostPlan"><option value="basic">Basic - 5GB</option><option value="pro">Pro - 20GB</option><option value="enterprise">Enterprise - 100GB</option></select></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'hostingModal\')">Cancel</button><button class="btn btn--primary" onclick="addHosting()">Create</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function addHosting() {
  api('/hosting/accounts', { method: 'POST', body: { domain: document.getElementById('hostDomain').value, plan: document.getElementById('hostPlan').value } }).then(function () {
    closeModal('hostingModal');
    renderPage('hosting');
  });
}

// ===== INSIGHTS =====
function renderInsights() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading insights</div>';
  api('/insights/overview').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-title" style="margin-bottom:1rem">Business Insights</div>' +
      '<div class="grid grid-4" style="margin-bottom:1.5rem">' +
      '<div class="metric-card"><div class="metric-card__value">' + (d.totalOrders || 0) + '</div><div class="metric-card__label">Orders</div></div>' +
      '<div class="metric-card"><div class="metric-card__value">$' + (d.totalRevenue || 0).toFixed(0) + '</div><div class="metric-card__label">Revenue</div></div>' +
      '<div class="metric-card"><div class="metric-card__value">' + (d.totalProjects || 0) + '</div><div class="metric-card__label">Projects</div></div>' +
      '<div class="metric-card"><div class="metric-card__value">' + (d.visitorGrowth || 0) + '</div><div class="metric-card__label">Visitors (30d)</div></div>' +
      '</div><div class="grid grid-3">' +
      '<div class="card"><div style="font-size:0.85rem;color:var(--muted)">SEO Score</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + (d.seoScore || 0) + '/100</div></div>' +
      '<div class="card"><div style="font-size:0.85rem;color:var(--muted)">Performance</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + (d.performanceScore || 0) + '/100</div></div>' +
      '<div class="card"><div style="font-size:0.85rem;color:var(--muted)">Security</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + (d.securityScore || 0) + '/100</div></div>' +
      '<div class="card"><div style="font-size:0.85rem;color:var(--muted)">Marketing</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + (d.marketingScore || 0) + '/100</div></div>' +
      '<div class="card"><div style="font-size:0.85rem;color:var(--muted)">Customer Growth</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">+' + (d.customerGrowth || 0) + '%</div></div>' +
      '<div class="card"><div style="font-size:0.85rem;color:var(--muted)">Open Tickets</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + (d.openTickets || 0) + '</div></div>' +
      '</div>';
    cont.innerHTML = html;
  });
}

// ===== TEAMS =====
function renderTeams() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading team</div>';
  api('/teams').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-header"><div class="section-title">Team Members</div>' +
      '<button class="btn btn--primary btn--sm" onclick="showInviteMember()">+ Invite</button></div>';
    if (!d.members || !d.members.length) {
      html += '<div class="empty-state"><div class="empty-state__text">No team members yet. Invite your team to collaborate.</div></div>';
    } else {
      html += '<div class="table-wrap"><table><tr><th>Name</th><th>Email</th><th>Role</th><th>Actions</th></tr>';
      d.members.forEach(function (m) {
        html += '<tr><td>' + (m.name || '') + '</td><td>' + m.email + '</td><td><span class="badge badge--active">' + (m.role || 'member') + '</span></td>' +
          '<td><button class="btn btn--sm btn--danger" onclick="removeMember(\'' + m.id + '\')">Remove</button></td></tr>';
      });
      html += '</table></div>';
    }
    cont.innerHTML = html;
  });
}

function showInviteMember() {
  var html = '<div class="modal-overlay active" id="inviteModal"><div class="modal"><div class="modal__header"><div class="modal__title">Invite Team Member</div><button class="modal__close" onclick="closeModal(\'inviteModal\')">&times;</button></div>' +
    '<div class="modal__body"><input type="email" id="inviteEmail" placeholder="Email address" required>' +
    '<select id="inviteRole"><option value="member">Member</option><option value="developer">Developer</option><option value="designer">Designer</option><option value="manager">Manager</option><option value="accountant">Accountant</option><option value="sales">Sales</option></select></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'inviteModal\')">Cancel</button><button class="btn btn--primary" onclick="inviteMember()">Invite</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function inviteMember() {
  var email = document.getElementById('inviteEmail').value;
  if (!email) { alert('Email required'); return; }
  api('/teams/invite', { method: 'POST', body: { email: email, role: document.getElementById('inviteRole').value } }).then(function (d) {
    if (d.error) { alert(d.error); return; }
    closeModal('inviteModal');
    renderPage('teams');
  });
}

function removeMember(id) {
  if (!confirm('Remove this team member?')) return;
  api('/teams/' + id, { method: 'DELETE' }).then(function () { renderPage('teams'); });
}

// ===== SUPPORT =====
function renderSupport() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading support</div>';
  api('/dashboard/support').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-header"><div class="section-title">Support Tickets</div>' +
      '<button class="btn btn--primary btn--sm" onclick="showNewTicket()">+ New Ticket</button></div>';
    if (!d.tickets || !d.tickets.length) {
      html += '<div class="empty-state"><div class="empty-state__text">No open tickets. Need help? Create a ticket!</div></div>';
    } else {
      html += '<div class="table-wrap"><table><tr><th>Subject</th><th>Status</th><th>Priority</th><th>Date</th></tr>';
      d.tickets.forEach(function (t) {
        html += '<tr><td>' + t.subject + '</td><td><span class="badge badge--' + (t.status === 'open' ? 'open' : 'active') + '">' + t.status + '</span></td><td>' + (t.priority || 'normal') + '</td><td>' + new Date(t.created_at).toLocaleDateString() + '</td></tr>';
      });
      html += '</table></div>';
    }
    cont.innerHTML = html;
  });
}

function showNewTicket() {
  var html = '<div class="modal-overlay active" id="ticketModal"><div class="modal"><div class="modal__header"><div class="modal__title">New Support Ticket</div><button class="modal__close" onclick="closeModal(\'ticketModal\')">&times;</button></div>' +
    '<div class="modal__body"><input type="text" id="ticketSubject" placeholder="Subject" required><textarea id="ticketMsg" placeholder="Describe your issue..." style="min-height:100px"></textarea>' +
    '<select id="ticketPriority"><option value="low">Low</option><option value="normal" selected>Normal</option><option value="high">High</option></select></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'ticketModal\')">Cancel</button><button class="btn btn--primary" onclick="createTicket()">Submit</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function createTicket() {
  var subject = document.getElementById('ticketSubject').value;
  if (!subject) { alert('Subject required'); return; }
  api('/dashboard/support', { method: 'POST', body: { subject: subject, message: document.getElementById('ticketMsg').value } }).then(function () {
    closeModal('ticketModal');
    renderPage('support');
  });
}

// ===== BOOKINGS =====
function renderBookings() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading bookings</div>';
  api('/bookings').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-header"><div class="section-title">Bookings & Schedule</div>' +
      '<button class="btn btn--primary btn--sm" onclick="showNewBooking()">+ Book</button></div>';
    if (!d.bookings || !d.bookings.length) {
      html += '<div class="empty-state"><div class="empty-state__text">No bookings yet. Schedule a consultation or meeting.</div></div>';
    } else {
      html += '<div class="table-wrap"><table><tr><th>Type</th><th>Title</th><th>Start</th><th>End</th><th>Status</th></tr>';
      d.bookings.forEach(function (b) {
        html += '<tr><td>' + b.type + '</td><td>' + (b.title || '-') + '</td><td>' + new Date(b.start_time).toLocaleString() + '</td><td>' + (b.end_time ? new Date(b.end_time).toLocaleString() : '-') + '</td><td><span class="badge badge--' + (b.status === 'confirmed' ? 'active' : 'pending') + '">' + b.status + '</span></td></tr>';
      });
      html += '</table></div>';
    }
    cont.innerHTML = html;
  });
}

function showNewBooking() {
  var html = '<div class="modal-overlay active" id="bookingModal"><div class="modal"><div class="modal__header"><div class="modal__title">New Booking</div><button class="modal__close" onclick="closeModal(\'bookingModal\')">&times;</button></div>' +
    '<div class="modal__body"><select id="bookType"><option value="consultation">Consultation</option><option value="meeting">Meeting</option><option value="training">Training</option><option value="support">Support Call</option></select>' +
    '<input type="text" id="bookTitle" placeholder="Title (optional)"><textarea id="bookDesc" placeholder="Description"></textarea>' +
    '<div class="form-row"><input type="datetime-local" id="bookStart"><input type="datetime-local" id="bookEnd"></div></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'bookingModal\')">Cancel</button><button class="btn btn--primary" onclick="createBooking()">Book</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function createBooking() {
  api('/bookings', { method: 'POST', body: { type: document.getElementById('bookType').value, title: document.getElementById('bookTitle').value, description: document.getElementById('bookDesc').value, start_time: document.getElementById('bookStart').value, end_time: document.getElementById('bookEnd').value } }).then(function () {
    closeModal('bookingModal');
    renderPage('bookings');
  });
}

// ===== AFFILIATES =====
function renderAffiliates() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading affiliate program</div>';
  api('/affiliates/status').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var a = d.affiliate || {};
    var html = '<div class="section-title" style="margin-bottom:1rem">Affiliate Program</div>' +
      '<div class="grid grid-4" style="margin-bottom:1.5rem">' +
      '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Referral Code</div><div style="font-size:1.25rem;font-weight:700;font-family:monospace;color:var(--accent)">' + (a.referral_code || '-') + '</div></div>' +
      '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Balance</div><div style="font-size:1.25rem;font-weight:700">$' + (a.balance || 0).toFixed(2) + '</div></div>' +
      '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Total Earned</div><div style="font-size:1.25rem;font-weight:700">$' + (a.total_earned || 0).toFixed(2) + '</div></div>' +
      '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Commission Rate</div><div style="font-size:1.25rem;font-weight:700">' + (a.commission_rate || 20) + '%</div></div>' +
      '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Clicks</div><div style="font-size:1.25rem;font-weight:700">' + (a.clicks || 0) + '</div></div>' +
      '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Signups</div><div style="font-size:1.25rem;font-weight:700">' + (a.signups || 0) + '</div></div>' +
      '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Sales</div><div style="font-size:1.25rem;font-weight:700">' + (a.sales || 0) + '</div></div>' +
      '<div class="card" style="display:flex;align-items:center;justify-content:center"><button class="btn btn--primary" onclick="withdrawEarnings()" ' + ((a.balance || 0) < 10 ? 'disabled style="opacity:0.5"' : '') + '>Withdraw</button></div>' +
      '</div>';
    // Share link
    var shareLink = window.location.origin + '/?ref=' + a.referral_code;
    html += '<div class="card" style="margin-bottom:1rem"><div style="font-size:0.85rem;margin-bottom:0.5rem">Your referral link:</div>' +
      '<div style="display:flex;gap:0.5rem"><input type="text" value="' + shareLink + '" readonly style="flex:1;padding:0.5rem;border:1px solid var(--border);border-radius:var(--radius);background:var(--card-bg);color:var(--text);font-size:0.85rem">' +
      '<button class="btn btn--primary btn--sm" onclick="copyReferral(\'' + shareLink + '\')">Copy</button></div></div>';
    cont.innerHTML = html;
  });
}

function copyReferral(link) {
  if (navigator.clipboard) { navigator.clipboard.writeText(link).then(function () { alert('Copied!'); }); }
  else { alert(link); }
}

function withdrawEarnings() {
  var amt = prompt('Enter amount to withdraw (min $10):', '10');
  if (!amt) return;
  api('/affiliates/withdraw', { method: 'POST', body: { amount: parseFloat(amt) } }).then(function (d) {
    if (d.error) { alert(d.error); return; }
    alert('Withdrawal request submitted!');
    renderPage('affiliates');
  });
}

// ===== VENDORS =====
function renderVendors() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading vendor portal</div>';
  api('/vendors/products').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-header"><div class="section-title">Vendor Portal</div>' +
      '<button class="btn btn--primary btn--sm" onclick="showNewVendorProduct()">+ Add Product</button></div>';
    api('/vendors/earnings').then(function (e) {
      html += '<div class="grid grid-3" style="margin-bottom:1.5rem">' +
        '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Total Earnings</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">$' + ((e && e.earnings) || 0).toFixed(2) + '</div></div>' +
        '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Sales</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + ((e && e.sales) || 0) + '</div></div>' +
        '<div class="card"><div style="font-size:0.8rem;color:var(--muted)">Commission</div><div style="font-size:1.5rem;font-weight:700;color:var(--accent)">' + ((e && e.commission_rate) || 15) + '%</div></div>' +
        '</div>';
      if (!d.products || !d.products.length) {
        html += '<div class="empty-state"><div class="empty-state__text">No products listed yet. Start selling your digital products!</div></div>';
      } else {
        html += '<div class="table-wrap"><table><tr><th>Product</th><th>Price</th><th>Category</th><th>Status</th></tr>';
        d.products.forEach(function (p) {
          html += '<tr><td>' + p.name + '</td><td>$' + p.price.toFixed(2) + '</td><td>' + (p.category || '-') + '</td><td><span class="badge badge--' + (p.status === 'approved' ? 'active' : 'pending') + '">' + p.status + '</span></td></tr>';
        });
        html += '</table></div>';
      }
      cont.innerHTML = html;
    });
  });
}

function showNewVendorProduct() {
  var html = '<div class="modal-overlay active" id="vendorModal"><div class="modal"><div class="modal__header"><div class="modal__title">New Vendor Product</div><button class="modal__close" onclick="closeModal(\'vendorModal\')">&times;</button></div>' +
    '<div class="modal__body"><input type="text" id="vpName" placeholder="Product name" required><textarea id="vpDesc" placeholder="Description"></textarea>' +
    '<div class="form-row"><input type="number" id="vpPrice" placeholder="Price" step="0.01"><select id="vpCategory"><option value="templates">Templates</option><option value="apps">Apps</option><option value="ai-tools">AI Tools</option><option value="design">Design</option><option value="marketing">Marketing</option><option value="other">Other</option></select></div></div>' +
    '<div class="modal__footer"><button class="btn btn--ghost" onclick="closeModal(\'vendorModal\')">Cancel</button><button class="btn btn--primary" onclick="createVendorProduct()">List Product</button></div></div></div>';
  document.body.insertAdjacentHTML('beforeend', html);
}

function createVendorProduct() {
  var name = document.getElementById('vpName').value;
  var price = parseFloat(document.getElementById('vpPrice').value);
  if (!name || !price) { alert('Name and price required'); return; }
  api('/vendors/products', { method: 'POST', body: { name: name, description: document.getElementById('vpDesc').value, price: price, category: document.getElementById('vpCategory').value } }).then(function () {
    closeModal('vendorModal');
    renderPage('vendors');
  });
}

// ===== NOTIFICATIONS =====
function renderNotifications() {
  var cont = document.getElementById('pageContent');
  cont.innerHTML = '<div class="loading">Loading notifications</div>';
  api('/notifications').then(function (d) {
    if (d.error) { cont.innerHTML = '<div class="empty-state">' + d.error + '</div>'; return; }
    var html = '<div class="section-header"><div class="section-title">Notifications</div>' +
      '<button class="btn btn--ghost btn--sm" onclick="markAllRead()">Mark All Read</button></div>';
    if (!d.notifications || !d.notifications.length) {
      html += '<div class="empty-state"><div class="empty-state__text">No notifications</div></div>';
    } else {
      d.notifications.forEach(function (n) {
        html += '<div class="card" style="margin-bottom:0.5rem;' + (n.read ? 'opacity:0.6' : 'border-left:3px solid var(--accent)') + '">' +
          '<div style="display:flex;justify-content:space-between;align-items:flex-start">' +
          '<div><span class="badge badge--' + (n.type === 'alert' ? 'open' : 'active') + '" style="margin-right:0.5rem">' + (n.type || 'info') + '</span>' +
          '<strong>' + n.title + '</strong></div>' +
          '<button class="btn btn--sm btn--ghost" onclick="markRead(\'' + n.id + '\')" ' + (n.read ? 'style="display:none"' : '') + '>Read</button></div>' +
          '<div style="font-size:0.8rem;color:var(--muted);margin-top:0.25rem">' + (n.content || '') + '</div>' +
          '<div style="font-size:0.7rem;color:var(--muted);margin-top:0.25rem">' + new Date(n.created_at).toLocaleString() + '</div></div>';
      });
    }
    cont.innerHTML = html;
  });
}

function markRead(id) {
  api('/notifications/' + id + '/read', { method: 'PUT' }).then(function () { renderPage('notifications'); loadNotifCount(); });
}

function markAllRead() {
  api('/notifications/read-all', { method: 'PUT' }).then(function () { renderPage('notifications'); loadNotifCount(); });
}

function closeModal(id) {
  var el = document.getElementById(id);
  if (el) el.remove();
}
