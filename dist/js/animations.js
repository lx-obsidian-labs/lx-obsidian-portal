void (function () {
  'use strict';

  /* ==========================================
     HERO PARTICLES
     ========================================== */

  function initParticles() {
    var container = document.getElementById('particles');
    if (!container) return;

    var count = 35;

    for (var i = 0; i < count; i++) {
      var particle = document.createElement('div');
      particle.className = 'hero__particle';

      var x = Math.random() * 100;
      var y = Math.random() * 100;
      var size = Math.random() * 2 + 1;
      var delay = Math.random() * 8;
      var duration = Math.random() * 6 + 5;

      particle.style.left = x + '%';
      particle.style.top = y + '%';
      particle.style.width = size + 'px';
      particle.style.height = size + 'px';
      particle.style.animationDelay = delay + 's';
      particle.style.animationDuration = duration + 's';

      container.appendChild(particle);
    }
  }

  /* ==========================================
     COUNTER ANIMATION
     ========================================== */

  function initCounters() {
    var counters = document.querySelectorAll('.counter[data-target]');
    if (!counters.length) return;

    var observer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            var counter = entry.target;
            var target = parseInt(counter.getAttribute('data-target'), 10);
            var duration = 2200;
            var startTime = null;

            var suffix = counter.getAttribute('data-suffix') || '';

            function animate(timestamp) {
              if (!startTime) startTime = timestamp;
              var progress = Math.min((timestamp - startTime) / duration, 1);
              var eased = 1 - Math.pow(1 - progress, 3);
              var current = Math.floor(eased * target);
              counter.textContent = current + suffix;
              if (progress < 1) {
                requestAnimationFrame(animate);
              } else {
                counter.textContent = target + suffix;
              }
            }

            requestAnimationFrame(animate);
            observer.unobserve(counter);
          }
        });
      },
      { threshold: 0.3 }
    );

    counters.forEach(function (c) {
      observer.observe(c);
    });
  }

  /* ==========================================
     CALCULATOR
     ========================================== */

  function initCalculator() {
    var inputs = {
      type: document.getElementById('calcType'),
      team: document.getElementById('calcTeam'),
      timeline: document.getElementById('calcTimeline'),
      features: document.getElementById('calcFeatures'),
      ai: document.getElementById('calcAI')
    };

    var displays = {
      type: document.getElementById('calcTypeValue'),
      team: document.getElementById('calcTeamValue'),
      timeline: document.getElementById('calcTimelineValue'),
      features: document.getElementById('calcFeaturesValue'),
      ai: document.getElementById('calcAIValue'),
      total: document.getElementById('calcTotal')
    };

    var hasAll = true;
    for (var key in inputs) {
      if (!inputs[key]) { hasAll = false; break; }
    }
    for (var dk in displays) {
      if (!displays[dk]) { hasAll = false; break; }
    }
    if (!hasAll) return;

    var typeLabels = ['Website', 'Mobile App', 'Enterprise'];
    var featureLabels = ['Basic', 'Medium', 'Complex'];
    var aiLabels = ['No', 'Yes'];

    function calculate() {
      var type = parseInt(inputs.type.value, 10);
      var team = parseInt(inputs.team.value, 10);
      var timeline = parseInt(inputs.timeline.value, 10);
      var features = parseInt(inputs.features.value, 10);
      var ai = parseInt(inputs.ai.value, 10);

      displays.type.textContent = typeLabels[type - 1] || 'Website';
      displays.team.textContent = team;
      displays.timeline.textContent = timeline + ' months';
      displays.features.textContent = featureLabels[features - 1] || 'Basic';
      displays.ai.textContent = aiLabels[ai] || 'No';

      var typeMultiplier = [1, 1.3, 2.2];
      var featureMultiplier = [1, 1.6, 2.8];
      var aiCost = ai === 1 ? 15000 : 0;
      var baseRate = 3500;

      var total = baseRate * team * timeline * typeMultiplier[type - 1] * featureMultiplier[features - 1] + aiCost;
      displays.total.textContent = '$' + Math.round(total).toLocaleString();
    }

    for (var key in inputs) {
      inputs[key].addEventListener('input', calculate);
    }

    calculate();
  }

  /* ==========================================
     EXPANDABLE TIMELINE
     ========================================== */

  function initTimeline() {
    var steps = document.querySelectorAll('.timeline-step');

    steps.forEach(function (step) {
      var header = step.querySelector('.timeline-step__header');
      if (!header) return;

      header.addEventListener('click', function () {
        var isActive = step.classList.contains('timeline-step--active');

        steps.forEach(function (s) {
          s.classList.remove('timeline-step--active');
        });

        if (!isActive) {
          step.classList.add('timeline-step--active');
        }
      });
    });
  }

  /* ==========================================
     PORTFOLIO FILTERS
     ========================================== */

  function initPortfolioFilters() {
    var filters = document.querySelectorAll('.portfolio-filter');
    var items = document.querySelectorAll('.portfolio-item');

    if (!filters.length || !items.length) return;

    filters.forEach(function (filter) {
      filter.addEventListener('click', function () {
        filters.forEach(function (f) { f.classList.remove('portfolio-filter--active'); });
        this.classList.add('portfolio-filter--active');

        var value = this.getAttribute('data-filter') || 'all';

        items.forEach(function (item) {
          var categories = item.getAttribute('data-categories') || '';
          if (value === 'all' || categories.indexOf(value) !== -1) {
            item.style.display = '';
            item.classList.add('reveal--visible');
          } else {
            item.style.display = 'none';
          }
        });
      });
    });
  }

  /* ==========================================
     FAQ ACCORDION
     ========================================== */

  function initAccordion() {
    var items = document.querySelectorAll('.faq-item');

    items.forEach(function (item) {
      var trigger = item.querySelector('.faq-item__trigger');

      trigger.addEventListener('click', function () {
        var isOpen = item.classList.contains('faq-item--open');

        items.forEach(function (i) {
          i.classList.remove('faq-item--open');
          var btn = i.querySelector('.faq-item__trigger');
          if (btn) btn.setAttribute('aria-expanded', 'false');
        });

        if (!isOpen) {
          item.classList.add('faq-item--open');
          trigger.setAttribute('aria-expanded', 'true');
        }
      });
    });
  }

  /* ==========================================
     TESTIMONIALS CAROUSEL
     ========================================== */

  function initTestimonials() {
    var track = document.getElementById('testimonialsTrack');
    var prevBtn = document.getElementById('testimonialPrev');
    var nextBtn = document.getElementById('testimonialNext');
    var dots = document.querySelectorAll('.testimonials-dot');

    if (!track) return;

    var cardWidth = 360;
    var currentIndex = 0;
    var maxIndex = 0;

    function updateDots() {
      dots.forEach(function (dot, i) {
        dot.classList.toggle('testimonials-dot--active', i === currentIndex);
      });
    }

    function goTo(index) {
      var cards = track.querySelectorAll('.testimonial-card');
      maxIndex = Math.max(0, cards.length - 1);
      if (index < 0) index = 0;
      if (index > maxIndex) index = maxIndex;
      currentIndex = index;
      track.scrollTo({ left: currentIndex * cardWidth, behavior: 'smooth' });
      updateDots();
    }

    if (prevBtn) {
      prevBtn.addEventListener('click', function () { goTo(currentIndex - 1); });
    }

    if (nextBtn) {
      nextBtn.addEventListener('click', function () { goTo(currentIndex + 1); });
    }

    dots.forEach(function (dot) {
      dot.addEventListener('click', function () {
        goTo(parseInt(this.getAttribute('data-index'), 10));
      });
    });

    updateDots();

    var isHovering = false;
    track.addEventListener('mouseenter', function () { isHovering = true; });
    track.addEventListener('mouseleave', function () { isHovering = false; });

    setInterval(function () {
      if (!isHovering) {
        goTo(currentIndex + 1);
        if (currentIndex >= maxIndex) {
          currentIndex = -1;
        }
      }
    }, 5000);
  }

  /* ==========================================
     SEARCH
     ========================================== */

  function initSearch() {
    var input = document.getElementById('searchInput');
    var results = document.getElementById('searchResults');

    if (!input || !results) return;

    var data = [
      { title: 'Website Development', type: 'Service', url: '#' },
      { title: 'Mobile App Development', type: 'Service', url: '#' },
      { title: 'AI Solutions', type: 'Service', url: '#' },
      { title: 'Enterprise Software', type: 'Service', url: '#' },
      { title: 'OptiScan', type: 'Application', url: '#' },
      { title: 'College Manager', type: 'Application', url: '#' },
      { title: 'BookMe Rivet', type: 'Application', url: '#' },
      { title: 'Inventory Pro', type: 'Application', url: '#' },
      { title: 'CRM Mobile', type: 'Application', url: '#' },
      { title: 'How Much Does Software Cost?', type: 'Article', url: '#' },
      { title: 'Choosing Between Website and Mobile App', type: 'Article', url: '#' },
      { title: 'Artificial Intelligence for Business', type: 'Article', url: '#' },
      { title: 'Digital Transformation Guide', type: 'Article', url: '#' },
      { title: 'How much does a website cost?', type: 'FAQ', url: '#faq' },
      { title: 'How long does development take?', type: 'FAQ', url: '#faq' },
      { title: 'Portfolio Showcase', type: 'Project', url: '#portfolio' },
    ];

    input.addEventListener('input', function () {
      var query = this.value.toLowerCase().trim();

      if (query.length < 1) {
        results.innerHTML = '<div class="search-result-item" style="border:none;color:var(--color-text-muted);font-size:var(--text-sm);padding:2rem;text-align:center;">Type to search services, applications, articles...</div>';
        return;
      }

      var matches = data.filter(function (item) {
        return item.title.toLowerCase().indexOf(query) !== -1;
      });

      if (matches.length === 0) {
        results.innerHTML = '<div class="search-result-item" style="border:none;color:var(--color-text-muted);font-size:var(--text-sm);padding:2rem;text-align:center;">No results found for "' + query + '"</div>';
        return;
      }

      results.innerHTML = '';
      matches.forEach(function (item) {
        var el = document.createElement('a');
        el.className = 'search-result-item';
        el.href = item.url;
        el.innerHTML =
          '<div class="search-result-item__type">' + item.type + '</div>' +
          '<div class="search-result-item__title">' + item.title + '</div>';
        results.appendChild(el);
      });
    });
  }

  /* ==========================================
     NEWSLETTER
     ========================================== */

  function initNewsletter() {
    var forms = document.querySelectorAll('.newsletter-form');
    forms.forEach(function (form) {
      form.addEventListener('submit', function (e) {
        e.preventDefault();
        var input = this.querySelector('.newsletter-input');
        if (input && input.value.trim()) {
          var btn = this.querySelector('.newsletter-submit');
          var originalText = btn.textContent;
          btn.textContent = 'Subscribed!';
          btn.disabled = true;
          input.value = '';
          setTimeout(function () {
            btn.textContent = originalText;
            btn.disabled = false;
          }, 2000);
        }
      });
    });
  }

  /* ==========================================
     INIT ALL
     ========================================== */

  function init() {
    initParticles();
    initCounters();
    initCalculator();
    initTimeline();
    initPortfolioFilters();
    initAccordion();
    initTestimonials();
    initSearch();
    initNewsletter();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
