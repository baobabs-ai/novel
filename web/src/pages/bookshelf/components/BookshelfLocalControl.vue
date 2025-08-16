<script lang="ts" setup>
import { useKeyModifier } from '@vueuse/core';

import { Locator } from '@/data';
import { Setting } from '@/data/setting/Setting';

import { useIsWideScreen } from '@/pages/util';
import { useBookshelfLocalStore } from '../BookshelfLocalStore';

const props = defineProps<{
  selectedIds: string[];
  favoredId: string;
}>();
defineEmits<{
  selectAll: [];
  invertSelection: [];
}>();

const message = useMessage();
const isWideScreen = useIsWideScreen(600);

const { setting } = Locator.settingRepository();

const store = useBookshelfLocalStore();

// 删除小说
const showDeleteModal = ref(false);

const openDeleteModal = () => {
  const ids = props.selectedIds;
  if (ids.length === 0) {
    message.info('No novel selected');
    return;
  }
  showDeleteModal.value = true;
};

const deleteSelected = async () => {
  const ids = props.selectedIds;
  const { success, failed } = await store.deleteVolumes(ids);
  message.info(`${success} novels were deleted, ${failed} failed`);
};

// 下载小说
const showDownloadModal = ref(false);

const downloadSelected = async () => {
  const ids = props.selectedIds;
  if (ids.length === 0) {
    message.info('No novel selected');
    return;
  }
  const { success, failed } = await store.downloadVolumes(ids);
  message.info(`${success} machine-translated novels were packaged, ${failed} failed`);
};

const downloadRawSelected = async () => {
  const ids = props.selectedIds;
  if (ids.length === 0) {
    message.info('No novel selected');
    return;
  }
  const { success, failed } = await store.downloadRawVolumes(ids);
  message.info(`${success} original novels were packaged, ${failed} failed`);
};

// 移动小说
const { favoreds } = Locator.favoredRepository();

const targetFavoredId = ref(props.favoredId);

const moveToFavored = async () => {
  const novels = props.selectedIds;
  if (novels.length === 0) {
    message.info('No novel selected');
    return;
  }

  if (targetFavoredId.value === props.favoredId) {
    message.info('No need to move');
    return;
  }

  const localVolumeRepository = await Locator.localVolumeRepository();

  let failed = 0;
  for (const volumeId of novels) {
    try {
      await localVolumeRepository.updateFavoredId(
        volumeId,
        targetFavoredId.value,
      );
    } catch (e) {
      failed += 1;
    }
  }
  const success = novels.length - failed;

  message.info(`${success} novels have been moved, ${failed} failed`);
  await store.loadVolumes();
};

// 生成翻译任务
const translateLevel = ref<'expire' | 'all'>('expire');
const reverseOrder = ref(false);
const shouldTopJob = useKeyModifier('Control');

const queueJobs = (type: 'gpt' | 'sakura') => {
  let ids = props.selectedIds;
  if (ids.length === 0) {
    message.info('No novel selected');
    return;
  }

  if (reverseOrder.value) {
    ids = ids.slice().reverse();
  }

  const { success, failed } = store.queueJobsToWorkspace(ids, {
    level: translateLevel.value,
    type,
    shouldTop: shouldTopJob.value ?? false,
  });
  message.info(`${success} novels have been queued, ${failed} failed`);
};
</script>

<template>
  <n-list bordered>
    <n-list-item>
      <n-flex vertical>
        <n-flex align="baseline">
          <n-button-group size="small">
            <c-button
              label="Select all"
              :round="false"
              @action="$emit('selectAll')"
            />
            <c-button
              label="Invert selection"
              :round="false"
              @action="$emit('invertSelection')"
            />
          </n-button-group>

          <n-button-group size="small">
            <c-button
              label="Download original text"
              :round="false"
              @action="downloadRawSelected"
            />
            <c-button
              label="Download machine translation"
              :round="false"
              @action="downloadSelected"
            />
            <c-button
              label="Download settings"
              :round="false"
              @action="showDownloadModal = true"
            />
          </n-button-group>

          <c-button
            label="Delete"
            secondary
            :round="false"
            size="small"
            type="error"
            @click="openDeleteModal"
          />
          <c-modal
            :title="`Confirm deletion ${
              selectedIds.length === 1
                ? selectedIds[0]
                : `${selectedIds.length} novels`
            }？`"
            v-model:show="showDeleteModal"
          >
            <template #action>
              <c-button label="Confirm" type="primary" @action="deleteSelected" />
            </template>
          </c-modal>
        </n-flex>

        <n-text depth="3"> Selected {{ selectedIds.length }} novels </n-text>
      </n-flex>
    </n-list-item>

    <n-list-item v-if="favoreds.local.length > 1">
      <n-p>The move novel function is temporarily closed</n-p>
      <n-flex v-if="false" vertical>
        <b>Move novel</b>

        <n-radio-group v-model:value="targetFavoredId">
          <n-flex align="center">
            <c-button
              label="Move"
              size="small"
              :round="false"
              @action="moveToFavored"
            />

            <n-radio
              v-for="favored in favoreds.local"
              :key="favored.id"
              :value="favored.id"
            >
              {{ favored.title }}
            </n-radio>
          </n-flex>
        </n-radio-group>
      </n-flex>
    </n-list-item>

    <n-list-item
      v-if="
        setting.enabledTranslator.includes('gpt') ||
        setting.enabledTranslator.includes('sakura')
      "
    >
      <n-flex vertical>
        <b>Generate translation task</b>

        <c-action-wrapper title="Options">
          <n-flex size="small">
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-flex :size="0" :wrap="false">
                  <tag-button
                    label="Expired"
                    :checked="translateLevel === 'expire'"
                    @update:checked="translateLevel = 'expire'"
                  />
                  <tag-button
                    label="Re-translate"
                    type="warning"
                    :checked="translateLevel === 'all'"
                    @update:checked="translateLevel = 'all'"
                  />
                </n-flex>
              </template>
              Expired: Translate chapters with expired glossaries<br />
              Re-translate: Re-translate all chapters<br />
            </n-tooltip>

            <tag-button label="Add in reverse order" v-model:checked="reverseOrder" />
          </n-flex>
        </c-action-wrapper>

        <c-action-wrapper title="Operation">
          <n-button-group size="small">
            <c-button
              v-if="setting.enabledTranslator.includes('gpt')"
              label="Queue GPT"
              :round="false"
              @action="queueJobs('gpt')"
            />
            <c-button
              v-if="setting.enabledTranslator.includes('sakura')"
              label="Queue Sakura"
              :round="false"
              @action="queueJobs('sakura')"
            />
          </n-button-group>
        </c-action-wrapper>
      </n-flex>
    </n-list-item>
  </n-list>

  <c-modal title="Download settings" v-model:show="showDownloadModal">
    <n-flex vertical size="large">
      <c-action-wrapper title="Language">
        <c-radio-group
          v-model:value="setting.downloadFormat.mode"
          :options="Setting.downloadModeOptions"
        />
      </c-action-wrapper>

      <c-action-wrapper title="Translate">
        <n-flex>
          <c-radio-group
            v-model:value="setting.downloadFormat.translationsMode"
            :options="Setting.downloadTranslationModeOptions"
          />
          <translator-check
            v-model:value="setting.downloadFormat.translations"
            show-order
            :two-line="!isWideScreen"
          />
        </n-flex>
      </c-action-wrapper>

      <n-text depth="3" style="font-size: 12px">
        # Some EPUB readers cannot correctly display light-colored fonts in Japanese paragraphs
      </n-text>
    </n-flex>
  </c-modal>
</template>
