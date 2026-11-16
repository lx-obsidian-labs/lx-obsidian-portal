/* LX newsletter: explicit consent and confirmed durable storage. */
(function () {
  'use strict';
  var forms = document.querySelectorAll('form[data-newsletter], #glassNews');
  forms.forEach(function(form) {
    var input = form.querySelector('input[type="email"]');
    var button = form.querySelector('button[type="submit"]');
    var note = form.closest('[data-newsletter-wrap]')?.querySelector('[data-newsletter-note]') || document.getElementById('glassNewsNote');
    if (!input || !button) return;
    var consent = form.querySelector('input[name="newsletterConsent"]');
    if (!consent) {
      var label = document.createElement('label');
      label.style.cssText='display:flex;align-items:flex-start;gap:8px;font-size:.78rem;line-height:1.45;flex-basis:100%;margin-top:8px;color:inherit';
      consent = document.createElement('input');
      consent.type='checkbox'; consent.name='newsletterConsent'; consent.required=true;
      label.appendChild(consent);
      label.appendChild(document.createTextNode('I agree to receive LX Obsidian Labs business technology updates and offers by email. I can unsubscribe at any time.'));
      form.appendChild(label);
      form.style.flexWrap='wrap';
    }
    if (!note) { note=document.createElement('p'); form.insertAdjacentElement('afterend',note); }
    note.setAttribute('role','status');
    function say(message,ok){note.hidden=false;note.textContent=message;if(ok)note.setAttribute('data-state','ok');else note.removeAttribute('data-state');}
    form.addEventListener('submit',async function(e){
      e.preventDefault();
      var email=input.value.trim();
      if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)){say('Please enter a valid email address.',false);input.focus();return;}
      if(!consent.checked){say('Please confirm you want to receive our newsletter.',false);consent.focus();return;}
      button.disabled=true;button.setAttribute('aria-busy','true');
      try {
        var res=await fetch('/api/subscribe',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:email,consent:true,source:form.getAttribute('data-source')||'footer'})});
        var data=await res.json();
        if(!res.ok||data.stored!==true)throw new Error(data.error||'Your subscription could not be confirmed. Please try again.');
        say('Thanks — your newsletter signup has been saved.',true);form.reset();
      } catch(err){say(err.message||'Unable to subscribe. Please try again.',false);}
      finally{button.disabled=false;button.removeAttribute('aria-busy');}
    });
  });
})();