import { Locator, formatError } from '@/data';
import {
  TranslateTaskCallback,
  TranslateTaskParams,
  WenkuTranslateTask,
  WenkuTranslateTaskDesc,
} from '@/model/Translator';

import { Translator } from './Translator';

export const translateWenku = async (
  { novelId, volumeId }: WenkuTranslateTaskDesc,
  { level }: TranslateTaskParams,
  callback: TranslateTaskCallback,
  translator: Translator,
  signal?: AbortSignal,
) => {
  const {
    getTranslateTask,
    getChapterTranslateTask,
    updateChapterTranslation,
  } = Locator.wenkuNovelRepository.createTranslationApi(
    novelId,
    volumeId,
    translator.id,
    signal,
  );

  // Task
  let task: WenkuTranslateTask;
  try {
    callback.log(`Getting untranslated chapters ${volumeId}`);
    task = await getTranslateTask();
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') {
      callback.log(`Aborting translation task`);
      return 'abort';
    } else {
      callback.log(`An error occurred, ending the translation task: ${e}`);
      return;
    }
  }

  const chapters = task.toc
    .filter(({ chapterId }) => chapterId !== undefined)
    .map(({ chapterId, glossaryId }, index) => ({
      index,
      chapterId: chapterId!,
      glossaryId,
    }))
    .filter(({ glossaryId }) => {
      if (level === 'normal') {
        return glossaryId === undefined;
      } else if (level === 'expire') {
        return glossaryId === undefined || glossaryId !== task.glossaryId;
      } else {
        return true;
      }
    });

  callback.onStart(chapters.length);
  if (chapters.length === 0) {
    callback.log(`No chapters to update`);
  }

  const forceSeg = level === 'all';
  for (const { index, chapterId } of chapters) {
    try {
      callback.log(`\n[${index}] ${volumeId}/${chapterId}`);
      const cTask = await getChapterTranslateTask(chapterId);

      if (!forceSeg && cTask.glossaryId === cTask.oldGlossaryId) {
        callback.log(`No translation needed`);
        callback.onChapterSuccess({});
      } else {
        const textsZh = await translator.translate(cTask.paragraphJp, {
          glossary: cTask.glossary,
          oldTextZh: cTask.oldParagraphZh,
          oldGlossary: cTask.oldGlossary,
          force: forceSeg,
          signal,
        });
        callback.log('Uploading chapter');
        const state = await updateChapterTranslation(chapterId, {
          glossaryId: cTask.glossaryId,
          paragraphsZh: textsZh,
        });
        callback.onChapterSuccess({ zh: state });
      }
    } catch (e) {
      if (e === 'quit') {
        callback.log(`An error occurred, ending the translation task`);
        return;
      } else if (e instanceof DOMException && e.name === 'AbortError') {
        callback.log(`Aborting translation task`);
        return 'abort';
      } else {
        callback.log(`An error occurred, skipping: ${await formatError(e)}`);
        callback.onChapterFailure();
      }
    }
  }
};
