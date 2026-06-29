void (function () {
  'use strict';

  var nav = document.getElementById('nav');
  var navToggle = document.getElementById('navToggle');
  var navLinks = document.getElementById('navLinks');
  var announcement = document.getElementById('announcement');
  var announcementClose = document.getElementById('announcementClose');
  var searchToggle = document.getElementById('searchToggle');
  var searchOverlay = document.getElementById('searchOverlay');
  var searchClose = document.getElementById('searchClose');
  var searchInput = document.getElementById('searchInput');

  /* ==========================================
     ACTIVE NAV LINK
     ========================================== */

  function setActiveNavLink() {
    var body = document.body;
    var pageClass = '';
    if (body.classList.contains('page-home')) pageClass = 'index.html';
    else if (body.classList.contains('page-services')) pageClass = 'services.html';
    else if (body.classList.contains('page-marketplace')) pageClass = 'marketplace.html';
    else if (body.classList.contains('page-portfolio')) pageClass = 'portfolio.html';
    else if (body.classList.contains('page-blog')) pageClass = 'blog.html';
    else if (body.classList.contains('page-about')) pageClass = 'about.html';
    else if (body.classList.contains('page-contact')) pageClass = 'contact.html';
    else if (body.classList.contains('page-faq')) pageClass = 'faq.html';

    if (!pageClass) return;

    document.querySelectorAll('.nav__link').forEach(function (link) {
      if (link.getAttribute('href') === pageClass) {
        link.classList.add('nav__link--active');
      }
    });

    document.querySelectorAll('.nav-dropdown__item').forEach(function (item) {
      if (item.getAttribute('href') === pageClass) {
        item.classList.add('nav-dropdown__item--active');
      }
    });
  }

  setActiveNavLink();

  /* ==========================================
     NAV BACKGROUND ON SCROLL
     ========================================== */

  function updateNav() {
    var hasNavSolid = document.body.classList.contains('page-home') === false;
    if (hasNavSolid) {
      nav.classList.remove('nav--transparent');
      nav.classList.add('nav--solid');
      return;
    }
    var isSolid = window.scrollY > 60;
    nav.classList.toggle('nav--transparent', !isSolid);
    nav.classList.toggle('nav--solid', isSolid);
  }

  window.addEventListener('scroll', updateNav, { passive: true });
  updateNav();

  /* ==========================================
     MOBILE NAV TOGGLE
     ========================================== */

  function toggleMobileNav() {
    var isOpen = navLinks.classList.toggle('nav__links--open');
    navToggle.classList.toggle('nav__toggle--active', isOpen);
    navToggle.setAttribute('aria-expanded', isOpen);
    document.body.style.overflow = isOpen ? 'hidden' : '';
  }

  function closeMobileNav() {
    navLinks.classList.remove('nav__links--open');
    navToggle.classList.remove('nav__toggle--active');
    navToggle.setAttribute('aria-expanded', 'false');
    document.body.style.overflow = '';
  }

  if (navToggle) {
    navToggle.addEventListener('click', toggleMobileNav);
  }

  if (navLinks) {
    navLinks.querySelectorAll('.nav__link, .nav-dropdown__item a').forEach(function (link) {
      link.addEventListener('click', closeMobileNav);
    });
  }

  /* ==========================================
     ANNOUNCEMENT CLOSE
     ========================================== */

  if (announcementClose) {
    announcementClose.addEventListener('click', function () {
      announcement.style.display = 'none';
      document.documentElement.style.setProperty('--announcement-height', '0px');
    });
  }

  /* ==========================================
     NAV DROPDOWNS (Desktop)
     ========================================== */

  var dropdowns = document.querySelectorAll('.nav-dropdown');

  dropdowns.forEach(function (dropdown) {
    var trigger = dropdown.querySelector('.nav-dropdown__trigger');
    var menu = dropdown.querySelector('.nav-dropdown__menu');

    if (!trigger || !menu) return;

    trigger.addEventListener('mouseenter', function () {
      menu.classList.add('nav-dropdown__menu--open');
    });

    dropdown.addEventListener('mouseleave', function () {
      menu.classList.remove('nav-dropdown__menu--open');
    });

    trigger.addEventListener('click', function (e) {
      e.preventDefault();
      menu.classList.toggle('nav-dropdown__menu--open');
    });

    document.addEventListener('click', function (e) {
      if (!dropdown.contains(e.target)) {
        menu.classList.remove('nav-dropdown__menu--open');
      }
    });
  });

  /* ==========================================
     SEARCH MODAL
     ========================================== */

  if (searchToggle && searchOverlay) {
    searchToggle.addEventListener('click', function () {
      searchOverlay.classList.add('search-overlay--open');
      document.body.style.overflow = 'hidden';
      if (searchInput) {
        setTimeout(function () { searchInput.focus(); }, 100);
      }
    });
  }

  function closeSearch() {
    searchOverlay.classList.remove('search-overlay--open');
    document.body.style.overflow = '';
  }

  if (searchClose) {
    searchClose.addEventListener('click', closeSearch);
  }

  if (searchOverlay) {
    searchOverlay.addEventListener('click', function (e) {
      if (e.target === searchOverlay) closeSearch();
    });
  }

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      if (searchOverlay && searchOverlay.classList.contains('search-overlay--open')) {
        closeSearch();
      }
      closeMobileNav();
    }
    if (e.key === '/' && !e.ctrlKey && !e.metaKey) {
      var active = document.activeElement;
      if (active && active.tagName !== 'INPUT' && active.tagName !== 'TEXTAREA') {
        e.preventDefault();
        if (searchToggle) searchToggle.click();
      }
    }
  });

})();
