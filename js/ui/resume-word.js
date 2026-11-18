// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
import { Document, HeadingLevel, Packer, Paragraph, TextRun } from 'docx';

export function makeWordDocument(rows) {
  const headings = { H2: HeadingLevel.TITLE, H3: HeadingLevel.HEADING_1, H4: HeadingLevel.HEADING_2 };
  const children = rows.map(({ tagName, text }) => new Paragraph({
    heading: headings[tagName],
    ...(tagName === 'LI' ? { bullet: { level: 0 } } : {}),
    children: text.split(/\r?\n/).map((line, index) => new TextRun({ text: line, break: index ? 1 : 0 })),
  }));
  const document = new Document({
    styles: { default: { document: { run: { font: 'Arial', size: 22 } } } },
    sections: [{ properties: { page: { size: { width: 11906, height: 16838 } } }, children }],
  });
  return Packer.toBlob(document);
}
