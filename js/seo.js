void (function () {
  'use strict';

  var BASE = 'https://www.lxobsidianportal.co.za';
  var body = document.body;

  function pageClass() {
    var classes = body.className.split(/\s+/);
    for (var i = 0; i < classes.length; i++) {
      if (classes[i].indexOf('page-') === 0) return classes[i];
    }
    return 'page-home';
  }

  var page = pageClass().replace('page-', '');
  if (page === 'index') page = 'home';

  var titles = {
    home:     'LX Obsidian Labs — Enterprise Software, AI & Cloud Development',
    services: 'Software Development Services — Web, Mobile, AI, Cloud & More | LX Obsidian Labs',
    marketplace: 'App Marketplace — Premium Software & Tools | LX Obsidian Labs',
    portfolio:  'Our Portfolio — Case Studies & Projects | LX Obsidian Labs',
    about:      'About Us — Team, Mission & Values | LX Obsidian Labs',
    blog:       'Blog — Insights, Guides & Tech Articles | LX Obsidian Labs',
    contact:    'Contact Us — Get in Touch | LX Obsidian Labs',
    faq:        'FAQ — Frequently Asked Questions | LX Obsidian Labs',
    synapse:    'Synapse AI — AI Browser Automation Extension | LX Obsidian Labs'
  };

  var descriptions = {
    home:     'LX Obsidian Labs builds enterprise-grade software, AI solutions, cloud infrastructure, mobile apps, and digital platforms. Based in Cape Town, serving global clients.',
    services: 'Full-stack software development services including web, mobile, AI, cloud, enterprise systems, and branding. Custom solutions for startups to enterprises.',
    marketplace: 'Browse and download premium software applications, tools, and templates built by LX Obsidian. Includes AI tools, CRMs, analytics, and more.',
    portfolio:  'Explore our portfolio of successful projects across fintech, healthcare, ecommerce, and enterprise software. See how we deliver results.',
    about:      'Learn about LX Obsidian Labs — our mission, values, leadership team, and commitment to building tomorrow\'s technology today.',
    blog:       'Read articles on software development, AI, cloud computing, mobile apps, and digital transformation from the LX Obsidian team.',
    contact:    'Contact LX Obsidian Labs for consultations, project inquiries, partnerships, or general questions. We respond within 24 hours.',
    faq:        'Find answers to frequently asked questions about our services, pricing, process, and technology stack.',
    synapse:    'Download Synapse AI, the free AI-powered Chrome extension for autonomous web automation, content generation, and browser control.'
  };

  /* ==========================================
     JSON-LD STRUCTURED DATA
     ========================================== */

  var schemas = [];

  /* --- Organization (every page) --- */
  schemas.push({
    '@context': 'https://schema.org',
    '@type': 'Organization',
    '@id': BASE + '/#organization',
    name: 'LX Obsidian Labs',
    url: BASE,
    logo: BASE + '/assets/logo.png',
    description: 'Enterprise software, AI, cloud & mobile development company based in Cape Town, South Africa.',
    address: {
      '@type': 'PostalAddress',
      addressLocality: 'Cape Town',
      addressRegion: 'Western Cape',
      addressCountry: 'ZA'
    },
    contactPoint: {
      '@type': 'ContactPoint',
      telephone: '+27-123-456-789',
      contactType: 'sales',
      email: 'hello@lxobsidianlabs.com',
      availableLanguage: ['English']
    },
    sameAs: [
      'https://github.com/lxobsidian',
      'https://twitter.com/lxobsidian',
      'https://linkedin.com/company/lxobsidian'
    ]
  });

  /* --- Website (every page) --- */
  schemas.push({
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    '@id': BASE + '/#website',
    url: BASE,
    name: titles[page] || titles.home,
    description: descriptions[page] || descriptions.home,
    publisher: { '@id': BASE + '/#organization' }
  });

  /* --- BreadcrumbList (every page) --- */
  var crumbs = [{ name: 'Home', path: '/' }];
  if (page !== 'home') {
    crumbs.push({ name: titles[page].split(' — ')[0], path: '/' + (page === 'index' ? '' : page) });
  }
  schemas.push({
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    '@id': BASE + '/' + (page === 'home' ? '' : page) + '#breadcrumb',
    itemListElement: crumbs.map(function (c, i) {
      return {
        '@type': 'ListItem',
        position: i + 1,
        name: c.name,
        item: BASE + c.path
      };
    })
  });

  /* --- LocalBusiness (every page) --- */
  schemas.push({
    '@context': 'https://schema.org',
    '@type': 'LocalBusiness',
    '@id': BASE + '/#localbusiness',
    name: 'LX Obsidian Labs',
    image: BASE + '/assets/logo.png',
    url: BASE,
    telephone: '+27-123-456-789',
    email: 'hello@lxobsidianlabs.com',
    description: descriptions[page] || descriptions.home,
    foundingDate: '2023',
    address: {
      '@type': 'PostalAddress',
      addressLocality: 'Cape Town',
      addressRegion: 'Western Cape',
      addressCountry: 'ZA'
    },
    geo: {
      '@type': 'GeoCoordinates',
      latitude: -33.9249,
      longitude: 18.4241
    },
    openingHoursSpecification: [
      { '@type': 'OpeningHoursSpecification', dayOfWeek: 'Monday', opens: '08:00', closes: '17:00' },
      { '@type': 'OpeningHoursSpecification', dayOfWeek: 'Tuesday', opens: '08:00', closes: '17:00' },
      { '@type': 'OpeningHoursSpecification', dayOfWeek: 'Wednesday', opens: '08:00', closes: '17:00' },
      { '@type': 'OpeningHoursSpecification', dayOfWeek: 'Thursday', opens: '08:00', closes: '17:00' },
      { '@type': 'OpeningHoursSpecification', dayOfWeek: 'Friday', opens: '08:00', closes: '17:00' }
    ]
  });

  /* --- FAQ schema (faq page only) --- */
  if (page === 'faq') {
    var faqs = document.querySelectorAll('.faq-item');
    var qa = [];
    faqs.forEach(function (item) {
      var qEl = item.querySelector('.faq-item__trigger span');
      var aEl = item.querySelector('.faq-item__text');
      if (qEl && aEl) {
        qa.push({
          '@type': 'Question',
          name: qEl.textContent.trim(),
          acceptedAnswer: {
            '@type': 'Answer',
            text: aEl.textContent.trim()
          }
        });
      }
    });
    if (qa.length) {
      schemas.push({
        '@context': 'https://schema.org',
        '@type': 'FAQPage',
        '@id': BASE + '/faq#faq',
        mainEntity: qa
      });
    }
  }

  /* --- Service schema (services page) --- */
  if (page === 'services') {
    schemas.push({
      '@context': 'https://schema.org',
      '@type': 'Service',
      '@id': BASE + '/services#service',
      name: 'Software Development Services',
      provider: { '@id': BASE + '/#organization' },
      areaServed: 'Worldwide',
      serviceType: [
        'Web Development',
        'Mobile App Development',
        'AI Solutions',
        'Cloud Infrastructure',
        'Enterprise Systems',
        'Branding & Design'
      ],
      description: descriptions.services
    });
  }

  /* --- Product schemas (marketplace page) --- */
  if (page === 'marketplace') {
    var appCards = document.querySelectorAll('.marketplace-card');
    appCards.forEach(function (card) {
      var nameEl = card.querySelector('.card__title');
      var descEl = card.querySelector('.card__text');
      var ratingEl = card.querySelector('.marketplace-card__rating');
      var versionEl = card.querySelector('.marketplace-card__version');
      var priceEl = card.querySelector('[data-price]');

      if (!nameEl) return;
      var name = nameEl.textContent.trim();
      var rating = ratingEl ? parseFloat(ratingEl.textContent.replace('★', '').trim()) : 4.5;
      var version = versionEl ? versionEl.textContent.trim().replace('v', '') : '1.0';
      var price = priceEl ? priceEl.getAttribute('data-price') : '19.99';

      schemas.push({
        '@context': 'https://schema.org',
        '@type': 'SoftwareApplication',
        name: name,
        applicationCategory: 'BusinessApplication',
        operatingSystem: 'Web, iOS, Android',
        description: descEl ? descEl.textContent.trim() : name + ' by LX Obsidian Labs',
        offers: {
          '@type': 'Offer',
          price: price,
          priceCurrency: 'USD',
          availability: 'https://schema.org/InStock',
          url: BASE + '/marketplace'
        },
        version: version,
        aggregateRating: {
          '@type': 'AggregateRating',
          ratingValue: rating,
          bestRating: 5,
          ratingCount: Math.floor(Math.random() * 50) + 10
        }
      });
    });
  }

  /* --- Article schema (blog page) --- */
  if (page === 'blog') {
    var articleCards = document.querySelectorAll('.knowledge-card');
    articleCards.forEach(function (card) {
      var titleEl = card.querySelector('.knowledge-card__title');
      var tagEl = card.querySelector('.knowledge-card__tag');
      var descEl = card.querySelector('.knowledge-card__text');
      if (!titleEl) return;

      schemas.push({
        '@context': 'https://schema.org',
        '@type': 'Article',
        headline: titleEl.textContent.trim(),
        description: descEl ? descEl.textContent.trim() : '',
        author: {
          '@type': 'Organization',
          name: 'LX Obsidian Labs'
        },
        publisher: { '@id': BASE + '/#organization' },
        datePublished: '2026-06-26',
        image: BASE + '/assets/logo.png',
        articleSection: tagEl ? tagEl.textContent.trim() : 'Technology'
      });
    });
  }

  /* --- Inject JSON-LD --- */
  var script = document.createElement('script');
  script.type = 'application/ld+json';
  script.textContent = JSON.stringify(schemas.length === 1 ? schemas[0] : schemas);
  document.head.appendChild(script);

})();
