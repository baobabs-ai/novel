<script lang="ts" setup>
import { FileDownloadOutlined, MoreVertOutlined } from '@vicons/material';

import { Locator } from '@/data';
import { Setting } from '@/data/setting/Setting';
import { LocalVolumeMetadata } from '@/model/LocalVolume';

import {
  BookshelfLocalUtil,
  useBookshelfLocalStore,
} from '@/pages/bookshelf/BookshelfLocalStore';

const props = defineProps<{
  options?: { [key: string]: (volumes: LocalVolumeMetadata[]) => void };
  filter?: (volume: LocalVolumeMetadata) => boolean;
}>();

const emit = defineEmits<{
  volumeAdd: [File];
}>();

const message = useMessage();
const { setting } = Locator.settingRepository();

const store = useBookshelfLocalStore();
const { volumes } = storeToRefs(store);

store.loadVolumes();

const options = computed(() => {
  return [...Object.keys(props.options ?? {}), 'Batch delete'].map((it) => ({
    label: it,
    key: it,
  }));
});
const handleSelect = (key: string) => {
  switch (key) {
    case 'Batch delete':
      openDeleteModal();
      break;
    default:
      props.options?.[key]?.(volumes.value ?? []);
      break;
  }
};

const downloadVolumes = async () => {
  if (sortedVolumes.value.length === 0) {
    message.info('No novel selected');
    return;
  }
  const ids = sortedVolumes.value.map((it) => it.id);
  const { success, failed } = await store.downloadVolumes(ids);
  message.info(`${success} novels were packaged, ${failed} failed`);
};

const showDeleteModal = ref(false);

const openDeleteModal = () => {
  if (sortedVolumes.value.length === 0) {
    message.info('No novel selected');
    return;
  }
  showDeleteModal.value = true;
};

const deleteAllVolumes = async () => {
  const ids = sortedVolumes.value.map((it) => it.id);
  const { success, failed } = await store.deleteVolumes(ids);
  showDeleteModal.value = false;
  message.info(`${success} novels were deleted, ${failed} failed`);
};

const search = reactive({
  query: '',
  enableRegexMode: false,
});

const favoredRepository = Locator.favoredRepository();
const favoreds = favoredRepository.favoreds;
const selectedFavored = ref<string | undefined>(favoreds.value.local[0]?.id);
const favoredsOptions = computed(() => {
  return favoreds.value.local.map(({ id, title }) => ({
    label: title,
    value: id,
  }));
});

const sortedVolumes = computed(() => {
  const filteredVolumes =
    props.filter === undefined
      ? volumes.value
      : volumes.value.filter(props.filter);
  return BookshelfLocalUtil.filterAndSortVolumes(filteredVolumes, {
    ...search,
    favoredId: selectedFavored.value,
    order: setting.value.localVolumeOrder,
  });
});
</script>

<template>
  <c-drawer-right title="Local novel">
    <template #action>
      <bookshelf-local-add-button
        :favored-id="selectedFavored"
        @done="emit('volumeAdd', $event)"
      />
      <c-button
        label="Download"
        :icon="FileDownloadOutlined"
        @click="downloadVolumes"
      />
      <n-dropdown
        trigger="click"
        :options="options"
        :keyboard="false"
        @select="handleSelect"
      >
        <n-button circle>
          <n-icon :component="MoreVertOutlined" />
        </n-button>
      </n-dropdown>
    </template>

    <div style="padding: 24px 16px">
      <n-flex vertical>
        <c-action-wrapper title="Search">
          <search-input
            v-model:value="search"
            placeholder="Search file name"
            style="max-width: 400px"
          />
        </c-action-wrapper>

        <c-action-wrapper v-if="favoreds.local.length > 1" title="Favorites">
          <n-select
            v-model:value="selectedFavored"
            :options="favoredsOptions"
            style="max-width: 400px"
          />
        </c-action-wrapper>

        <c-action-wrapper title="Sort" align="center">
          <order-sort
            v-model:value="setting.localVolumeOrder"
            :options="Setting.localVolumeOrderOptions"
          />
        </c-action-wrapper>
        <slot name="extra" />
      </n-flex>

      <n-divider style="margin: 16px 0 8px" />

      <n-spin v-if="sortedVolumes === undefined" style="margin-top: 20px" />

      <n-empty
        v-else-if="sortedVolumes.length === 0"
        description="No files"
        style="margin-top: 20px"
      />

      <n-scrollbar v-else trigger="none" :size="24" style="flex: auto">
        <n-list style="padding-bottom: 48px; padding-right: 12px">
          <n-list-item v-for="volume of sortedVolumes ?? []" :key="volume.id">
            <slot name="volume" v-bind="volume" />
          </n-list-item>
        </n-list>
      </n-scrollbar>

      <c-modal title="Clear all files" v-model:show="showDeleteModal">
        <n-p>
          This will clear all EPUB/TXT files saved in your browser, including translated chapters and glossaries, and cannot be recovered. Are you sure?
        </n-p>

        <template #action>
          <c-button label="Confirm" type="primary" @action="deleteAllVolumes" />
        </template>
      </c-modal>
    </div>
  </c-drawer-right>
</template>
