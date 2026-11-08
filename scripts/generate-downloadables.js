/* LX Obsidian Labs - dependency-free PDF resources generated at build time.
 * No external network calls, no customer information, no invented metrics.
 */
const fs = require('fs');
const path = require('path');
const OUT = path.resolve(__dirname, '../dist/downloads');
fs.mkdirSync(OUT, {recursive:true});
const PAGE_W = 595, PAGE_H = 842;
const textClean = s => String(s).replace(/[\u2013\u2014]/g,'-').replace(/[\u2018\u2019]/g,"'").replace(/[\u201c\u201d]/g,'"').replace(/\u2022/g,'-').replace(/[^\x20-\x7e]/g,' ');
const esc = s => textClean(s).replace(/\\/g,'\\\\').replace(/\(/g,'\\(').replace(/\)/g,'\\)');
const fmt = n => Number(n).toFixed(2);
class Document {
  constructor(label) { this.label = label; this.pages=[]; this.newPage(); }
  newPage() {
    this.commands=[];
    this.pages.push(this.commands);
    this.y=755;
    this.rect(0,805,PAGE_W,37,0.045,0.052,0.066);
    this.draw(43,817,'LX  /  OBSIDIAN LABS',11,true,[1,1,1]);
    this.draw(43,24,'LX OBSIDIAN LABS  /  '+this.label.toUpperCase(),8,false,[.44,.48,.53]);
  }
  rect(x,y,w,h,r,g,b) {this.commands.push(fmt(r)+' '+fmt(g)+' '+fmt(b)+' rg '+fmt(x)+' '+fmt(y)+' '+fmt(w)+' '+fmt(h)+' re f');}
  rule(y){this.rect(43,y,509,.7,.83,.85,.87)}
  draw(x,y,t,size=11,bold=false,color=[.08,.09,.12]) {
    this.commands.push(fmt(color[0])+' '+fmt(color[1])+' '+fmt(color[2])+' rg BT /'+(bold?'FB':'FR')+' '+fmt(size)+' Tf 1 0 0 1 '+fmt(x)+' '+fmt(y)+' Tm ('+esc(t)+') Tj ET');
  }
  ensure(height=36) { if(this.y-height < 68)this.newPage(); }
  title(t,subtitle) {
    this.ensure(95);this.draw(43,this.y,t,26,true);this.y-=32;
    if(subtitle){this.paragraph(subtitle,11,[.33,.38,.44]);this.y-=6;}
    this.rule(this.y);this.y-=26;
  }
  section(t) {this.ensure(55);this.draw(43,this.y,t,15,true);this.y-=24;}
  paragraph(t,size=11,color=[.18,.22,.27]) {
    let words=textClean(t).split(/\s+/),line='',lines=[];
    for(const word of words){let next=line?line+' '+word:word;if(next.length>(size===10?98:88)){lines.push(line);line=word;}else line=next;}
    if(line)lines.push(line);
    for(const s of lines){this.ensure(17);this.draw(43,this.y,s,size,false,color);this.y-=17;}
    this.y-=9;
  }
  bullet(t){this.ensure(20);this.draw(48,this.y,'-',12,true);this.paragraph('  '+t,10,[.17,.21,.26]);}
  field(label,extra=30){
    this.ensure(extra+35);this.draw(43,this.y,label,11,true);this.y-=20;
    this.rule(this.y);this.y-=extra;
  }
  footers(){
    this.pages.forEach((p,i)=>{
      const old=this.commands;this.commands=p;
      this.rect(43,48,509,.7,.82,.83,.85);
      this.draw(43,33,'lxobsidianportal.co.za  |  Information subject to project scoping',8,false,[.42,.45,.5]);
      this.draw(521,33,String(i+1),8,true,[.42,.45,.5]);
      this.commands=old;
    });
  }
  save(name) {
    this.footers();
    const objects=[
      '<< /Type /Catalog /Pages 2 0 R >>',
      '',
      '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
      '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>'
    ];
    const refs=[];
    for(const commands of this.pages) {
      const pageId=objects.length+1, streamId=pageId+1;
      objects.push('<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /FR 3 0 R /FB 4 0 R >> >> /Contents '+streamId+' 0 R >>');
      const stream=commands.join('\n');
      objects.push('<< /Length '+Buffer.byteLength(stream,'latin1')+' >>\nstream\n'+stream+'\nendstream');
      refs.push(pageId+' 0 R');
    }
    objects[1]='<< /Type /Pages /Kids ['+refs.join(' ')+'] /Count '+refs.length+' >>';
    let body='%PDF-1.4\n%\xE2\xE3\xCF\xD3\n', offsets=[0];
    objects.forEach((obj,i)=>{offsets.push(Buffer.byteLength(body,'latin1'));body+=(i+1)+' 0 obj\n'+obj+'\nendobj\n';});
    const start=Buffer.byteLength(body,'latin1');
    body+='xref\n0 '+(objects.length+1)+'\n0000000000 65535 f \n';
    offsets.slice(1).forEach(n=>{body+=String(n).padStart(10,'0')+' 00000 n \n';});
    body+='trailer\n<< /Size '+(objects.length+1)+' /Root 1 0 R >>\nstartxref\n'+start+'\n%%EOF\n';
    const file=path.join(OUT,name);
    fs.writeFileSync(file,Buffer.from(body,'latin1'));
    console.log('Download generated: '+name+' ('+this.pages.length+' pages)');
  }
}
const capabilities=new Document('Capabilities profile');
capabilities.title('ENGINEERING THAT MOVES BUSINESS FORWARD','2026 capabilities overview | Custom software, AI workflows and digital platforms.');
capabilities.section('Who we are');
capabilities.paragraph('LX Obsidian Labs is a South African software engineering and AI studio. We design, develop and improve software around the workflows, users and operational needs of each client.');
capabilities.section('Where we can help');
[
'CUSTOM SOFTWARE: Bespoke business platforms, administrative systems, portals and internal productivity tools.',
'AI & AUTOMATION: Workflow design, human-reviewed AI assistance, integration of model APIs and repetitive-task automation.',
'WEB & MOBILE: Responsive web applications, customer platforms and mobile experiences aligned to agreed requirements.',
'SYSTEMS & INTEGRATIONS: API connections, data flows, modernization and deployment architecture following feasibility assessment.',
'PRODUCT DESIGN: User journeys, prototypes, accessible interfaces and iterative usability improvements.'
].forEach(t=>capabilities.bullet(t));
capabilities.section('How an engagement works');
[
'01  Discover: Establish the problem, stakeholders, constraints and intended outcomes.',
'02  Scope: Agree priorities, deliverables, timeline and pricing before development.',
'03  Build: Develop in reviewable milestones with feedback and testing.',
'04  Handover: Agree release, documentation, maintenance and ownership terms.'
].forEach(t=>capabilities.bullet(t));
capabilities.section('Engineering examples');
capabilities.paragraph('Visit our public portfolio for project snapshots spanning education websites, retail interfaces and studio products. These examples demonstrate technical scope and are not unverified performance guarantees.');
capabilities.section('Discuss your project');
capabilities.paragraph('Website: https://www.lxobsidianportal.co.za/contact');
capabilities.paragraph('Email: vilanenathan@gmail.com  |  South Africa  |  Remote project discussions available');
capabilities.save('lx-obsidian-capabilities-2026.pdf');

const discovery=new Document('Project discovery workbook');
discovery.title('PROJECT DISCOVERY WORKBOOK','An editable planning aid: print this PDF or use our accompanying CSV template.');
discovery.paragraph('You do not need technical terminology. Fill in what you know, leave the rest blank, and use the answers to prepare a discovery conversation.');
discovery.section('01  Your business and the challenge');
discovery.field('Business / organization name',32);
discovery.field('Contact person and preferred reply method',32);
discovery.field('What is currently difficult, slow or expensive?',65);
discovery.field('Who experiences the problem? (staff, customers, suppliers)',48);
discovery.section('02  Your intended solution');
discovery.field('What would success look like six months after launch?',65);
discovery.field('Which three functions matter most?',65);
discovery.field('Do you have existing software, documents or data to integrate?',48);
discovery.section('03  Practical requirements');
discovery.field('Platforms needed (website, desktop, Android, iOS, internal portal)',43);
discovery.field('Who should have access? Roles and approvals',43);
discovery.field('Expected timing and any immovable dates',43);
discovery.field('Indicative budget range or constraints (optional)',43);
discovery.field('Data sensitivity, compliance and security considerations',52);
discovery.section('04  Next steps');
discovery.paragraph('Share the relevant information through our enquiry form. Do not send passwords, identification numbers, confidential records or protected material before appropriate arrangements are in place.');
discovery.paragraph('https://www.lxobsidianportal.co.za/contact');
discovery.save('project-discovery-workbook.pdf');

const install=new Document('Vista installation guide');
install.title('VISTA CINEMA / INSTALL GUIDE','Android phone, tablet and Android TV | Version 1.0.10 listed on the LX website.');
install.section('Before you begin');
install.bullet('Choose the package that matches your device: Android phone/tablet or Android TV.');
install.bullet('Use the official LX Obsidian download hub or the linked LX GitHub repository. Do not use unrelated download mirrors.');
install.bullet('Confirm that your device has sufficient storage and permits installation from the selected source.');
install.section('Android phone or tablet');
install.bullet('1. Visit https://www.lxobsidianportal.co.za/downloads on your Android device.');
install.bullet('2. Select Vista Cinema for Android and download the APK.');
install.bullet('3. Open the downloaded file in your Files or Downloads application.');
install.bullet('4. If prompted, grant installation permission only to the trusted app used to open the APK.');
install.bullet('5. Follow the Android installation prompts and launch Vista Cinema.');
install.bullet('New viewers see an introduction after choosing guest browsing or signing in; your name is optional. Existing users can replay it in Settings > About.');
install.bullet('Eligible movie/episode downloads wait for an unmetered connection. Manage completed offline copies in Library > Downloads; some sources remain streaming-only.');
install.section('Android TV / Google TV');
install.bullet('1. Open the official download page on a compatible device.');
install.bullet('2. Choose the Android TV build. Transfer the APK to the TV if required.');
install.bullet('3. Open it with your TV file manager; follow your device-specific installation steps.');
install.bullet('4. Launch the TV application and follow its on-screen guidance.');
install.section('Security and support');
install.paragraph('Android may display warnings for apps installed outside an app store. Do not override device security warnings unless you understand and trust the source. Older APK versions are maintained for compatibility access and may lack fixes.');
install.paragraph('Support: https://www.lxobsidianportal.co.za/contact');
install.save('vista-cinema-install-guide.pdf');

const rows=[
['Section','Question','Your answer','Priority (must / should / later)'],
['Business','Business or organization name','',''],
['Challenge','Problem the software must solve','','must'],
['Users','Primary users and stakeholders','','must'],
['Goals','How will you measure success?','','must'],
['Functionality','Top three capabilities','','must'],
['Integrations','Existing tools and systems','',''],
['Data','Data types and sensitivity','',''],
['Platforms','Web / Android / iOS / Desktop','',''],
['Security','Authentication and access roles','',''],
['Delivery','Preferred timeframe','',''],
['Budget','Indicative budget or constraints','',''],
['Operations','Support and handover expectations','',''],
['Other','Additional notes','','']
];
const csv=rows.map(row=>row.map(s=>'"'+String(s).replace(/"/g,'""')+'"').join(',')).join('\r\n')+'\r\n';
fs.writeFileSync(path.join(OUT,'project-requirements-template.csv'),csv,'utf8');
console.log('Download generated: project-requirements-template.csv');
