/**
 * Velora navigation behaviour.
 *
 * Accessibility: disclosure pattern (not role="menu") — each trigger is a
 * <button> with aria-expanded + aria-controls pointing at its panel.
 *   - Desktop: hover or click/Enter/Space opens a panel; ArrowDown/ArrowUp open
 *     and move focus into it; Escape closes and restores focus to the trigger;
 *     Tab out or an outside click closes it.
 *   - Mobile (<=1100px): burger toggles a full-screen panel + backdrop with body
 *     scroll lock; panels become inline accordions.
 * All Velora transitions are CSS-driven and untouched here.
 */
void (function () {
  'use strict';

  var nav = document.getElementById('veloraNav');
  var links = document.getElementById('veloraLinks');
  var burger = document.getElementById('veloraBurger');
  if (!nav || !links || !burger) return;

  var TOGGLE_BP = 1100;
  var FOCUSABLE = 'a[href],button:not([disabled]),[tabindex]:not([tabindex="-1"])';
  var items = Array.prototype.slice.call(nav.querySelectorAll('.velora-item'));

  // Keep the mobile panel state aligned with the viewport after rotation or resize.
  var desktopQuery = window.matchMedia('(min-width: ' + (TOGGLE_BP + 1) + 'px)');
  desktopQuery.addEventListener('change', function (event) {
    if (event.matches) {
      setMobileOpen(false);
      closeAllMenus();
    }
  });

  function isDesktop() {
    return window.matchMedia('(min-width: ' + (TOGGLE_BP + 1) + 'px)').matches;
  }

  function triggerOf(item) {
    return item.querySelector('[data-velora-menu]');
  }

  function panelOf(item) {
    return item.querySelector('.velora-menu');
  }

  var backdrop = document.querySelector('.velora-backdrop');
  if (!backdrop) {
    backdrop = document.createElement('div');
    backdrop.className = 'velora-backdrop';
    backdrop.setAttribute('aria-hidden', 'true');
    document.body.appendChild(backdrop);
  }

  /* ── Disclosure panels ────────────────────────────────────────── */

  function openMenu(item, focusFirst) {
    items.forEach(function (other) {
      if (other !== item) closeMenu(other);
    });
    item.classList.add('is-open');
    var btn = triggerOf(item);
    if (btn) btn.setAttribute('aria-expanded', 'true');
    if (focusFirst) {
      var panel = panelOf(item);
      var target = panel && panel.querySelector(FOCUSABLE);
      if (target) target.focus();
    }
  }

  function closeMenu(item) {
    item.classList.remove('is-open');
    var btn = triggerOf(item);
    if (btn) btn.setAttribute('aria-expanded', 'false');
  }

  function closeAllMenus() {
    items.forEach(closeMenu);
  }

  items.forEach(function (item) {
    var btn = triggerOf(item);
    if (!btn) return;
    var timer = null;

    item.addEventListener('mouseenter', function () {
      if (!isDesktop()) return;
      clearTimeout(timer);
      openMenu(item);
    });

    item.addEventListener('mouseleave', function () {
      if (!isDesktop()) return;
      clearTimeout(timer);
      timer = setTimeout(function () {
        closeMenu(item);
      }, 140);
    });

    btn.addEventListener('click', function (e) {
      e.preventDefault();
      if (item.classList.contains('is-open')) closeMenu(item);
      else openMenu(item);
    });

    // Arrow keys open the panel and move focus into it (ARIA APG disclosure).
    btn.addEventListener('keydown', function (e) {
      if (!isDesktop()) return;
      if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;
      e.preventDefault();
      openMenu(item);
      var panel = panelOf(item);
      if (!panel) return;
      var focusables = panel.querySelectorAll(FOCUSABLE);
      if (!focusables.length) return;
      var target = e.key === 'ArrowDown' ? focusables[0] : focusables[focusables.length - 1];
      target.focus();
    });
  });

  /* ── Mobile panel ─────────────────────────────────────────────── */

  function isMobileOpen() {
    return links.classList.contains('is-open');
  }

  function setMobileOpen(shouldOpen) {
    var open = !!shouldOpen;
    links.classList.toggle('is-open', open);
    burger.setAttribute('aria-expanded', open ? 'true' : 'false');
    burger.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
    backdrop.classList.toggle('is-open', open);
    document.body.classList.toggle('velora-open', open);
    if (!open) closeAllMenus();
  }

  burger.addEventListener('click', function () {
    setMobileOpen(!isMobileOpen());
  });

  links.addEventListener('click', function (e) {
    var target = e.target;
    if (!target || !target.closest) return;
    // Follow real links (closes the panel); dropdown triggers fall through.
    if (target.closest('a')) setMobileOpen(false);
  });

  // Trap keyboard focus inside the open mobile navigation, without trapping desktop users.
  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Tab' || !isMobileOpen() || isDesktop()) return;
    var nodes = Array.prototype.slice.call(nav.querySelectorAll(FOCUSABLE)).filter(function (el) {
      return el.getClientRects().length && getComputedStyle(el).visibility !== 'hidden';
    });
    if (!nodes.length) return;
    var first = nodes[0], last = nodes[nodes.length - 1];
    if (!nav.contains(document.activeElement)) { e.preventDefault(); first.focus(); }
    else if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
  });

  backdrop.addEventListener('click', function () {
    setMobileOpen(false);
  });

  /* ── Dismissal ────────────────────────────────────────────────── */

  document.addEventListener('click', function (e) {
    var onCard = e.target && e.target.closest ? e.target.closest('.velora-card') : null;
    if (nav.contains(e.target) && !onCard) return;
    closeAllMenus();
  });

  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    if (isMobileOpen()) {
      setMobileOpen(false);
      burger.focus();
      return;
    }
    var open = items.filter(function (i) {
      return i.classList.contains('is-open');
    })[0];
    if (!open) return;
    closeMenu(open);
    var btn = triggerOf(open);
    if (btn) btn.focus();
  });

  nav.addEventListener('focusout', function (e) {
    if (!isDesktop()) return;
    if (e.relatedTarget && nav.contains(e.relatedTarget)) return;
    closeAllMenus();
  });

  var resizeTimer;
  window.addEventListener('resize', function () {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(function () {
      if (window.innerWidth > TOGGLE_BP) {
        setMobileOpen(false);
        closeAllMenus();
      }
    }, 150);
  });

  // Match both clean URLs and .html URLs consistently.
  function normalizePath(path) {
    path = path.replace(/\.html$/, '').replace(/\/$/, '');
    return path === '/index' || path === '' ? '/' : path;
  }
  var currentPath = normalizePath(location.pathname);
  nav.querySelectorAll('a[href]').forEach(function (a) {
    try {
      var url = new URL(a.getAttribute('href'), location.href);
      if (url.origin !== location.origin || url.hash) return;
      if (normalizePath(url.pathname) === currentPath) a.setAttribute('aria-current', 'page');
    } catch (_) {}
  });

  setMobileOpen(false);
})();
