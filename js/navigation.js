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
     SCROLL PROGRESS BAR
     ========================================== */

  var scrollProgress = document.createElement('div');
  scrollProgress.className = 'scroll-progress';
  scrollProgress.setAttribute('aria-hidden', 'true');
  document.body.prepend(scrollProgress);

  function updateScrollProgress() {
    var scrollTop = window.scrollY;
    var docHeight = document.documentElement.scrollHeight - window.innerHeight;
    var progress = docHeight > 0 ? (scrollTop / docHeight) * 100 : 0;
    scrollProgress.style.width = progress + '%';
  }

  window.addEventListener('scroll', updateScrollProgress, { passive: true });
  updateScrollProgress();

  /* ==========================================
     MOBILE NAV BACKDROP
     ========================================== */

  var backdrop = document.createElement('div');
  backdrop.className = 'nav-backdrop';
  document.body.appendChild(backdrop);

  function isMobile() {
    return window.innerWidth <= 768;
  }

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
    else if (body.classList.contains('page-synapse')) pageClass = 'synapse.html';
    else if (body.classList.contains('page-industries')) pageClass = 'industries.html';
    else if (body.classList.contains('page-partners')) pageClass = 'partners.html';

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
    backdrop.classList.toggle('active', isOpen);
    document.body.style.overflow = isOpen ? 'hidden' : '';
  }

  function closeMobileNav() {
    navLinks.classList.remove('nav__links--open');
    navToggle.classList.remove('nav__toggle--active');
    navToggle.setAttribute('aria-expanded', 'false');
    backdrop.classList.remove('active');
    document.body.style.overflow = '';

    // Close all open dropdowns
    document.querySelectorAll('.nav-dropdown--open').forEach(function (d) {
      d.classList.remove('nav-dropdown--open');
    });
  }

  if (navToggle) {
    navToggle.addEventListener('click', toggleMobileNav);
  }

  if (backdrop) {
    backdrop.addEventListener('click', closeMobileNav);
  }

  if (navLinks) {
    navLinks.querySelectorAll('.nav__link, .nav-dropdown__item a').forEach(function (link) {
      link.addEventListener('click', closeMobileNav);
    });
  }

  /* ==========================================
     MOBILE DROPDOWN TOGGLE (Accordion)
     ========================================== */

  document.querySelectorAll('.nav-dropdown').forEach(function (dropdown) {
    var trigger = dropdown.querySelector('.nav-dropdown__trigger');
    if (!trigger) return;

    trigger.addEventListener('click', function (e) {
      if (!isMobile()) return;
      e.preventDefault();
      e.stopPropagation();

      var isOpen = dropdown.classList.contains('nav-dropdown--open');

      // Close other dropdowns
      document.querySelectorAll('.nav-dropdown--open').forEach(function (d) {
        if (d !== dropdown) d.classList.remove('nav-dropdown--open');
      });

      dropdown.classList.toggle('nav-dropdown--open', !isOpen);
    });
  });

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
     NAV DROPDOWNS (Desktop hover)
     ========================================== */

  var dropdowns = document.querySelectorAll('.nav-dropdown');

  dropdowns.forEach(function (dropdown) {
    var trigger = dropdown.querySelector('.nav-dropdown__trigger');
    var menu = dropdown.querySelector('.nav-dropdown__menu');

    if (!trigger || !menu) return;

    // Desktop hover
    trigger.addEventListener('mouseenter', function () {
      if (isMobile()) return;
      menu.classList.add('nav-dropdown__menu--open');
    });

    dropdown.addEventListener('mouseleave', function () {
      if (isMobile()) return;
      menu.classList.remove('nav-dropdown__menu--open');
    });

    // Desktop click fallback
    trigger.addEventListener('click', function (e) {
      if (isMobile()) return;
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
    if (searchOverlay) searchOverlay.classList.remove('search-overlay--open');
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

  /* ==========================================
     KEYBOARD SHORTCUTS
     ========================================== */

  document.addEventListener('keydown', function (e) {
    // Escape closes everything
    if (e.key === 'Escape') {
      if (searchOverlay && searchOverlay.classList.contains('search-overlay--open')) {
        closeSearch();
      }
      if (isMobile() && navLinks && navLinks.classList.contains('nav__links--open')) {
        closeMobileNav();
      }
    }

    // "/" opens search (when not in input)
    if (e.key === '/' && !e.ctrlKey && !e.metaKey) {
      var active = document.activeElement;
      if (active && active.tagName !== 'INPUT' && active.tagName !== 'TEXTAREA') {
        e.preventDefault();
        if (searchToggle) searchToggle.click();
      }
    }
  });

  /* ==========================================
     CLOSE MOBILE NAV ON RESIZE TO DESKTOP
     ========================================== */

  var resizeTimer;
  window.addEventListener('resize', function () {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(function () {
      if (!isMobile() && navLinks && navLinks.classList.contains('nav__links--open')) {
        closeMobileNav();
      }
    }, 150);
  });

  /* ==========================================
     SCROLL TO ANCHOR — SMOOTH WITH NAV OFFSET
     ========================================== */

  document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
    anchor.addEventListener('click', function (e) {
      var targetId = this.getAttribute('href');
      if (targetId === '#' || targetId === '#affiliate') return;
      var target = document.querySelector(targetId);
      if (target) {
        e.preventDefault();
        var navHeight = nav ? nav.offsetHeight : 64;
        var targetPos = target.getBoundingClientRect().top + window.scrollY - navHeight - 16;
        window.scrollTo({ top: targetPos, behavior: 'smooth' });
        closeMobileNav();
      }
    });
  });

})();
