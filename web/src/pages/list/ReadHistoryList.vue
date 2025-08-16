<script lang="ts" setup>
import { DeleteOutlineOutlined } from '@vicons/material';

import { Locator } from '@/data';
import { WebNovelOutlineDto } from '@/model/WebNovel';
import { runCatching } from '@/util/result';

import { Loader } from '../list/components/NovelPage.vue';
import { doAction } from '../util';

defineProps<{
  page: number;
}>();

const message = useMessage();

const readHistoryRepository = Locator.readHistoryRepository();
const { readHistoryPaused } = readHistoryRepository;

onMounted(() => {
  readHistoryRepository.loadReadHistoryPausedState();
});

const loader: Loader<WebNovelOutlineDto> = (page, _query, _selected) =>
  runCatching(readHistoryRepository.listReadHistoryWeb({ page, pageSize: 30 }));

const clearHistory = () =>
  doAction(
    readHistoryRepository.clearReadHistoryWeb().then(() => {
      window.location.reload();
    }),
    'Clear',
    message,
  );

const deleteHistory = (providerId: string, novelId: string) =>
  doAction(
    readHistoryRepository.deleteReadHistoryWeb(providerId, novelId).then(() => {
      window.location.reload();
    }),
    'Delete',
    message,
  );
</script>

<template>
  <div class="layout-content">
    <n-h1>Reading history</n-h1>

    <n-flex style="margin-bottom: 24px">
      <c-button-confirm
        hint="Are you sure you want to clear the records?"
        label="Clear records"
        :icon="DeleteOutlineOutlined"
        @action="clearHistory()"
      />
      <c-button
        v-if="readHistoryPaused"
        label="Continue recording history"
        @action="readHistoryRepository.resumeReadHistory()"
      />
      <c-button
        v-else
        label="Pause recording history"
        @action="readHistoryRepository.pauseReadHistory()"
      />
    </n-flex>

    <n-text v-if="readHistoryPaused" type="warning">
      Note: The history function has been suspended
    </n-text>

    <novel-page :page="page" :loader="loader" :options="[]" v-slot="{ items }">
      <novel-list-web :items="items" simple>
        <template #action="item">
          <c-button
            size="tiny"
            label="Delete"
            style="margin-top: 2px"
            @action="deleteHistory(item.providerId, item.novelId)"
          />
        </template>
      </novel-list-web>
    </novel-page>
  </div>
</template>
