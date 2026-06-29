void (function () {
  'use strict';

  /* ==========================================
     THEME TOGGLE
     ========================================== */

  function initThemeToggle() {
    var toggle = document.getElementById('themeToggle');
    if (!toggle) return;

    var saved = localStorage.getItem('lx-theme');
    if (saved === 'light') {
      document.documentElement.setAttribute('data-theme', 'light');
    }

    toggle.addEventListener('click', function () {
      var html = document.documentElement;
      var isLight = html.getAttribute('data-theme') === 'light';
      if (isLight) {
        html.removeAttribute('data-theme');
        localStorage.setItem('lx-theme', 'dark');
      } else {
        html.setAttribute('data-theme', 'light');
        localStorage.setItem('lx-theme', 'light');
      }
    });
  }

  /* ==========================================
     CHAT WIDGET
     ========================================== */

  function initChatWidget() {
    var chatWidget = document.getElementById('chatWidget');
    var chatInput = document.getElementById('chatInput');
    var chatSend = document.getElementById('chatSend');
    var chatClose = document.getElementById('chatClose');
    var chatMessages = document.getElementById('chatMessages');
    var chatFloatingBtn = document.querySelector('.floating-btn--chat');

    if (!chatWidget) return;

    function toggleChat() {
      chatWidget.classList.toggle('chat-widget--open');
    }

    function addMessage(text, type) {
      var msg = document.createElement('div');
      msg.className = 'chat-message chat-message--' + type;
      msg.textContent = text;
      chatMessages.appendChild(msg);
      chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function addTypingIndicator() {
      var indicator = document.createElement('div');
      indicator.className = 'chat-message chat-message--bot chat-message--typing';
      indicator.id = 'chatTyping';
      indicator.textContent = '...';
      chatMessages.appendChild(indicator);
      chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function removeTypingIndicator() {
      var indicator = document.getElementById('chatTyping');
      if (indicator) indicator.remove();
    }

    function handleSend() {
      var text = chatInput.value.trim();
      if (!text) return;

      addMessage(text, 'user');
      chatInput.value = '';
      addTypingIndicator();

      var xhr = new XMLHttpRequest();
      xhr.open('POST', '/api/chat', true);
      xhr.setRequestHeader('Content-Type', 'application/json');

      xhr.onload = function () {
        removeTypingIndicator();
        if (xhr.status === 200) {
          try {
            var data = JSON.parse(xhr.responseText);
            addMessage(data.reply || 'Thanks for your message! Our team will follow up shortly.', 'bot');
          } catch (e) {
            addMessage('Thanks for your message! Our team will follow up shortly.', 'bot');
          }
        } else {
          addMessage('I apologize, but I\'m having trouble connecting right now. Please try again or contact our team directly.', 'bot');
        }
      };

      xhr.onerror = function () {
        removeTypingIndicator();
        addMessage('I apologize, but I\'m having trouble connecting right now. Please try again or contact our team directly.', 'bot');
      };

      xhr.send(JSON.stringify({ message: text }));
    }

    if (chatFloatingBtn) {
      chatFloatingBtn.addEventListener('click', toggleChat);
    }

    if (chatClose) {
      chatClose.addEventListener('click', toggleChat);
    }

    if (chatSend) {
      chatSend.addEventListener('click', handleSend);
    }

    if (chatInput) {
      chatInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') handleSend();
      });
    }
  }

  /* ==========================================
     CONTACT FORM VALIDATION
     ========================================== */

  function initContactForm() {
    var form = document.getElementById('contactForm');
    if (!form) return;

    var fields = [
      { id: 'contactName', error: 'contactNameError', validate: function (v) { return v.trim().length > 0; } },
      { id: 'contactEmail', error: 'contactEmailError', validate: function (v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim()); } },
      { id: 'contactSubject', error: 'contactSubjectError', validate: function (v) { return v.trim().length > 0; } },
      { id: 'contactMessage', error: 'contactMessageError', validate: function (v) { return v.trim().length > 0; } }
    ];

    var successEl = document.getElementById('contactSuccess');

    function validateField(field) {
      var input = document.getElementById(field.id);
      var error = document.getElementById(field.error);
      var isValid = field.validate(input.value);
      input.classList.toggle('contact-form__input--error', !isValid);
      error.classList.toggle('contact-form__error--visible', !isValid);
      return isValid;
    }

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var allValid = true;
      fields.forEach(function (f) {
        if (!validateField(f)) allValid = false;
      });
      if (!allValid) return;

      var btn = form.querySelector('button[type="submit"]');
      var originalText = btn.textContent;
      btn.textContent = 'Sending...';
      btn.disabled = true;

      var data = {};
      fields.forEach(function (f) { data[f.id.replace('contact', '').toLowerCase()] = document.getElementById(f.id).value; });

      fetch('/api/contact', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      }).then(function (res) {
        if (!res.ok) throw new Error('Failed');
        form.style.display = 'none';
        successEl.classList.add('contact-form__success--visible');
      }).catch(function () {
        btn.textContent = originalText;
        btn.disabled = false;
        if (window.LXToast) { window.LXToast('Failed to send. Please try again.', 'error'); }
      });
    });

    fields.forEach(function (f) {
      var input = document.getElementById(f.id);
      input.addEventListener('blur', function () { validateField(f); });
      input.addEventListener('input', function () {
        input.classList.remove('contact-form__input--error');
        document.getElementById(f.error).classList.remove('contact-form__error--visible');
      });
    });
  }

  /* ==========================================
     COOKIE CONSENT
     ========================================== */

  function initCookieConsent() {
    var banner = document.getElementById('cookieConsent');
    var accept = document.getElementById('cookieAccept');
    var decline = document.getElementById('cookieDecline');
    if (!banner) return;

    if (localStorage.getItem('lx-cookie')) return;

    banner.classList.add('cookie-consent--visible');

    function dismiss() {
      banner.classList.remove('cookie-consent--visible');
      localStorage.setItem('lx-cookie', 'true');
    }

    if (accept) accept.addEventListener('click', dismiss);
    if (decline) decline.addEventListener('click', dismiss);
  }

  /* ==========================================
     DASHBOARD PROGRESS ANIMATION
     ========================================== */

  function initDashboard() {
    var fills = document.querySelectorAll('.dashboard-progress__fill');
    if (!fills.length) return;

    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          var fill = entry.target;
          var w = fill.style.width;
          fill.style.width = '0%';
          setTimeout(function () { fill.style.width = w; }, 200);
          observer.unobserve(fill);
        }
      });
    }, { threshold: 0.3 });

    fills.forEach(function (f) { observer.observe(f); });
  }

  /* ==========================================
     BLOG MODAL
     ========================================== */

  function initBlogModal() {
    var overlay = document.getElementById('blogOverlay');
    var closeBtn = document.getElementById('blogModalClose');
    var tagEl = document.getElementById('blogModalTag');
    var titleEl = document.getElementById('blogModalTitle');
    var bodyEl = document.getElementById('blogModalBody');
    var authorEl = document.getElementById('blogModalAuthor');
    var dateEl = document.getElementById('blogModalDate');

    if (!overlay) return;

    var articles = {
      'How Much Does Software Cost?': {
        tag: 'Business',
        content: '<p>Understanding software costs is crucial for budgeting your digital transformation. The answer depends on several key factors including complexity, team size, timeline, and technology choices.</p><h3>Key Cost Factors</h3><ul><li><strong>Scope & Complexity:</strong> A simple landing page costs significantly less than a full enterprise CRM system.</li><li><strong>Team Composition:</strong> Senior engineers command higher rates but deliver faster, higher-quality results.</li><li><strong>Timeline:</strong> Rushed timelines require more resources working in parallel, increasing costs.</li><li><strong>Technology Stack:</strong> Choice of languages, frameworks, and infrastructure affects both development and ongoing costs.</li></ul><h3>Average Investment Ranges</h3><p>Basic websites typically range from $5,000 to $15,000. Mobile apps average $20,000 to $80,000 depending on features. Enterprise systems can range from $50,000 to $500,000+ for complex implementations.</p><p>At LX Obsidian Labs, we provide transparent, detailed quotes during our discovery phase — no surprises, no hidden fees.</p>',
        author: 'Kairo Vilane',
        date: 'June 24, 2026'
      },
      'Artificial Intelligence for Business': {
        tag: 'AI',
        content: '<p>Artificial Intelligence is no longer a futuristic concept — it is a practical tool that businesses of all sizes can leverage to gain competitive advantages, automate operations, and deliver personalized experiences.</p><h3>Practical AI Applications</h3><ul><li><strong>Customer Service:</strong> AI-powered chatbots and virtual assistants that handle 80% of routine inquiries.</li><li><strong>Data Analytics:</strong> Machine learning models that uncover patterns and insights humans might miss.</li><li><strong>Process Automation:</strong> Intelligent automation of repetitive tasks, freeing teams for strategic work.</li><li><strong>Personalization:</strong> Recommendation engines that tailor experiences to individual users.</li></ul><h3>Getting Started with AI</h3><p>Start small. Identify a specific business problem, gather quality data, and build a focused solution. Our team helps businesses navigate the AI landscape — from strategy to deployment.</p>',
        author: 'Sarah Tshabalala',
        date: 'June 20, 2026'
      },
      'Digital Transformation Guide': {
        tag: 'Strategy',
        content: '<p>Digital transformation is the integration of digital technology into all areas of a business, fundamentally changing how you operate and deliver value to customers.</p><h3>The Transformation Framework</h3><ul><li><strong>Audit:</strong> Assess your current technology stack, processes, and digital maturity.</li><li><strong>Strategy:</strong> Define clear goals, KPIs, and a roadmap aligned with business objectives.</li><li><strong>Implementation:</strong> Execute in phases with agile methodology, delivering value at every step.</li><li><strong>Optimization:</strong> Continuously measure, learn, and improve based on real data.</li></ul><h3>Why Transform?</h3><p>Companies that embrace digital transformation see 26% higher profitability, 12x higher revenue growth, and significantly improved customer satisfaction scores. The cost of inaction far exceeds the investment required.</p>',
        author: 'Marcus James',
        date: 'June 16, 2026'
      },
      'Cloud Computing Explained': {
        tag: 'Cloud',
        content: '<p>Cloud computing delivers computing services — servers, storage, databases, networking, software — over the internet, offering faster innovation, flexible resources, and economies of scale.</p><h3>Cloud Service Models</h3><ul><li><strong>IaaS:</strong> Virtualized computing resources over the internet (AWS EC2, Google Compute Engine).</li><li><strong>PaaS:</strong> Platform allowing customers to develop, run, and manage applications (Heroku, Google App Engine).</li><li><strong>SaaS:</strong> Software applications over the internet (Salesforce, Microsoft 365).</li></ul><h3>Benefits for Business</h3><p>Cloud computing reduces capital expenditure, provides infinite scalability, improves disaster recovery, and enables remote collaboration. At LX Obsidian, we design cloud-native architectures that maximize these benefits.</p>',
        author: 'Amina Ndlovu',
        date: 'June 12, 2026'
      },
      'Choosing Between Website and Mobile App': {
        tag: 'Development',
        content: '<p>One of the most common questions we hear is whether to build a website, a mobile app, or both. The answer depends on your users, your goals, and your budget.</p><h3>When to Build a Website</h3><p>Websites are ideal for content delivery, SEO visibility, broad accessibility, and lower initial investment. Progressive Web Apps (PWAs) bridge the gap between web and native experiences.</p><h3>When to Build a Mobile App</h3><p>Mobile apps excel at leveraging device features (camera, GPS, notifications), providing offline functionality, and delivering rich, immersive experiences. They build deeper user engagement.</h3><h3>Our Recommendation</h3><p>Start with a responsive web presence, validate your product-market fit, then invest in native mobile apps for your most engaged users. We build both, seamlessly integrated.</p>',
        author: 'Kairo Vilane',
        date: 'June 8, 2026'
      },
      'SEO Basics': {
        tag: 'Marketing',
        content: '<p>Search Engine Optimization (SEO) is the practice of increasing the quantity and quality of traffic to your website through organic search engine results.</p><h3>Core SEO Principles</h3><ul><li><strong>Technical SEO:</strong> Ensure your site is crawlable, fast, and mobile-friendly.</li><li><strong>On-Page SEO:</strong> Optimize content, headers, meta descriptions, and image alt text.</li><li><strong>Off-Page SEO:</strong> Build quality backlinks and social signals.</li><li><strong>Content:</strong> Create valuable, relevant content that answers user questions.</li></ul><h3>Quick Wins</h3><p>Start with keyword research, optimize your page titles and meta descriptions, improve page load speed, and ensure your site is mobile-responsive. These foundational steps deliver immediate improvements to search rankings.</p>',
        author: 'Marcus James',
        date: 'June 4, 2026'
      }
    };

    function openArticle(title) {
      var article = articles[title];
      if (!article) return;
      tagEl.textContent = article.tag;
      titleEl.textContent = title;
      bodyEl.innerHTML = article.content;
      authorEl.textContent = article.author;
      dateEl.textContent = article.date;
      overlay.classList.add('blog-overlay--open');
      document.body.style.overflow = 'hidden';
    }

    function closeArticle() {
      overlay.classList.remove('blog-overlay--open');
      document.body.style.overflow = '';
    }

    document.querySelectorAll('.knowledge-card__link').forEach(function (link) {
      link.addEventListener('click', function (e) {
        e.preventDefault();
        var card = this.closest('.knowledge-card');
        var title = card ? card.querySelector('.knowledge-card__title').textContent : '';
        if (articles[title]) {
          openArticle(title);
        }
      });
    });

    if (closeBtn) closeBtn.addEventListener('click', closeArticle);

    overlay.addEventListener('click', function (e) {
      if (e.target === overlay) closeArticle();
    });

    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && overlay.classList.contains('blog-overlay--open')) {
        closeArticle();
      }
    });
  }

  /* ==========================================
     TYPING EFFECT
     ========================================== */

  function initTypingEffect() {
    var el = document.getElementById('typingEffect');
    if (!el) return;

    el.addEventListener('animationend', function () {
      el.classList.add('typing-text--done');
    });
  }

  /* ==========================================
     CONSULTATION FORM
     ========================================== */

  function initConsultForm() {
    var btn = document.getElementById('consultSubmit');
    if (!btn) return;

    btn.addEventListener('click', function () {
      var name = document.getElementById('consultName');
      var email = document.getElementById('consultEmail');
      var date = document.getElementById('consultDate');
      var message = document.getElementById('consultMessage');

      if (!name || !email || !date || !name.value || !email.value || !date.value) {
        if (window.LXToast) window.LXToast('Please fill in all required fields.', 'error');
        return;
      }

      btn.disabled = true;
      btn.textContent = 'Booking...';

      fetch('/api/contact', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: name.value,
          email: email.value,
          subject: 'Consultation Booking - ' + date.value,
          message: message ? message.value : 'Consultation requested for ' + date.value
        })
      }).then(function (res) {
        if (!res.ok) throw new Error('Failed');
        btn.style.display = 'none';
        var success = document.getElementById('consultSuccess');
        if (success) success.style.display = 'block';
      }).catch(function () {
        btn.disabled = false;
        btn.textContent = 'Book Consultation';
        if (window.LXToast) window.LXToast('Failed to book. Please try again.', 'error');
      });
    });
  }

  /* ==========================================
     INIT ALL
     ========================================== */

  function init() {
    initThemeToggle();
    initChatWidget();
    initContactForm();
    initCookieConsent();
    initDashboard();
    initBlogModal();
    initTypingEffect();
    initConsultForm();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
