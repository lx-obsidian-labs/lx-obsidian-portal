// Engineering: LX Obsidian Labs | https://www.lxobsidianportal.co.za
const encoder = new TextEncoder();

function checksum(bytes) {
  let crc = -1;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
  }
  return (crc ^ -1) >>> 0;
}

// Word documents are ZIP packages. These small XML parts can be stored uncompressed.
function zip(files) {
  const local = [];
  const directory = [];
  let offset = 0;
  let directorySize = 0;

  for (const [path, contents] of Object.entries(files)) {
    const name = encoder.encode(path);
    const bytes = typeof contents === 'string' ? encoder.encode(contents) : contents;
    const crc = checksum(bytes);
    const header = new DataView(new ArrayBuffer(30));
    header.setUint32(0, 0x04034b50, true);
    header.setUint16(4, 20, true);
    header.setUint16(12, 33, true); // DOS date: 1980-01-01.
    header.setUint32(14, crc, true);
    header.setUint32(18, bytes.length, true);
    header.setUint32(22, bytes.length, true);
    header.setUint16(26, name.length, true);

    const entry = new DataView(new ArrayBuffer(46));
    entry.setUint32(0, 0x02014b50, true);
    entry.setUint16(4, 20, true);
    entry.setUint16(6, 20, true);
    entry.setUint16(14, 33, true);
    entry.setUint32(16, crc, true);
    entry.setUint32(20, bytes.length, true);
    entry.setUint32(24, bytes.length, true);
    entry.setUint16(28, name.length, true);
    entry.setUint32(42, offset, true);

    local.push(header.buffer, name, bytes);
    directory.push(entry.buffer, name);
    offset += header.byteLength + name.length + bytes.length;
    directorySize += entry.byteLength + name.length;
  }

  const end = new DataView(new ArrayBuffer(22));
  end.setUint32(0, 0x06054b50, true);
  end.setUint16(8, directory.length / 2, true);
  end.setUint16(10, directory.length / 2, true);
  end.setUint32(12, directorySize, true);
  end.setUint32(16, offset, true);
  return new Blob([...local, ...directory, end.buffer], {
    type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  });
}

function xml(text) {
  const entities = { '&': '&amp;', '<': '&lt;', '>': '&gt;' };
  return text.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\uFFFE\uFFFF]/g, '')
    .replace(/[&<>]/g, value => entities[value]);
}

function portraitParagraph(alignment) {
  const size = 800100;
  return '<w:p><w:pPr><w:keepNext/>' + alignment + '</w:pPr><w:r><w:drawing><wp:inline>' +
    '<wp:extent cx="' + size + '" cy="' + size + '"/><wp:docPr id="1" name="CV portrait" descr="CV portrait"/>' +
    '<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic>' +
    '<pic:nvPicPr><pic:cNvPr id="1" name="portrait.jpg"/><pic:cNvPicPr/></pic:nvPicPr>' +
    '<pic:blipFill><a:blip r:embed="rId2"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>' +
    '<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="' + size + '" cy="' + size + '"/></a:xfrm>' +
    '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>' +
    '</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>';
}

export function createWordDocument(blocks, template, photo = '') {
  const accent = template.accent;
  const rule = template.sectionRule;
  const headerAlignment = template.centreHeader ? '<w:jc w:val="center"/>' : '';
  const font = template.font || 'Arial';
  const titleSize = (template.titlePoints || 24) * 2;
  const border = size => '<w:pBdr><w:bottom w:val="single" w:sz="' + size + '" w:space="3" w:color="' + rule + '"/></w:pBdr>';
  const word = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main';
  const office = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships';
  const packageNs = 'http://schemas.openxmlformats.org/package/2006';
  const namespaces = photo ? ' xmlns:r="' + office + '" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"' : '';
  const paragraphs = blocks.map(({ kind, text, headerEnd }) => {
    const style = { name: 'Title', title: 'CVTitle', contact: 'CVContact', portfolio: 'CVPortfolio', section: 'Heading1', heading: 'Heading2', bullet: 'CVBullet' }[kind] || 'Normal';
    const value = kind === 'bullet' ? '• ' + text : text;
    const headerBorder = headerEnd && template.headerBorder ? border(template.separatePortfolio ? 18 : 4) : '';
    return '<w:p><w:pPr><w:pStyle w:val="' + style + '"/>' + headerBorder + '</w:pPr><w:r><w:t xml:space="preserve">' + xml(value) + '</w:t></w:r></w:p>';
  }).join('');
  return zip({
    '[Content_Types].xml': '<Types xmlns="' + packageNs + '/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/>' + (photo ? '<Default Extension="jpg" ContentType="image/jpeg"/>' : '') + '<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/></Types>',
    '_rels/.rels': '<Relationships xmlns="' + packageNs + '/relationships"><Relationship Id="rId1" Type="' + office + '/officeDocument" Target="word/document.xml"/></Relationships>',
    'word/_rels/document.xml.rels': '<Relationships xmlns="' + packageNs + '/relationships"><Relationship Id="rId1" Type="' + office + '/styles" Target="styles.xml"/>' + (photo ? '<Relationship Id="rId2" Type="' + office + '/image" Target="media/portrait.jpg"/>' : '') + '</Relationships>',
    'word/document.xml': '<w:document xmlns:w="' + word + '"' + namespaces + '><w:body>' + (photo ? portraitParagraph(headerAlignment) : '') + paragraphs + '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="950" w:right="950" w:bottom="950" w:left="950" w:header="0" w:footer="0" w:gutter="0"/></w:sectPr></w:body></w:document>',
    ...(photo ? { 'word/media/portrait.jpg': Uint8Array.from(atob(photo.split(',')[1]), char => char.charCodeAt(0)) } : {}),
    'word/styles.xml': '<w:styles xmlns:w="' + word + '">' +
      '<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:pPr><w:spacing w:after="85" w:line="275" w:lineRule="auto"/></w:pPr><w:rPr><w:rFonts w:ascii="' + font + '" w:hAnsi="' + font + '"/><w:sz w:val="22"/></w:rPr></w:style>' +
      '<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:pPr><w:keepNext/>' + headerAlignment + '</w:pPr><w:rPr><w:b/><w:color w:val="' + accent + '"/><w:sz w:val="' + titleSize + '"/></w:rPr></w:style>' +
      '<w:style w:type="paragraph" w:styleId="CVTitle"><w:name w:val="CV Role"/><w:basedOn w:val="Normal"/><w:pPr><w:keepNext/>' + headerAlignment + '</w:pPr></w:style>' +
      '<w:style w:type="paragraph" w:styleId="CVContact"><w:name w:val="CV Contact"/><w:basedOn w:val="Normal"/><w:pPr>' + headerAlignment + '</w:pPr><w:rPr><w:color w:val="44516A"/><w:sz w:val="20"/></w:rPr></w:style>' +
      '<w:style w:type="paragraph" w:styleId="CVPortfolio"><w:name w:val="CV Portfolio"/><w:basedOn w:val="CVContact"/><w:rPr><w:color w:val="' + accent + '"/></w:rPr></w:style>' +
      '<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:pPr><w:keepNext/>' + (template.sectionBorder ? border(4) : '') + '<w:spacing w:before="230" w:after="90"/><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:b/><w:color w:val="' + accent + '"/><w:sz w:val="24"/></w:rPr></w:style>' +
      '<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:pPr><w:keepNext/><w:spacing w:before="105" w:after="45"/><w:outlineLvl w:val="1"/></w:pPr><w:rPr><w:b/></w:rPr></w:style>' +
      '<w:style w:type="paragraph" w:styleId="CVBullet"><w:name w:val="CV Bullet"/><w:basedOn w:val="Normal"/><w:pPr><w:ind w:left="260" w:hanging="160"/></w:pPr></w:style></w:styles>'
  });
}
