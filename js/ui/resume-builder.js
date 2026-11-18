// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
(() => {
  const byId = id => document.getElementById(id);
  const form = byId('resume-form');
  const paper = byId('resume-paper');
  const save = byId('resume-save');
  const status = byId('resume-status');
  const aiStatus = byId('resume-ai-status');
  const suggestion = byId('resume-suggestion');
  const result = byId('resume-result');
  const storageKey = 'lx-resume-draft-v1';
  const profileKeys = ['fullName', 'role', 'email', 'phone', 'location', 'website', 'summary', 'skills'];
  const entryFields = { title: 120, organisation: 120, dates: 80, description: 1500 };
  const sections = {
    experience: { heading: 'Experience and Projects', labels: ['Role or project', 'Organisation', 'Achievements'] },
    education: { heading: 'Education and Training', labels: ['Qualification or course', 'Institution', 'Details'] },
  };
  const maxEntries = 20;
  let revision = 0;
  let currentRequest;
  let documentText = '';

  function addEntry(section, entry = {}, focus = false) {
    const list = form.querySelector('[data-entries="' + section + '"]');
    if (list.children.length >= maxEntries) return;
    const row = byId('resume-entry').content.firstElementChild.cloneNode(true);
    const [title, organisation, description] = sections[section].labels;
    row.querySelector('[data-label="title"]').textContent = title;
    row.querySelector('[data-label="organisation"]').textContent = organisation;
    row.querySelector('[data-label="description"]').textContent = description;
    for (const [key, maxLength] of Object.entries(entryFields)) {
      row.querySelector('[data-key="' + key + '"]').value = String(entry?.[key] ?? '').slice(0, maxLength);
    }
    list.append(row);
    labelEntries(section, list);
    if (focus) row.querySelector('input').focus();
  }

  function labelEntries(section, list) {
    Array.from(list.children).forEach((row, index) => {
      const label = (section === 'experience' ? 'Experience or project ' : 'Education or training ') + (index + 1);
      row.querySelector('legend').textContent = label;
      row.querySelector('[data-remove]').setAttribute('aria-label', 'Remove ' + label.toLowerCase());
    });
    form.querySelector('[data-add="' + section + '"]').disabled = list.children.length >= maxEntries;
  }

  function getDraft() {
    const draft = Object.fromEntries(profileKeys.map(key => [key, form.elements[key].value.trim()]));
    for (const section of Object.keys(sections)) {
      draft[section] = Array.from(form.querySelector('[data-entries="' + section + '"]').children, row =>
        Object.fromEntries(Array.from(row.querySelectorAll('[data-key]'), input => [input.dataset.key, input.value.trim()]))
      );
    }
    return draft;
  }

  function lines(text) {
    return text.split(/\r?\n/).map(line => line.trim().replace(/^[-•*]\s+/, '')).filter(Boolean);
  }

  function append(parent, tag, value, className = '') {
    const node = document.createElement(tag);
    node.textContent = value;
    if (className) node.className = className;
    parent.append(node);
    return node;
  }

  function appendList(parent, items) {
    if (!items.length) return;
    const list = append(parent, 'ul', '');
    items.forEach(item => append(list, 'li', item));
  }

  function render() {
    const draft = getDraft();
    const contact = [draft.email, draft.phone, draft.location, draft.website].filter(Boolean).join(' | ');
    const text = [draft.fullName, draft.role, contact].filter(Boolean);
    paper.replaceChildren();
    if (draft.fullName) append(paper, 'h2', draft.fullName);
    if (draft.role) append(paper, 'p', draft.role, 'resume-document-role');
    if (contact) append(paper, 'p', contact);
    if (draft.summary) {
      append(paper, 'h3', 'Professional Summary');
      append(paper, 'p', draft.summary);
      text.push('', 'Professional Summary', draft.summary);
    }
    for (const [section, { heading }] of Object.entries(sections)) {
      const entries = draft[section].filter(entry => Object.values(entry).some(Boolean));
      if (!entries.length) continue;
      append(paper, 'h3', heading);
      text.push('', heading);
      for (const entry of entries) {
        const title = [entry.title, entry.organisation].filter(Boolean).join(' | ');
        const achievements = lines(entry.description);
        if (title) append(paper, 'h4', title);
        if (entry.dates) append(paper, 'p', entry.dates);
        appendList(paper, achievements);
        text.push(...[title, entry.dates].filter(Boolean), ...achievements.map(item => '- ' + item), '');
      }
    }
    const skills = lines(draft.skills);
    if (skills.length) {
      append(paper, 'h3', 'Skills');
      appendList(paper, skills);
      text.push('', 'Skills', ...skills.map(item => '- ' + item));
    }
    documentText = text.join('\n').trim();
    byId('resume-print').disabled = !documentText;
    byId('resume-text').disabled = !documentText;
    byId('resume-docx').disabled = !documentText;
    if (!documentText) append(paper, 'p', 'Your CV will appear here as you fill in the editor.', 'resume-empty');
    if (save.checked) {
      try {
        localStorage.setItem(storageKey, JSON.stringify(draft));
      } catch {
        save.checked = false;
        status.textContent = 'Local storage is unavailable. Export a copy before leaving.';
      }
    }
  }

  function loadDraft(draft = {}) {
    for (const key of profileKeys) {
      const field = form.elements[key];
      field.value = String(draft?.[key] ?? '').slice(0, field.maxLength);
    }
    for (const section of Object.keys(sections)) {
      const list = form.querySelector('[data-entries="' + section + '"]');
      list.replaceChildren();
      const entries = Array.isArray(draft?.[section]) ? draft[section].slice(0, maxEntries) : [];
      (entries.length ? entries : [{}]).forEach(entry => addEntry(section, entry));
    }
    render();
  }

  let storedDraft;
  try {
    const saved = localStorage.getItem(storageKey);
    if (saved) storedDraft = JSON.parse(saved);
  } catch {
    // The manual editor works when local storage is blocked.
  }
  save.checked = Boolean(storedDraft);
  loadDraft(storedDraft);
  byId('resume-workspace').hidden = false;

  form.addEventListener('submit', event => event.preventDefault());
  form.addEventListener('input', event => {
    if (event.target.name || event.target.dataset.key) render();
  });
  form.addEventListener('click', event => {
    const { target } = event;
    if (target.matches('[data-add]')) {
      addEntry(target.dataset.add, {}, true);
      render();
    }
    if (target.matches('[data-remove]')) {
      const row = target.closest('.resume-entry');
      const list = row.parentElement;
      const nextFocus = row.nextElementSibling?.querySelector('input') ??
        row.previousElementSibling?.querySelector('input') ??
        form.querySelector('[data-add="' + list.dataset.entries + '"]');
      row.remove();
      labelEntries(list.dataset.entries, list);
      render();
      nextFocus.focus();
    }
  });

  save.addEventListener('change', () => {
    if (save.checked) {
      render();
      if (save.checked) status.textContent = 'Draft saved on this device.';
      return;
    }
    removeStoredDraft();
  });

  function removeStoredDraft() {
    try {
      localStorage.removeItem(storageKey);
      status.textContent = 'Saved draft cleared from this device.';
    } catch {
      status.textContent = 'The browser blocked access to local storage.';
    }
  }

  byId('resume-reset').addEventListener('click', () => {
    if (!window.confirm('Clear this CV and its saved draft?')) return;
    revision++;
    currentRequest?.abort();
    save.checked = false;
    form.reset();
    loadDraft();
    removeStoredDraft();
    suggestion.textContent = '';
    result.hidden = true;
    aiStatus.textContent = '';
  });

  function filename() {
    return form.elements.fullName.value.trim().replace(/[^\p{L}\p{N}]+/gu, '-').replace(/^-|-$/g, '') || 'CV';
  }

  function downloadFile(blob, name) {
    const url = URL.createObjectURL(blob);
    const download = document.createElement('a');
    download.href = url;
    download.download = name;
    download.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  byId('resume-text').addEventListener('click', () => {
    downloadFile(new Blob([documentText + '\n'], { type: 'text/plain;charset=utf-8' }), filename() + '.txt');
  });

  byId('resume-docx').addEventListener('click', async event => {
    const button = event.currentTarget;
    const name = filename() + '.docx';
    const rows = Array.from(paper.querySelectorAll('h2, h3, h4, p, li'), node => ({
      tagName: node.tagName,
      text: node.textContent,
    }));
    button.disabled = true;
    try {
      const { makeWordDocument } = await import('/js/ui/resume-word.js');
      downloadFile(await makeWordDocument(rows), name);
      status.textContent = 'Word export is ready.';
    } catch {
      status.textContent = 'Word export failed. Try again, or use PDF or text.';
    } finally {
      button.disabled = !documentText;
    }
  });

  byId('resume-print').addEventListener('click', () => {
    const title = document.title;
    document.title = filename() + ' CV';
    try { window.print(); } finally { document.title = title; }
  });

  byId('resume-suggest').addEventListener('click', async event => {
    const button = event.currentTarget;
    const source = byId('resume-ai-source').value.trim();
    const job = byId('resume-ai-job').value.trim();
    if (!byId('resume-ai-consent').checked || source.length < 12) {
      aiStatus.textContent = 'Enter at least 12 characters and agree to send the excerpts.';
      return;
    }
    const startedAt = revision;
    const controller = new AbortController();
    currentRequest = controller;
    const timeout = setTimeout(() => controller.abort(), 20000);
    result.hidden = true;
    button.disabled = true;
    aiStatus.textContent = 'Preparing a suggestion…';
    try {
      const response = await fetch('/api/resume-assist', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ source, job, mode: byId('resume-ai-mode').value, consent: true }),
        signal: controller.signal,
      });
      const data = await response.json();
      if (revision !== startedAt) return;
      if (!response.ok) {
        aiStatus.textContent = data.error || 'AI assistance is temporarily unavailable.';
        return;
      }
      suggestion.textContent = data.suggestion;
      result.hidden = false;
      aiStatus.textContent = 'Check each claim against your own experience before using the suggestion.';
    } catch {
      if (revision === startedAt) aiStatus.textContent = 'AI assistance is temporarily unavailable. You can keep editing your CV.';
    } finally {
      clearTimeout(timeout);
      currentRequest = undefined;
      button.disabled = false;
    }
  });

  byId('resume-copy-suggestion').addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(suggestion.textContent);
      aiStatus.textContent = 'Suggestion copied.';
    } catch {
      const range = document.createRange();
      range.selectNodeContents(suggestion);
      window.getSelection()?.removeAllRanges();
      window.getSelection()?.addRange(range);
      aiStatus.textContent = 'Suggestion selected. Use your browser’s copy command.';
    }
  });
})();
