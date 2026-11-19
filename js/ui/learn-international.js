/* LX Obsidian Labs: progressive enhancement for public learning previews.
   No user data is stored, submitted or shared. */
(function(){
  'use strict';
  const search=document.getElementById('ol-book-search'),stage=document.getElementById('ol-stage-filter'),bar=document.getElementById('ol-library-filter'),count=document.getElementById('ol-library-results'),empty=document.getElementById('ol-no-results');
  if(search&&stage&&bar&&count&&empty){
    const sections=Array.from(document.querySelectorAll('[data-library-stage]'));
    const normalize=s=>s.toLowerCase().normalize('NFKD').replace(/\p{M}/gu,'').trim();
    function filter(){
      const q=normalize(search.value),terms=q.split(/\s+/).filter(Boolean);
      let visible=0;
      sections.forEach(section=>{
        let shown=0;
        section.querySelectorAll('[data-library-book]').forEach(card=>{
          const matches=(stage.value==='all'||stage.value===section.dataset.libraryStage)&&terms.every(t=>normalize(card.textContent).includes(t));
          card.hidden=!matches;if(matches){shown++;visible++;}
        });
        section.hidden=shown===0;
      });
      count.textContent=visible+' of 13 books shown';empty.hidden=visible!==0;
    }
    search.addEventListener('input',filter);stage.addEventListener('change',filter);
    document.getElementById('ol-clear-filter').addEventListener('click',()=>{search.value='';stage.value='all';filter();search.focus()});
    bar.hidden=false;filter();
    window.addEventListener('hashchange',()=>{const target=document.getElementById(location.hash.slice(1));if(target&&(target.hidden||target.closest('[hidden]'))){search.value='';stage.value='all';filter();target.scrollIntoView({block:'start'});}});
  }
  const form=document.getElementById('ol-sample-form'),result=document.getElementById('ol-sample-result');
  if(form&&result){form.addEventListener('submit',e=>{e.preventDefault();const selected=form.querySelector('input[name="answer"]:checked');result.textContent=!selected?'Choose an answer first.':selected.value==='60'?'Correct: 3 ÷ 5 × 100 = 60%.':'Try again. Divide the completed tasks by the total tasks, then multiply by 100.';});}
  function openBook(){const id=location.hash.slice(1);if(!/^book-\d{2}$/.test(id))return;const target=document.getElementById(id);if(target&&target.tagName==='DETAILS'){target.open=true;target.scrollIntoView({block:'start'});}}
  openBook();window.addEventListener('hashchange',openBook);
})();