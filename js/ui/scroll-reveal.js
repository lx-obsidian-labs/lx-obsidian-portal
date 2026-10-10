/**
 * Scroll Reveal Text — vanilla port of the Framer "ScrollRevealText" component.
 *
 * Splits plain-text headings into characters (grouped in nowrap word wrappers)
 * and ties each character's opacity, colour, blur and offset to scroll position,
 * staggered left to right. Only elements whose content is a single text node are
 * enrolled, so headings containing markup (links, spans) are left untouched.
 *
 * Enrol explicitly with `data-reveal`, or automatically via plain-text
 * h1/h2/h3 and .saas-heading.
 */
void (function () {
  'use strict';

  var STAGGER = 0.2;
  var DUR = 0.7;
  var X_OFFSET = 7;
  var START_FRAC = 0.8;
  var END_FRAC = 0.2;

  var reduceMotion =
    window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  var entries = [];

  function isPlainText(el) {
    if (!el || !el.childNodes.length) return false;
    for (var i = 0; i < el.childNodes.length; i++) {
      if (el.childNodes[i].nodeType !== 3) return false;
    }
    return /\S/.test(el.textContent);
  }

  function split(el) {
    var text = el.textContent;
    var units = [];
    var frag = document.createDocumentFragment();
    var tokens = text.split(/(\s+)/);
    var idx = 0;

    for (var t = 0; t < tokens.length; t++) {
      var tok = tokens[t];
      if (!tok) continue;
      if (/^\s+$/.test(tok)) {
        frag.appendChild(document.createTextNode(tok));
        continue;
      }
      var word = document.createElement('span');
      word.className = 'rv-w';
      for (var c = 0; c < tok.length; c++) {
        var ch = document.createElement('span');
        ch.className = 'rv-c';
        ch.textContent = tok.charAt(c);
        word.appendChild(ch);
        units.push({ el: ch, unit: idx++ });
      }
      frag.appendChild(word);
    }

    el.setAttribute('aria-label', text);
    el.textContent = '';
    el.appendChild(frag);
    return units;
  }

  function build(el) {
    var units = split(el);
    if (!units.length) return null;
    var entry = { el: el, units: units, visible: false, scheduled: false };
    var totalUnits = units.length;
    var totalTime = DUR + Math.max(0, totalUnits - 1) * STAGGER;

    entry.apply = function (p) {
      var time = p * totalTime;
      for (var i = 0; i < units.length; i++) {
        var u = units[i];
        var pu =
          totalUnits <= 1
            ? p
            : Math.max(0, Math.min(1, (time - u.unit * STAGGER) / DUR));
        var s = u.el.style;
        s.opacity = (0.3 + pu * 0.7).toFixed(3);
        s.transform = 'translateX(' + (-X_OFFSET + X_OFFSET * pu).toFixed(2) + 'px)';
        s.color =
          'color-mix(in srgb, var(--rv-revealed) ' +
          Math.round(pu * 100) +
          '%, var(--rv-hidden))';
      }
    };

    if (reduceMotion) {
      entry.apply(1);
      return null;
    }
    entry.apply(0);
    return entry;
  }

  function update(entry) {
    var vh = window.innerHeight;
    var rect = entry.el.getBoundingClientRect();
    var range = (START_FRAC - END_FRAC) * vh;
    if (range <= 0) return;
    var p = Math.max(0, Math.min(1, (START_FRAC * vh - rect.top) / range));
    entry.apply(p);
  }

  function onScroll() {
    for (var i = 0; i < entries.length; i++) {
      var entry = entries[i];
      if (!entry.visible || entry.scheduled) continue;
      (function (e) {
        e.scheduled = true;
        requestAnimationFrame(function () {
          update(e);
          e.scheduled = false;
        });
      })(entry);
    }
  }

  function collect() {
    var explicit = document.querySelectorAll('[data-reveal]');
    var i;
    for (i = 0; i < explicit.length; i++) enrol(explicit[i]);
    var auto = document.querySelectorAll('h1, h2, h3, .saas-heading');
    for (i = 0; i < auto.length; i++) enrol(auto[i]);
  }

  function enrol(el) {
    if (!isPlainText(el)) return;
    if (el.getAttribute('data-reveal-built') === 'true') return;
    el.setAttribute('data-reveal-built', 'true');
    var entry = build(el);
    if (!entry) return;
    entries.push(entry);
    if (typeof IntersectionObserver === 'function') {
      var io = new IntersectionObserver(
        function (records) {
          var r = records[0];
          entry.visible = r.isIntersecting;
          if (r.isIntersecting) update(entry);
        },
        { rootMargin: '200px' }
      );
      io.observe(el);
    } else {
      entry.visible = true;
      update(entry);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', collect);
  } else {
    collect();
  }

  window.addEventListener('scroll', onScroll, { passive: true });
  window.addEventListener('resize', onScroll);
})();
