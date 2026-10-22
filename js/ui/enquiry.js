/* Confirm an enquiry only after the server has persisted it. */
(function () {
  'use strict';
  var form = document.getElementById('enquiryForm');
  if (!form) return;
  var intent = document.getElementById('enquiryIntent');
  var requested = new URLSearchParams(window.location.search).get('intent');
  if (['project', 'partnership', 'beta'].includes(requested)) intent.value = requested;
  function updateCopy() {
    var copy = {
      project: ['Tell us what you have in mind.', 'Share the problem you want to solve. We’ll use your brief to understand the work and discuss a practical next step.'],
      partnership: ['Let’s build together.', 'Tell us about your expertise, your organisation and the collaboration you have in mind.'],
      beta: ['Help shape what comes next.', 'Tell us about your tools and a workflow you’d like the platform to improve. We’ll use your enquiry to discuss early-access opportunities.']
    }[intent.value];
    document.getElementById('enquiryTitle').textContent = copy[0];
    document.getElementById('enquiryIntro').textContent = copy[1];
  }
  updateCopy();
  intent.addEventListener('change', updateCopy);
  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    if (!form.reportValidity()) return;
    var button = form.querySelector('button[type=submit]');
    var status = document.getElementById('enquiryStatus');
    button.disabled = true;
    button.setAttribute('aria-busy', 'true');
    status.textContent = 'Saving your enquiry…';
    try {
      var data = Object.fromEntries(new FormData(form));
      var response = await fetch('/api/contact', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)
      });
      var result = await response.json();
      if (!response.ok || !result.ok || !result.stored) throw new Error(result.error || 'Your enquiry could not be saved. Please try again.');
      status.textContent = 'Your enquiry has been saved. Thank you — we’ll review your brief and reply by email.';
      form.reset();
      if (requested && ['project', 'partnership', 'beta'].includes(requested)) intent.value = requested;
      updateCopy();
    } catch (error) {
      status.textContent = error.message || 'We could not save your enquiry. Please try again. Your brief is still here.';
    } finally {
      button.disabled = false;
      button.removeAttribute('aria-busy');
      status.focus();
    }
  });
})();
