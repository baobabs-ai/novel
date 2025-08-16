<script lang="ts" setup>
import { ChecklistOutlined } from '@vicons/material';

import { Locator } from '@/data';
import { WebNovelOutlineDto } from '@/model/WebNovel';
import { runCatching } from '@/util/result';

import { useIsWideScreen } from '@/pages/util';
import NovelListWeb from '../list/components/NovelListWeb.vue';
import { Loader } from '../list/components/NovelPage.vue';

const props = defineProps<{
  page: number;
  query: string;
  selected: number[];
  favoredId: string;
}>();

const route = useRoute();

const isWideScreen = useIsWideScreen();

const { setting } = Locator.settingRepository();

const options = computed(() => [
  {
    label: 'Source',
    tags: [
      'Kakuyomu',
      "Let's become a novelist",
      'Novelup',
      'Hameln',
      'Pixiv',
      'Alphapolis',
    ],
    multiple: true,
  },
  {
    label: 'Type',
    tags: ['All', 'In serialization', 'Completed', 'Short story'],
  },
  {
    label: 'Rating',
    tags: ['All', 'For all ages', 'R18'],
  },
  {
    label: 'Translate',
    tags: ['All', 'GPT', 'Sakura'],
  },
  {
    label: 'Sort',
    tags: setting.value.favoriteCreateTimeFirst
      ? ['Favorite time', 'Update time']
      : ['Update time', 'Favorite time'],
  },
]);

const loader = computed<Loader<WebNovelOutlineDto>>(() => {
  const { favoredId } = props;
  return (page, query, selected) => {
    if (query !== '') {
      document.title = 'My favorites search: ' + query;
    }
    const parseProviderBitFlags = (n: number): string => {
      const providerMap: { [key: string]: string } = {
        Kakuyomu: 'kakuyomu',
        "Let's become a novelist": 'syosetu',
        Novelup: 'novelup',
        Hameln: 'hameln',
        Pixiv: 'pixiv',
        Alphapolis: 'alphapolis',
      };
      return options.value[n].tags
        .filter((_, index) => (selected[n] & (1 << index)) !== 0)
        .map((tag) => providerMap[tag])
        .join();
    };

    const parseSort = (sortIndex: number): 'create' | 'update' => {
      const sortOption = (options.value.find((opt) => opt.label === 'Sort')
        ?.tags ?? [])[sortIndex];
      switch (sortOption) {
        case 'Favorite time':
          return 'create';
        case 'Update time':
        default:
          return 'update';
      }
    };
    return runCatching(
      Locator.favoredRepository()
        .listFavoredWebNovel(favoredId, {
          page,
          pageSize: 30,
          query,
          provider: parseProviderBitFlags(0),
          type: selected[1],
          level: selected[2],
          translate: selected[3],
          sort: parseSort(selected[4]),
        })
        .then((it) => ({ type: 'web', ...it })),
    );
  };
});

const webSearchHistoryRepository = Locator.webSearchHistoryRepository();

const search = computed(() => {
  const searchHistory = webSearchHistoryRepository.ref.value;
  return {
    suggestions: searchHistory.queries,
    tags: searchHistory.tags
      .sort((a, b) => Math.log2(b.used) - Math.log2(a.used))
      .map((it) => it.tag)
      .slice(0, 8),
  };
});

watch(
  route,
  async (route) => {
    let query = '';
    if (typeof route.query.query === 'string') {
      query = route.query.query;
    }
    webSearchHistoryRepository.addHistory(query);
  },
  { immediate: true },
);

const showControlPanel = ref(false);

const novelListRef = ref<InstanceType<typeof NovelListWeb>>();
</script>

<template>
  <bookshelf-layout :menu-key="`web/${favoredId}`">
    <n-flex style="margin-bottom: 24px">
      <c-button
        label="Select"
        :icon="ChecklistOutlined"
        @action="showControlPanel = !showControlPanel"
      />
      <bookshelf-list-button
        v-if="!isWideScreen"
        :menu-key="`web/${favoredId}`"
      />
    </n-flex>

    <n-collapse-transition :show="showControlPanel" style="margin-bottom: 16px">
      <bookshelf-web-control
        :selected-novels="novelListRef!.selectedNovels"
        :favored-id="favoredId"
        @select-all="novelListRef!.selectAll()"
        @invert-selection="novelListRef!.invertSelection()"
      />
    </n-collapse-transition>

    <novel-page
      :page="page"
      :query="query"
      :selected="selected"
      :loader="loader"
      :options="options"
      :search="search"
      v-slot="{ items }"
    >
      <novel-list-web
        ref="novelListRef"
        :items="items"
        :selectable="showControlPanel"
        :simple="!setting.showTagInWebFavored"
      />
    </novel-page>
  </bookshelf-layout>
</template>
