// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
import { createWordDocument } from './resume-word.js';

const byId = id => document.getElementById(id);
const form = byId('cv-form');
const paper = byId('cv-paper');
const preview = byId('cv-preview');
const save = byId('cv-save');
const dialog = byId('cv-review');
const reviewStatus = byId('cv-review-status');
const reviewContent = byId('cv-review-content');
const suggestion = byId('cv-suggestion');
const apply = byId('cv-apply');
const send = byId('cv-send');
const draftKey = 'lx-resume-draft-v1';
const fields = [...form.querySelectorAll('[data-field]')];
const groups = {
  experience: {
    label: 'Experience or project',
    fields: [
      ['name', 'Role or project', 120],
      ['place', 'Organisation or context', 120],
      ['dates', 'Dates', 80],
      ['details', 'What you did and what changed (one point per line)', 1800]
    ]
  },
  education: {
    label: 'Education or training',
    fields: [
      ['name', 'Qualification or course', 120],
      ['place', 'Institution or training provider', 120],
      ['dates', 'Dates', 80],
      ['details', 'Relevant coursework, certification or achievements', 1800]
    ]
  }
};
const entryLimit = 12;
let pending;
let controller;

function element(tag, value, className) {
  const item = document.createElement(tag);
  if (value) item.textContent = value;
  if (className) item.className = className;
  return item;
}

function controlText(value, limit) {
  return typeof value === 'string' ? value.slice(0, limit) : '';
}

function groupContainer(group) {
  return byId('cv-' + group);
}

function updateEntryLabels(group) {
  const entries = [...groupContainer(group).children];
  entries.forEach((entry, index) => {
    entry.querySelector('h3').textContent = groups[group].label + ' ' + (index + 1);
    entry.querySelector('[data-remove]').setAttribute('aria-label', 'Remove ' + groups[group].label.toLowerCase() + ' ' + (index + 1));
  });
  form.querySelector('[data-add="' + group + '"]').disabled = entries.length >= entryLimit;
}

function addEntry(group, values = {}, focus = false) {
  const list = groupContainer(group);
  if (list.children.length >= entryLimit) return;
  const article = element('section', null, 'cv-entry');
  const head = element('div', null, 'cv-entry-head');
  const remove = element('button', 'Remove', 'cv-button');
  remove.type = 'button';
  remove.dataset.remove = group;
  head.append(element('h3'), remove);
  const block = element('div', null, 'cv-fields');
  for (const [key, title, limit] of groups[group].fields) {
    const label = element('label', title);
    const field = element(key === 'details' ? 'textarea' : 'input');
    field.dataset.item = key;
    field.maxLength = limit;
    field.value = controlText(values?.[key], limit);
    if (key === 'details') {
      field.rows = 4;
      if (group === 'experience') field.placeholder = 'e.g. Built an Excel tracker to help a team follow up on stock orders.';
    }
    label.append(field);
    (key === 'details' ? article : block).append(label);
  }
  article.prepend(head, block);
  if (group === 'experience') {
    const rewrite = element('button', 'Suggest stronger bullets', 'cv-button cv-ai');
    rewrite.type = 'button';
    rewrite.dataset.ai = 'bullets';
    article.append(rewrite);
  }
  list.append(article);
  updateEntryLabels(group);
  if (focus) article.querySelector('input').focus();
}

function readCV() {
  const cv = Object.fromEntries(fields.map(field => [field.dataset.field, field.value.trim()]));
  for (const group of Object.keys(groups)) {
    cv[group] = [...groupContainer(group).children].map(entry =>
      Object.fromEntries([...entry.querySelectorAll('[data-item]')].map(field => [field.dataset.item, field.value.trim()]))
    );
  }
  return cv;
}

function nonempty(item) {
  return Object.values(item).some(Boolean);
}

function lines(text) {
  return text.split(/\r?\n/).map(line => line.replace(/^\s*(?:[-*•]\s+|\d+[.)]\s+)/, '').trim()).filter(Boolean);
}

function getBlocks(cv) {
  const blocks = [];
  function push(kind, text) {
    if (text) blocks.push({ kind, text });
  }
  push('name', cv.name);
  push('title', cv.title);
  push('contact', [cv.email, cv.phone, cv.location, cv.link].filter(Boolean).join(' | '));
  if (cv.summary) {
    push('section', 'Professional summary');
    cv.summary.split(/\r?\n/).map(line => line.trim()).forEach(line => push('paragraph', line));
  }
  if (cv.skills) {
    push('section', 'Skills');
    push('paragraph', cv.skills);
  }
  for (const group of Object.keys(groups)) {
    const entries = cv[group].filter(nonempty);
    if (!entries.length) continue;
    push('section', group === 'experience' ? 'Experience and projects' : 'Education and training');
    for (const item of entries) {
      push('heading', item.name);
      push('meta', [item.place, item.dates].filter(Boolean).join(' | '));
      lines(item.details).forEach(line => push(group === 'experience' ? 'bullet' : 'paragraph', line));
    }
  }
  return blocks;
}

function render() {
  const cv = readCV();
  const blocks = getBlocks(cv);
  const output = document.createDocumentFragment();
  let bulletList;
  const tag = { name: 'h2', section: 'h3', heading: 'h4' };
  for (const { kind, text } of blocks) {
    if (kind === 'bullet') {
      if (!bulletList) {
        bulletList = element('ul');
        output.append(bulletList);
      }
      bulletList.append(element('li', text));
      continue;
    }
    bulletList = null;
    const className = { meta: 'cv-meta', contact: 'cv-meta cv-contact' }[kind];
    output.append(element(tag[kind] || 'p', text, className));
  }
  if (!blocks.length) output.append(element('p', 'Add your name and experience to see your CV here.', 'cv-meta'));
  paper.replaceChildren(output);

  const checks = [
    [cv.name, 'Add your full name.'],
    [cv.email || cv.phone, 'Add at least one way for an employer to contact you.'],
    [cv.summary, 'Write a short professional summary.'],
    [cv.skills, 'List relevant skills you can demonstrate.'],
    [cv.experience.some(item => item.name && item.details), 'Add a role, project or volunteer activity with a specific contribution.'],
    [cv.education.some(item => item.name), 'Add relevant education, training or certification.']
  ];
  const messages = checks.filter(([ready]) => !ready).map(([, message]) => message);
  if (!messages.length) messages.push('Your main CV sections are filled in. Check the facts and preview page breaks before sending.');
  if (byId('cv-advert').value.trim()) messages.push('Compare the advert with your CV. Include requirements you can demonstrate and leave unsupported claims out.');
  byId('cv-checklist').replaceChildren(...messages.map(message => element('li', message)));
}

function storeDraft() {
  const status = byId('cv-draft-status');
  try {
    if (save.checked) {
      localStorage.setItem(draftKey, JSON.stringify(readCV()));
      status.textContent = 'Draft saved in this browser.';
    } else {
      localStorage.removeItem(draftKey);
      status.textContent = 'Browser saving is off.';
    }
  } catch {
    status.textContent = 'Browser storage is unavailable. Export your CV before leaving.';
  }
}

function restoreDraft() {
  try {
    const stored = localStorage.getItem(draftKey);
    if (!stored) return;
    const cv = JSON.parse(stored);
    if (!cv || typeof cv !== 'object' || Array.isArray(cv)) return;
    for (const field of fields) field.value = controlText(cv[field.dataset.field], field.maxLength);
    for (const group of Object.keys(groups)) {
      const entries = Array.isArray(cv[group]) ? cv[group].slice(0, entryLimit) : [];
      entries.forEach(entry => addEntry(group, entry));
    }
    save.checked = true;
    byId('cv-draft-status').textContent = 'Draft restored from this browser.';
  } catch {
    byId('cv-draft-status').textContent = 'Your saved draft could not be loaded.';
  }
}

function update() {
  render();
  byId('cv-export-status').textContent = '';
  if (save.checked) storeDraft();
}

function payloadFor(mode, field) {
  const cv = readCV();
  const source = field.value.trim();
  const context = [];
  if (mode === 'summary') {
    if (cv.skills) context.push('Skills: ' + cv.skills);
    if (cv.title) context.push('Current focus: ' + cv.title);
    for (const item of [...cv.experience, ...cv.education].filter(nonempty)) {
      context.push([item.name, item.details].filter(Boolean).join(' — '));
    }
  } else {
    const role = field.closest('.cv-entry').querySelector('[data-item="name"]').value.trim();
    if (role) context.push('Role or project: ' + role);
  }
  return {
    mode,
    source: source.slice(0, 1800),
    context: context.join('\n').slice(0, 3500),
    target: cv.target,
    advert: byId('cv-advert').value.trim()
  };
}

const sendContent = byId('cv-send-content');
const sendFields = ['source', 'context', 'target', 'advert'];

function openSuggestion(button) {
  const mode = button.dataset.ai;
  const field = mode === 'summary' ? byId('cv-summary') : button.closest('.cv-entry').querySelector('textarea');
  const payload = payloadFor(mode, field);
  sendContent.hidden = true;
  if (mode === 'bullets' ? payload.source.length < 20 : (payload.source + payload.context).trim().length < 20) {
    reviewStatus.textContent = mode === 'bullets'
      ? 'Add a few details about what you did in this entry before asking AI for bullets.'
      : 'Add skills, experience, education or a few lines of summary before asking AI for wording.';
    send.hidden = true;
  } else {
    reviewStatus.textContent = 'Review the exact text that will go to Cloudflare Workers AI or OpenRouter. Edit or remove private details. These edits apply only to this request.';
    send.hidden = false;
    sendContent.hidden = false;
    for (const key of sendFields) byId('cv-send-' + key).value = payload[key];
  }
  pending = { field, payload };
  reviewContent.hidden = true;
  apply.hidden = true;
  send.disabled = false;
  dialog.showModal();
}

async function requestSuggestion() {
  const current = pending;
  if (!current || send.disabled) return;
  const approved = Object.fromEntries(sendFields.map(key => [key, byId('cv-send-' + key).value.trim()]));
  const request = new AbortController();
  controller = request;
  send.disabled = true;
  reviewStatus.textContent = 'Writing a suggestion…';
  const timeout = setTimeout(() => request.abort(), 30000);
  try {
    const response = await fetch('/api/resume-suggest', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...approved, mode: current.payload.mode, consent: true }),
      signal: request.signal
    });
    const result = await response.json();
    if (current !== pending || !dialog.open) return;
    if (!response.ok || typeof result.suggestion !== 'string' || !result.suggestion.trim()) {
      reviewStatus.textContent = result.error || 'AI is temporarily unavailable. You can continue editing your CV.';
      return;
    }
    byId('cv-original').value = current.field.value;
    suggestion.maxLength = current.field.maxLength;
    suggestion.value = result.suggestion.slice(0, suggestion.maxLength);
    reviewStatus.textContent = 'Review the suggestion and edit anything you want to change.';
    sendContent.hidden = true;
    reviewContent.hidden = false;
    apply.hidden = false;
    send.hidden = true;
    suggestion.focus();
  } catch {
    if (current === pending && dialog.open) reviewStatus.textContent = 'AI is temporarily unavailable. You can retry or continue editing your CV.';
  } finally {
    clearTimeout(timeout);
    if (current === pending) send.disabled = false;
    if (controller === request) controller = null;
  }
}

function exportCV(type) {
  const cv = readCV();
  if (!cv.name) {
    byId('cv-export-status').textContent = 'Enter your full name before exporting.';
    byId('cv-name').focus();
    return;
  }
  if (type === 'print') {
    byId('cv-export-status').textContent = 'In the print options, choose Save as PDF and turn off browser headers and footers if shown.';
    window.print();
    return;
  }
  const url = URL.createObjectURL(createWordDocument(getBlocks(cv)));
  const link = element('a');
  link.href = url;
  link.download = (cv.name.replace(/[^\p{L}\p{N}]+/gu, '-').replace(/^-|-$/g, '').slice(0, 75) || 'Resume') + '-CV.docx';
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 30000);
  byId('cv-export-status').textContent = 'Word download started.';
}

form.addEventListener('submit', event => event.preventDefault());
form.addEventListener('input', event => {
  if (event.target !== save) update();
});
form.addEventListener('click', event => {
  const button = event.target.closest('button');
  if (!button) return;
  if (button.dataset.add) {
    addEntry(button.dataset.add, {}, true);
    update();
  }
  if (button.dataset.remove) {
    const group = button.dataset.remove;
    const entry = button.closest('.cv-entry');
    const next = entry.nextElementSibling || entry.previousElementSibling;
    entry.remove();
    updateEntryLabels(group);
    (next?.querySelector('input') || form.querySelector('[data-add="' + group + '"]')).focus();
    update();
  }
  if (button.dataset.ai) openSuggestion(button);
});
save.addEventListener('change', storeDraft);
byId('cv-clear').addEventListener('click', () => {
  if (!window.confirm('Clear this CV and the draft saved in this browser?')) return;
  form.reset();
  for (const group of Object.keys(groups)) {
    groupContainer(group).replaceChildren();
    addEntry(group);
  }
  storeDraft();
  update();
  byId('cv-name').focus();
});
dialog.addEventListener('close', () => {
  pending = null;
  controller?.abort();
  byId('cv-original').value = '';
  suggestion.value = '';
  for (const key of sendFields) byId('cv-send-' + key).value = '';
});
byId('cv-dismiss').addEventListener('click', () => dialog.close());
send.addEventListener('click', requestSuggestion);
apply.addEventListener('click', () => {
  if (!pending || !suggestion.value.trim()) {
    reviewStatus.textContent = 'Enter some wording before applying the suggestion.';
    return;
  }
  const field = pending.field;
  field.value = suggestion.value.trim();
  dialog.close();
  update();
  field.focus();
});
byId('cv-word').addEventListener('click', () => exportCV('word'));
byId('cv-print').addEventListener('click', () => exportCV('print'));
restoreDraft();
for (const group of Object.keys(groups)) {
  if (!groupContainer(group).children.length) addEntry(group);
}
render();
form.hidden = false;
preview.hidden = false;
