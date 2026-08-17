void (function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    console.log('[LX Obsidian Portal] Application initialized');

    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js').catch(function () {});
    }
  });
})();
