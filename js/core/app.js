void (function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js').catch(function () {});
    }
  });
})();
