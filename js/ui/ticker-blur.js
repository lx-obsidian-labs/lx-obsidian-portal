/**
 * Blurred Text Ticker — vanilla port of the Framer "Ticker Blurred Text".
 *
 * A vertical ticker showing one word at a time: the active word is sharp, the
 * neighbours are blurred, stroked and grained, and the rest fade out, inside a
 * 3-row window masked top and bottom.
 *
 * Declarative usage:
 *   <div class="ticker-blur"
 *        data-texts="Automate|Integrate|Scale|Ship"
 *        data-speed="1600"
 *        data-blur="2"
 *        data-dissolve="0.4"
 *        data-active-color="#FDFDFD"
 *        data-inactive-color="#9A9A9A"></div>
 *
 * Tune size with CSS: --tb-font-size / --tb-font-size-md / --tb-font-size-sm.
 * Exposes window.veloraTickerBlur for inspection.
 */
void (function () {
  'use strict';

  var REPEAT = 40;

  var reduceMotion =
    window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function parseList(raw) {
    return String(raw || '')
      .split('|')
      .map(function (t) { return t.trim(); })
      .filter(Boolean);
  }

  function num(el, name, fallback) {
    var v = parseFloat(el.getAttribute(name));
    return isFinite(v) ? v : fallback;
  }

  function currentFontSize(el) {
    var cs = getComputedStyle(el);
    var v = parseFloat(cs.getPropertyValue('--tb-font-size'));
    if (window.innerWidth <= 640) {
      var sm = parseFloat(cs.getPropertyValue('--tb-font-size-sm'));
      if (isFinite(sm)) return sm;
    } else if (window.innerWidth <= 1024) {
      var md = parseFloat(cs.getPropertyValue('--tb-font-size-md'));
      if (isFinite(md)) return md;
    }
    return isFinite(v) ? v : 44;
  }

  function mount(root) {
    if (root.getAttribute('data-tb-built') === 'true') return null;

    var texts = parseList(root.getAttribute('data-texts'));
    if (!texts.length) return null;

    var speed = Math.max(200, num(root, 'data-speed', 1600));
    var blur = num(root, 'data-blur', 2);
    var dissolve = Math.max(0, Math.min(1, num(root, 'data-dissolve', 0.4)));
    var gap = Math.max(0, num(root, 'data-gap', 0));
    var activeColor = root.getAttribute('data-active-color') || 'var(--saas-text)';
    var inactiveColor = root.getAttribute('data-inactive-color') || 'var(--saas-muted)';

    root.setAttribute('data-tb-built', 'true');

    var fontSize = currentFontSize(root);
    var rowHeight = fontSize * 1.2;
    var step = rowHeight + gap;

    var window_ = document.createElement('div');
    window_.className = 'tb-window';
    window_.style.height = rowHeight * 3 + gap * 2 + 'px';

    var track = document.createElement('div');
    track.className = 'tb-track';

    var total = texts.length * REPEAT;
    var baseOffset = texts.length * Math.floor(REPEAT / 2);
    var index = baseOffset;

    var rows = [];
    for (var r = 0; r < total; r++) {
      var text = texts[r % texts.length];
      var row = document.createElement('div');
      row.className = 'tb-row';
      row.style.height = rowHeight + 'px';
      row.style.marginBottom = gap + 'px';
      row.style.fontSize = fontSize + 'px';

      var label = document.createElement('span');
      label.className = 'tb-label';
      label.textContent = text;
      row.appendChild(label);

      // Neighbour dressing: stroked ghost + film-grain fill.
      var ghost = document.createElement('span');
      ghost.className = 'tb-ghost';
      ghost.setAttribute('aria-hidden', 'true');
      ghost.textContent = text;
      row.appendChild(ghost);

      var grain = document.createElement('span');
      grain.className = 'tb-grain';
      grain.setAttribute('aria-hidden', 'true');
      grain.textContent = text;
      row.appendChild(grain);

      track.appendChild(row);
      rows.push({ el: row, ghost: ghost, grain: grain });
    }

    window_.appendChild(track);
    root.appendChild(window_);

    var timer = null;

    function paint() {
      var active = index;
      for (var i = 0; i < rows.length; i++) {
        var row = rows[i];
        var rel = i - active;
        var distance = Math.abs(rel);
        var isActive = distance === 0;
        var adjacent = texts.length > 2 ? distance === 1 : texts.length === 2 ? rel === 1 : false;
        var opacity = isActive ? 1 : adjacent ? 0.15 + dissolve * 0.75 : 0;
        var filterValue = isActive
          ? 'none'
          : 'blur(' + (adjacent ? blur : blur * 1.2) + 'px)';
        var layerShift = (1 - dissolve) * 3 + distance;

        row.el.style.opacity = String(opacity);
        row.el.style.filter = filterValue;
        row.el.style.color = isActive ? activeColor : inactiveColor;
        row.el.style.textShadow = isActive
          ? 'none'
          : '0 0 ' + (2 + dissolve * 5) + 'px ' + inactiveColor;

        var showDressing = !isActive && adjacent;
        row.ghost.style.display = showDressing ? 'flex' : 'none';
        row.grain.style.display = showDressing ? 'flex' : 'none';
        row.ghost.style.webkitTextStroke = '0.6px ' + inactiveColor;
        row.ghost.style.opacity = showDressing ? String(0.12 + dissolve * 0.22) : '0';
        row.ghost.style.setProperty('--tb-shift', layerShift + 'px');
        row.grain.style.setProperty('--tb-grain', inactiveColor);
        row.grain.style.opacity = showDressing ? String(0.18 + dissolve * 0.5) : '0';
      }
      track.style.transform = 'translateY(' + -(active * step - step) + 'px)';
    }

    function advance() {
      index += 1;
      paint();
      // Wrap without a visible jump once we drift too far into the repeat.
      if (index > texts.length * (REPEAT - 3)) {
        track.classList.add('tb-track--snap');
        index = baseOffset + (index % texts.length);
        paint();
        // force reflow so the snap is not animated
        void track.offsetHeight;
        requestAnimationFrame(function () {
          track.classList.remove('tb-track--snap');
        });
      }
    }

    paint();

    function start() {
      if (reduceMotion || texts.length <= 1) return;
      timer = window.setInterval(advance, speed);
    }

    function onResize() {
      var size = currentFontSize(root);
      if (size === fontSize) return;
      fontSize = size;
      rowHeight = fontSize * 1.2;
      step = rowHeight + gap;
      window_.style.height = rowHeight * 3 + gap * 2 + 'px';
      for (var i = 0; i < rows.length; i++) {
        rows[i].el.style.height = rowHeight + 'px';
        rows[i].el.style.fontSize = fontSize + 'px';
      }
      paint();
    }

    start();
    window.addEventListener('resize', onResize);

    return {
      el: root,
      texts: texts,
      advance: advance,
      stop: function () { if (timer) window.clearInterval(timer); },
      state: function () {
        return {
          texts: texts.length,
          index: index,
          baseOffset: baseOffset,
          step: step,
          fontSize: fontSize,
          speed: speed,
          blur: blur,
          dissolve: dissolve,
          reducedMotion: reduceMotion,
        };
      },
    };
  }

  function init() {
    var nodes = document.querySelectorAll('.ticker-blur');
    var instances = [];
    for (var i = 0; i < nodes.length; i++) {
      var inst = mount(nodes[i]);
      if (inst) instances.push(inst);
    }
    window.veloraTickerBlur = {
      instances: instances,
      count: function () { return instances.length; },
      state: function () { return instances.length ? instances[0].state() : null; },
    };
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
