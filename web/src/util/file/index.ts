import { Epub } from './epub';
import { Srt } from './srt';
import { Txt } from './txt';
import { StandardNovel } from './standard';

export { Epub, Srt, Txt, StandardNovel };
export type ParsedFile = Epub | Srt | Txt;

export const getFullContent = async (file: File) => {
  if (file.name.endsWith('.txt') || file.name.endsWith('.srt')) {
    const txt = await Txt.fromFile(file);
    return txt.text;
  } else if (file.name.endsWith('.epub')) {
    const epub = await Epub.fromFile(file);
    return await epub.getText();
  } else {
    return '';
  }
};

export const parseFile = async (
  file: File,
  allowExts = ['epub', 'txt', 'srt'],
) => {
  const ext = file.name.split('.').pop()?.toLowerCase();
  if (ext === undefined) throw 'Unable to get file extension';
  if (allowExts.includes(ext)) {
    try {
      if (ext === 'txt') {
        return await Txt.fromFile(file);
      } else if (ext === 'epub') {
        return await Epub.fromFile(file);
      } else if (ext === 'srt') {
        return await Srt.fromFile(file);
      }
    } catch (e) {
      throw `Unable to parse ${ext.toUpperCase()} file, because: ${e}`;
    }
  }
  throw 'Unsupported file format';
};
