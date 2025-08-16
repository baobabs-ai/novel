interface EpubParser {
  extractText: (doc: Document) => string[];
  injectTranslation: (
    doc: Document,
    mode: 'en' | 'en-jp' | 'jp-en' | 'zh' | 'zh-jp' | 'jp-zh',
    zhLinesList: string[][],
  ) => Document;
}

export const EpubParserV1: EpubParser = {
  extractText: (doc: Document) => {
    Array.from(doc.getElementsByTagName('rt')).forEach((node) =>
      node.parentNode!.removeChild(node),
    );
    Array.from(doc.getElementsByTagName('rp')).forEach((node) =>
      node.parentNode!.removeChild(node),
    );
    return Array.from(doc.body.getElementsByTagName('p'))
      .map((el) => el.innerText)
      .filter((it) => it.trim().length !== 0);
  },
  injectTranslation: (
    doc: Document,
    mode: 'en' | 'en-jp' | 'jp-en' | 'zh' | 'zh-jp' | 'jp-zh',
    zhLinesList: string[][],
  ) => {
    Array.from(doc.body.getElementsByTagName('p'))
      .filter((el) => el.innerText.trim().length !== 0)
      .forEach((el, index) => {
        if (mode === 'zh' || mode === 'en') {
          zhLinesList.forEach((lines) => {
            const p = document.createElement('p');
            const t = document.createTextNode(lines[index]);
            p.appendChild(t);
            el.parentNode!.insertBefore(p, el);
          });
          el.parentNode!.removeChild(el);
        } else if (mode === 'jp-zh' || mode === 'jp-en') {
          zhLinesList.forEach((lines) => {
            const p = document.createElement('p');
            const t = document.createTextNode(lines[index]);
            p.appendChild(t);
            el.parentNode!.insertBefore(p, el.nextSibling);
          });
          el.setAttribute('style', 'opacity:0.4;');
        } else {
          zhLinesList.forEach((lines) => {
            const p = document.createElement('p');
            const t = document.createTextNode(lines[index]);
            p.appendChild(t);
            el.parentNode!.insertBefore(p, el);
          });
          el.setAttribute('style', 'opacity:0.4;');
        }
      });

    return doc;
  },
};
