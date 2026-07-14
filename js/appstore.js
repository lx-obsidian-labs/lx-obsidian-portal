void (function () {
  'use strict';

  /* ==========================================
     LX OBSIDIAN APP STORE
     Apps · Downloads · Reviews · Categories
     ========================================== */

  var APP_DATA = [
    {
      id: 'optiscan',
      name: 'OptiScan',
      tagline: 'AI-powered diagnostic scanning',
      desc: 'Advanced AI diagnostic engine that scans codebases, networks, and systems for vulnerabilities, performance bottlenecks, and optimization opportunities. Features real-time reporting and automated fix suggestions.',
      category: 'developer-tools',
      categoryLabel: 'Developer Tools',
      price: 29.99,
      rating: 4.8,
      reviews: 342,
      downloads: 12850,
      version: '2.4.1',
      size: '24 MB',
      color: 'blue',
      icon: 'settings',
      features: ['AI vulnerability scanning', 'Real-time performance monitoring', 'Automated fix suggestions', 'CI/CD integration', 'Multi-language support', 'Export reports (PDF/JSON/HTML)'],
      requirements: ['Windows 10+, macOS 12+, or Linux', '4 GB RAM minimum', '500 MB disk space', 'Node.js 18+ (optional)'],
      screenshots: ['dashboard', 'scan-results', 'settings'],
      author: 'LX Obsidian Labs',
      released: 'March 2025',
      updated: 'June 15, 2026',
      license: 'Single-user license'
    },
    {
      id: 'college-manager',
      name: 'College Manager',
      tagline: 'Comprehensive campus management',
      desc: 'All-in-one campus management platform with student admissions, course scheduling, grade tracking, attendance monitoring, and communication tools. Integrates with popular LMS systems.',
      category: 'business',
      categoryLabel: 'Business',
      price: 39.99,
      rating: 4.6,
      reviews: 218,
      downloads: 8720,
      version: '3.1.0',
      size: '42 MB',
      color: 'green',
      icon: 'monitor',
      features: ['Student admission portal', 'Course & timetable scheduling', 'Grade & transcript management', 'Attendance tracking', 'Parent communication portal', 'LMS integration (Moodle, Canvas)'],
      requirements: ['Windows Server 2019+ or Linux', '8 GB RAM, 4 CPU cores', '2 GB disk space', 'PostgreSQL 14+ or MySQL 8+'],
      screenshots: ['dashboard', 'students', 'courses'],
      author: 'LX Obsidian Labs',
      released: 'August 2024',
      updated: 'June 10, 2026',
      license: 'Site license (per institution)'
    },
    {
      id: 'bookme-rivet',
      name: 'BookMe Rivet',
      tagline: 'Seamless appointment platform',
      desc: 'Modern appointment booking and scheduling platform with calendar sync, automated reminders, payment processing, and resource management. Perfect for clinics, salons, and service businesses.',
      category: 'business',
      categoryLabel: 'Business',
      price: 24.99,
      rating: 4.7,
      reviews: 189,
      downloads: 15430,
      version: '1.9.3',
      size: '18 MB',
      color: 'amber',
      icon: 'calendar',
      features: ['Online booking widget', 'Google/Outlook calendar sync', 'SMS & email reminders', 'Stripe payment integration', 'Staff resource management', 'Analytics dashboard'],
      requirements: ['Node.js 18+', 'MongoDB 6+ or PostgreSQL 14+', '4 GB RAM', 'Stripe account'],
      screenshots: ['booking', 'calendar', 'analytics'],
      author: 'LX Obsidian Labs',
      released: 'January 2025',
      updated: 'May 28, 2026',
      license: 'Monthly subscription'
    },
    {
      id: 'inventory-pro',
      name: 'Inventory Pro',
      tagline: 'Real-time inventory tracking',
      desc: 'Enterprise inventory management system with barcode scanning, stock forecasting, multi-warehouse support, and purchase order management. Real-time sync across all locations.',
      category: 'business',
      categoryLabel: 'Business',
      price: 49.99,
      rating: 4.5,
      reviews: 156,
      downloads: 6540,
      version: '2.2.0',
      size: '36 MB',
      color: 'purple',
      icon: 'star',
      features: ['Barcode & QR scanning', 'Stock forecasting (AI)', 'Multi-warehouse support', 'Purchase order management', 'Supplier portal', 'Real-time sync'],
      requirements: ['Windows/Linux/macOS', '8 GB RAM', '1 GB disk space', 'PostgreSQL 14+ or MySQL 8+'],
      screenshots: ['inventory', 'forecast', 'orders'],
      author: 'LX Obsidian Labs',
      released: 'November 2024',
      updated: 'June 20, 2026',
      license: 'Per-seat license'
    },
    {
      id: 'crm-mobile',
      name: 'CRM Mobile',
      tagline: 'On-the-go relationship management',
      desc: 'Mobile-first CRM platform with contact management, deal tracking, pipeline visualization, and team collaboration. Works offline with auto-sync when connected.',
      category: 'mobile',
      categoryLabel: 'Mobile',
      price: 34.99,
      rating: 4.9,
      reviews: 423,
      downloads: 22100,
      version: '4.0.1',
      size: '15 MB',
      color: 'teal',
      icon: 'lock',
      features: ['Contact & lead management', 'Deal pipeline tracking', 'Offline mode with auto-sync', 'Email & calendar integration', 'Team collaboration', 'Custom report builder'],
      requirements: ['iOS 15+ or Android 12+', '2 GB RAM', '200 MB disk space', 'Cloud account required'],
      screenshots: ['contacts', 'pipeline', 'reports'],
      author: 'LX Obsidian Labs',
      released: 'March 2024',
      updated: 'June 22, 2026',
      license: 'Per-user subscription'
    },
    {
      id: 'pulse-monitor',
      name: 'Pulse Monitor',
      tagline: 'System health monitoring',
      desc: 'Real-time infrastructure monitoring for servers, containers, databases, and applications. Custom alerts, dashboards, and incident response automation.',
      category: 'developer-tools',
      categoryLabel: 'Developer Tools',
      price: 0,
      rating: 4.4,
      reviews: 97,
      downloads: 18900,
      version: '1.6.2',
      size: '12 MB',
      color: 'red',
      icon: 'message',
      features: ['Server & container monitoring', 'Custom alert thresholds', 'Dashboard builder', 'Incident response automation', 'Multi-cloud support', 'Team notifications (Slack, Email, PagerDuty)'],
      requirements: ['Linux (Ubuntu 20+, CentOS 8+)', '2 GB RAM', '500 MB disk space', 'Docker (optional)'],
      screenshots: ['dashboard', 'alerts', 'incidents'],
      author: 'LX Obsidian Labs',
      released: 'October 2024',
      updated: 'June 5, 2026',
      license: 'Free (Community Edition)'
    },
    {
      id: 'datavault',
      name: 'DataVault',
      tagline: 'Secure data storage & backup',
      desc: 'Enterprise backup and disaster recovery solution with AES-256 encryption, incremental backups, cloud replication, and one-click restore. SOC 2 compliant.',
      category: 'business',
      categoryLabel: 'Business',
      price: 59.99,
      rating: 4.8,
      reviews: 276,
      downloads: 9930,
      version: '3.3.0',
      size: '28 MB',
      color: 'indigo',
      icon: 'cloud',
      features: ['AES-256 encryption at rest & transit', 'Incremental & differential backups', 'Multi-cloud replication (AWS/Azure/GCP)', 'One-click disaster recovery', 'SOC 2 & GDPR compliant', 'Audit logging'],
      requirements: ['Linux (Ubuntu 22+, RHEL 9+)', '8 GB RAM, 4 CPU cores', '10 GB disk space (varies by data)', 'Cloud storage account'],
      screenshots: ['backup', 'restore', 'compliance'],
      author: 'LX Obsidian Labs',
      released: 'September 2024',
      updated: 'June 18, 2026',
      license: 'Per-TB license'
    },
    {
      id: 'flowforge',
      name: 'FlowForge',
      tagline: 'Visual workflow automation',
      desc: 'Drag-and-drop workflow automation builder with 200+ integrations. Automate complex business processes, data pipelines, and approval chains without writing code.',
      category: 'developer-tools',
      categoryLabel: 'Developer Tools',
      price: 44.99,
      rating: 4.6,
      reviews: 198,
      downloads: 11200,
      version: '2.0.4',
      size: '34 MB',
      color: 'blue',
      icon: 'settings',
      features: ['Visual drag-and-drop builder', '200+ pre-built integrations', 'Conditional logic & branching', 'Approval workflow engine', 'Scheduled triggers & webhooks', 'Execution logs & debugging'],
      requirements: ['Docker or Kubernetes', '4 GB RAM, 2 CPU cores', 'PostgreSQL 14+', 'Node.js 18+'],
      screenshots: ['builder', 'integrations', 'logs'],
      author: 'LX Obsidian Labs',
      released: 'February 2025',
      updated: 'June 12, 2026',
      license: 'Per-workflow license'
    },
    {
      id: 'cloudsync',
      name: 'CloudSync',
      tagline: 'Multi-cloud file sync',
      desc: 'Enterprise file synchronization across AWS S3, Google Cloud Storage, Azure Blob, and on-premise storage. Real-time syncing with conflict resolution and version history.',
      category: 'developer-tools',
      categoryLabel: 'Developer Tools',
      price: 29.99,
      rating: 4.7,
      reviews: 145,
      downloads: 7800,
      version: '1.8.0',
      size: '16 MB',
      color: 'green',
      icon: 'monitor',
      features: ['Multi-cloud sync (AWS/GCP/Azure)', 'Real-time file watching', 'Conflict resolution', 'Version history (90 days)', 'Bandwidth throttling', 'End-to-end encryption'],
      requirements: ['Linux or macOS', '2 GB RAM', '200 MB disk space', 'Cloud provider credentials'],
      screenshots: ['sync', 'conflicts', 'settings'],
      author: 'LX Obsidian Labs',
      released: 'April 2025',
      updated: 'May 30, 2026',
      license: 'Per-connector license'
    },
    {
      id: 'teamboard',
      name: 'TeamBoard',
      tagline: 'Collaborative project management',
      desc: 'Visual project management with Kanban boards, Gantt charts, time tracking, and resource allocation. Built for agile teams with real-time collaboration.',
      category: 'business',
      categoryLabel: 'Business',
      price: 24.99,
      rating: 4.5,
      reviews: 312,
      downloads: 19600,
      version: '2.1.3',
      size: '20 MB',
      color: 'amber',
      icon: 'calendar',
      features: ['Kanban & Scrum boards', 'Gantt chart timeline', 'Time tracking & reporting', 'Resource allocation', 'Real-time collaboration', 'Jira & GitHub integration'],
      requirements: ['Web browser (Chrome/Firefox/Edge)', 'Internet connection', '2 GB RAM recommended'],
      screenshots: ['board', 'gantt', 'reports'],
      author: 'LX Obsidian Labs',
      released: 'July 2024',
      updated: 'June 8, 2026',
      license: 'Per-user subscription'
    },
    {
      id: 'analytics-pro',
      name: 'Analytics Pro',
      tagline: 'Advanced BI dashboard',
      desc: 'Business intelligence platform with drag-and-drop dashboard builder, SQL query editor, custom visualizations, and automated report scheduling.',
      category: 'developer-tools',
      categoryLabel: 'Developer Tools',
      price: 79.99,
      rating: 4.9,
      reviews: 167,
      downloads: 5430,
      version: '5.0.2',
      size: '45 MB',
      color: 'purple',
      icon: 'star',
      features: ['Drag-and-drop dashboard builder', 'SQL query editor', 'Custom visualization library', 'Automated report scheduling', 'Data source connectors (50+)', 'Role-based access control'],
      requirements: ['Linux (Ubuntu 22+, RHEL 9+)', '8 GB RAM, 4 CPU cores', '2 GB disk space', 'PostgreSQL 14+ or MySQL 8+'],
      screenshots: ['dashboard', 'queries', 'reports'],
      author: 'LX Obsidian Labs',
      released: 'January 2024',
      updated: 'June 25, 2026',
      license: 'Per-seat license'
    },
    {
      id: 'securegate',
      name: 'SecureGate',
      tagline: 'Enterprise access control',
      desc: 'Zero-trust access control platform with SSO, MFA, role-based policies, and real-time threat detection. Manage access across cloud, on-premise, and hybrid environments.',
      category: 'business',
      categoryLabel: 'Business',
      price: 69.99,
      rating: 4.6,
      reviews: 134,
      downloads: 6890,
      version: '1.3.1',
      size: '22 MB',
      color: 'teal',
      icon: 'lock',
      features: ['Single Sign-On (SAML/OIDC)', 'Multi-factor authentication', 'Role-based access control', 'Real-time threat detection', 'Access audit trails', 'Zero-trust architecture'],
      requirements: ['Linux (Ubuntu 22+, RHEL 9+)', '4 GB RAM, 2 CPU cores', '1 GB disk space', 'PostgreSQL 14+'],
      screenshots: ['dashboard', 'policies', 'audit'],
      author: 'LX Obsidian Labs',
      released: 'May 2025',
      updated: 'June 20, 2026',
      license: 'Per-user license'
    },
    {
      id: 'synapse-ai',
      name: 'Synapse AI',
      tagline: 'AI browser automation extension',
      desc: 'An AI-powered Chrome / Edge / Brave extension that turns any web page into an autonomous, controllable workspace. It runs an observe → plan → execute → verify loop to navigate, click, type, extract, and automate tasks. Works out of the box — no API key required.',
      category: 'ai',
      categoryLabel: 'AI',
      price: 0,
      rating: 5.0,
      reviews: 128,
      downloads: 5400,
      version: '1.1.1',
      size: '115 KB',
      color: 'purple',
      icon: 'message',
      isExtension: true,
      githubUrl: 'https://github.com/lx-obsidian-labs/synapse-social',
      features: [
        'Autonomous web automation — navigate, click, type, extract, fill forms',
        'AI content generation in your brand voice (Facebook, social OS)',
        'Vision fallback via page screenshots when the DOM is empty',
        'Self-learning memory, model rotation & failover',
        'No API key required — secure hosted NVIDIA proxy',
        'Playbooks for ChatGPT, Gmail, YouTube, Facebook, Google, LinkedIn & more'
      ],
      requirements: [
        'Google Chrome, Microsoft Edge, or Brave (v100+)',
        'Developer mode enabled in extensions page',
        'Unzip the downloaded folder (contains manifest.json)',
        'No API key needed — works immediately'
      ],
      installSteps: [
        'Download the ZIP and unzip it (you get a folder with manifest.json).',
        'Open chrome://extensions (or edge://extensions, brave://extensions).',
        'Turn on Developer mode (top-right toggle).',
        'Click Load unpacked and select the unzipped folder.',
        'Pin Synapse AI and open the side panel — the AI works out of the box.'
      ],
      screenshots: ['dashboard', 'settings', 'scan-results'],
      author: 'LX Obsidian Labs',
      released: 'June 2026',
      updated: 'June 2026',
      license: 'Free (Open Distribution)'
    }
  ];

  var CATEGORIES = [
    { id: 'all', label: 'All Apps' },
    { id: 'developer-tools', label: 'Developer Tools' },
    { id: 'ai', label: 'AI' },
    { id: 'business', label: 'Business' },
    { id: 'mobile', label: 'Mobile' }
  ];

  var currentFilter = 'all';
  var currentApp = null;

  /* ---------- RENDER GRID ---------- */

  function renderGrid(filter) {
    var grid = document.getElementById('appStoreGrid');
    if (!grid) return;

    var apps = filter === 'all' ? APP_DATA : APP_DATA.filter(function (a) { return a.category === filter; });
    var html = '';

    apps.forEach(function (app, i) {
      var isFree = app.price === 0;
      var priceLabel = isFree ? 'Free' : '$' + app.price.toFixed(2);
      var delay = (i % 4) + 1;
      var freeBadge = isFree ? '<span class="app-card__badge app-card__badge--free">Free</span>' : '';
      var premiumBadge = !isFree ? '<span class="app-card__badge app-card__badge--premium">Premium</span>' : '';

      html +=
        '<div class="app-card reveal reveal--delay-' + delay + '" data-app-id="' + app.id + '">' +
          '<div class="app-card__header">' +
            '<div class="app-card__icon" data-color="' + app.color + '">' +
              '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">' + iconSVG(app.icon) + '</svg>' +
            '</div>' +
            freeBadge + premiumBadge +
          '</div>' +
          '<div class="app-card__body">' +
            '<h3 class="app-card__title">' + app.name + '</h3>' +
            '<p class="app-card__tagline">' + app.tagline + '</p>' +
            '<div class="app-card__meta">' +
              '<span class="app-card__rating">&#9733; ' + app.rating + '</span>' +
              '<span class="app-card__downloads">' + formatNumber(app.downloads) + ' downloads</span>' +
            '</div>' +
            '<div class="app-card__cats">' +
              '<span class="app-card__cat">' + app.categoryLabel + '</span>' +
              '<span class="app-card__version">v' + app.version + '</span>' +
            '</div>' +
          '</div>' +
          '<div class="app-card__footer">' +
            '<button class="btn btn--primary btn--full app-card__btn" data-app="' + app.id + '" data-action="details">View Details</button>' +
            (isFree
              ? '<button class="btn btn--secondary btn--full app-card__btn" data-app="' + app.id + '" data-action="download">' + (app.isExtension ? 'Download Extension' : 'Download Free') + '</button>'
              : '<button class="btn btn--secondary btn--full app-card__btn" data-app="' + app.id + '" data-action="details">View Details</button>'
            ) +
          '</div>' +
        '</div>';
    });

    grid.innerHTML = html || '<div class="app-card__empty"><p>No apps found in this category.</p></div>';

    grid.querySelectorAll('[data-action="details"]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var id = btn.getAttribute('data-app');
        openAppModal(id);
      });
    });

    grid.querySelectorAll('[data-action="download"]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var id = btn.getAttribute('data-app');
        downloadApp(id);
      });
    });
  }

  /* ---------- APP DETAIL MODAL ---------- */

  function openAppModal(id) {
    var app = APP_DATA.find(function (a) { return a.id === id; });
    if (!app) return;

    currentApp = app;

    var overlay = document.getElementById('appDetailOverlay');
    if (!overlay) return;

    var isFree = app.price === 0;
    var priceLabel = isFree ? 'Free' : '$' + app.price.toFixed(2);

    var featuresHtml = '';
    app.features.forEach(function (f) {
      featuresHtml += '<li class="app-modal__feature">' +
        '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>' +
        f +
      '</li>';
    });

    var reqHtml = '';
    app.requirements.forEach(function (r) {
      reqHtml += '<li class="app-modal__req">' + r + '</li>';
    });

    overlay.innerHTML =
      '<div class="app-modal" role="dialog" aria-modal="true" aria-label="' + app.name + ' details">' +
        '<div class="app-modal__header">' +
          '<div class="app-modal__icon" data-color="' + app.color + '">' +
            '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">' + iconSVG(app.icon) + '</svg>' +
          '</div>' +
          '<div class="app-modal__title-group">' +
            '<h2 class="app-modal__title">' + app.name + '</h2>' +
            '<p class="app-modal__tagline">' + app.tagline + '</p>' +
            '<div class="app-modal__cats">' +
              '<span class="app-card__cat">' + app.categoryLabel + '</span>' +
              '<span class="app-card__version">v' + app.version + '</span>' +
            '</div>' +
          '</div>' +
          '<button class="app-modal__close" id="appDetailClose" aria-label="Close details">&times;</button>' +
        '</div>' +
        '<div class="app-modal__body">' +
          '<div class="app-modal__section">' +
            '<h3 class="app-modal__section-title">About</h3>' +
            '<p class="app-modal__desc">' + app.desc + '</p>' +
          '</div>' +
          '<div class="app-modal__section">' +
            '<h3 class="app-modal__section-title">Features</h3>' +
            '<ul class="app-modal__feature-list">' + featuresHtml + '</ul>' +
          '</div>' +
          '<div class="app-modal__section">' +
            '<h3 class="app-modal__section-title">System Requirements</h3>' +
            '<ul class="app-modal__req-list">' + reqHtml + '</ul>' +
          '</div>' +
          (app.installSteps
            ? '<div class="app-modal__section">' +
                '<h3 class="app-modal__section-title">Install</h3>' +
                '<ol class="app-modal__install">' +
                  app.installSteps.map(function (s) { return '<li>' + s + '</li>'; }).join('') +
                '</ol>' +
              '</div>'
            : '') +
          '<div class="app-modal__section">' +
            '<h3 class="app-modal__section-title">Details</h3>' +
            '<table class="app-modal__table">' +
              '<tr><td>Author</td><td>' + app.author + '</td></tr>' +
              '<tr><td>Released</td><td>' + app.released + '</td></tr>' +
              '<tr><td>Last Updated</td><td>' + app.updated + '</td></tr>' +
              '<tr><td>Size</td><td>' + app.size + '</td></tr>' +
              '<tr><td>License</td><td>' + app.license + '</td></tr>' +
              '<tr><td>Downloads</td><td>' + formatNumber(app.downloads) + '</td></tr>' +
              '<tr><td>Rating</td><td>&#9733; ' + app.rating + '/5 (' + app.reviews + ' reviews)</td></tr>' +
            '</table>' +
          '</div>' +
          '<div class="app-modal__section">' +
            '<h3 class="app-modal__section-title">User Reviews</h3>' +
            '<div class="app-modal__reviews" id="appReviews">' +
              renderReviews(app.id) +
            '</div>' +
          '</div>' +
        '</div>' +
        '<div class="app-modal__footer">' +
          '<div class="app-modal__price">' +
            '<span class="app-modal__price-value">' + priceLabel + '</span>' +
            '<span class="app-modal__price-label">' + (isFree ? 'Open Source' : 'Premium') + '</span>' +
          '</div>' +
          '<div class="app-modal__actions">' +
            (isFree
              ? '<button class="btn btn--primary app-modal__action-btn" id="appDetailDownload">' + (app.isExtension ? 'Download Extension' : 'Download Free') + '</button>'
              : '<a href="/contact" class="btn btn--primary app-modal__action-btn">Contact for Pricing</a>'
            ) +
            (app.isExtension ? '<a href="synapse.html" class="btn btn--ghost app-modal__action-btn" target="_blank" rel="noopener">Install Guide</a>' : '') +
            '<button class="btn btn--ghost app-modal__action-btn" id="appDetailCloseBtn">Close</button>' +
          '</div>' +
        '</div>' +
      '</div>';

    overlay.classList.add('app-detail-overlay--visible');
    document.body.style.overflow = 'hidden';

    document.getElementById('appDetailClose').addEventListener('click', closeAppModal);
    document.getElementById('appDetailCloseBtn').addEventListener('click', closeAppModal);
    overlay.addEventListener('click', function (e) {
      if (e.target === overlay) closeAppModal();
    });

    var dlBtn = document.getElementById('appDetailDownload');
    if (dlBtn) dlBtn.addEventListener('click', function () { downloadApp(app.id); });
  }

  function closeAppModal() {
    var overlay = document.getElementById('appDetailOverlay');
    if (overlay) {
      overlay.classList.remove('app-detail-overlay--visible');
      document.body.style.overflow = '';
    }
  }

  /* ---------- DOWNLOAD APP ---------- */

  function downloadApp(id) {
    var app = APP_DATA.find(function (a) { return a.id === id; });
    if (!app) return;

    if (app.isExtension) {
      downloadExtension(app);
      return;
    }

    var downloads = JSON.parse(localStorage.getItem('lx-downloads') || '{}');
    downloads[id] = (downloads[id] || 0) + 1;
    localStorage.setItem('lx-downloads', JSON.stringify(downloads));

    var msg = 'Downloading ' + app.name + ' v' + app.version + '...\n\n' +
      'File: ' + app.id + '-v' + app.version.replace(/\./g, '-') + '.zip\n' +
      'Size: ' + app.size + '\n' +
      'License: ' + app.license + '\n\n' +
      'Your download will begin shortly.\n\n' +
      'Download count: ' + (downloads[id]) + ' (all time)';

    if (window.LXToast) window.LXToast('Downloading ' + app.name, 'success');
    else alert(msg);
  }

  /* Real download for browser-extension apps — pulls latest GitHub Release asset */
  function downloadExtension(app) {
    var RELEASE_API = 'https://api.github.com/repos/lx-obsidian-labs/synapse-social/releases/latest';
    var FALLBACK = 'https://github.com/lx-obsidian-labs/synapse-social/releases/download/v1.1.1/synapse-ai-v1.1.1.zip';

    if (window.LXToast) window.LXToast('Preparing your download…', 'success');

    function go(url) { window.location.href = url; }

    fetch(RELEASE_API, { headers: { 'Accept': 'application/vnd.github+json' } })
      .then(function (r) { return r.ok ? r.json() : Promise.reject(); })
      .then(function (data) {
        var asset = data.assets && data.assets[0];
        go((asset && asset.browser_download_url) ? asset.browser_download_url : FALLBACK);
      })
      .catch(function () { go(FALLBACK); });
  }

  /* ---------- REVIEWS ---------- */

  function renderReviews(appId) {
    var allReviews = JSON.parse(localStorage.getItem('lx-reviews') || '{}');
    var appReviews = allReviews[appId] || sampleReviews(appId);

    if (appReviews.length === 0) {
      return '<p class="app-modal__no-reviews">No reviews yet. Be the first to review!</p>';
    }

    var html = '';
    appReviews.forEach(function (r) {
      var stars = '';
      for (var i = 0; i < 5; i++) {
        stars += i < r.rating ? '&#9733;' : '&#9734;';
      }
      html +=
        '<div class="app-modal__review">' +
          '<div class="app-modal__review-header">' +
            '<strong>' + r.author + '</strong>' +
            '<span class="app-modal__review-rating">' + stars + '</span>' +
          '</div>' +
          '<p class="app-modal__review-text">' + r.text + '</p>' +
          '<span class="app-modal__review-date">' + r.date + '</span>' +
        '</div>';
    });

    html +=
      '<div class="app-modal__write-review">' +
        '<h4 style="margin-bottom:0.5rem;color:var(--color-text);font-size:var(--text-sm);">Write a Review</h4>' +
        '<div class="app-modal__review-form">' +
          '<input type="text" class="app-modal__review-input" id="reviewAuthor" placeholder="Your name" style="width:100%;padding:var(--space-2);margin-bottom:var(--space-2);background:var(--color-bg);border:1px solid var(--color-border);border-radius:var(--radius-md);color:var(--color-text);">' +
          '<div class="app-modal__star-select" id="starSelect">' +
            '<button class="star-btn" data-star="1">&#9733;</button>' +
            '<button class="star-btn" data-star="2">&#9733;</button>' +
            '<button class="star-btn" data-star="3">&#9733;</button>' +
            '<button class="star-btn" data-star="4">&#9733;</button>' +
            '<button class="star-btn" data-star="5">&#9733;</button>' +
          '</div>' +
          '<textarea class="app-modal__review-textarea" id="reviewText" placeholder="Write your review..." style="width:100%;padding:var(--space-2);margin-bottom:var(--space-2);background:var(--color-bg);border:1px solid var(--color-border);border-radius:var(--radius-md);color:var(--color-text);min-height:80px;font-family:inherit;"></textarea>' +
          '<button class="btn btn--primary btn--small" id="submitReview" data-app="' + appId + '">Submit Review</button>' +
        '</div>' +
      '</div>';

    return html;
  }

  function sampleReviews(appId) {
    var samples = {
      'optiscan': [
        { author: 'Sarah K.', rating: 5, text: 'Game changer for our CI/CD pipeline. Caught vulnerabilities we had missed for months.', date: '2 weeks ago' },
        { author: 'Marcus J.', rating: 4, text: 'Excellent tool. The AI suggestions are usually spot-on. Would love more language support.', date: '1 month ago' }
      ],
      'pulse-monitor': [
        { author: 'DevOps Team A', rating: 5, text: 'Great free tool for monitoring our K8s cluster. Easy to set up.', date: '3 weeks ago' },
        { author: 'Alex R.', rating: 4, text: 'Solid monitoring for the price (free!). Alerting could be more granular.', date: '2 months ago' }
      ],
      'crm-mobile': [
        { author: 'Jane D.', rating: 5, text: 'Best mobile CRM I\'ve used. Offline mode is a lifesaver.', date: '1 week ago' },
        { author: 'Tom W.', rating: 5, text: 'Replaced our old CRM entirely. The team loves it.', date: '3 weeks ago' }
      ]
    };
    return samples[appId] || [
      { author: 'Verified User', rating: 4, text: 'Solid application. Met our requirements well.', date: '1 month ago' },
      { author: 'Tech Lead', rating: 5, text: 'Great support from the LX team. Would recommend.', date: '2 months ago' }
    ];
  }

  /* ---------- STAR RATING ---------- */

  function initStarSelector() {
    document.addEventListener('click', function (e) {
      var starBtn = e.target.closest('.star-btn');
      if (!starBtn) return;

      var rating = parseInt(starBtn.getAttribute('data-star'));
      var container = starBtn.closest('.app-modal__star-select');
      if (!container) return;

      container.querySelectorAll('.star-btn').forEach(function (btn, i) {
        btn.style.color = i < rating ? 'var(--color-accent)' : 'var(--color-muted)';
        btn.setAttribute('data-selected', i < rating ? 'true' : 'false');
      });
    });
  }

  /* ---------- SUBMIT REVIEW ---------- */

  function initReviewSubmit() {
    document.addEventListener('click', function (e) {
      var submitBtn = e.target.closest('#submitReview');
      if (!submitBtn) return;

      var appId = submitBtn.getAttribute('data-app');
      var author = document.getElementById('reviewAuthor');
      var text = document.getElementById('reviewText');
      var starContainer = document.getElementById('starSelect');

      if (!author || !text || !starContainer) return;

      var name = author.value.trim() || 'Anonymous';
      var reviewText = text.value.trim();
      var selectedStars = starContainer.querySelectorAll('.star-btn[data-selected="true"]').length;

      if (!reviewText) {
        if (window.LXToast) window.LXToast('Please write your review.', 'error');
        else alert('Please write your review.');
        return;
      }

      if (selectedStars === 0) {
        if (window.LXToast) window.LXToast('Please select a star rating.', 'error');
        else alert('Please select a star rating.');
        return;
      }

      var allReviews = JSON.parse(localStorage.getItem('lx-reviews') || '{}');
      if (!allReviews[appId]) allReviews[appId] = [];
      allReviews[appId].push({
        author: name,
        rating: selectedStars,
        text: reviewText,
        date: 'Just now'
      });
      localStorage.setItem('lx-reviews', JSON.stringify(allReviews));

      author.value = '';
      text.value = '';

      if (window.LXToast) window.LXToast('Thank you for your review!', 'success');

      var reviewsSection = document.getElementById('appReviews');
      if (reviewsSection) {
        reviewsSection.innerHTML = renderReviews(appId);
      }
    });
  }

  /* ---------- CATEGORY FILTERS ---------- */

  function initFilters() {
    var container = document.getElementById('appCategoryFilters');
    if (!container) return;

    var html = '';
    CATEGORIES.forEach(function (cat) {
      html += '<button class="app-filter' + (cat.id === currentFilter ? ' app-filter--active' : '') + '" data-filter="' + cat.id + '">' + cat.label + '</button>';
    });
    container.innerHTML = html;

    container.querySelectorAll('.app-filter').forEach(function (btn) {
      btn.addEventListener('click', function () {
        container.querySelectorAll('.app-filter').forEach(function (b) { b.classList.remove('app-filter--active'); });
        btn.classList.add('app-filter--active');
        currentFilter = btn.getAttribute('data-filter');
        renderGrid(currentFilter);
      });
    });
  }

  /* ---------- SEARCH ---------- */

  function initAppSearch() {
    var input = document.getElementById('appSearchInput');
    if (!input) return;

    input.addEventListener('input', function () {
      var q = input.value.trim().toLowerCase();
      var grid = document.getElementById('appStoreGrid');
      if (!grid) return;

      var cards = grid.querySelectorAll('.app-card');
      cards.forEach(function (card) {
        var title = card.querySelector('.app-card__title');
        var tagline = card.querySelector('.app-card__tagline');
        var text = (title ? title.textContent : '') + ' ' + (tagline ? tagline.textContent : '');
        card.style.display = text.toLowerCase().indexOf(q) > -1 ? '' : 'none';
      });
    });
  }

  /* ---------- SEARCH MODAL ---------- */

  function initAppSearchToggle() {
    var searchBtn = document.getElementById('appStoreSearch');
    var searchBar = document.getElementById('appSearchBar');
    if (searchBtn && searchBar) {
      searchBtn.addEventListener('click', function () {
        searchBar.classList.toggle('app-search-bar--visible');
        if (searchBar.classList.contains('app-search-bar--visible')) {
          var input = searchBar.querySelector('#appSearchInput');
          if (input) setTimeout(function () { input.focus(); }, 100);
        }
      });
    }
  }

  /* ---------- UTILITIES ---------- */

  function formatNumber(n) {
    if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M';
    if (n >= 1000) return (n / 1000).toFixed(1) + 'K';
    return n.toString();
  }

  function iconSVG(name) {
    var icons = {
      settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>',
      monitor: '<rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>',
      calendar: '<rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>',
      star: '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>',
      lock: '<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>',
      message: '<path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>',
      cloud: '<path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 002 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/>'
    };
    return icons[name] || icons.settings;
  }

  /* ---------- KEYBOARD SHORTCUTS ---------- */

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') closeAppModal();
  });

  /* ---------- INIT ---------- */

  function init() {
    initFilters();
    renderGrid('all');
    initAppSearch();
    initAppSearchToggle();
    initStarSelector();
    initReviewSubmit();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  window.LXAppStore = {
    data: APP_DATA,
    openAppModal: openAppModal,
    closeAppModal: closeAppModal,
    downloadApp: downloadApp,
    renderGrid: renderGrid,
    renderReviews: renderReviews
  };

})();
