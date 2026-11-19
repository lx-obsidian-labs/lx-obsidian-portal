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
      count.textContent=visible+' of '+document.querySelectorAll('[data-library-book]').length+' books shown';empty.hidden=visible!==0;
    }
    search.addEventListener('input',filter);stage.addEventListener('change',filter);
    document.getElementById('ol-clear-filter').addEventListener('click',()=>{search.value='';stage.value='all';filter();search.focus()});
    bar.hidden=false;filter();
    window.addEventListener('hashchange',()=>{const target=document.getElementById(location.hash.slice(1));if(target&&(target.hidden||target.closest('[hidden]'))){search.value='';stage.value='all';filter();target.scrollIntoView({block:'start'});}});
  }
  const form=document.getElementById('ol-sample-form'),result=document.getElementById('ol-sample-result');
  if(form&&result){form.addEventListener('submit',e=>{e.preventDefault();const selected=form.querySelector('input[name="answer"]:checked');result.textContent=!selected?'Choose an answer first.':selected.value==='60'?'Correct: 3 ÷ 5 × 100 = 60%.':'Try again. Divide the completed tasks by the total tasks, then multiply by 100.';});}
  function openBook(){const id=location.hash.slice(1);if(!/^(book-\d{2}|bt-book-[1-6])$/.test(id))return;const target=document.getElementById(id);if(target&&target.tagName==='DETAILS'){target.open=true;target.scrollIntoView({block:'start'});}}
  const bf=document.getElementById('ol-bt-sample'),br=document.getElementById('ol-bt-result');if(bf&&br)bf.addEventListener('submit',e=>{e.preventDefault();const v=bf.querySelector('input[name="answer"]:checked');br.textContent=!v?'Choose an answer first.':v.value==='1900'?'Correct: R4,000 − R2,100 = R1,900.':'Try again: use paid sales, not all invoiced sales.';});
  openBook();window.addEventListener('hashchange',openBook);

  document.querySelectorAll('.ol-track-quiz').forEach(form=>form.addEventListener('submit',e=>{e.preventDefault();const a=form.querySelector('input[name="answer"]:checked'),r=form.querySelector('.ol-quiz-result');r.textContent=!a?'Choose an answer first.':a.value===form.dataset.correct?form.dataset.feedback:'Try again. Think about the evidence in the example.';}));
  const grid=document.getElementById('ol-course-grid'),sidebar=document.getElementById('ol-catalogue-filter'),searchForm=document.getElementById('ol-programme-search-form'),query=document.getElementById('ol-programme-search'),sort=document.getElementById('ol-programme-sort'),status=document.getElementById('ol-programme-count'),none=document.getElementById('ol-programme-empty');
  if(grid&&sidebar&&searchForm&&query&&sort&&status&&none){
    const cards=[...grid.querySelectorAll('[data-programme-category]')];
    const selected=n=>sidebar.querySelector('input[name="'+n+'"]:checked')?.value||'all';
    const filter=()=>{const terms=query.value.toLowerCase().trim().split(/\s+/).filter(Boolean),subject=selected('ol-subject'),duration=selected('ol-duration'),materials=selected('ol-materials');
      let visible=0;for(const card of cards){const w=Number(card.dataset.weeks),match=terms.every(t=>card.textContent.toLowerCase().includes(t))&&(subject==='all'||subject===card.dataset.programmeCategory)&&(duration==='all'||(duration==='short'?w<=10:w>10))&&(materials==='all'||materials===card.dataset.materials);card.hidden=!match;if(match)visible++;}
      const ordered=[...cards];if(sort.value==='short')ordered.sort((a,b)=>Number(a.dataset.weeks)-Number(b.dataset.weeks));if(sort.value==='long')ordered.sort((a,b)=>Number(b.dataset.weeks)-Number(a.dataset.weeks));if(sort.value==='az')ordered.sort((a,b)=>a.querySelector('h3').textContent.localeCompare(b.querySelector('h3').textContent));ordered.forEach(c=>grid.appendChild(c));status.textContent=visible+' of '+cards.length+' programmes';none.hidden=visible!==0;
    };
    sidebar.hidden=false;searchForm.hidden=false;document.getElementById('ol-sort-wrap').hidden=false;
    query.addEventListener('input',filter);searchForm.addEventListener('submit',e=>{e.preventDefault();filter();document.getElementById('catalogue').scrollIntoView({block:'start'});});sidebar.addEventListener('change',filter);sort.addEventListener('change',filter);
    document.getElementById('ol-programme-reset').addEventListener('click',()=>{query.value='';sort.value='default';sidebar.querySelectorAll('input[value="all"]').forEach(i=>i.checked=true);filter();query.focus();});filter();
  }
})();