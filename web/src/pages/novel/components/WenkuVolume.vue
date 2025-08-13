<script lang="ts" setup>
import { FileDownloadOutlined } from '@vicons/material';
import { useKeyModifier } from '@vueuse/core';

import { Locator } from '@/data';
import { WenkuNovelRepository } from '@/data/api';
import {
  TranslateTaskDescriptor,
  TranslateTaskParams,
} from '@/model/Translator';
import { VolumeJpDto } from '@/model/WenkuNovel';
import TranslateTask from '@/components/TranslateTask.vue';

const { novelId, volume, getParams } = defineProps<{
  novelId: string;
  volume: VolumeJpDto;
  getParams: () => TranslateTaskParams;
}>();

const emit = defineEmits<{
  delete: [];
}>();

const message = useMessage();

const { setting } = Locator.settingRepository();
const { whoami } = Locator.authRepository();

const translateTask = ref<InstanceType<typeof TranslateTask>>();
const startTranslateTask = (translatorId: 'baidu' | 'youdao') => {
  return translateTask?.value?.startTask(
    { type: 'wenku', novelId, volumeId: volume.volumeId },
    getParams(),
    { id: translatorId },
  );
};

const file = computed(() => {
  const { mode, translationsMode, translations } = setting.value.downloadFormat;

  const { url, filename } = WenkuNovelRepository.createFileUrl({
    novelId,
    volumeId: volume.volumeId,
    mode,
    translationsMode,
    translations,
  });
  return { url, filename };
});

const shouldTopJob = useKeyModifier('Control');
const submitJob = (id: 'gpt' | 'sakura') => {
  const task = TranslateTaskDescriptor.wenku(
    novelId,
    volume.volumeId,
    getParams(),
  );
  const workspace =
    id === 'gpt'
      ? Locator.gptWorkspaceRepository()
      : Locator.sakuraWorkspaceRepository();
  const job = {
    task,
    description: volume.volumeId,
    createAt: Date.now(),
  };
  const success = workspace.addJob(job);
  if (success) {
    message.success('Queued successfully');
    if (shouldTopJob.value) {
      workspace.topJob(job);
    }
  } else {
    message.error('Queue failed: translation task already exists');
  }
};
</script>

<template>
  <n-flex align="center" justify="space-between" :wrap="false">
    <n-flex :size="4" vertical>
      <n-text>{{ volume.volumeId }}</n-text>

      <n-text depth="3">
        Total {{ volume.total }} / Baidu {{ volume.baidu }} / Youdao
        {{ volume.youdao }} / GPT {{ volume.gpt }} / Sakura {{ volume.sakura }}
      </n-text>

      <n-flex :size="8">
        <c-button
          v-if="setting.enabledTranslator.includes('baidu')"
          label="Update Baidu"
          size="tiny"
          secondary
          @action="startTranslateTask('baidu')"
        />
        <c-button
          v-if="setting.enabledTranslator.includes('youdao')"
          label="Update Youdao"
          size="tiny"
          secondary
          @action="startTranslateTask('youdao')"
        />

        <c-button
          v-if="setting.enabledTranslator.includes('gpt')"
          label="Queue GPT"
          size="tiny"
          secondary
          @action="submitJob('gpt')"
        />
        <c-button
          v-if="setting.enabledTranslator.includes('sakura')"
          label="Queue Sakura"
          size="tiny"
          secondary
          @action="submitJob('sakura')"
        />
        <c-button-confirm
          v-if="whoami.asAdmin"
          :hint="`Are you sure you want to delete '${volume.volumeId}'?`"
          label="Delete"
          type="error"
          size="tiny"
          secondary
          @action="emit('delete')"
        />
      </n-flex>
    </n-flex>

    <c-button
      label="Download"
      :icon="FileDownloadOutlined"
      tag="a"
      :href="file.url"
      :download="file.filename"
      target="_blank"
    />
  </n-flex>

  <TranslateTask
    ref="translateTask"
    @update:baidu="(zh) => (volume.baidu = zh)"
    @update:youdao="(zh) => (volume.youdao = zh)"
    @update:gpt="(zh) => (volume.gpt = zh)"
    @update:sakura="(zh) => (volume.sakura = zh)"
    style="margin-top: 20px"
  />
</template>
