void (function () {
  'use strict';

  /* ==========================================
     UX POLISH — Toasts · Breadcrumbs · Quick Nav · Bottom Nav
     ========================================== */

  /* ---------- TOAST NOTIFICATIONS ---------- */

  var toastContainer;

  function ensureToastContainer() {
    if (!toastContainer) {
      toastContainer = document.createElement('div');
      toastContainer.className = 'toast-container';
      toastContainer.setAttribute('aria-live', 'polite');
      document.body.appendChild(toastContainer);
    }
    return toastContainer;
  }

  function showToast(message, type) {
    var container = ensureToastContainer();
    var toast = document.createElement('div');
    toast.className = 'toast toast--' + (type || 'info');
    toast.setAttribute('role', 'alert');

    var icon = '';
    if (type === 'success') icon = '<span class="toast__icon">&#10003;</span>';
    else if (type === 'error') icon = '<span class="toast__icon">&#10007;</span>';
    else if (type === 'info') icon = '<span class="toast__icon">&#8505;</span>';

    toast.innerHTML = icon + '<span class="toast__text">' + message + '</span>';
    container.appendChild(toast);

    requestAnimationFrame(function () {
      toast.classList.add('toast--visible');
    });

    setTimeout(function () {
      toast.classList.remove('toast--visible');
      toast.addEventListener('transitionend', function () { toast.remove(); });
    }, 3500);
  }

  /* ---------- BREADCRUMBS ---------- */

  function generateBreadcrumbs() {
    var section = document.querySelector('.section__header');
    if (!section) return;

    var nav = document.querySelector('.breadcrumb');
    if (nav) return;

    var page = getPageName();
    if (page === 'home' || page === 'index') return;

    var crumbs = [{ label: 'Home', href: '/' }];
    var labels = {
      services: 'Services',
      marketplace: 'App Store',
      portfolio: 'Portfolio',
      about: 'About',
      blog: 'Blog',
      contact: 'Contact',
      faq: 'FAQ'
    };

    if (labels[page]) {
      crumbs.push({ label: labels[page], href: '/' + page });
    }

    var bc = document.createElement('nav');
    bc.className = 'breadcrumb';
    bc.setAttribute('aria-label', 'Breadcrumb');

    var html = '<ol class="breadcrumb__list">';
    crumbs.forEach(function (c, i) {
      if (i < crumbs.length - 1) {
        html += '<li class="breadcrumb__item"><a href="' + c.href + '" class="breadcrumb__link">' + c.label + '</a><span class="breadcrumb__sep">/</span></li>';
      } else {
        html += '<li class="breadcrumb__item breadcrumb__item--current" aria-current="page">' + c.label + '</li>';
      }
    });
    html += '</ol>';

    bc.innerHTML = html;
    section.parentNode.insertBefore(bc, section);
  }

  function getPageName() {
    var classes = document.body.className.split(/\s+/);
    for (var i = 0; i < classes.length; i++) {
      if (classes[i].indexOf('page-') === 0) return classes[i].replace('page-', '');
    }
    return 'home';
  }

  /* ---------- QUICK NAV SECTIONS ---------- */

  function initQuickNav() {
    var headings = document.querySelectorAll('.section__title');
    var sections = [];
    headings.forEach(function (h) {
      var parent = h.closest('.section');
      var label = parent ? parent.querySelector('.section__label') : null;
      if (label && parent && !parent.id) {
        var id = label.textContent.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
        if (id && id !== 'page-header' && id !== 'call-to-action') {
          parent.id = id;
          sections.push({ id: id, label: label.textContent.trim() });
        }
      }
    });

    if (sections.length < 2) return;

    var nav = document.createElement('div');
    nav.className = 'quick-nav';
    nav.setAttribute('role', 'navigation');
    nav.setAttribute('aria-label', 'Section navigation');

    var btn = document.createElement('button');
    btn.className = 'quick-nav__toggle';
    btn.setAttribute('aria-label', 'Toggle section navigation');
    btn.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/></svg>';
    nav.appendChild(btn);

    var menu = document.createElement('ul');
    menu.className = 'quick-nav__menu';
    sections.forEach(function (s) {
      var li = document.createElement('li');
      li.className = 'quick-nav__item';
      var a = document.createElement('a');
      a.className = 'quick-nav__link';
      a.href = '#' + s.id;
      a.textContent = s.label;
      a.addEventListener('click', function (e) {
        e.preventDefault();
        var target = document.getElementById(s.id);
        if (target) {
          target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
        menu.classList.remove('quick-nav__menu--open');
      });
      li.appendChild(a);
      menu.appendChild(li);
    });
    nav.appendChild(menu);
    document.body.appendChild(nav);

    btn.addEventListener('click', function () {
      menu.classList.toggle('quick-nav__menu--open');
    });

    document.addEventListener('click', function (e) {
      if (!nav.contains(e.target)) {
        menu.classList.remove('quick-nav__menu--open');
      }
    });
  }

  /* ---------- MOBILE TOUCH IMPROVEMENTS ---------- */

  function initTouchNav() {
    var navToggle = document.getElementById('navToggle');
    var navLinks = document.getElementById('navLinks');

    if (!navToggle || !navLinks) return;

    var startX;

    navLinks.addEventListener('touchstart', function (e) {
      startX = e.touches[0].clientX;
    }, { passive: true });

    navLinks.addEventListener('touchmove', function (e) {
      if (!startX) return;
      var dx = e.touches[0].clientX - startX;

      if (navLinks.classList.contains('nav__links--open') && dx < -50) {
        navLinks.classList.remove('nav__links--open');
        navToggle.classList.remove('nav__toggle--active');
        navToggle.setAttribute('aria-expanded', 'false');
        document.body.style.overflow = '';
        startX = null;
      }
    }, { passive: true });
  }

  /* ---------- MOBILE BOTTOM NAV ---------- */

  function initBottomNav() {
    if (window.innerWidth > 768) return;

    var existing = document.querySelector('.bottom-nav');
    if (existing) return;

    var page = getPageName();
    var nav = document.createElement('nav');
    nav.className = 'bottom-nav';
    nav.setAttribute('aria-label', 'Mobile navigation');

    var items = [
      { icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>', label: 'Home', href: '/', active: page === 'home' },
      { icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>', label: 'Services', href: '/services', active: page === 'services' },
      { icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>', label: 'Store', href: '/marketplace', active: page === 'marketplace' },
      { icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>', label: 'Contact', href: '/contact', active: page === 'contact' }
    ];

    var html = '';
    items.forEach(function (item) {
      html += '<a href="' + item.href + '" class="bottom-nav__item' + (item.active ? ' bottom-nav__item--active' : '') + '">' +
        item.icon +
        '<span>' + item.label + '</span>' +
      '</a>';
    });

    nav.innerHTML = html;
    document.body.appendChild(nav);

    requestAnimationFrame(function () {
      nav.classList.add('bottom-nav--visible');
    });
  }

  /* ---------- SMOOTH ANCHORS ---------- */

  document.addEventListener('click', function (e) {
    var a = e.target.closest('a[href^="#"]');
    if (!a) return;
    var id = a.getAttribute('href').slice(1);
    if (!id) return;
    var target = document.getElementById(id);
    if (target) {
      e.preventDefault();
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      target.setAttribute('tabindex', '-1');
      target.focus({ preventScroll: true });
    }
  });

  /* ---------- COMMAND PALETTE (Cmd+K / Ctrl+K) ---------- */

  var paletteOverlay;

  function openCommandPalette() {
    if (!paletteOverlay) {
      paletteOverlay = document.createElement('div');
      paletteOverlay.className = 'cmd-palette-overlay';
      paletteOverlay.innerHTML =
        '<div class="cmd-palette" role="dialog" aria-modal="true" aria-label="Command palette">' +
          '<div class="cmd-palette__header">' +
            '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>' +
            '<input class="cmd-palette__input" id="cmdInput" type="text" placeholder="Type a command or page name..." autocomplete="off" aria-label="Search pages and commands">' +
            '<kbd class="cmd-palette__shortcut">ESC</kbd>' +
          '</div>' +
          '<div class="cmd-palette__results" id="cmdResults"></div>' +
        '</div>';
      document.body.appendChild(paletteOverlay);

      paletteOverlay.addEventListener('click', function (e) {
        if (e.target === paletteOverlay) closeCommandPalette();
      });

      document.getElementById('cmdInput').addEventListener('input', filterCommands);
      document.getElementById('cmdInput').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
          var active = paletteOverlay.querySelector('.cmd-palette__result--active') || paletteOverlay.querySelector('.cmd-palette__result');
          if (active) {
            e.preventDefault();
            var href = active.getAttribute('data-href');
            if (href) window.location.href = href;
            else if (active.getAttribute('data-action') === 'theme') toggleThemeFromPalette();
            closeCommandPalette();
          }
        }
        if (e.key === 'ArrowDown') {
          e.preventDefault();
          movePaletteSelection(1);
        }
        if (e.key === 'ArrowUp') {
          e.preventDefault();
          movePaletteSelection(-1);
        }
      });
    }

    paletteOverlay.classList.add('cmd-palette-overlay--visible');
    document.body.style.overflow = 'hidden';
    setTimeout(function () { document.getElementById('cmdInput').focus(); }, 100);
    renderDefaultCommands();
  }

  function closeCommandPalette() {
    if (paletteOverlay) {
      paletteOverlay.classList.remove('cmd-palette-overlay--visible');
      document.body.style.overflow = '';
    }
  }

  var COMMANDS = [
    { label: 'Home', href: '/', icon: 'home' },
    { label: 'Services', href: '/services', icon: 'services' },
    { label: 'App Store', href: '/marketplace', icon: 'store' },
    { label: 'Portfolio', href: '/portfolio', icon: 'portfolio' },
    { label: 'About', href: '/about', icon: 'about' },
    { label: 'Blog', href: '/blog', icon: 'blog' },
    { label: 'Contact', href: '/contact', icon: 'contact' },
    { label: 'FAQ', href: '/faq', icon: 'faq' },
    { label: 'Toggle Theme', action: 'theme', icon: 'theme', shortcut: 'T' }
  ];

  var CMD_ICONS = {
    home: '<path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>',
    services: '<polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/>',
    store: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>',
    portfolio: '<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>',
    about: '<path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>',
    blog: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>',
    contact: '<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/>',
    faq: '<circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>',
    theme: '<path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/>'
  };

  function renderDefaultCommands() {
    renderPaletteResults(COMMANDS);
  }

  function filterCommands() {
    var input = document.getElementById('cmdInput');
    var q = input.value.trim().toLowerCase();
    if (!q) { renderDefaultCommands(); return; }
    var filtered = COMMANDS.filter(function (c) {
      return c.label.toLowerCase().indexOf(q) > -1;
    });
    renderPaletteResults(filtered, q);
  }

  function renderPaletteResults(items, query) {
    var container = document.getElementById('cmdResults');
    if (!container) return;

    if (!items.length) {
      container.innerHTML = '<div class="cmd-palette__empty">No results found</div>';
      return;
    }

    var html = '';
    items.forEach(function (item, i) {
      var iconSvg = CMD_ICONS[item.icon] || CMD_ICONS.home;
      var label = item.label;
      if (query) {
        var idx = label.toLowerCase().indexOf(query);
        if (idx > -1) {
          label = label.slice(0, idx) + '<strong>' + label.slice(idx, idx + query.length) + '</strong>' + label.slice(idx + query.length);
        }
      }
      html +=
        '<div class="cmd-palette__result' + (i === 0 ? ' cmd-palette__result--active' : '') + '" ' +
          (item.href ? 'data-href="' + item.href + '"' : '') +
          (item.action ? 'data-action="' + item.action + '"' : '') +
          ' tabindex="-1">' +
          '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' + iconSvg + '</svg>' +
          '<span>' + label + '</span>' +
          (item.shortcut ? '<kbd class="cmd-palette__key">' + item.shortcut + '</kbd>' : '') +
        '</div>';
    });
    container.innerHTML = html;

    container.querySelectorAll('.cmd-palette__result').forEach(function (el) {
      el.addEventListener('click', function () {
        var href = el.getAttribute('data-href');
        if (href) { closeCommandPalette(); window.location.href = href; return; }
        if (el.getAttribute('data-action') === 'theme') { toggleThemeFromPalette(); closeCommandPalette(); }
      });
      el.addEventListener('mouseenter', function () {
        container.querySelectorAll('.cmd-palette__result').forEach(function (r) { r.classList.remove('cmd-palette__result--active'); });
        el.classList.add('cmd-palette__result--active');
      });
    });
  }

  function movePaletteSelection(dir) {
    var items = paletteOverlay.querySelectorAll('.cmd-palette__result');
    if (!items.length) return;
    var current = paletteOverlay.querySelector('.cmd-palette__result--active');
    var idx = Array.prototype.indexOf.call(items, current);
    items[idx].classList.remove('cmd-palette__result--active');
    idx = (idx + dir + items.length) % items.length;
    items[idx].classList.add('cmd-palette__result--active');
    items[idx].scrollIntoView({ block: 'nearest' });
  }

  function toggleThemeFromPalette() {
    var html = document.documentElement;
    var isLight = html.getAttribute('data-theme') === 'light';
    if (isLight) { html.removeAttribute('data-theme'); localStorage.setItem('lx-theme', 'dark'); }
    else { html.setAttribute('data-theme', 'light'); localStorage.setItem('lx-theme', 'light'); }
  }

  /* ---------- KEYBOARD SHORTCUTS ---------- */

  function showShortcutsHelp() {
    var existing = document.getElementById('shortcutsHelp');
    if (existing) existing.remove();

    var overlay = document.createElement('div');
    overlay.className = 'cmd-palette-overlay';
    overlay.id = 'shortcutsHelp';
    overlay.innerHTML =
      '<div class="cmd-palette cmd-palette--help" role="dialog" aria-modal="true" aria-label="Keyboard shortcuts">' +
        '<div class="cmd-palette__header">' +
          '<span style="font-weight:600;color:var(--color-text);">Keyboard Shortcuts</span>' +
          '<button class="cmd-palette__close" id="shortcutsClose" aria-label="Close">&times;</button>' +
        '</div>' +
        '<div class="cmd-palette__results" style="padding:var(--space-2) 0;">' +
          '<div class="shortcuts-list">' +
            '<div class="shortcut-row"><kbd>Ctrl+K</kbd><span>Open Command Palette</span></div>' +
            '<div class="shortcut-row"><kbd>?</kbd><span>Show Keyboard Shortcuts</span></div>' +
            '<div class="shortcut-row"><kbd>/</kbd><span>Open Search</span></div>' +
            '<div class="shortcut-row"><kbd>T</kbd><span>Toggle Theme</span></div>' +
            '<div class="shortcut-row"><kbd>Esc</kbd><span>Close Modals</span></div>' +
            '<div class="shortcut-row"><kbd>↑ ↓</kbd><span>Navigate Results</span></div>' +
            '<div class="shortcut-row"><kbd>Enter</kbd><span>Select Result</span></div>' +
          '</div>' +
        '</div>' +
      '</div>';
    document.body.appendChild(overlay);
    requestAnimationFrame(function () { overlay.classList.add('cmd-palette-overlay--visible'); });

    document.getElementById('shortcutsClose').addEventListener('click', function () { overlay.remove(); });
    overlay.addEventListener('click', function (e) { if (e.target === overlay) overlay.remove(); });
  }

  document.addEventListener('keydown', function (e) {
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
      e.preventDefault();
      if (paletteOverlay && paletteOverlay.classList.contains('cmd-palette-overlay--visible')) {
        closeCommandPalette();
      } else {
        openCommandPalette();
      }
    }
    if (e.key === '?' && !e.ctrlKey && !e.metaKey && !e.altKey) {
      var active = document.activeElement;
      if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA')) return;
      e.preventDefault();
      showShortcutsHelp();
    }
    if (e.key === 't' && !e.ctrlKey && !e.metaKey && !e.altKey) {
      var active2 = document.activeElement;
      if (active2 && (active2.tagName === 'INPUT' || active2.tagName === 'TEXTAREA')) return;
      toggleThemeFromPalette();
    }
    if (e.key === 'Escape') {
      closeCommandPalette();
    }
  });

  /* ---------- READING PROGRESS BAR (blog only) ---------- */

  function initReadingProgress() {
    if (!document.body.classList.contains('page-blog')) return;
    var existing = document.querySelector('.reading-progress');
    if (existing) return;

    var bar = document.createElement('div');
    bar.className = 'reading-progress';
    bar.setAttribute('aria-hidden', 'true');
    document.body.appendChild(bar);

    window.addEventListener('scroll', function () {
      var scrollTop = window.scrollY;
      var docHeight = document.documentElement.scrollHeight - window.innerHeight;
      var progress = docHeight > 0 ? (scrollTop / docHeight) * 100 : 0;
      bar.style.width = progress + '%';
    }, { passive: true });
  }

  /* ---------- ANNOUNCEMENT AUTO-DISMISS ---------- */

  function initAnnouncement() {
    var ann = document.getElementById('announcement');
    if (!ann || ann.style.display === 'none') return;
    if (localStorage.getItem('lx-announcement-dismissed')) {
      ann.style.display = 'none';
      return;
    }
    setTimeout(function () {
      ann.classList.add('announcement--hiding');
      setTimeout(function () { ann.style.display = 'none'; }, 500);
    }, 8000);
  }

  /* ---------- EXIT-INTENT EMAIL POPUP ---------- */

  function initExitIntent() {
    if (localStorage.getItem('lx-exit-shown')) return;
    var shown = false;

    function showExitPopup() {
      if (shown) return;
      shown = true;
      localStorage.setItem('lx-exit-shown', '1');

      var overlay = document.createElement('div');
      overlay.className = 'cmd-palette-overlay';
      overlay.id = 'exitPopup';
      overlay.innerHTML =
        '<div class="exit-popup" role="dialog" aria-modal="true" aria-label="Don\'t miss out">' +
          '<button class="exit-popup__close" id="exitPopupClose" aria-label="Close">&times;</button>' +
          '<div class="exit-popup__icon">' +
            '<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--color-accent)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>' +
          '</div>' +
          '<h3 class="exit-popup__title">Before you go...</h3>' +
          '<p class="exit-popup__text">Subscribe to our newsletter for exclusive updates on new apps, tools, and special offers.</p>' +
          '<div class="exit-popup__form">' +
            '<input type="email" class="exit-popup__input" id="exitEmail" placeholder="your@email.com" aria-label="Email address">' +
            '<button class="btn btn--primary exit-popup__btn" id="exitPopupSubmit">Subscribe</button>' +
          '</div>' +
          '<p class="exit-popup__footnote">No spam. Unsubscribe anytime.</p>' +
        '</div>';
      document.body.appendChild(overlay);
      requestAnimationFrame(function () {
        overlay.classList.add('cmd-palette-overlay--visible');
        setTimeout(function () { document.getElementById('exitEmail').focus(); }, 300);
      });

      document.getElementById('exitPopupClose').addEventListener('click', function () { overlay.remove(); });
      document.getElementById('exitPopupSubmit').addEventListener('click', function () {
        var email = document.getElementById('exitEmail').value.trim();
        if (email && email.indexOf('@') > -1) {
          showToast('Thanks for subscribing!', 'success');
          overlay.remove();
        } else {
          showToast('Please enter a valid email address', 'error');
        }
      });
      overlay.addEventListener('click', function (e) { if (e.target === overlay) overlay.remove(); });
    }

    document.addEventListener('mouseleave', function (e) {
      if (shown) return;
      if (e.clientY > 0) return;
      showExitPopup();
    });

    document.addEventListener('visibilitychange', function () {
      if (document.hidden && !shown) {
        showExitPopup();
      }
    });
  }

  /* ---------- PORTFOLIO LIGHTBOX ---------- */

  function initPortfolioLightbox() {
    if (!document.body.classList.contains('page-portfolio')) return;

    document.querySelectorAll('.portfolio-item__link').forEach(function (link) {
      link.addEventListener('click', function (e) {
        e.preventDefault();
        var item = link.closest('.portfolio-item');
        if (!item) return;

        var title = item.querySelector('.portfolio-item__title');
        var desc = item.querySelector('.portfolio-item__description');
        var tags = item.querySelectorAll('.portfolio-item__tag');

        var tagHtml = '';
        tags.forEach(function (t) { tagHtml += '<span class="portfolio-item__tag">' + t.textContent + '</span>'; });

        var overlay = document.createElement('div');
        overlay.className = 'cmd-palette-overlay';
        overlay.innerHTML =
          '<div class="portfolio-lightbox" role="dialog" aria-modal="true" aria-label="' + (title ? title.textContent : 'Case Study') + '">' +
            '<button class="exit-popup__close" id="lightboxClose" aria-label="Close">&times;</button>' +
            '<div class="portfolio-lightbox__body">' +
              '<div class="portfolio-lightbox__icon">' +
                item.querySelector('.portfolio-item__image')?.innerHTML || '' +
              '</div>' +
              '<h2 class="portfolio-lightbox__title">' + (title ? title.textContent : '') + '</h2>' +
              '<div class="portfolio-lightbox__tags">' + tagHtml + '</div>' +
              '<p class="portfolio-lightbox__desc">' + (desc ? desc.textContent : '') + '</p>' +
              '<div class="portfolio-lightbox__metrics">' +
                '<div class="portfolio-lightbox__metric"><strong>Timeline</strong><span>8 weeks</span></div>' +
                '<div class="portfolio-lightbox__metric"><strong>Role</strong><span>Full-stack development</span></div>' +
                '<div class="portfolio-lightbox__metric"><strong>Impact</strong><span>40% efficiency gain</span></div>' +
              '</div>' +
              '<a href="/contact" class="btn btn--primary">Start a similar project</a>' +
            '</div>' +
          '</div>';
        document.body.appendChild(overlay);
        requestAnimationFrame(function () { overlay.classList.add('cmd-palette-overlay--visible'); });

        document.getElementById('lightboxClose').addEventListener('click', function () { overlay.remove(); });
        overlay.addEventListener('click', function (e) { if (e.target === overlay) overlay.remove(); });
      });
    });
  }

  /* ---------- SOCIAL SHARE BUTTONS ---------- */

  function initShareButtons() {
    document.querySelectorAll('.share-btn').forEach(function (btn) {
      btn.addEventListener('click', function (e) {
        e.preventDefault();
        var url = encodeURIComponent(window.location.href);
        var text = encodeURIComponent(document.title);
        var platform = btn.getAttribute('data-share');

        var shareUrl = '';
        if (platform === 'twitter') shareUrl = 'https://twitter.com/intent/tweet?text=' + text + '&url=' + url;
        else if (platform === 'linkedin') shareUrl = 'https://linkedin.com/sharing/share-offsite/?url=' + url;
        else if (platform === 'facebook') shareUrl = 'https://facebook.com/sharer/sharer.php?u=' + url;
        else if (platform === 'whatsapp') shareUrl = 'https://wa.me/?text=' + text + '%20' + url;
        else if (platform === 'copy') {
          navigator.clipboard.writeText(window.location.href).then(function () {
            showToast('Link copied to clipboard!', 'success');
          });
          return;
        }

        if (shareUrl) window.open(shareUrl, '_blank', 'width=600,height=400');
      });
    });
  }

  /* ---------- BLOG READ TIME ---------- */

  function initReadTime() {
    if (!document.body.classList.contains('page-blog')) return;
    document.querySelectorAll('.knowledge-card__excerpt').forEach(function (excerpt) {
      var words = excerpt.textContent.trim().split(/\s+/).length;
      var min = Math.max(1, Math.round(words / 200));
      var badge = document.createElement('span');
      badge.className = 'knowledge-card__read-time';
      badge.textContent = min + ' min read';
      excerpt.parentNode.insertBefore(badge, excerpt.nextSibling);
    });
  }

  /* ---------- INIT ---------- */

  function init() {
    generateBreadcrumbs();
    initQuickNav();
    initTouchNav();
    initBottomNav();
    initReadingProgress();
    initAnnouncement();
    initExitIntent();
    initPortfolioLightbox();
    initShareButtons();
    initReadTime();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  window.LXToast = showToast;

})();
