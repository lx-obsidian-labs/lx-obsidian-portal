void (function () {
  'use strict';

  /* ==========================================
     LX OBSIDIAN APP STORE — Advanced Edition
     Sorting · Filtering · Compare · Recently Viewed
     Share · Changelog · View Toggle · Search
     ========================================== */

  var APP_DATA = [
    {
      id: 'optiscan', name: 'OptiScan', tagline: 'AI-powered diagnostic scanning',
      desc: 'Advanced AI diagnostic engine that scans codebases, networks, and systems for vulnerabilities, performance bottlenecks, and optimization opportunities. Features real-time reporting and automated fix suggestions.',
      category: 'developer-tools', categoryLabel: 'Developer Tools',
      price: 29.99, rating: 4.8, reviews: 342, downloads: 12850,
      version: '2.4.1', size: '24 MB', color: 'blue', icon: 'settings',
      features: ['AI vulnerability scanning', 'Real-time performance monitoring', 'Automated fix suggestions', 'CI/CD integration', 'Multi-language support', 'Export reports (PDF/JSON/HTML)'],
      requirements: ['Windows 10+, macOS 12+, or Linux', '4 GB RAM minimum', '500 MB disk space', 'Node.js 18+ (optional)'],
      changelog: ['v2.4.1 — Fixed false positives in Python scan results', 'v2.4.0 — Added Docker container scanning support', 'v2.3.0 — New CI/CD plugin for GitHub Actions', 'v2.2.0 — Multi-language support (Python, Go, Rust)'],
      tags: ['security', 'scanning', 'devops', 'ci-cd'],
      author: 'LX Obsidian Labs', released: 'March 2025', updated: 'June 15, 2026', license: 'Single-user license'
    },
    {
      id: 'college-manager', name: 'College Manager', tagline: 'Comprehensive campus management',
      desc: 'All-in-one campus management platform with student admissions, course scheduling, grade tracking, attendance monitoring, and communication tools.',
      category: 'business', categoryLabel: 'Business',
      price: 39.99, rating: 4.6, reviews: 218, downloads: 8720,
      version: '3.1.0', size: '42 MB', color: 'green', icon: 'monitor',
      features: ['Student admission portal', 'Course & timetable scheduling', 'Grade & transcript management', 'Attendance tracking', 'Parent communication portal', 'LMS integration (Moodle, Canvas)'],
      requirements: ['Windows Server 2019+ or Linux', '8 GB RAM, 4 CPU cores', '2 GB disk space', 'PostgreSQL 14+ or MySQL 8+'],
      changelog: ['v3.1.0 — New parent portal mobile view', 'v3.0.0 — Redesigned dashboard with analytics', 'v2.9.0 — Added Canvas LMS integration'],
      tags: ['education', 'management', 'scheduling', 'enterprise'],
      author: 'LX Obsidian Labs', released: 'August 2024', updated: 'June 10, 2026', license: 'Site license (per institution)'
    },
    {
      id: 'bookme-rivet', name: 'BookMe Rivet', tagline: 'Seamless appointment platform',
      desc: 'Modern appointment booking and scheduling platform with calendar sync, automated reminders, payment processing, and resource management.',
      category: 'business', categoryLabel: 'Business',
      price: 24.99, rating: 4.7, reviews: 189, downloads: 15430,
      version: '1.9.3', size: '18 MB', color: 'amber', icon: 'calendar',
      features: ['Online booking widget', 'Google/Outlook calendar sync', 'SMS & email reminders', 'Stripe payment integration', 'Staff resource management', 'Analytics dashboard'],
      requirements: ['Node.js 18+', 'MongoDB 6+ or PostgreSQL 14+', '4 GB RAM', 'Stripe account'],
      changelog: ['v1.9.3 — Fixed Outlook calendar sync bug', 'v1.9.0 — Added WhatsApp reminders', 'v1.8.0 — New analytics dashboard'],
      tags: ['booking', 'scheduling', 'payments', 'saas'],
      author: 'LX Obsidian Labs', released: 'January 2025', updated: 'May 28, 2026', license: 'Monthly subscription'
    },
    {
      id: 'inventory-pro', name: 'Inventory Pro', tagline: 'Real-time inventory tracking',
      desc: 'Enterprise inventory management system with barcode scanning, stock forecasting, multi-warehouse support, and purchase order management.',
      category: 'business', categoryLabel: 'Business',
      price: 49.99, rating: 4.5, reviews: 156, downloads: 6540,
      version: '2.2.0', size: '36 MB', color: 'purple', icon: 'star',
      features: ['Barcode & QR scanning', 'Stock forecasting (AI)', 'Multi-warehouse support', 'Purchase order management', 'Supplier portal', 'Real-time sync'],
      requirements: ['Windows/Linux/macOS', '8 GB RAM', '1 GB disk space', 'PostgreSQL 14+ or MySQL 8+'],
      changelog: ['v2.2.0 — AI stock forecasting engine', 'v2.1.0 — Multi-warehouse support', 'v2.0.0 — Complete UI redesign'],
      tags: ['inventory', 'warehouse', 'supply-chain', 'enterprise'],
      author: 'LX Obsidian Labs', released: 'November 2024', updated: 'June 20, 2026', license: 'Per-seat license'
    },
    {
      id: 'crm-mobile', name: 'CRM Mobile', tagline: 'On-the-go relationship management',
      desc: 'Mobile-first CRM platform with contact management, deal tracking, pipeline visualization, and team collaboration. Works offline with auto-sync.',
      category: 'mobile', categoryLabel: 'Mobile',
      price: 34.99, rating: 4.9, reviews: 423, downloads: 22100,
      version: '4.0.1', size: '15 MB', color: 'teal', icon: 'lock',
      features: ['Contact & lead management', 'Deal pipeline tracking', 'Offline mode with auto-sync', 'Email & calendar integration', 'Team collaboration', 'Custom report builder'],
      requirements: ['iOS 15+ or Android 12+', '2 GB RAM', '200 MB disk space', 'Cloud account required'],
      changelog: ['v4.0.1 — Fixed offline sync conflict resolution', 'v4.0.0 — New pipeline visualization', 'v3.9.0 — Added WhatsApp integration'],
      tags: ['crm', 'mobile', 'sales', 'contacts'],
      author: 'LX Obsidian Labs', released: 'March 2024', updated: 'June 22, 2026', license: 'Per-user subscription'
    },
    {
      id: 'pulse-monitor', name: 'Pulse Monitor', tagline: 'System health monitoring',
      desc: 'Real-time infrastructure monitoring for servers, containers, databases, and applications. Custom alerts, dashboards, and incident response automation.',
      category: 'developer-tools', categoryLabel: 'Developer Tools',
      price: 0, rating: 4.4, reviews: 97, downloads: 18900,
      version: '1.6.2', size: '12 MB', color: 'red', icon: 'message',
      features: ['Server & container monitoring', 'Custom alert thresholds', 'Dashboard builder', 'Incident response automation', 'Multi-cloud support', 'Team notifications (Slack, Email, PagerDuty)'],
      requirements: ['Linux (Ubuntu 20+, CentOS 8+)', '2 GB RAM', '500 MB disk space', 'Docker (optional)'],
      changelog: ['v1.6.2 — Security patch for alert API', 'v1.6.0 — PagerDuty integration', 'v1.5.0 — Custom dashboard builder'],
      tags: ['monitoring', 'devops', 'alerts', 'infrastructure'],
      author: 'LX Obsidian Labs', released: 'October 2024', updated: 'June 5, 2026', license: 'Free (Community Edition)'
    },
    {
      id: 'datavault', name: 'DataVault', tagline: 'Secure data storage & backup',
      desc: 'Enterprise backup and disaster recovery solution with AES-256 encryption, incremental backups, cloud replication, and one-click restore.',
      category: 'business', categoryLabel: 'Business',
      price: 59.99, rating: 4.8, reviews: 276, downloads: 9930,
      version: '3.3.0', size: '28 MB', color: 'indigo', icon: 'cloud',
      features: ['AES-256 encryption at rest & transit', 'Incremental & differential backups', 'Multi-cloud replication (AWS/Azure/GCP)', 'One-click disaster recovery', 'SOC 2 & GDPR compliant', 'Audit logging'],
      requirements: ['Linux (Ubuntu 22+, RHEL 9+)', '8 GB RAM, 4 CPU cores', '10 GB disk space', 'Cloud storage account'],
      changelog: ['v3.3.0 — Added GCP Cloud Storage support', 'v3.2.0 — SOC 2 compliance certification', 'v3.1.0 — One-click disaster recovery'],
      tags: ['backup', 'security', 'encryption', 'cloud'],
      author: 'LX Obsidian Labs', released: 'September 2024', updated: 'June 18, 2026', license: 'Per-TB license'
    },
    {
      id: 'flowforge', name: 'FlowForge', tagline: 'Visual workflow automation',
      desc: 'Drag-and-drop workflow automation builder with 200+ integrations. Automate complex business processes without writing code.',
      category: 'developer-tools', categoryLabel: 'Developer Tools',
      price: 44.99, rating: 4.6, reviews: 198, downloads: 11200,
      version: '2.0.4', size: '34 MB', color: 'blue', icon: 'settings',
      features: ['Visual drag-and-drop builder', '200+ pre-built integrations', 'Conditional logic & branching', 'Approval workflow engine', 'Scheduled triggers & webhooks', 'Execution logs & debugging'],
      requirements: ['Docker or Kubernetes', '4 GB RAM, 2 CPU cores', 'PostgreSQL 14+', 'Node.js 18+'],
      changelog: ['v2.0.4 — Performance improvements for large workflows', 'v2.0.0 — New visual builder with AI suggestions', 'v1.9.0 — Added Slack & Teams integrations'],
      tags: ['automation', 'no-code', 'integration', 'workflows'],
      author: 'LX Obsidian Labs', released: 'February 2025', updated: 'June 12, 2026', license: 'Per-workflow license'
    },
    {
      id: 'cloudsync', name: 'CloudSync', tagline: 'Multi-cloud file sync',
      desc: 'Enterprise file synchronization across AWS S3, Google Cloud Storage, Azure Blob, and on-premise storage. Real-time syncing with conflict resolution.',
      category: 'developer-tools', categoryLabel: 'Developer Tools',
      price: 29.99, rating: 4.7, reviews: 145, downloads: 7800,
      version: '1.8.0', size: '16 MB', color: 'green', icon: 'monitor',
      features: ['Multi-cloud sync (AWS/GCP/Azure)', 'Real-time file watching', 'Conflict resolution', 'Version history (90 days)', 'Bandwidth throttling', 'End-to-end encryption'],
      requirements: ['Linux or macOS', '2 GB RAM', '200 MB disk space', 'Cloud provider credentials'],
      changelog: ['v1.8.0 — Azure Blob Storage support', 'v1.7.0 — Conflict resolution UI', 'v1.6.0 — End-to-end encryption'],
      tags: ['sync', 'cloud', 'storage', 'multi-cloud'],
      author: 'LX Obsidian Labs', released: 'April 2025', updated: 'May 30, 2026', license: 'Per-connector license'
    },
    {
      id: 'teamboard', name: 'TeamBoard', tagline: 'Collaborative project management',
      desc: 'Visual project management with Kanban boards, Gantt charts, time tracking, and resource allocation. Built for agile teams.',
      category: 'business', categoryLabel: 'Business',
      price: 24.99, rating: 4.5, reviews: 312, downloads: 19600,
      version: '2.1.3', size: '20 MB', color: 'amber', icon: 'calendar',
      features: ['Kanban & Scrum boards', 'Gantt chart timeline', 'Time tracking & reporting', 'Resource allocation', 'Real-time collaboration', 'Jira & GitHub integration'],
      requirements: ['Web browser (Chrome/Firefox/Edge)', 'Internet connection', '2 GB RAM recommended'],
      changelog: ['v2.1.3 — Fixed Gantt drag performance', 'v2.1.0 — Resource allocation view', 'v2.0.0 — New Kanban board with swimlanes'],
      tags: ['project-management', 'agile', 'kanban', 'collaboration'],
      author: 'LX Obsidian Labs', released: 'July 2024', updated: 'June 8, 2026', license: 'Per-user subscription'
    },
    {
      id: 'analytics-pro', name: 'Analytics Pro', tagline: 'Advanced BI dashboard',
      desc: 'Business intelligence platform with drag-and-drop dashboard builder, SQL query editor, custom visualizations, and automated report scheduling.',
      category: 'developer-tools', categoryLabel: 'Developer Tools',
      price: 79.99, rating: 4.9, reviews: 167, downloads: 5430,
      version: '5.0.2', size: '45 MB', color: 'purple', icon: 'star',
      features: ['Drag-and-drop dashboard builder', 'SQL query editor', 'Custom visualization library', 'Automated report scheduling', 'Data source connectors (50+)', 'Role-based access control'],
      requirements: ['Linux (Ubuntu 22+, RHEL 9+)', '8 GB RAM, 4 CPU cores', '2 GB disk space', 'PostgreSQL 14+ or MySQL 8+'],
      changelog: ['v5.0.2 — Bug fix for BigQuery connector', 'v5.0.0 — New AI-powered insights engine', 'v4.9.0 — Added Snowflake connector'],
      tags: ['analytics', 'bi', 'dashboards', 'reporting'],
      author: 'LX Obsidian Labs', released: 'January 2024', updated: 'June 25, 2026', license: 'Per-seat license'
    },
    {
      id: 'securegate', name: 'SecureGate', tagline: 'Enterprise access control',
      desc: 'Zero-trust access control platform with SSO, MFA, role-based policies, and real-time threat detection.',
      category: 'business', categoryLabel: 'Business',
      price: 69.99, rating: 4.6, reviews: 134, downloads: 6890,
      version: '1.3.1', size: '22 MB', color: 'teal', icon: 'lock',
      features: ['Single Sign-On (SAML/OIDC)', 'Multi-factor authentication', 'Role-based access control', 'Real-time threat detection', 'Access audit trails', 'Zero-trust architecture'],
      requirements: ['Linux (Ubuntu 22+, RHEL 9+)', '4 GB RAM, 2 CPU cores', '1 GB disk space', 'PostgreSQL 14+'],
      changelog: ['v1.3.1 — Patch for SAML assertion handling', 'v1.3.0 — OIDC provider support', 'v1.2.0 — Real-time threat detection'],
      tags: ['security', 'sso', 'access-control', 'zero-trust'],
      author: 'LX Obsidian Labs', released: 'May 2025', updated: 'June 20, 2026', license: 'Per-user license'
    },
    {
      id: 'synapse-ai', name: 'Synapse AI', tagline: 'AI browser automation extension',
      desc: 'An AI-powered Chrome / Edge / Brave extension that turns any web page into an autonomous, controllable workspace. Observe, plan, execute, verify — no API key required.',
      category: 'ai', categoryLabel: 'AI',
      price: 0, rating: 5.0, reviews: 128, downloads: 5400,
      version: '1.1.1', size: '115 KB', color: 'purple', icon: 'message',
      isExtension: true, githubUrl: 'https://github.com/lx-obsidian-labs/synapse-social',
      features: ['Autonomous web automation', 'AI content generation in your brand voice', 'Vision fallback via page screenshots', 'Self-learning memory & model rotation', 'No API key required', 'Playbooks for ChatGPT, Gmail, YouTube & more'],
      requirements: ['Google Chrome, Edge, or Brave (v100+)', 'Developer mode enabled', 'Unzip downloaded folder', 'No API key needed'],
      installSteps: ['Download the ZIP and unzip it', 'Open chrome://extensions', 'Turn on Developer mode', 'Click Load unpacked and select the folder', 'Pin Synapse AI and open the side panel'],
      changelog: ['v1.1.1 — Improved DOM extraction reliability', 'v1.1.0 — Added LinkedIn & Facebook playbooks', 'v1.0.0 — Initial release with 5 playbooks'],
      tags: ['ai', 'automation', 'extension', 'browser', 'free'],
      author: 'LX Obsidian Labs', released: 'June 2026', updated: 'June 2026', license: 'Free (Open Distribution)'
    }
  ];

  var CATEGORIES = [
    { id: 'all', label: 'All Apps', count: 0 },
    { id: 'developer-tools', label: 'Developer Tools', count: 0 },
    { id: 'ai', label: 'AI', count: 0 },
    { id: 'business', label: 'Business', count: 0 },
    { id: 'mobile', label: 'Mobile', count: 0 }
  ];

  CATEGORIES.forEach(function (cat) {
    cat.count = cat.id === 'all' ? APP_DATA.length : APP_DATA.filter(function (a) { return a.category === cat.id; }).length;
  });

  var state = {
    filter: 'all',
    sort: 'popular',
    priceFilter: 'all',
    view: 'grid',
    search: '',
    compare: [],
    recent: JSON.parse(localStorage.getItem('lx-mkt-recent') || '[]')
  };

  /* ---------- SORTING ---------- */

  function sortApps(apps, sortBy) {
    var sorted = apps.slice();
    switch (sortBy) {
      case 'popular': sorted.sort(function (a, b) { return b.downloads - a.downloads; }); break;
      case 'rating': sorted.sort(function (a, b) { return b.rating - a.rating || b.reviews - a.reviews; }); break;
      case 'newest': sorted.sort(function (a, b) { return new Date(b.updated) - new Date(a.updated); }); break;
      case 'price-low': sorted.sort(function (a, b) { return a.price - b.price; }); break;
      case 'price-high': sorted.sort(function (a, b) { return b.price - a.price; }); break;
      case 'name': sorted.sort(function (a, b) { return a.name.localeCompare(b.name); }); break;
      case 'downloads': sorted.sort(function (a, b) { return b.downloads - a.downloads; }); break;
    }
    return sorted;
  }

  function filterApps() {
    var apps = APP_DATA;
    if (state.filter !== 'all') apps = apps.filter(function (a) { return a.category === state.filter; });
    if (state.priceFilter === 'free') apps = apps.filter(function (a) { return a.price === 0; });
    else if (state.priceFilter === 'paid') apps = apps.filter(function (a) { return a.price > 0; });
    if (state.search) {
      var q = state.search.toLowerCase();
      apps = apps.filter(function (a) {
        return a.name.toLowerCase().indexOf(q) > -1 ||
          a.tagline.toLowerCase().indexOf(q) > -1 ||
          a.categoryLabel.toLowerCase().indexOf(q) > -1 ||
          (a.tags && a.tags.join(' ').toLowerCase().indexOf(q) > -1);
      });
    }
    return sortApps(apps, state.sort);
  }

  /* ---------- RENDER GRID ---------- */

  function renderGrid() {
    var grid = document.getElementById('appStoreGrid');
    var noResults = document.getElementById('mktNoResults');
    var resultsInfo = document.getElementById('mktResultsInfo');
    if (!grid) return;

    var apps = filterApps();

    if (resultsInfo) {
      resultsInfo.textContent = apps.length + ' app' + (apps.length !== 1 ? 's' : '') + ' found';
    }

    if (apps.length === 0) {
      grid.innerHTML = '';
      if (noResults) noResults.style.display = '';
      return;
    }
    if (noResults) noResults.style.display = 'none';

    var isListView = state.view === 'list';
    grid.className = isListView ? 'app-store-grid app-store-grid--list' : 'app-store-grid';

    var html = '';
    apps.forEach(function (app, i) {
      var isFree = app.price === 0;
      var priceLabel = isFree ? 'Free' : '$' + app.price.toFixed(2);
      var delay = (i % 4) + 1;
      var isComparing = state.compare.indexOf(app.id) > -1;

      if (isListView) {
        html += renderListCard(app, isFree, priceLabel, isComparing);
      } else {
        html += renderGridCard(app, isFree, priceLabel, delay, isComparing);
      }
    });

    grid.innerHTML = html;
    bindCardEvents(grid);
  }

  function renderGridCard(app, isFree, priceLabel, delay, isComparing) {
    var freeBadge = isFree ? '<span class="app-card__badge app-card__badge--free">Free</span>' : '';
    var premiumBadge = !isFree ? '<span class="app-card__badge app-card__badge--premium">Premium</span>' : '';
    var compareCheck = isComparing ? ' app-card--compare' : '';

    return '<div class="app-card reveal reveal--delay-' + delay + compareCheck + '" data-app-id="' + app.id + '">' +
      '<div class="app-card__header">' +
        '<div class="app-card__icon" data-color="' + app.color + '">' +
          '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">' + iconSVG(app.icon) + '</svg>' +
        '</div>' +
        '<div class="app-card__badges">' + freeBadge + premiumBadge + '</div>' +
        '<button class="app-card__compare-toggle" data-compare="' + app.id + '" title="Add to compare" aria-label="Compare ' + app.name + '">' +
          '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5"/></svg>' +
        '</button>' +
      '</div>' +
      '<div class="app-card__body">' +
        '<h3 class="app-card__title">' + app.name + '</h3>' +
        '<p class="app-card__tagline">' + app.tagline + '</p>' +
        '<div class="app-card__meta">' +
          '<span class="app-card__rating">&#9733; ' + app.rating + '</span>' +
          '<span class="app-card__reviews">' + formatNumber(app.reviews) + ' reviews</span>' +
          '<span class="app-card__downloads">' + formatNumber(app.downloads) + ' downloads</span>' +
        '</div>' +
        '<div class="app-card__cats">' +
          '<span class="app-card__cat">' + app.categoryLabel + '</span>' +
          '<span class="app-card__version">v' + app.version + '</span>' +
          (app.size ? '<span class="app-card__size">' + app.size + '</span>' : '') +
        '</div>' +
        '<div class="app-card__tags">' +
          (app.tags || []).slice(0, 3).map(function (t) { return '<span class="app-card__tag">' + t + '</span>'; }).join('') +
        '</div>' +
      '</div>' +
      '<div class="app-card__footer">' +
        '<button class="btn btn--primary btn--full app-card__btn" data-app="' + app.id + '" data-action="details">View Details</button>' +
        (isFree
          ? '<button class="btn btn--secondary btn--full app-card__btn" data-app="' + app.id + '" data-action="download">' + (app.isExtension ? 'Download Extension' : 'Download Free') + '</button>'
          : '<button class="btn btn--secondary btn--full app-card__btn" data-app="' + app.id + '" data-action="details">$' + app.price.toFixed(2) + ' — Buy Now</button>'
        ) +
      '</div>' +
    '</div>';
  }

  function renderListCard(app, isFree, priceLabel, isComparing) {
    var compareCheck = isComparing ? ' app-card--compare' : '';
    return '<div class="app-card app-card--list' + compareCheck + '" data-app-id="' + app.id + '">' +
      '<div class="app-card__list-left">' +
        '<div class="app-card__icon" data-color="' + app.color + '">' +
          '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">' + iconSVG(app.icon) + '</svg>' +
        '</div>' +
        '<div class="app-card__list-info">' +
          '<h3 class="app-card__title">' + app.name + '</h3>' +
          '<p class="app-card__tagline">' + app.tagline + '</p>' +
          '<div class="app-card__cats"><span class="app-card__cat">' + app.categoryLabel + '</span><span class="app-card__version">v' + app.version + '</span></div>' +
        '</div>' +
      '</div>' +
      '<div class="app-card__list-right">' +
        '<div class="app-card__meta"><span class="app-card__rating">&#9733; ' + app.rating + '</span><span class="app-card__downloads">' + formatNumber(app.downloads) + '</span></div>' +
        '<span class="app-card__price-label">' + (isFree ? 'Free' : '$' + app.price.toFixed(2)) + '</span>' +
        '<div class="app-card__list-actions">' +
          '<button class="btn btn--primary btn--small app-card__btn" data-app="' + app.id + '" data-action="details">Details</button>' +
          '<button class="app-card__compare-toggle" data-compare="' + app.id + '" title="Compare"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5"/></svg></button>' +
        '</div>' +
      '</div>' +
    '</div>';
  }

  function bindCardEvents(grid) {
    grid.querySelectorAll('[data-action="details"]').forEach(function (btn) {
      btn.addEventListener('click', function () { openAppModal(btn.getAttribute('data-app')); });
    });
    grid.querySelectorAll('[data-action="download"]').forEach(function (btn) {
      btn.addEventListener('click', function () { downloadApp(btn.getAttribute('data-app')); });
    });
    grid.querySelectorAll('[data-compare]').forEach(function (btn) {
      btn.addEventListener('click', function (e) {
        e.stopPropagation();
        toggleCompare(btn.getAttribute('data-compare'));
      });
    });
  }

  /* ---------- APP DETAIL MODAL ---------- */

  function openAppModal(id) {
    var app = APP_DATA.find(function (a) { return a.id === id; });
    if (!app) return;

    addToRecent(id);

    var overlay = document.getElementById('appDetailOverlay');
    if (!overlay) return;

    var isFree = app.price === 0;
    var priceLabel = isFree ? 'Free' : '$' + app.price.toFixed(2);

    var featuresHtml = app.features.map(function (f) {
      return '<li class="app-modal__feature"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-green)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>' + f + '</li>';
    }).join('');

    var reqHtml = app.requirements.map(function (r) { return '<li class="app-modal__req">' + r + '</li>'; }).join('');

    var changelogHtml = (app.changelog || []).map(function (c) {
      var parts = c.split(' — ');
      return '<li class="app-modal__changelog-item"><strong>' + parts[0] + '</strong>' + (parts[1] ? ' — ' + parts[1] : '') + '</li>';
    }).join('');

    var tagsHtml = (app.tags || []).map(function (t) {
      return '<span class="app-modal__tag">' + t + '</span>';
    }).join('');

    var related = APP_DATA.filter(function (a) { return a.id !== app.id && a.category === app.category; }).slice(0, 3);
    var relatedHtml = related.map(function (r) {
      return '<div class="app-modal__related-card" data-app="' + r.id + '" style="cursor:pointer;">' +
        '<div class="app-card__icon" data-color="' + r.color + '" style="width:36px;height:36px;">' +
          '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">' + iconSVG(r.icon) + '</svg>' +
        '</div>' +
        '<div><strong>' + r.name + '</strong><span>&#9733; ' + r.rating + '</span></div>' +
      '</div>';
    }).join('');

    var shareUrl = encodeURIComponent(window.location.origin + '/marketplace.html?app=' + app.id);
    var shareText = encodeURIComponent('Check out ' + app.name + ' on LX Obsidian Labs Marketplace!');

    overlay.innerHTML =
      '<div class="app-modal" role="dialog" aria-modal="true" aria-label="' + app.name + ' details">' +
        '<div class="app-modal__header">' +
          '<div class="app-modal__icon" data-color="' + app.color + '">' +
            '<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">' + iconSVG(app.icon) + '</svg>' +
          '</div>' +
          '<div class="app-modal__title-group">' +
            '<h2 class="app-modal__title">' + app.name + '</h2>' +
            '<p class="app-modal__tagline">' + app.tagline + '</p>' +
            '<div class="app-modal__cats">' +
              '<span class="app-card__cat">' + app.categoryLabel + '</span>' +
              '<span class="app-card__version">v' + app.version + '</span>' +
              '<span class="app-card__size">' + app.size + '</span>' +
            '</div>' +
          '</div>' +
          '<div class="app-modal__header-actions">' +
            '<div class="app-modal__share-btns">' +
              '<a href="https://twitter.com/intent/tweet?text=' + shareText + '&url=' + shareUrl + '" target="_blank" rel="noopener" class="app-modal__share-link" title="Share on X/Twitter"><svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg></a>' +
              '<a href="https://www.linkedin.com/shareArticle?mini=true&url=' + shareUrl + '&title=' + shareText + '" target="_blank" rel="noopener" class="app-modal__share-link" title="Share on LinkedIn"><svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/></svg></a>' +
              '<button class="app-modal__share-link" title="Copy link" onclick="navigator.clipboard.writeText(window.location.origin+\'/marketplace.html?app='+app.id+'\');if(window.LXToast)window.LXToast(\'Link copied!\',\'success\')"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg></button>' +
            '</div>' +
            '<button class="app-modal__close" id="appDetailClose" aria-label="Close">&times;</button>' +
          '</div>' +
        '</div>' +
        '<div class="app-modal__body">' +
          '<div class="app-modal__section"><h3 class="app-modal__section-title">About</h3><p class="app-modal__desc">' + app.desc + '</p></div>' +
          '<div class="app-modal__section"><h3 class="app-modal__section-title">Features</h3><ul class="app-modal__feature-list">' + featuresHtml + '</ul></div>' +
          '<div class="app-modal__section"><h3 class="app-modal__section-title">System Requirements</h3><ul class="app-modal__req-list">' + reqHtml + '</ul></div>' +
          (app.installSteps
            ? '<div class="app-modal__section"><h3 class="app-modal__section-title">Install</h3><ol class="app-modal__install">' +
                app.installSteps.map(function (s) { return '<li>' + s + '</li>'; }).join('') + '</ol></div>'
            : '') +
          '<div class="app-modal__section"><h3 class="app-modal__section-title">Details</h3>' +
            '<table class="app-modal__table">' +
              '<tr><td>Author</td><td>' + app.author + '</td></tr>' +
              '<tr><td>Released</td><td>' + app.released + '</td></tr>' +
              '<tr><td>Last Updated</td><td>' + app.updated + '</td></tr>' +
              '<tr><td>Size</td><td>' + app.size + '</td></tr>' +
              '<tr><td>License</td><td>' + app.license + '</td></tr>' +
              '<tr><td>Downloads</td><td>' + formatNumber(app.downloads) + '</td></tr>' +
              '<tr><td>Rating</td><td>&#9733; ' + app.rating + '/5 (' + formatNumber(app.reviews) + ' reviews)</td></tr>' +
            '</table></div>' +
          (changelogHtml
            ? '<div class="app-modal__section"><h3 class="app-modal__section-title">What\'s New</h3><ul class="app-modal__changelog">' + changelogHtml + '</ul></div>'
            : '') +
          (tagsHtml
            ? '<div class="app-modal__section"><h3 class="app-modal__section-title">Tags</h3><div class="app-modal__tags">' + tagsHtml + '</div></div>'
            : '') +
          '<div class="app-modal__section"><h3 class="app-modal__section-title">User Reviews</h3><div class="app-modal__reviews" id="appReviews">' + renderReviews(app.id) + '</div></div>' +
          (relatedHtml
            ? '<div class="app-modal__section"><h3 class="app-modal__section-title">Related Apps</h3><div class="app-modal__related">' + relatedHtml + '</div></div>'
            : '') +
        '</div>' +
        '<div class="app-modal__footer">' +
          '<div class="app-modal__price"><span class="app-modal__price-value">' + priceLabel + '</span><span class="app-modal__price-label">' + (isFree ? 'Free' : 'Premium') + '</span></div>' +
          '<div class="app-modal__actions">' +
            (isFree
              ? '<button class="btn btn--primary app-modal__action-btn" id="appDetailDownload">' + (app.isExtension ? 'Download Extension' : 'Download Free') + '</button>'
              : '<a href="contact.html" class="btn btn--primary app-modal__action-btn">Contact for Pricing</a>'
            ) +
            (app.isExtension ? '<a href="synapse.html" class="btn btn--ghost app-modal__action-btn">Install Guide</a>' : '') +
            '<button class="btn btn--ghost app-modal__action-btn" id="appDetailCloseBtn">Close</button>' +
          '</div>' +
        '</div>' +
      '</div>';

    overlay.classList.add('app-detail-overlay--visible');
    document.body.style.overflow = 'hidden';

    document.getElementById('appDetailClose').addEventListener('click', closeAppModal);
    document.getElementById('appDetailCloseBtn').addEventListener('click', closeAppModal);
    overlay.addEventListener('click', function (e) { if (e.target === overlay) closeAppModal(); });

    var dlBtn = document.getElementById('appDetailDownload');
    if (dlBtn) dlBtn.addEventListener('click', function () { downloadApp(app.id); });

    overlay.querySelectorAll('.app-modal__related-card').forEach(function (card) {
      card.addEventListener('click', function () {
        closeAppModal();
        setTimeout(function () { openAppModal(card.getAttribute('data-app')); }, 300);
      });
    });
  }

  function closeAppModal() {
    var overlay = document.getElementById('appDetailOverlay');
    if (overlay) { overlay.classList.remove('app-detail-overlay--visible'); document.body.style.overflow = ''; }
  }

  /* ---------- COMPARE ---------- */

  function toggleCompare(id) {
    var idx = state.compare.indexOf(id);
    if (idx > -1) { state.compare.splice(idx, 1); }
    else if (state.compare.length < 4) { state.compare.push(id); }
    else { if (window.LXToast) window.LXToast('Max 4 apps to compare', 'error'); return; }
    updateCompareTray();
    renderGrid();
  }

  function updateCompareTray() {
    var tray = document.getElementById('mktCompareTray');
    var count = document.getElementById('mktCompareCount');
    var items = document.getElementById('mktCompareItems');
    if (!tray) return;

    if (state.compare.length === 0) { tray.classList.remove('mkt-compare-tray--visible'); return; }
    tray.classList.add('mkt-compare-tray--visible');
    if (count) count.textContent = state.compare.length;

    if (items) {
      items.innerHTML = state.compare.map(function (id) {
        var app = APP_DATA.find(function (a) { return a.id === id; });
        if (!app) return '';
        return '<div class="mkt-compare-tray__item">' +
          '<span>' + app.name + '</span>' +
          '<button class="mkt-compare-tray__remove" data-remove="' + id + '">&times;</button>' +
        '</div>';
      }).join('');

      items.querySelectorAll('[data-remove]').forEach(function (btn) {
        btn.addEventListener('click', function () { toggleCompare(btn.getAttribute('data-remove')); });
      });
    }
  }

  function openCompareModal() {
    if (state.compare.length < 2) { if (window.LXToast) window.LXToast('Select at least 2 apps to compare', 'error'); return; }

    var apps = state.compare.map(function (id) { return APP_DATA.find(function (a) { return a.id === id; }); }).filter(Boolean);
    var overlay = document.getElementById('mktCompareOverlay');
    if (!overlay) return;

    var allFeatures = [];
    apps.forEach(function (app) {
      app.features.forEach(function (f) { if (allFeatures.indexOf(f) === -1) allFeatures.push(f); });
    });

    var headerHtml = '<div class="mkt-compare__header"><h2>Compare Apps</h2><button class="app-modal__close" id="compareClose">&times;</button></div>';

    var colsHtml = apps.map(function (app) {
      return '<div class="mkt-compare__col">' +
        '<div class="app-card__icon" data-color="' + app.color + '" style="width:48px;height:48px;margin:0 auto 0.5rem;">' +
          '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">' + iconSVG(app.icon) + '</svg>' +
        '</div>' +
        '<h3>' + app.name + '</h3>' +
        '<p>' + app.tagline + '</p>' +
      '</div>';
    }).join('');

    var rows = [
      { label: 'Price', fn: function (a) { return a.price === 0 ? 'Free' : '$' + a.price.toFixed(2); } },
      { label: 'Rating', fn: function (a) { return '&#9733; ' + a.rating + '/5'; } },
      { label: 'Reviews', fn: function (a) { return formatNumber(a.reviews); } },
      { label: 'Downloads', fn: function (a) { return formatNumber(a.downloads); } },
      { label: 'Version', fn: function (a) { return 'v' + a.version; } },
      { label: 'Size', fn: function (a) { return a.size; } },
      { label: 'License', fn: function (a) { return a.license; } },
      { label: 'Updated', fn: function (a) { return a.updated; } }
    ];

    var rowsHtml = rows.map(function (row) {
      var cells = apps.map(function (app) { return '<td>' + row.fn(app) + '</td>'; }).join('');
      return '<tr><td class="mkt-compare__row-label">' + row.label + '</td>' + cells + '</tr>';
    }).join('');

    var featRows = allFeatures.map(function (f) {
      var cells = apps.map(function (app) {
        var has = app.features.indexOf(f) > -1;
        return '<td>' + (has ? '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-green)" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>' : '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-text-muted)" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>') + '</td>';
      }).join('');
      return '<tr><td class="mkt-compare__row-label">' + f + '</td>' + cells + '</tr>';
    }).join('');

    overlay.innerHTML =
      '<div class="mkt-compare__modal">' +
        headerHtml +
        '<div class="mkt-compare__body">' +
          '<div class="mkt-compare__grid" style="grid-template-columns:200px repeat(' + apps.length + ', 1fr);">' +
            '<div class="mkt-compare__col mkt-compare__col--label"></div>' + colsHtml +
          '</div>' +
          '<table class="mkt-compare__table"><tbody>' + rowsHtml + '</tbody></table>' +
          '<h3 class="mkt-compare__section-title">Features</h3>' +
          '<table class="mkt-compare__table mkt-compare__table--features"><tbody>' + featRows + '</tbody></table>' +
        '</div>' +
      '</div>';

    overlay.classList.add('mkt-compare-overlay--visible');
    document.body.style.overflow = 'hidden';

    document.getElementById('compareClose').addEventListener('click', function () {
      overlay.classList.remove('mkt-compare-overlay--visible');
      document.body.style.overflow = '';
    });
    overlay.addEventListener('click', function (e) {
      if (e.target === overlay) { overlay.classList.remove('mkt-compare-overlay--visible'); document.body.style.overflow = ''; }
    });
  }

  /* ---------- RECENTLY VIEWED ---------- */

  function addToRecent(id) {
    state.recent = state.recent.filter(function (r) { return r !== id; });
    state.recent.unshift(id);
    if (state.recent.length > 6) state.recent = state.recent.slice(0, 6);
    localStorage.setItem('lx-mkt-recent', JSON.stringify(state.recent));
    renderRecent();
  }

  function renderRecent() {
    var section = document.getElementById('mktRecent');
    var track = document.getElementById('mktRecentTrack');
    if (!section || !track) return;

    var apps = state.recent.map(function (id) { return APP_DATA.find(function (a) { return a.id === id; }); }).filter(Boolean);
    if (apps.length === 0) { section.style.display = 'none'; return; }
    section.style.display = '';

    track.innerHTML = apps.map(function (app) {
      var isFree = app.price === 0;
      return '<div class="mkt-recent__card" data-app="' + app.id + '" style="cursor:pointer;">' +
        '<div class="app-card__icon" data-color="' + app.color + '">' +
          '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">' + iconSVG(app.icon) + '</svg>' +
        '</div>' +
        '<div class="mkt-recent__info">' +
          '<strong>' + app.name + '</strong>' +
          '<span>&#9733; ' + app.rating + ' · ' + (isFree ? 'Free' : '$' + app.price.toFixed(2)) + '</span>' +
        '</div>' +
      '</div>';
    }).join('');

    track.querySelectorAll('.mkt-recent__card').forEach(function (card) {
      card.addEventListener('click', function () { openAppModal(card.getAttribute('data-app')); });
    });
  }

  /* ---------- DOWNLOAD ---------- */

  function downloadApp(id) {
    var app = APP_DATA.find(function (a) { return a.id === id; });
    if (!app) return;
    if (app.isExtension) { downloadExtension(app); return; }
    if (window.LXToast) window.LXToast('Downloading ' + app.name, 'success');
    else alert('Downloading ' + app.name + ' v' + app.version + '...');
  }

  function downloadExtension(app) {
    var RELEASE_API = 'https://api.github.com/repos/lx-obsidian-labs/synapse-social/releases/latest';
    var FALLBACK = 'https://github.com/lx-obsidian-labs/synapse-social/releases/download/v1.1.1/synapse-ai-v1.1.1.zip';
    if (window.LXToast) window.LXToast('Preparing your download...', 'success');
    function go(url) { window.location.href = url; }
    fetch(RELEASE_API, { headers: { 'Accept': 'application/vnd.github+json' } })
      .then(function (r) { return r.ok ? r.json() : Promise.reject(); })
      .then(function (data) { var asset = data.assets && data.assets[0]; go((asset && asset.browser_download_url) ? asset.browser_download_url : FALLBACK); })
      .catch(function () { go(FALLBACK); });
  }

  /* ---------- REVIEWS ---------- */

  function renderReviews(appId) {
    var allReviews = JSON.parse(localStorage.getItem('lx-reviews') || '{}');
    var appReviews = allReviews[appId] || sampleReviews(appId);

    var html = appReviews.map(function (r) {
      var stars = '';
      for (var i = 0; i < 5; i++) stars += i < r.rating ? '&#9733;' : '&#9734;';
      return '<div class="app-modal__review"><div class="app-modal__review-header"><strong>' + r.author + '</strong><span class="app-modal__review-rating">' + stars + '</span></div><p class="app-modal__review-text">' + r.text + '</p><span class="app-modal__review-date">' + r.date + '</span></div>';
    }).join('');

    html += '<div class="app-modal__write-review"><h4>Write a Review</h4>' +
      '<div class="app-modal__review-form">' +
        '<input type="text" class="app-modal__review-input" id="reviewAuthor" placeholder="Your name">' +
        '<div class="app-modal__star-select" id="starSelect"><button class="star-btn" data-star="1">&#9733;</button><button class="star-btn" data-star="2">&#9733;</button><button class="star-btn" data-star="3">&#9733;</button><button class="star-btn" data-star="4">&#9733;</button><button class="star-btn" data-star="5">&#9733;</button></div>' +
        '<textarea class="app-modal__review-textarea" id="reviewText" placeholder="Write your review..."></textarea>' +
        '<button class="btn btn--primary btn--small" id="submitReview" data-app="' + appId + '">Submit Review</button>' +
      '</div></div>';

    return html;
  }

  function sampleReviews(appId) {
    var samples = {
      'optiscan': [{ author: 'Sarah K.', rating: 5, text: 'Game changer for our CI/CD pipeline. Caught vulnerabilities we missed for months.', date: '2 weeks ago' }, { author: 'Marcus J.', rating: 4, text: 'Excellent tool. AI suggestions are spot-on.', date: '1 month ago' }],
      'pulse-monitor': [{ author: 'DevOps Team A', rating: 5, text: 'Great free tool for monitoring our K8s cluster.', date: '3 weeks ago' }, { author: 'Alex R.', rating: 4, text: 'Solid monitoring for the price.', date: '2 months ago' }],
      'crm-mobile': [{ author: 'Jane D.', rating: 5, text: 'Best mobile CRM I\'ve used. Offline mode is a lifesaver.', date: '1 week ago' }, { author: 'Tom W.', rating: 5, text: 'Replaced our old CRM entirely.', date: '3 weeks ago' }],
      'synapse-ai': [{ author: 'Kai M.', rating: 5, text: 'Automates my entire social media workflow. No API key needed!', date: '5 days ago' }, { author: 'Lerato P.', rating: 5, text: 'The Gmail playbook alone saves me hours every week.', date: '2 weeks ago' }]
    };
    return samples[appId] || [{ author: 'Verified User', rating: 4, text: 'Solid application. Met our requirements well.', date: '1 month ago' }, { author: 'Tech Lead', rating: 5, text: 'Great support from the LX team.', date: '2 months ago' }];
  }

  /* ---------- INIT EVENTS ---------- */

  function initFilters() {
    var container = document.getElementById('appCategoryFilters');
    if (!container) return;
    container.innerHTML = CATEGORIES.map(function (cat) {
      return '<button class="app-filter' + (cat.id === state.filter ? ' app-filter--active' : '') + '" data-filter="' + cat.id + '">' + cat.label + ' <span class="app-filter__count">' + cat.count + '</span></button>';
    }).join('');

    container.querySelectorAll('.app-filter').forEach(function (btn) {
      btn.addEventListener('click', function () {
        container.querySelectorAll('.app-filter').forEach(function (b) { b.classList.remove('app-filter--active'); });
        btn.classList.add('app-filter--active');
        state.filter = btn.getAttribute('data-filter');
        renderGrid();
      });
    });
  }

  function initSort() {
    var sel = document.getElementById('mktSortSelect');
    if (sel) {
      sel.value = state.sort;
      sel.addEventListener('change', function () { state.sort = sel.value; renderGrid(); });
    }
  }

  function initPriceFilter() {
    document.querySelectorAll('.mkt-price-filter__btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        document.querySelectorAll('.mkt-price-filter__btn').forEach(function (b) { b.classList.remove('mkt-price-filter__btn--active'); });
        btn.classList.add('mkt-price-filter__btn--active');
        state.priceFilter = btn.getAttribute('data-price');
        renderGrid();
      });
    });
  }

  function initViewToggle() {
    document.querySelectorAll('.mkt-view-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        document.querySelectorAll('.mkt-view-btn').forEach(function (b) { b.classList.remove('mkt-view-btn--active'); });
        btn.classList.add('mkt-view-btn--active');
        state.view = btn.getAttribute('data-view');
        renderGrid();
      });
    });
  }

  function initSearch() {
    var input = document.getElementById('appSearchInput');
    var searchBtn = document.getElementById('appStoreSearch');
    var searchBar = document.getElementById('appSearchBar');
    if (searchBtn && searchBar) {
      searchBtn.addEventListener('click', function () {
        searchBar.classList.toggle('app-search-bar--visible');
        if (searchBar.classList.contains('app-search-bar--visible')) {
          var inp = searchBar.querySelector('#appSearchInput');
          if (inp) setTimeout(function () { inp.focus(); }, 100);
        }
      });
    }
    if (input) {
      var debounce;
      input.addEventListener('input', function () {
        clearTimeout(debounce);
        debounce = setTimeout(function () { state.search = input.value.trim(); renderGrid(); }, 200);
      });
    }
  }

  function initCompare() {
    var compareBtn = document.getElementById('mktCompareBtn');
    var clearBtn = document.getElementById('mktCompareClear');
    if (compareBtn) compareBtn.addEventListener('click', openCompareModal);
    if (clearBtn) clearBtn.addEventListener('click', function () { state.compare = []; updateCompareTray(); renderGrid(); });
  }

  function initStarSelector() {
    document.addEventListener('click', function (e) {
      var starBtn = e.target.closest('.star-btn');
      if (!starBtn) return;
      var rating = parseInt(starBtn.getAttribute('data-star'));
      var container = starBtn.closest('.app-modal__star-select');
      if (!container) return;
      container.querySelectorAll('.star-btn').forEach(function (btn, i) {
        btn.style.color = i < rating ? 'var(--color-accent)' : 'var(--color-text-muted)';
        btn.setAttribute('data-selected', i < rating ? 'true' : 'false');
      });
    });
  }

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

      if (!reviewText) { if (window.LXToast) window.LXToast('Please write your review.', 'error'); return; }
      if (selectedStars === 0) { if (window.LXToast) window.LXToast('Please select a star rating.', 'error'); return; }

      var allReviews = JSON.parse(localStorage.getItem('lx-reviews') || '{}');
      if (!allReviews[appId]) allReviews[appId] = [];
      allReviews[appId].push({ author: name, rating: selectedStars, text: reviewText, date: 'Just now' });
      localStorage.setItem('lx-reviews', JSON.stringify(allReviews));

      author.value = '';
      text.value = '';
      if (window.LXToast) window.LXToast('Thank you for your review!', 'success');
      var reviewsSection = document.getElementById('appReviews');
      if (reviewsSection) reviewsSection.innerHTML = renderReviews(appId);
    });
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

  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeAppModal(); });

  function init() {
    initFilters();
    initSort();
    initPriceFilter();
    initViewToggle();
    initSearch();
    initCompare();
    initStarSelector();
    initReviewSubmit();
    renderGrid();
    renderRecent();
  }

  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', init); } else { init(); }

  window.LXAppStore = { data: APP_DATA, openAppModal: openAppModal, closeAppModal: closeAppModal, downloadApp: downloadApp, renderGrid: renderGrid, renderReviews: renderReviews };

})();
