void (function () {
  'use strict';

  var btn = document.getElementById('saasMenu');
  var links = document.getElementById('navLinks');
  if (!btn || !links) return;

  var TOGGLE_BP = 1100;

  var backdrop = document.querySelector('.nav-backdrop');
  if (!backdrop) {
    backdrop = document.createElement('div');
    backdrop.className = 'nav-backdrop';
    backdrop.setAttribute('aria-hidden', 'true');
    document.body.appendChild(backdrop);
  }

  function isOpen() {
    return links.classList.contains('open');
  }

  function setIsOpen(shouldOpen) {
    var open = !!shouldOpen;
    links.classList.toggle('open', open);
    btn.classList.toggle('is-open', open);
    btn.setAttribute('aria-expanded', open ? 'true' : 'false');
    btn.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
    backdrop.classList.toggle('active', open);
    document.body.style.overflow = open ? 'hidden' : '';
  }

  function toggle() {
    setIsOpen(!isOpen());
  }

  btn.addEventListener('click', toggle);

  links.addEventListener('click', function (e) {
    if (e.target.closest && e.target.closest('a')) setIsOpen(false);
  });

  backdrop.addEventListener('click', function () {
    setIsOpen(false);
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && isOpen()) {
      setIsOpen(false);
      btn.focus();
    }
  });

  var resizeTimer;
  window.addEventListener('resize', function () {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(function () {
      if (window.innerWidth > TOGGLE_BP) setIsOpen(false);
    }, 150);
  });

  setIsOpen(false);
})();