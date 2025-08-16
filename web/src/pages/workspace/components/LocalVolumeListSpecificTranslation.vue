<script lang="ts" setup>
import { DeleteOutlineOutlined } from '@vicons/material';
import { useKeyModifier } from '@vueuse/core';

import { Locator } from '@/data';
import { Setting } from '@/data/setting/Setting';
import { GenericNovelId } from '@/model/Common';
import { LocalVolumeMetadata } from '@/model/LocalVolume';
import { downloadFile } from '@/util';

import { useBookshelfLocalStore } from '@/pages/bookshelf/BookshelfLocalStore';
import { doAction } from '@/pages/util';
import TranslateOptions from '@/pages/novel/components/TranslateOptions.vue';

const translateOptions = ref<InstanceType<typeof TranslateOptions>>();

const props = defineProps<{
  type: 'gpt' | 'sakura';
}>();

const message = useMessage();

const { setting } = Locator.settingRepository();

const store = useBookshelfLocalStore();

const deleteVolume = (volumeId: string) =>
  doAction(store.deleteVolume(volumeId), 'Delete', message);

const calculateFinished = (volume: LocalVolumeMetadata) =>
  volume.toc.filter((it) => {
    let chapterGlossaryId: string | undefined;
    if (props.type === 'gpt') {
      chapterGlossaryId = it.gpt;
    } else {
      chapterGlossaryId = it.sakura;
    }
    return chapterGlossaryId === volume.glossaryId;
  }).length;

const calculateExpired = (volume: LocalVolumeMetadata) =>
  volume.toc.filter((it) => {
    let chapterGlossaryId: string | undefined;
    if (props.type === 'gpt') {
      chapterGlossaryId = it.gpt;
    } else {
      chapterGlossaryId = it.sakura;
    }
    return (
      chapterGlossaryId !== undefined && chapterGlossaryId !== volume.glossaryId
    );
  }).length;

const queueAllVolumes = (volumes: LocalVolumeMetadata[]) => {
  const ids = volumes.map((it) => it.id);
  const { success, failed } = store.queueJobsToWorkspace(ids, {
    level: 'expire',
    type: props.type,
    shouldTop: shouldTopJob.value ?? false,
  });
  message.info(`${success} novels have been queued, ${failed} failed`);
};

const shouldTopJob = useKeyModifier('Control');
const queueVolume = (volumeId: string, total: number = 65536) => {
  const { startIndex, endIndex, level, forceMetadata } =
    translateOptions.value!.getTranslateTaskParams();
  const taskNumber = translateOptions.value!.getTaskNumber();
  const success = store.queueJobToWorkspace(volumeId, {
    level: level,
    type: props.type,
    shouldTop: shouldTopJob.value ?? false,
    startIndex: startIndex,
    endIndex: endIndex,
    taskNumber: taskNumber,
    total: total,
  });
  if (success) {
    message.success('Queue successful');
  } else {
    message.error('Queue failed: translation task already exists');
  }
};

const downloadVolume = async (volumeId: string) => {
  const { mode } = setting.value.downloadFormat;
  const repo = await Locator.localVolumeRepository();

  try {
    const { filename, blob } = await repo.getTranslationFile({
      id: volumeId,
      mode,
      translationsMode: 'priority',
      translations: [props.type],
    });
    downloadFile(filename, blob);
  } catch (error) {
    message.error(`File generation error: ${error}`);
  }
};

const progressFilter = ref<'all' | 'finished' | 'unfinished'>('all');
const progressFilterOptions = [
  { value: 'all', label: 'All' },
  { value: 'finished', label: 'Finished' },
  { value: 'unfinished', label: 'Unfinished' },
];
const progressFilterFunc = computed(() => {
  if (progressFilter.value === 'finished') {
    return (volume: LocalVolumeMetadata) => {
      return volume.toc.length === calculateFinished(volume);
    };
  } else if (progressFilter.value === 'unfinished') {
    return (volume: LocalVolumeMetadata) => {
      return volume.toc.length !== calculateFinished(volume);
    };
  } else {
    return undefined;
  }
});
</script>

<template>
  <local-volume-list
    :filter="progressFilterFunc"
    :options="{ 'Queue all': queueAllVolumes }"
    @volume-add="queueVolume($event.name)"
  >
    <template #extra>
      <translate-options
        ref="translateOptions"
        :gnid="GenericNovelId.local('')"
        :glossary="{}"
      />
      <n-divider style="margin: 12px 0" />
      <c-action-wrapper title="Status">
        <c-radio-group
          v-model:value="progressFilter"
          :options="progressFilterOptions"
          size="small"
        />
      </c-action-wrapper>

      <c-action-wrapper title="Language">
        <c-radio-group
          v-model:value="setting.downloadFormat.mode"
          :options="Setting.downloadModeOptions"
          size="small"
        />
      </c-action-wrapper>
    </template>

    <template #volume="volume">
      <n-flex :size="4" vertical>
        <n-text>{{ volume.id }}</n-text>

        <n-text depth="3">
          <n-time :time="volume.createAt" type="relative" />
          / Total {{ volume.toc.length }} / Finished
          {{ calculateFinished(volume) }} / Expired
          {{ calculateExpired(volume) }}
        </n-text>

        <n-flex :size="8">
          <c-button
            label="Queue"
            size="tiny"
            secondary
            @action="queueVolume(volume.id, volume.toc.length)"
          />

          <router-link
            v-if="!volume.id.endsWith('.epub')"
            :to="`/workspace/reader/${encodeURIComponent(volume.id)}/0`"
            target="_blank"
          >
            <c-button label="Read" size="tiny" secondary />
          </router-link>

          <c-button
            label="Download"
            size="tiny"
            secondary
            @action="downloadVolume(volume.id)"
          />

          <glossary-button
            :gnid="GenericNovelId.local(volume.id)"
            :value="volume.glossary"
            size="tiny"
            secondary
          />

          <div style="flex: 1" />

          <c-button-confirm
            :hint="`Are you sure you want to delete '${volume.id}'?`"
            :icon="DeleteOutlineOutlined"
            size="tiny"
            secondary
            circle
            type="error"
            @action="deleteVolume(volume.id)"
          />
        </n-flex>
      </n-flex>
    </template>
  </local-volume-list>
</template>
