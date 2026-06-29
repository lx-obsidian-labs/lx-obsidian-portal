void (function () {
  'use strict';

  var style = document.createElement('style');
  style.textContent = '.adsbygoogle:empty { display: none !important; } .lx-fallback-ad { margin: 1rem 0; }';
  document.head.appendChild(style);

  var AD_NETWORK = window.LX_AD_CONFIG || {};

  var DEFAULT_ADS = [
    {
      id: 'ad-1',
      headline: 'Deploy globally in seconds',
      body: 'Cloudflare Workers — serverless at the edge. Scale your apps with zero cold starts.',
      domain: 'cloudflare.com',
      url: 'https://cloudflare.com/workers',
      cta: 'Learn More',
      type: 'native'
    },
    {
      id: 'ad-2',
      headline: 'Ship faster with Vercel',
      body: 'The platform for frontend developers. Deploy your Next.js, React, and static sites instantly.',
      domain: 'vercel.com',
      url: 'https://vercel.com',
      cta: 'Start Free',
      type: 'native'
    },
    {
      id: 'ad-3',
      headline: 'Supabase — Open Source Firebase',
      body: 'Database, auth, storage, and edge functions in one platform. Start your next project for free.',
      domain: 'supabase.com',
      url: 'https://supabase.com',
      cta: 'Build Free',
      type: 'native'
    },
    {
      id: 'ad-4',
      headline: 'AI Chat for your docs',
      body: 'Embed a ChatGPT-style assistant on any website. Supports markdown, code blocks, and file uploads.',
      domain: 'openai.com',
      url: 'https://openai.com',
      cta: 'Try Free',
      type: 'card'
    },
    {
      id: 'ad-5',
      headline: 'Monitor your uptime',
      body: 'Real-time monitoring, alerts, and status pages for your APIs and websites. Trusted by 50k+ teams.',
      domain: 'betteruptime.com',
      url: 'https://betteruptime.com',
      cta: 'Start Free',
      type: 'card'
    },
    {
      id: 'ad-6',
      headline: 'GitHub Copilot — Your AI pair programmer',
      body: 'Write code faster with AI-powered suggestions. Works with VS Code, JetBrains, and more.',
      domain: 'github.com',
      url: 'https://github.com/features/copilot',
      cta: 'Try Free',
      type: 'text'
    }
  ];

  var ads = AD_NETWORK.ads || DEFAULT_ADS;
  var adIndex = 0;
  var fallbackElements = [];

  function renderNative(ad) {
    return '<div class="ad ad--native lx-fallback-ad" data-ad-id="' + ad.id + '">' +
      '<span class="ad__label">Sponsored</span>' +
      '<a href="' + ad.url + '" class="ad__link" target="_blank" rel="noopener noreferrer">' +
        '<div class="ad__content">' +
          '<div class="ad__icon">' +
            '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-accent)" stroke-width="2" stroke-linecap="round"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>' +
          '</div>' +
          '<div class="ad__text">' +
            '<div class="ad__headline">' + ad.headline + '</div>' +
            '<div class="ad__body">' + ad.body + '</div>' +
          '</div>' +
        '</div>' +
        '<div class="ad__footer">' +
          '<span class="ad__domain">' + ad.domain + '</span>' +
          '<span class="ad__cta">' + ad.cta + '</span>' +
        '</div>' +
      '</a>' +
    '</div>';
  }

  function renderCard(ad) {
    return '<div class="ad ad--card lx-fallback-ad" data-ad-id="' + ad.id + '">' +
      '<span class="ad__label">Sponsored</span>' +
      '<a href="' + ad.url + '" class="ad__link" target="_blank" rel="noopener noreferrer">' +
        '<div class="ad__card-icon">' +
          '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--color-accent)" stroke-width="1.5" stroke-linecap="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="17"/></svg>' +
        '</div>' +
        '<div class="ad__card-headline">' + ad.headline + '</div>' +
        '<div class="ad__card-body">' + ad.body + '</div>' +
        '<span class="ad__cta ad__cta--card">' + ad.cta + ' &rarr;</span>' +
      '</a>' +
    '</div>';
  }

  function renderText(ad) {
    return '<div class="ad ad--text lx-fallback-ad" data-ad-id="' + ad.id + '">' +
      '<span class="ad__label">Sponsored</span>' +
      '<a href="' + ad.url + '" class="ad__link" target="_blank" rel="noopener noreferrer">' +
        '<span class="ad__text-headline">' + ad.headline + '</span>' +
        '<span class="ad__text-cta">' + ad.cta + '</span>' +
      '</a>' +
    '</div>';
  }

  function renderBanner(ad) {
    return '<div class="ad ad--banner lx-fallback-ad" data-ad-id="' + ad.id + '">' +
      '<span class="ad__label">Sponsored</span>' +
      '<a href="' + ad.url + '" class="ad__link" target="_blank" rel="noopener noreferrer">' +
        '<div class="ad__banner-content">' +
          '<div class="ad__banner-icon">' +
            '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-accent)" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' +
          '</div>' +
          '<div class="ad__banner-text">' +
            '<span class="ad__banner-headline">' + ad.headline + '</span>' +
            '<span class="ad__banner-body">' + ad.body + '</span>' +
          '</div>' +
          '<span class="ad__cta">' + ad.cta + '</span>' +
        '</div>' +
      '</a>' +
    '</div>';
  }

  function getNextAd() {
    var ad = ads[adIndex % ads.length];
    adIndex++;
    return ad;
  }

  function fillSlot(el) {
    if (el.hasAttribute('data-ad-filled')) return;
    var type = el.getAttribute('data-ad-type') || 'native';
    var ad = getNextAd();
    var html = '';
    switch (type) {
      case 'card': html = renderCard(ad); break;
      case 'text': html = renderText(ad); break;
      case 'banner': html = renderBanner(ad); break;
      default: html = renderNative(ad); break;
    }
    el.insertAdjacentHTML('afterend', html);
    el.setAttribute('data-ad-filled', 'true');
    if (el.nextElementSibling && el.nextElementSibling.classList.contains('lx-fallback-ad')) {
      fallbackElements.push(el.nextElementSibling);
    }
  }

  function hideFallbacks() {
    for (var i = 0; i < fallbackElements.length; i++) {
      if (fallbackElements[i]) {
        fallbackElements[i].style.display = 'none';
      }
    }
  }

  function checkRealAds() {
    var slots = document.querySelectorAll('.adsbygoogle');
    if (!slots.length) return;
    for (var i = 0; i < slots.length; i++) {
      var iframe = slots[i].querySelector('iframe');
      if (iframe && slots[i].offsetHeight > 50) {
        hideFallbacks();
        break;
      }
    }
  }

  function initAds() {
    var slots = document.querySelectorAll('[data-ad-slot]');
    if (!slots.length) return;
    for (var i = 0; i < slots.length; i++) {
      fillSlot(slots[i]);
    }
    setTimeout(checkRealAds, 4000);
    setTimeout(checkRealAds, 8000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAds);
  } else {
    initAds();
  }

  window.LXAdClick = function (id) {
    console.log('[LX Ads] Click: ' + id);
  };

})();
