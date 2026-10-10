/**
 * Liquid Glass Footer — newsletter form.
 * No backend endpoint is provisioned, so validate client-side and acknowledge.
 * If the form carries a `data-endpoint`, POST the email there instead.
 */
void (function () {
  'use strict';

  var form = document.getElementById('glassNews');
  if (!form) return;

  var input = form.querySelector('.glass-news__input');
  var note = document.getElementById('glassNewsNote');

  function say(message, ok) {
    if (!note) return;
    note.hidden = false;
    if (ok) note.setAttribute('data-state', 'ok');
    else note.removeAttribute('data-state');
    note.textContent = message;
  }

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    var value = input ? input.value.trim() : '';
    var valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

    if (!valid) {
      say('Please enter a valid email address.', false);
      if (input) input.focus();
      return;
    }

    var endpoint = form.getAttribute('data-endpoint');
    if (endpoint && window.fetch) {
      var button = form.querySelector('.glass-news__btn');
      if (button) {
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
      }
      var done = function () {
        if (button) {
          button.disabled = false;
          button.removeAttribute('aria-busy');
        }
      };
      window
        .fetch(endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: value, source: 'footer' }),
        })
        .then(function (res) {
          return res
            .json()
            .catch(function () {
              return {};
            })
            .then(function (data) {
              if (!res.ok) throw new Error(data.error || 'Request failed');
              return data;
            });
        })
        .then(function () {
          say('Thanks — you are on the list.', true);
          form.reset();
        })
        .catch(function (err) {
          say(
            (err && err.message) || 'Something went wrong. Please try again.',
            false
          );
        })
        .then(done);
      return;
    }

    say('Thanks — you are on the list.', true);
    form.reset();
  });
})();
