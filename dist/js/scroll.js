void (function () {
  'use strict';

  /* ==========================================
     INTERSECTION OBSERVER — Reveal Animations
     ========================================== */

  function initRevealObserver() {
    var elements = document.querySelectorAll('.reveal, .reveal--left, .reveal--right, .reveal--scale');

    if (!elements.length) return;

    if ('IntersectionObserver' in window) {
      var observer = new IntersectionObserver(
        function (entries) {
          entries.forEach(function (entry) {
            if (entry.isIntersecting) {
              entry.target.classList.add('reveal--visible');
              observer.unobserve(entry.target);
            }
          });
        },
        { threshold: 0.1, rootMargin: '0px 0px -60px 0px' }
      );

      elements.forEach(function (el) {
        observer.observe(el);
      });
    } else {
      elements.forEach(function (el) {
        el.classList.add('reveal--visible');
      });
    }
  }

  /* ==========================================
     PARALLAX
     ========================================== */

  function initParallax() {
    var elements = document.querySelectorAll('.parallax');
    if (!elements.length || !window.requestAnimationFrame) return;

    function updateParallax() {
      elements.forEach(function (el) {
        var speed = parseFloat(el.getAttribute('data-speed')) || 0.08;
        var rect = el.getBoundingClientRect();
        var centerY = rect.top + rect.height / 2;
        var windowCenter = window.innerHeight / 2;
        var offset = (centerY - windowCenter) * speed * -1;
        el.style.transform = 'translateY(' + offset + 'px)';
      });
    }

    window.addEventListener('scroll', function () {
      requestAnimationFrame(updateParallax);
    }, { passive: true });

    updateParallax();
  }

  /* ==========================================
     LOADING BAR
     ========================================== */

  function initLoadingBar() {
    var bar = document.getElementById('loadingBarFill');
    if (!bar) return;

    var progress = 0;
    var interval = setInterval(function () {
      progress += Math.random() * 20 + 5;
      if (progress >= 85) {
        progress = 85;
        clearInterval(interval);
      }
      bar.style.width = progress + '%';
    }, 250);

    function finishLoad() {
      clearInterval(interval);
      bar.style.width = '100%';
      setTimeout(function () {
        bar.style.opacity = '0';
        setTimeout(function () {
          bar.style.display = 'none';
        }, 300);
      }, 400);
    }

    if (document.readyState === 'complete') {
      finishLoad();
    } else {
      window.addEventListener('load', finishLoad);
    }
  }

  /* ==========================================
     SMOOTH SCROLL
     ========================================== */

  function initSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
      anchor.addEventListener('click', function (e) {
        var targetId = this.getAttribute('href');
        if (targetId === '#') return;
        var target = document.querySelector(targetId);
        if (target) {
          e.preventDefault();
          var navHeight = nav ? nav.offsetHeight : 76;
          var targetPos = target.getBoundingClientRect().top + window.scrollY - navHeight - 20;
          window.scrollTo({ top: targetPos, behavior: 'smooth' });
        }
      });
    });
  }

  /* ==========================================
     SCROLL TO TOP (Floating button)
     ========================================== */

  function initScrollToTop() {
    var btn = document.getElementById('scrollToTop');
    if (!btn) return;

    window.addEventListener('scroll', function () {
      if (window.scrollY > 400) {
        btn.classList.add('floating-btn--visible');
      } else {
        btn.classList.remove('floating-btn--visible');
      }
    }, { passive: true });

    btn.addEventListener('click', function () {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  /* ==========================================
     INIT
     ========================================== */

  var nav = document.getElementById('nav');

  function init() {
    initRevealObserver();
    initParallax();
    initLoadingBar();
    initSmoothScroll();
    initScrollToTop();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
