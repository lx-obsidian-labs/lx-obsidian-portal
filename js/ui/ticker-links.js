/**
 * Interactive Ticker Link — vanilla port of the Framer component.
 *
 * A seamlessly looping horizontal marquee of link cards with edge fade masks.
 * The script duplicates the track once so a `translateX(-100%)` CSS loop is
 * continuous. Hovering or focusing pauses the marquee and lifts the hovered card;
 * cards with a `data-image` reveal a floating preview.
 *
 * Declarative usage (write real anchors once; the script adds the clone):
 *   <div class="ticker-links" data-speed="38">
 *     <div class="ticker-links__track">
 *       <a class="ticker-link" href="ai.html" data-image="assets/...png">
 *         <span class="ticker-link__media">…</span>
 *         <span class="ticker-link__title">AI Platform</span>
 *         <span class="ticker-link__desc">Autonomous AI systems</span>
 *       </a>
 *       …
 *     </div>
 *   </div>
 *
 * `data-speed` = seconds for one full loop (default 38). Exposes
 * window.veloraTickerLinks.
 */
void (function () {
  'use strict';

  var reduceMotion =
    window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function mount(root) {
    if (root.getAttribute('data-tl-built') === 'true') return null;

    var track = root.querySelector('.ticker-links__track');
    if (!track) return null;

    var cards = track.querySelectorAll('.ticker-link');
    if (!cards.length) return null;

    root.setAttribute('data-tl-built', 'true');

    var speed = parseFloat(root.getAttribute('data-speed'));
    if (!isFinite(speed) || speed <= 0) speed = 38;

    if (reduceMotion || cards.length < 2) {
      root.classList.add('is-static');
      return { el: root, cards: cards.length, speed: speed, looping: false, reducedMotion: reduceMotion };
    }

    // One duplicate set makes the -100% CSS loop seamless.
    var clone = track.cloneNode(true);
    clone.setAttribute('aria-hidden', 'true');
    var clonedCards = clone.querySelectorAll('.ticker-link');
    for (var i = 0; i < clonedCards.length; i++) {
      clonedCards[i].setAttribute('tabindex', '-1');
      clonedCards[i].setAttribute('aria-hidden', 'true');
    }
    root.appendChild(clone);
    root.style.setProperty('--tl-duration', speed + 's');

    return {
      el: root,
      cards: cards.length,
      speed: speed,
      looping: true,
      reducedMotion: false,
    };
  }

  function init() {
    var nodes = document.querySelectorAll('.ticker-links');
    var instances = [];
    for (var i = 0; i < nodes.length; i++) {
      var inst = mount(nodes[i]);
      if (inst) instances.push(inst);
    }
    window.veloraTickerLinks = {
      instances: instances,
      count: function () { return instances.length; },
      state: function () {
        return instances.length
          ? {
              cards: instances[0].cards,
              speed: instances[0].speed,
              looping: instances[0].looping,
              reducedMotion: instances[0].reducedMotion,
            }
          : null;
      },
    };
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
