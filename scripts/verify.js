// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
// Preserve existing third-party attribution and applicable notices.
const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const dist = path.resolve(__dirname, '../dist');
if (fs.existsSync(path.join(dist,'docs'))) throw Error('Internal engineering documentation leaked into static output');
const required = ['index.html', 'pdf/index.html', 'synapse/web/index.html', '404.html', '_redirects', 'css/lx.css'];
for (const name of required) {
  if (!fs.existsSync(path.join(dist, name))) throw Error('Missing required asset: ' + name);
}
const home = fs.readFileSync(path.join(dist, 'index.html'), 'utf8');
const routes = fs.readFileSync(path.join(dist, '_redirects'), 'utf8');
if (!home.includes('href="/pdf/"')) throw Error('Homepage PDF Studio link missing');
if (!fs.existsSync(path.join(dist, 'pdf/index.html'))) throw Error('PDF Studio directory route missing');
if (fs.existsSync(path.join(dist, 'pdf.html'))) throw Error('Duplicate PDF entrypoint causes Pages clean URL conflict');
function check(dir) {
  for (const item of fs.readdirSync(dir, {withFileTypes:true})) {
    const file = path.join(dir, item.name);
    if (item.isDirectory()) check(file);
    else if (file.endsWith('.js') && !file.endsWith('.min.js')) {
      const result = cp.spawnSync(process.execPath, ['--check', file], {encoding:'utf8'});
      if (result.status !== 0) throw Error('JavaScript syntax error in ' + file + ': ' + result.stderr);
    }
  }
}
check(dist);
console.log('Release verification passed: required assets, PDF route, homepage link and JS syntax');

const synapseWeb = fs.readFileSync(path.join(dist, 'synapse/web/index.html'), 'utf8');
if (!synapseWeb.includes("fetch('/api/chat'")) throw Error('Synapse web chat endpoint missing');
const synapseLanding = fs.readFileSync(path.join(dist, 'synapse.html'), 'utf8');
if (!synapseLanding.includes('/synapse/web') || !synapseLanding.includes('release-notes')) throw Error('Synapse release notes or web link missing');


// Marketing and enquiry regression checks.
const contact = fs.readFileSync(path.join(dist,'contact.html'),'utf8');
const contactApi = fs.readFileSync(path.join(dist,'functions/api/contact.js'),'utf8');
const portfolio = fs.readFileSync(path.join(dist,'portfolio.html'),'utf8');
const integrations = fs.readFileSync(path.join(dist,'integrations.html'),'utf8');
const ai = fs.readFileSync(path.join(dist,'ai.html'),'utf8');
if(!contact.includes("fetch('/api/contact'") || !contact.includes('result.saved') || !contact.includes('result.reference')) throw Error('Contact flow lacks server-confirmed durable submission');
if(contact.includes("window.open('https://wa.me/27694829711?text='")) throw Error('Legacy false-positive WhatsApp contact handler remains');
if(!contactApi.includes('INSERT INTO project_leads') || !contactApi.includes('env.DB.prepare')) throw Error('Lead storage endpoint is missing');
if(!contactApi.includes('Untrusted origin') || !contactApi.includes('emailOK')) throw Error('Server-side validation guard missing');
if(!portfolio.includes('id="project-snapshots"')) throw Error('Engineering case-study snapshots missing');
if(integrations.includes('href="#" class="integration-card"')) throw Error('Dead integration CTAs present');
if(ai.includes('index.html#pricing')) throw Error('Invalid AI pricing link present');
if(!/<h1\b[^>]*id="home-title"[^>]*>Website development, apps and AI<br>\s*<span>for South African businesses\.<\/span><\/h1>/.test(home)) throw Error('Homepage positioning reverted');
console.log('Marketing verification passed: secure lead capture, honest CTAs, case study snapshots, updated positioning');

// Site-wide design, navigation and link-integrity guardrails.
const htmlFiles = fs.readdirSync(dist).filter(name => name.endsWith('.html'));
const canonicalNavigation = home.match(/<header class="velora-nav"[^>]*>[\s\S]*?<\/header>/)?.[0];
if(!canonicalNavigation || !canonicalNavigation.includes('href="/#start-here"') || !canonicalNavigation.includes('href="/#study-tools"')) {
 throw Error('Shared navigation must use cross-page-safe homepage anchor URLs');
}
if (home.includes('https://www.linkedin.com') || home.includes('https://x.com') || home.includes('https://www.youtube.com')) throw Error('Generic social media domains are not verified company accounts');
for(const file of htmlFiles) {
 const content=fs.readFileSync(path.join(dist,file),'utf8');
 if(!/<h1[\s>]/i.test(content)) throw Error('Missing page H1: '+file);
 const nav=content.match(/<header class="velora-nav"[^>]*>[\s\S]*?<\/header>/)?.[0];
 if(nav && nav!==canonicalNavigation) throw Error('Navigation drift: '+file);
 for(const match of content.matchAll(/\b(?:src|href)=(["'])([^"']+)\1/g)) {
  const href=match[2];
  if(!href || /^(https?:|mailto:|tel:|data:|blob:|javascript:|\/\/|#)/i.test(href)) continue;
  let route=href.split(/[?#]/)[0].replace(/^\//,'');
  if(!route || route.startsWith('api/')) continue;
  const candidates=[route,route+'.html',route+'/index.html'];
  if(!candidates.some(candidate=>fs.existsSync(path.join(dist,candidate)))) throw Error('Broken local link '+href+' in '+file);
 }
}
if (fs.readFileSync(path.join(dist,'services.html'),'utf8').includes('Website Launch Pricing')) throw Error('Old price-led enterprise services section remains');
if (fs.readFileSync(path.join(dist,'about.html'),'utf8').includes("We don't use templates")) throw Error('Absolute no-templates claim remains');
console.log('Site-wide audit passed for '+htmlFiles.length+' pages: nav consistency, local links, core content claims');

// Download hub regression tests: verified generated files and honest links.
const downloads = fs.readFileSync(path.join(dist,'downloads.html'),'utf8');
const vistaRelease = JSON.parse(fs.readFileSync(path.join(dist,'vista-version.json'),'utf8'));
const expectedDownloads = [
 'lx-obsidian-capabilities-2026.pdf',
 'project-discovery-workbook.pdf',
 'vista-watch-install-guide.pdf',
 'project-requirements-template.csv'
];
for (const file of expectedDownloads) {
 const output=path.join(dist,'downloads',file);
 if (!fs.existsSync(output)) throw Error('Missing downloadable resource: '+file);
 if (!downloads.includes('/downloads/'+file)) throw Error('Downloads page does not link to: '+file);
 const bytes=fs.readFileSync(output);
 if (bytes.length<150) throw Error('Resource unexpectedly small: '+file);
 if (file.endsWith('.pdf') && (!bytes.subarray(0,8).toString('latin1').startsWith('%PDF-1.') || !bytes.toString('latin1').trimEnd().endsWith('%%EOF'))) throw Error('Invalid PDF wrapper: '+file);
}
if (!home.includes('href="/downloads"') || !home.includes('Downloads &amp; Resources')) throw Error('Downloads not discoverable in primary navigation/footer');
if (!downloads.includes('Version '+vistaRelease.versionName) || !downloads.includes('/api/vista-download?platform=android') || !downloads.includes('/api/vista-download?platform=tv')) throw Error('Latest official Android builds missing from downloads');
if (!downloads.includes('GitHub sign-in') || !downloads.includes('artifact may expire')) throw Error('Synapse build caveat missing');
if (downloads.includes('href="#"')) throw Error('Placeholder link on download hub');
console.log('Downloads checks passed: PDF headers, files, discoverability, release links');

// Resource-centre UX regression checks.
if (!downloads.includes('id="dlSearch"') || !downloads.includes('id="dlResults"') || !downloads.includes('id="dlEmpty"')) throw Error('Download centre search/empty states missing');
if (!downloads.includes('id="download-help"') || !downloads.includes('data-copy="'+vistaRelease.sha256+'"')) throw Error('Download safety guidance or published checksum missing');
if (!fs.readFileSync(path.join(dist,'services.html'),'utf8').includes('/downloads/project-discovery-workbook.pdf')) throw Error('Services resource CTA missing');
if (!contact.includes('/downloads/project-discovery-workbook.pdf')) throw Error('Contact brief workbook link missing');
if (!fs.readFileSync(path.join(dist,'vista.html'),'utf8').includes('Open Downloads &amp; Resources')) throw Error('Vista download centre link missing');
console.log('Resource centre UX checks passed');

for (const file of htmlFiles) {
 const html = fs.readFileSync(path.join(dist,file),'utf8');
 if (html.includes('Vista Cinema') || html.includes('VISTA CINEMA')) throw Error('Retired product name published in '+file);
 if (html.includes('vista-marketing-1.png') || html.includes('vista-marketing-2.png')) throw Error('Retired brand artwork referenced in '+file);
}
if (!downloads.includes('Vista Watch') || !fs.existsSync(path.join(dist,'assets/vista-watch-mark.svg'))) throw Error('Vista Watch identity is missing');
console.log('Vista Watch branding passed across published pages, identity assets and download guides');

for (const id of ['start-here','study-tools']) if (!home.includes('id="'+id+'"')) throw Error('Missing visitor journey: '+id);
for (const href of ['/pdf/','/synapse','/downloads','/contact']) if (!home.includes('href="'+href+'"')) throw Error('Missing task destination: '+href);

// Company history and transparent pricing regression guards.
const servicePage=fs.readFileSync(path.join(dist,'services.html'),'utf8');
const aboutPage=fs.readFileSync(path.join(dist,'about.html'),'utf8');
const aiSummary=fs.readFileSync(path.join(dist,'llms.txt'),'utf8');
for(const [name,content] of [['homepage',home],['about',aboutPage],['llms.txt',aiSummary]]) {
 if(!content.includes('Swaziland (now Eswatini)') || !content.includes('2020') || /Founded in South Africa in 2016/i.test(content)) throw Error('Company origin story mismatch: '+name);
}
const pricingKeys=['website','professional','ecommerce','mobile','automation','system','social-starter','social-growth','social-plus','care','seo','managed','domain-coza','domain-com'];
for(const key of pricingKeys){
 const card=servicePage.match(new RegExp('<article class="lx-rate" data-package="'+key+'">[\\s\\S]*?<\\/article>'))?.[0];
 if(!card || !card.includes('What you get') || !card.includes('What costs extra:') || !card.includes('lx-rate-price')) throw Error('Incomplete pricing disclosure: '+key);
 if(!contact.includes('value="'+key+'"')) throw Error('Quote option missing: '+key);
}
for(const domainKey of ['domain-coza','domain-com']) if(!servicePage.includes('href="/contact?offer='+domainKey+'"')) throw Error('Domain quote routing missing: '+domainKey);
if(!contact.includes('id="lxQuoteFeatures"') || !contact.includes('id="lxQuoteExclusions"') || !contact.includes("p.term==='annual'")) throw Error('Quote scope/annual pricing missing');
const currentFooter=home.match(/<footer class="glass-footer"[^>]*>[\s\S]*?<\/footer>/)?.[0]||'';
for(const local of ['nathan','sales','info','support','accounts','admin','partnerships','careers','hello']) {
 const email=local+'@lxobsidianportal.co.za';
 if(!currentFooter.includes('href="mailto:'+email+'"') || !currentFooter.includes('>'+email+'</a>')) throw Error('Public business email missing: '+email);
}
console.log('Company history, 14 pricing packages, quote options and public contact links verified');

// Public ecosystem: discovery, honest availability and working contact routes.
for (const slug of ['automation','careers','partners']) {
 const page=fs.readFileSync(path.join(dist,slug+'.html'),'utf8');
 if (!home.includes('href="/'+slug+'"') || !page.includes('https://www.lxobsidianportal.co.za/'+slug) || !page.includes('mailto:sales@lxobsidianportal.co.za?subject=')) throw Error('Ecosystem route or enquiry missing: '+slug);
 if (!fs.readFileSync(path.join(dist,'sitemap.xml'),'utf8').includes('https://www.lxobsidianportal.co.za/'+slug+'</loc>')) throw Error('Missing sitemap route: '+slug);
}
if (!fs.readFileSync(path.join(dist,'automation.html'),'utf8').includes('Public MCP releases are not yet available')) throw Error('MCP availability disclosure missing');
if (!fs.readFileSync(path.join(dist,'careers.html'),'utf8').includes('No open vacancies are currently advertised')) throw Error('Career availability disclosure missing');
if (/^\/partners\s+\/contact/m.test(routes)) throw Error('Partner page shadowed by legacy redirect');
console.log('Public ecosystem checks passed');

const publicTools=fs.readFileSync(path.join(dist,'tools.html'),'utf8');if(!home.includes('href="/tools"')||!publicTools.includes('data-tool="currency"')||!publicTools.includes('data-tool="password"')||!fs.existsSync(path.join(dist,'functions/api/public-data.js')))throw Error('Public tools release incomplete');console.log('Public utilities checks passed');

// Pricing consistency: every advertised base price matches the quote calculator.
const rateCard={"website":6000,"professional":12000,"ecommerce":22000,"mobile":35000,"automation":18000,"system":45000,"social-starter":950,"social-growth":1800,"social-plus":3500,"care":850,"seo":1800,"managed":3000,"domain-coza":249,"domain-com":499};
const planMatch=contact.match(/const plans=(\{[^\n]+\});/);if(!planMatch)throw Error('Quote rate data missing');const quoteRates=JSON.parse(planMatch[1]);
for(const [key,value] of Object.entries(rateCard)){const card=servicePage.match(new RegExp('<article class="lx-rate" data-package="'+key+'">[\\s\\S]*?<\\/article>'))?.[0];if(!card||!card.includes('R'+value.toLocaleString('en-US'))||quoteRates[key]?.price!==value)throw Error('Price mismatch: '+key);const option=contact.match(new RegExp('<option value="'+key+'">[^<]+<\\/option>'))?.[0];if(!option||!option.includes('R'+value.toLocaleString('en-US')))throw Error('Quote option price mismatch: '+key);}
if(!home.includes('R6,000')||!aiSummary.includes('R6,000')||!contact.includes('x*650+y')||!contact.includes('value="850">Website care'))throw Error('Pricing rollout incomplete');
console.log('Pricing consistency verified');

// Canonical consistency and high-priority SEO discovery.
const seoSitemap=fs.readFileSync(path.join(dist,'sitemap.xml'),'utf8');
const seoOrigin='https://www.lxobsidianportal.co.za';
const seoUrls=[...seoSitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map(m=>m[1]);
if(new Set(seoUrls).size!==seoUrls.length)throw Error('Duplicate sitemap entries');
for(const [route,file] of [['/pdf/','pdf/index.html'],['/privacy-policy','privacy-policy.html'],['/vista-terms','vista-terms.html'],['/synapse/web','synapse/web/index.html']]){const content=fs.readFileSync(path.join(dist,file),'utf8');if(!seoUrls.includes(seoOrigin+route)||!content.includes('rel="canonical" href="'+seoOrigin+route+'"'))throw Error('Canonical mismatch: '+route);}
for(const route of ['/software-development-guide-south-africa','/ai-automation-guide','/obsidian-labs','/engineering-case-studies'])if(!home.includes('href="'+route+'"'))throw Error('SEO discovery link missing: '+route);
if(!home.includes('R6,000')||!servicePage.includes('Website development, software and AI services in South Africa'))throw Error('Commercial SEO positioning missing');
console.log('SEO canonical and discovery checks passed');

// Obsidian Learn: public previews must not expose private paid course materials.
const courseCatalogue=fs.readFileSync(path.join(dist,'courses.html'),'utf8');
const firstCourse=fs.readFileSync(path.join(dist,'course-junior-developer.html'),'utf8');
if(!home.includes('href="/courses"')||!downloads.includes('href="/courses"')||!courseCatalogue.includes('href="/course-junior-developer"')) throw Error('Course discovery regression');
if(!firstCourse.includes('Paid enrolment') && !firstCourse.includes('paid enrolment')) throw Error('Missing enrolment disclosure');
if(!firstCourse.includes('id="curriculum"')||!firstCourse.includes('Video lessons')&&!firstCourse.includes('video lessons')) throw Error('Course syllabus or video status missing');
if(/href="[^"]+\.(pdf|zip)(?:[?#][^"]*)?"/i.test(firstCourse.match(/<main[\s\S]*?<\/main>/)[0])) throw Error('Private course materials linked publicly');
const courseSitemap=fs.readFileSync(path.join(dist,'sitemap.xml'),'utf8');
for(const route of ['/courses','/course-junior-developer']) if(!courseSitemap.includes('https://www.lxobsidianportal.co.za'+route+'</loc>')) throw Error('Course sitemap missing '+route);
if(!fs.existsSync(path.join(dist,'assets','learn-developer-001.svg'))) throw Error('Course artwork missing');
console.log('Obsidian Learn course discovery, syllabus and paid-preview guardrails passed');

const navGroups=[...canonicalNavigation.matchAll(/data-nav-group="([^"]+)"/g)].map(x=>x[1]);
if(navGroups.join(',')!=='solutions,apps,learn,company') throw Error('Navigation must have exactly four destinations');
for(const route of ['/services#pricing','/portfolio','/downloads','/marketplace','/courses','/course-junior-developer','/#study-tools','/contact']) if(!canonicalNavigation.includes('href="'+route+'"')) throw Error('Grouped nav lost route '+route);
const bookIDs=[...firstCourse.matchAll(/data-book-id="(A\d{2})"/g)].map(m=>m[1]);
if(bookIDs.length!==13 || new Set(bookIDs).size!==13 || !firstCourse.includes('id="assessment"')) throw Error('13-book curriculum or assessment missing');
if(!courseCatalogue.includes('13 books')||!firstCourse.includes('Book 01 prepared')) throw Error('Programme material status missing');
console.log('Consolidated navigation and 13-book outcomes curriculum passed');

for(const id of ['academic-quality','academic-integrity']) if(!firstCourse.includes('id="'+id+'"')) throw Error('Academic framework missing: '+id);
if(!firstCourse.includes('not accredited')||!firstCourse.includes('not operational yet')||!firstCourse.includes('13 packs prepared')||!firstCourse.includes('automatically determine grades or sanctions')) throw Error('Academic claims or human oversight guard missing');
console.log('Academic quality and integrity preview guardrails passed');

// Publication catalogue: every book is represented and all paid materials stay outside public dist.
const publicationLibrary=fs.readFileSync(path.join(dist,'learn-library.html'),'utf8');
const libraryMain=publicationLibrary.match(/<main[\s\S]*?<\/main>/)[0];
for(let i=1;i<=13;i++){const n=String(i).padStart(2,'0');if(!libraryMain.includes('id="book-'+n+'"')||!firstCourse.includes('id="book-'+n+'"')||!fs.existsSync(path.join(dist,'assets','learn','books','b'+n+'.svg')))throw Error('Missing book '+n);}
if(!libraryMain.includes('386')||!libraryMain.includes('464')||!libraryMain.includes('Paid access and videos are not yet available'))throw Error('Library metadata/availability incorrect');
if(/href="[^"]+\.(pdf|pptx|zip)(?:[?#][^"]*)?"/i.test(libraryMain))throw Error('Private learning file exposed');
if(!canonicalNavigation.includes('href="/learn-library"')||!courseSitemap.includes('https://www.lxobsidianportal.co.za/learn-library</loc>'))throw Error('Library discovery missing');
console.log('13-book library preview and access guardrails passed');

const layoutCss=fs.readFileSync(path.join(dist,'css','site-layout.css'),'utf8');
for(const page of htmlFiles){const html=fs.readFileSync(path.join(dist,page),'utf8');if(!html.includes('/css/site-layout.css?v=20261119-3'))throw Error('Shared layout version missing: '+page);}
if(!currentFooter.includes('class="lx-footer-directory"')||!currentFooter.includes('lx-footer-links')||!home.includes('class="lx-home-paths"'))throw Error('Shared layout navigation/footer regression');
if((libraryMain.match(/class="learn-book-card lx-library-card"/g)||[]).length!==13||!libraryMain.includes('class="lx-book-preview"')||!firstCourse.includes('lx-course-jumps'))throw Error('Compact learning layout missing');
if(!layoutCss.includes('--lx-hero-top')||!layoutCss.includes('minmax(min(100%,300px),1fr)'))throw Error('Responsive layout rules missing');
console.log('Site-wide layout v2, compact library and footer disclosure checks passed');

for(const page of [courseCatalogue,firstCourse,publicationLibrary]){if(!page.includes('ol-international')||!page.includes('/css/learn-international.css?v=20261119-3')||!page.includes('aria-label="Obsidian Learn navigation"'))throw Error('School design system missing');}
if(!firstCourse.includes('id="sample-lesson"')||!firstCourse.includes('id="ol-sample-form"')||!firstCourse.includes('ol-enrol-card')||(firstCourse.match(/class="ol-module-group"/g)||[]).length!==4)throw Error('Programme structure or sample missing');
if((libraryMain.match(/data-library-book="/g)||[]).length!==13||!libraryMain.includes('id="ol-book-search"')||!libraryMain.includes('id="ol-stage-filter"'))throw Error('Library filtering missing');
if(!home.includes('id="ol-home-title"'))throw Error('School home discovery missing');
for(const file of ['css/learn-international.css','js/ui/learn-international.js'])if(!fs.existsSync(path.join(dist,file)))throw Error('School asset missing '+file);
console.log('International school layout, searchable library and sample lesson guardrails passed');

const businessCourse=fs.readFileSync(path.join(dist,'course-business-technology.html'),'utf8');
const businessLibrary=fs.readFileSync(path.join(dist,'learn-library-business.html'),'utf8');
if((businessCourse.match(/data-business-book="/g)||[]).length!==6||(businessLibrary.match(/data-library-book="/g)||[]).length!==6)throw Error('Business programme missing six books');
for(const route of ['/course-business-technology','/learn-library-business'])if(!courseCatalogue.includes('href="'+route+'"')||!courseSitemap.includes('https://www.lxobsidianportal.co.za'+route+'</loc>'))throw Error('Programme discovery missing '+route);
if(!courseCatalogue.includes('id="how-it-works"')||!courseCatalogue.includes('ol-student-photo')||!courseCatalogue.includes('id="libraries"'))throw Error('School journey or imagery missing');
if(/href="[^"]+\.(pdf|pptx|zip)(?:[?#][^"]*)?"/i.test(businessCourse.match(/<main[\s\S]*?<\/main>/)[0]+businessLibrary.match(/<main[\s\S]*?<\/main>/)[0]))throw Error('Private course materials exposed');
console.log('Two-programme school, learner journey and business course guardrails passed');

if((courseCatalogue.match(/data-programme-category="/g)||[]).length!==6||!courseCatalogue.includes('id="ol-course-grid"')||!courseCatalogue.includes('id="ol-programme-search"'))throw Error('Six-course catalogue grid missing');
for(const [slug,n] of [["course-ai-literacy",5],["course-digital-entrepreneurship",5],["course-cybersecurity-foundations",5],["course-data-analytics",6]]){const page=fs.readFileSync(path.join(dist,slug+'.html'),'utf8');if((page.match(/data-track-module="/g)||[]).length!==n||!page.includes('class="ol-track-quiz"')||!page.includes('Compact first-study editions prepared offline')||!courseCatalogue.includes('href="/'+slug+'"')||!courseSitemap.includes('https://www.lxobsidianportal.co.za/'+slug+'</loc>'))throw Error('Track preview incomplete: '+slug);}
console.log('Six-programme searchable catalogue grid and curriculum previews passed');

if(!courseCatalogue.includes('id="ol-mobile-filters"')||!fs.readFileSync(path.join(dist,'css','learn-international.css'),'utf8').includes('#ol-course-grid.ol-programme-grid'))throw Error('Catalogue responsive grid guard missing');

for(const [slug,lib,n] of [["course-junior-developer","learn-library",13],["course-business-technology","learn-library-business",6],["course-ai-literacy","learn-library-ai",5],["course-digital-entrepreneurship","learn-library-entrepreneurship",5],["course-cybersecurity-foundations","learn-library-cybersecurity",5],["course-data-analytics","learn-library-data-analytics",6]]){const c=fs.readFileSync(path.join(dist,slug+'.html'),'utf8'),l=fs.readFileSync(path.join(dist,lib+'.html'),'utf8'),m=l.match(/<main[\s\S]*?<\/main>/)[0];if((m.match(/data-library-book="/g)||[]).length!==n||!courseCatalogue.includes('href="/'+lib+'"')||!courseSitemap.includes('https://www.lxobsidianportal.co.za/'+lib+'</loc>'))throw Error('Professional library incomplete '+lib);if(/href="[^"]+\.(pdf|pptx|zip)(?:[?#][^"]*)?"/i.test(m+c.match(/<main[\s\S]*?<\/main>/)[0]))throw Error('Protected learning file exposed');}
if(!courseCatalogue.includes('650 pages and 881 slides')||!courseCatalogue.includes('40 textbook PDFs'))throw Error('Professional edition inventory missing');
console.log('Six professional study libraries and protected file guardrails passed');

// Verify each programme has subject photography and an original project diagram.
for(const [slug,n] of [["course-junior-developer","01"],["course-business-technology","02"],["course-ai-literacy","03"],["course-digital-entrepreneurship","04"],["course-cybersecurity-foundations","05"],["course-data-analytics","06"]]){const page=fs.readFileSync(path.join(dist,slug+'.html'),'utf8');if(!page.includes('id="visual-project"')||!page.includes('class="ol-student-photo ol-subject-photo"')||!page.includes('/assets/learn/project-'+n+'.svg')||!fs.existsSync(path.join(dist,'assets','learn','project-'+n+'.svg')))throw Error('Visual project missing '+slug);}
if((courseCatalogue.match(/class="ol-photo-credit"/g)||[]).length!==6)throw Error('Catalogue photo attribution missing');
console.log('Six subject photo cards and project diagrams verified');
