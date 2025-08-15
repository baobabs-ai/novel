<script lang="ts" setup>
import { TranslatorConfig, translate } from '@/domain/translate';
import { TranslateTaskDesc, TranslateTaskParams } from '@/model/Translator';
import { releaseKeepAlive, requestKeepAlive } from '@/util';

import CTaskCard from './CTaskCard.vue';

const emit = defineEmits<{
  'update:jp': [number];
  'update:baidu': [number];
  'update:youdao': [number];
  'update:gpt': [number];
  'update:sakura': [number];
}>();

const message = useMessage();

const title = ref('');
const chapterTotal = ref<number>();
const chapterFinished = ref(0);
const chapterError = ref(0);

const percentage = computed(() => {
  const processed = chapterFinished.value + chapterError.value;
  const total = chapterTotal.value ?? 1;
  return total === 0 ? 100 : Math.round((1000 * processed) / total) / 10;
});

const running = ref(false);
const cardRef = ref<InstanceType<typeof CTaskCard>>();

const startTask = async (
  desc: TranslateTaskDesc,
  params: TranslateTaskParams,
  translatorDesc: TranslatorConfig,
  callback?: {
    onProgressUpdated: (progress: {
      finished: number;
      error: number;
      total: number;
    }) => void;
  },
  signal?: AbortSignal,
) => {
  if (running.value) {
    message.info('A task is already running.');
    return 'fail';
  }

  const buildLabel = () => {
    const idToLaber = {
      baidu: 'Baidu',
      youdao: 'Youdao',
      gpt: 'GPT',
      sakura: 'Sakura',
    };
    let label = `${idToLaber[translatorDesc.id]} translation`;
    const suffixParts: string[] = [];
    if (params.level === 'expire') {
      suffixParts.push('Expired chapters');
    } else if (params.level === 'all') {
      suffixParts.push('All chapters');
    } else if (params.level === 'sync') {
      suffixParts.push('Sync from source');
    }
    if (params.forceMetadata) {
      suffixParts.push('Re-translate table of contents');
    }
    if (suffixParts.length > 0) {
      label = label + ` [${suffixParts.join('/')}]`;
    }
    return label;
  };

  running.value = true;
  cardRef.value!.clearLog();

  title.value = buildLabel();
  chapterTotal.value = undefined;
  chapterFinished.value = 0;
  chapterError.value = 0;

  const onProgressUpdated = () =>
    callback?.onProgressUpdated({
      finished: chapterFinished.value,
      error: chapterError.value,
      total: chapterTotal.value ?? 0,
    });

  await requestKeepAlive();
  const state = await translate(
    desc,
    params,
    {
      onStart: (total) => {
        chapterTotal.value = total;
        onProgressUpdated();
      },
      onChapterSuccess: ({ jp, zh }) => {
        if (jp !== undefined) emit('update:jp', jp);
        if (zh !== undefined) {
          if (translatorDesc.id === 'baidu') {
            emit('update:baidu', zh);
          } else if (translatorDesc.id === 'youdao') {
            emit('update:youdao', zh);
          } else if (translatorDesc.id === 'gpt') {
            emit('update:gpt', zh);
          } else {
            emit('update:sakura', zh);
          }
        }
        chapterFinished.value += 1;
        onProgressUpdated();
      },
      onChapterFailure: () => {
        chapterError.value += 1;
        onProgressUpdated();
      },
      log: (message: string, detail?: string[]) => {
        cardRef.value!.pushLog({ message, detail });
      },
    },
    translatorDesc,
    signal,
  );

  cardRef.value!.pushLog({ message: '\nFinished' });
  running.value = false;
  releaseKeepAlive();

  if (state === 'abort') {
    return 'abort';
  } else if (
    chapterTotal.value ===
    chapterFinished.value + chapterError.value
  ) {
    return 'complete';
  } else {
    return 'uncomplete';
  }
};

defineExpose({ startTask });
</script>

<template>
  <c-task-card ref="cardRef" :title="title" :running="running">
    <n-flex align="center" vertical size="large" style="flex: none">
      <n-progress type="circle" :percentage="percentage" />
      <n-text>
        Success {{ chapterFinished }}/{{ chapterTotal ?? '-' }}
        <br />
        Failure {{ chapterError }}/{{ chapterTotal ?? '-' }}
      </n-text>
    </n-flex>
  </c-task-card>
</template>
