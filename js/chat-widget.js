void (function () {
  'use strict';

  var API_URL = '/api/chat';
  var MODELS_URL = '/api/models';
  var STORAGE_KEY = 'lx_chat_history';
  var MAX_HISTORY = 20;

  var state = {
    open: false,
    sending: false,
    provider: 'nvidia',
    model: 'meta/llama3-70b-instruct',
    messages: []
  };

  // Load saved history
  try {
    var saved = sessionStorage.getItem(STORAGE_KEY);
    if (saved) state.messages = JSON.parse(saved);
  } catch (e) {}

  function createWidget() {
    var widget = document.createElement('div');
    widget.className = 'chat-widget';
    widget.innerHTML = [
      '<div class="chat-widget__panel" id="chatPanel">',
      '  <div class="chat-widget__header">',
      '    <div class="chat-widget__header-title">',
      '      <span class="chat-widget__dot" style="width:8px;height:8px;border-radius:50%;background:#34d399;box-shadow:0 0 8px rgba(52,211,153,0.5)"></span>',
      '      LX AI Assistant',
      '    </div>',
      '    <button class="chat-widget__minimize" id="chatMinimize" aria-label="Close chat">✕</button>',
      '  </div>',
      '  <div class="chat-widget__messages" id="chatMessages"></div>',
      '  <div class="chat-widget__input-wrap">',
      '    <input class="chat-widget__input" id="chatInput" type="text" placeholder="Ask LX Assistant..." autocomplete="off" />',
      '    <button class="chat-widget__send" id="chatSend" aria-label="Send message">➤</button>',
      '  </div>',
      '</div>',
      '<button class="chat-widget__trigger" id="chatTrigger" aria-label="Open AI chat">', '&#x1f4ac;', '</button>'
    ].join('\n');
    document.body.appendChild(widget);
    return widget;
  }

  function renderMessages() {
    var container = document.getElementById('chatMessages');
    if (!container) return;
    container.innerHTML = '';

    if (state.messages.length === 0) {
      container.innerHTML = '<div style="text-align:center;color:#6e6e7c;font-size:0.8rem;padding:40px 16px;line-height:1.6">Ask me anything about software development, AI, cloud computing, or our products.<br><br><span style="color:#9a9aa8;font-size:0.72rem">Powered by NVIDIA + OpenRouter</span></div>';
      return;
    }

    state.messages.forEach(function (msg) {
      var div = document.createElement('div');
      div.className = 'chat-widget__msg chat-widget__msg--' + (msg.role === 'user' ? 'user' : 'bot');
      div.textContent = msg.content;
      container.appendChild(div);
    });

    container.scrollTop = container.scrollHeight;
  }

  function showTyping() {
    var container = document.getElementById('chatMessages');
    if (!container) return;
    var typing = document.createElement('div');
    typing.className = 'chat-widget__typing';
    typing.id = 'chatTyping';
    typing.innerHTML = '<span></span><span></span><span></span>';
    container.appendChild(typing);
    container.scrollTop = container.scrollHeight;
  }

  function removeTyping() {
    var el = document.getElementById('chatTyping');
    if (el) el.remove();
  }

  function saveHistory() {
    var toSave = state.messages.slice(-MAX_HISTORY);
    try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify(toSave)); } catch (e) {}
  }

  function sendMessage() {
    var input = document.getElementById('chatInput');
    var sendBtn = document.getElementById('chatSend');
    if (!input || !sendBtn) return;

    var text = input.value.trim();
    if (!text || state.sending) return;

    state.sending = true;
    sendBtn.disabled = true;
    input.value = '';

    state.messages.push({ role: 'user', content: text });
    renderMessages();
    showTyping();

    fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: text,
        provider: state.provider,
        model: state.model
      })
    })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        removeTyping();
        var reply = data.reply || 'Sorry, I encountered an error.';
        state.messages.push({ role: 'assistant', content: reply });
        if (data.provider) state.provider = data.provider;
        if (data.model) state.model = data.model;
        renderMessages();
        saveHistory();
      })
      .catch(function () {
        removeTyping();
        state.messages.push({ role: 'assistant', content: 'Connection error. Please try again.' });
        renderMessages();
      })
      .finally(function () {
        state.sending = false;
        sendBtn.disabled = false;
        input.focus();
      });
  }

  function togglePanel() {
    state.open = !state.open;
    var panel = document.getElementById('chatPanel');
    var trigger = document.getElementById('chatTrigger');
    if (panel) panel.classList.toggle('chat-widget__panel--open', state.open);
    if (trigger) trigger.innerHTML = state.open ? '✕' : '&#x1f4ac;';
    if (state.open) {
      renderMessages();
      var input = document.getElementById('chatInput');
      if (input) setTimeout(function () { input.focus(); }, 100);
    }
  }

  function init() {
    createWidget();

    var trigger = document.getElementById('chatTrigger');
    var minimize = document.getElementById('chatMinimize');
    var sendBtn = document.getElementById('chatSend');
    var input = document.getElementById('chatInput');

    if (trigger) trigger.addEventListener('click', togglePanel);
    if (minimize) minimize.addEventListener('click', togglePanel);
    if (sendBtn) sendBtn.addEventListener('click', sendMessage);
    if (input) {
      input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
          e.preventDefault();
          sendMessage();
        }
      });
    }

    renderMessages();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
