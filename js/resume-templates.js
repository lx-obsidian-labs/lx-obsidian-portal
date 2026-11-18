// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
export const templates = [
  {
    id: 'classic',
    name: 'Classic',
    useCase: 'General applications',
    description: 'A straightforward layout for most applications.',
    sections: ['skills', 'experience', 'education'],
    accent: '162033',
    sectionRule: 'A9B3C7',
    sample: {
      name: 'Thandi Maseko',
      title: 'Office Administrator',
      email: 'thandi@example.test',
      location: 'Johannesburg',
      summary: 'Office administrator experienced in coordinating schedules, maintaining records and supporting customer enquiries.',
      skills: 'Excel, record keeping, scheduling',
      experience: [{ name: 'Office Assistant', place: 'Cedar Lane Services', dates: '2023–Present', details: 'Managed a shared calendar for a team of six.\nIntroduced a tracker for outstanding requests.' }],
      education: [{ name: 'Diploma in Office Management', place: '', dates: '2022', details: '' }]
    }
  },
  {
    id: 'graduate',
    name: 'Graduate',
    useCase: 'First jobs',
    description: 'Puts qualifications first for students and recent graduates.',
    sections: ['education', 'skills', 'experience'],
    accent: '1D4B91',
    sectionRule: '9FB9DF',
    centreHeader: true,
    sample: {
      name: 'Kabelo Dlamini',
      title: 'Business Management Graduate',
      email: 'kabelo@example.test',
      location: 'Middelburg',
      summary: 'Business Management graduate with practical coursework in budgeting, spreadsheets and office administration.',
      skills: 'Excel, budgeting, stock records',
      experience: [{ name: 'Stock Tracking Project', place: 'Course project', dates: '2026', details: 'Built an Excel workbook to record stock movements.\nUsed formulas to flag low-stock items.' }],
      education: [{ name: 'N6 Business Management', place: 'TVET coursework', dates: '2026', details: 'Financial accounting and entrepreneurship.' }]
    }
  },
  {
    id: 'modern',
    name: 'Modern',
    useCase: 'Work & projects',
    description: 'Highlights projects and experience before the skills list.',
    sections: ['experience', 'skills', 'education'],
    accent: '145B4D',
    sectionRule: '92BCB0',
    sample: {
      name: 'Lerato Nkosi',
      title: 'Junior Web Developer',
      email: 'lerato@example.test',
      location: 'Cape Town',
      summary: 'Junior developer who builds accessible interfaces and documents how to maintain them.',
      skills: 'JavaScript, Git, responsive design',
      experience: [{ name: 'Community Events Website', place: 'Volunteer project', dates: '2026', details: 'Built a mobile-friendly event directory.\nAdded keyboard navigation to the category filters.' }],
      education: [{ name: 'Web Development Certificate', place: '', dates: '2025', details: '' }]
    }
  }
];

export function getTemplate(id) {
  return templates.find(template => template.id === id) || templates[0];
}

export const accents = {
  navy: { accent: '162033', sectionRule: 'A9B3C7' },
  blue: { accent: '1D4B91', sectionRule: '9FB9DF' },
  green: { accent: '145B4D', sectionRule: '92BCB0' },
  plum: { accent: '63345E', sectionRule: 'C9AAC5' }
};

export function getCustomTemplate(template, { accent, alignment, firstSection }) {
  const palette = Object.hasOwn(accents, accent) ? accents[accent] : {};
  return {
    ...template,
    ...palette,
    centreHeader: alignment === 'default' ? template.centreHeader : alignment === 'centre',
    sections: template.sections.includes(firstSection)
      ? [firstSection, ...template.sections.filter(section => section !== firstSection)]
      : template.sections
  };
}
