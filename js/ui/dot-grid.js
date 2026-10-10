/**
 * Dot Grid Background — vanilla port of the Framer "Dot Grid BG" component.
 *
 * Renders a fixed, full-viewport canvas behind all page content. Dots sit on a
 * regular grid; dots near the cursor brighten, grow and drift along a tilted 3D
 * orbit ring, then settle back once the pointer leaves.
 *
 * Density / brightness / feel are driven by CSS custom properties (see lx.css):
 *   --dot-color  --dot-opacity  --dot-size  --dot-spacing  --dot-impact  --dot-scale
 *
 * Resting dots are batched into a single path + fill per frame; only the dots
 * inside the impact radius are drawn individually, which keeps a dense grid
 * cheap. Exposes `window.veloraDotGrid` for inspection.
 */
void (function () {
  'use strict';

  var CFG = {
    dotColor: '#C0C0C0',
    baseAlpha: 0.3,
    dotSize: 2.5,
    dotSpacing: 22,
    orbitSpeed: 1.5,
    impactRadius: 110,
    scaleOnHover: 1.9,
    enableRevolve: true,
  };

  var TAU = Math.PI * 2;

  var reduceMotion =
    window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function readCssConfig() {
    if (!window.getComputedStyle) return;
    var cs = window.getComputedStyle(document.documentElement);
    if (!cs || !cs.getPropertyValue) return;
    var num = function (name, fallback) {
      var v = parseFloat(cs.getPropertyValue(name));
      return isFinite(v) ? v : fallback;
    };
    var str = function (name, fallback) {
      var v = (cs.getPropertyValue(name) || '').trim();
      return v || fallback;
    };
    CFG.dotColor = str('--dot-color', CFG.dotColor);
    CFG.baseAlpha = num('--dot-opacity', CFG.baseAlpha);
    CFG.dotSize = num('--dot-size', CFG.dotSize);
    CFG.dotSpacing = Math.max(6, num('--dot-spacing', CFG.dotSpacing));
    CFG.impactRadius = num('--dot-impact', CFG.impactRadius);
    CFG.scaleOnHover = num('--dot-scale', CFG.scaleOnHover);
  }

  var canvas = document.querySelector('.dot-grid-bg');
  if (!canvas) {
    canvas = document.createElement('canvas');
    canvas.className = 'dot-grid-bg';
    canvas.setAttribute('aria-hidden', 'true');
    document.body.appendChild(canvas);
  }

  var ctx = canvas.getContext('2d');
  if (!ctx) return;

  readCssConfig();

  var dpr = Math.min(window.devicePixelRatio || 1, 2);
  var W = 0;
  var H = 0;
  var dots = [];
  var affected = [];
  var mouse = { x: -9999, y: -9999 };
  var hovering = false;
  var leaveTs = 0;
  var prevTs = 0;
  var globalAngle = 0;
  var raf = 0;

  function smoothstep(t) {
    var c = Math.max(0, Math.min(1, t));
    return c * c * (3 - 2 * c);
  }

  function parseColor(raw) {
    var color = String(raw || '').trim();
    if (color.indexOf('rgb') === 0) {
      var m = color.match(/[\d.]+/g) || [];
      return { r: Number(m[0]) || 0, g: Number(m[1]) || 0, b: Number(m[2]) || 0 };
    }
    var hex = color.replace('#', '');
    if (hex.length === 3) {
      hex = hex.split('').map(function (c) { return c + c; }).join('');
    }
    var n = parseInt(hex.slice(0, 6), 16);
    if (isNaN(n)) return { r: 136, g: 136, b: 136 };
    return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
  }

  function rgba(c, a) {
    return 'rgba(' + c.r + ',' + c.g + ',' + c.b + ',' + a + ')';
  }

  function buildDots() {
    dots = [];
    var sp = CFG.dotSpacing;
    var cols = Math.ceil(W / sp) + 2;
    var rows = Math.ceil(H / sp) + 2;
    for (var r = 0; r < rows; r++) {
      for (var c = 0; c < cols; c++) {
        dots.push({
          bx: c * sp,
          by: r * sp,
          inclination: Math.random() * Math.PI,
          ascension: Math.random() * Math.PI * 2,
          phase: Math.random() * Math.PI * 2,
          speedMult: 0.7 + Math.random() * 0.6,
        });
      }
    }
  }

  function resize() {
    var rect = canvas.getBoundingClientRect();
    W = rect.width;
    H = rect.height;
    canvas.width = Math.max(1, Math.round(W * dpr));
    canvas.height = Math.max(1, Math.round(H * dpr));
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    buildDots();
    if (reduceMotion) drawStatic();
  }

  function setMouse(clientX, clientY) {
    var rect = canvas.getBoundingClientRect();
    mouse.x = clientX - rect.left;
    mouse.y = clientY - rect.top;
  }

  function drawStatic() {
    var rgb = parseColor(CFG.dotColor);
    var r = CFG.dotSize / 2;
    ctx.clearRect(0, 0, W, H);
    ctx.beginPath();
    for (var i = 0; i < dots.length; i++) {
      ctx.moveTo(dots[i].bx + r, dots[i].by);
      ctx.arc(dots[i].bx, dots[i].by, r, 0, TAU);
    }
    ctx.fillStyle = rgba(rgb, CFG.baseAlpha);
    ctx.fill();
  }

  function loop(ts) {
    raf = requestAnimationFrame(loop);
    var dt = Math.min((ts - (prevTs || ts)) / 1000, 0.05);
    prevTs = ts;
    globalAngle += CFG.orbitSpeed * dt;

    var rgb = parseColor(CFG.dotColor);
    ctx.clearRect(0, 0, W, H);

    var timeSinceLeave = hovering ? 0 : Math.max(0, ts - leaveTs) / 1000;
    var decay = hovering ? 1 : smoothstep(Math.max(0, 1 - timeSinceLeave * 1.5));
    var mx = mouse.x;
    var my = mouse.y;
    var impact = CFG.impactRadius;
    var baseR = CFG.dotSize / 2;
    var hoverScale = CFG.scaleOnHover;

    // Pass 1 — every dot at rest, in one path + one fill.
    ctx.beginPath();
    affected.length = 0;
    for (var i = 0; i < dots.length; i++) {
      var d = dots[i];
      var dx = d.bx - mx;
      var dy = d.by - my;
      if (dx * dx + dy * dy < impact * impact) {
        affected.push(i);
        continue;
      }
      ctx.moveTo(d.bx + baseR, d.by);
      ctx.arc(d.bx, d.by, baseR, 0, TAU);
    }
    ctx.fillStyle = rgba(rgb, CFG.baseAlpha);
    ctx.fill();

    // Pass 2 — only dots inside the impact radius, drawn individually.
    for (var k = 0; k < affected.length; k++) {
      var dot = dots[affected[k]];
      var ax = dot.bx - mx;
      var ay = dot.by - my;
      var dist = Math.sqrt(ax * ax + ay * ay);
      if (dist <= 0) continue;

      var t = dist / impact;
      var inf = smoothstep(1 - t) * decay;
      var x = dot.bx;
      var y = dot.by;
      var scale = 1;
      var alpha = CFG.baseAlpha;

      if (inf > 0.001) {
        if (CFG.enableRevolve) {
          var orbitR = (1 - t) * CFG.dotSpacing * 0.7 * inf;
          var theta = globalAngle * dot.speedMult + dot.phase;
          var cosA = Math.cos(dot.ascension);
          var sinA = Math.sin(dot.ascension);
          var cosI = Math.cos(dot.inclination);
          var sinI = Math.sin(dot.inclination);
          var lx = Math.cos(theta);
          var ly = Math.sin(theta) * cosI;
          var lz = Math.sin(theta) * sinI;
          x = dot.bx + (lx * cosA - ly * sinA) * orbitR;
          y = dot.by + (lx * sinA + ly * cosA) * orbitR;
          var depthScale = 0.75 + 0.25 * ((lz + 1) * 0.5);
          scale = (1 + (hoverScale - 1) * inf) * depthScale;
          alpha = (CFG.baseAlpha + (1 - CFG.baseAlpha) * inf) * depthScale;
        } else {
          scale = 1 + (hoverScale - 1) * inf;
          alpha = CFG.baseAlpha + (1 - CFG.baseAlpha) * inf;
        }
      }

      ctx.beginPath();
      ctx.arc(x, y, baseR * scale, 0, TAU);
      ctx.fillStyle = rgba(rgb, alpha);
      ctx.fill();
    }
  }

  window.addEventListener('mousemove', function (e) {
    setMouse(e.clientX, e.clientY);
  }, { passive: true });

  window.addEventListener('mouseover', function (e) {
    setMouse(e.clientX, e.clientY);
    hovering = true;
  }, { passive: true });

  document.addEventListener('mouseleave', function () {
    mouse.x = -9999;
    mouse.y = -9999;
    hovering = false;
    leaveTs = performance.now();
  });

  if (typeof ResizeObserver === 'function') {
    new ResizeObserver(resize).observe(canvas);
  } else {
    window.addEventListener('resize', resize);
  }

  window.veloraDotGrid = {
    ready: true,
    reducedMotion: reduceMotion,
    count: function () {
      return dots.length;
    },
    config: function () {
      return {
        color: CFG.dotColor,
        baseAlpha: CFG.baseAlpha,
        size: CFG.dotSize,
        spacing: CFG.dotSpacing,
        impact: CFG.impactRadius,
        scale: CFG.scaleOnHover,
        orbitSpeed: CFG.orbitSpeed,
      };
    },
    canvas: function () {
      return { width: canvas.width, height: canvas.height, cssWidth: W, cssHeight: H };
    },
  };

  resize();
  if (!reduceMotion) raf = requestAnimationFrame(loop);
})();
