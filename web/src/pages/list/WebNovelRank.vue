<script lang="ts" setup>
import { Locator } from '@/data';
import { WebNovelOutlineDto } from '@/model/WebNovel';
import { runCatching } from '@/util/result';

import { useIsWideScreen } from '@/pages/util';
import { Loader } from './components/NovelPage.vue';

const props = defineProps<{
  providerId: string;
  typeId: string;
  page: number;
  selected: number[];
}>();

const isWideScreen = useIsWideScreen();
const route = useRoute();

type Descriptor = {
  [key: string]: {
    title: string;
    search: boolean;
    options: { label: string; tags: string[] }[];
  };
};

const descriptorsKakuyomu: Descriptor = {
  '1': {
    title: 'Kakuyomu: Genre',
    search: false,
    options: [
      {
        label: 'Genre',
        tags: [
          'Comprehensive',
          'Isekai Fantasy',
          'Modern Fantasy',
          'Sci-Fi',
          'Romance',
          'Romantic Comedy',
          'Modern Drama',
          'Horror',
          'Mystery',
          'Essay/Non-fiction',
          'History/Era/Legend',
          'Creation Theory/Commentary',
          'Poem/Fairy Tale/Other',
        ],
      },
      {
        label: 'Range',
        tags: ['Total', 'Yearly', 'Monthly', 'Weekly', 'Daily'],
      },
    ],
  },
};

const commonOptionsSyosetu = [
  {
    label: 'Range',
    tags: ['Total', 'Yearly', 'Quarterly', 'Monthly', 'Weekly', 'Daily'],
  },
  {
    label: 'Status',
    tags: ['All', 'Short Story', 'Serializing', 'Finished'],
  },
];
const descriptorsSyosetu: Descriptor = {
  '1': {
    title: 'Syosetu: Genre',
    search: false,
    options: [
      {
        label: 'Genre',
        tags: [
          'Romance: Isekai',
          'Romance: Real World',
          'Fantasy: High Fantasy',
          'Fantasy: Low Fantasy',
          'Literature: Pure Literature',
          'Literature: Human Drama',
          'Literature: History',
          'Literature: Mystery',
          'Literature: Horror',
          'Literature: Action',
          'Literature: Comedy',
          'Sci-Fi: VR Game',
          'Sci-Fi: Space',
          'Sci-Fi: Science Fiction',
          'Sci-Fi: Thriller',
          'Other: Fairy Tale',
          'Other: Poem',
          'Other: Essay',
          'Other: Other',
        ],
      },
      ...commonOptionsSyosetu,
    ],
  },
  '2': {
    title: 'Syosetu: Comprehensive',
    search: false,
    options: commonOptionsSyosetu,
  },
  '3': {
    title: 'Syosetu: Isekai Tensei/Ten\'i',
    search: false,
    options: [
      {
        label: 'Genre',
        tags: ['Romance', 'Fantasy', 'Literature/Sci-Fi/Other'],
      },
      ...commonOptionsSyosetu,
    ],
  },
};

const descriptiors: { [key: string]: Descriptor } = {
  syosetu: descriptorsSyosetu,
  kakuyomu: descriptorsKakuyomu,
};

const descriptior = computed(
  () => descriptiors[props.providerId][props.typeId],
);

const loader = computed<Loader<WebNovelOutlineDto>>(() => {
  const providerId = props.providerId;
  const typeId = props.typeId;

  return (page, _query, selected) => {
    const optionNth = (n: number): string =>
      descriptior.value.options[n].tags[selected[n]];

    let filters = {};
    if (providerId == 'syosetu') {
      const types: { [key: string]: string } = {
        '1': 'Genre',
        '2': 'Comprehensive',
        '3': 'Isekai Tensei/Ten\'i',
      };
      if (typeId === '2') {
        filters = {
          type: types[typeId],
          range: optionNth(0),
          status: optionNth(1),
          page,
        };
      } else {
        filters = {
          type: types[typeId],
          genre: optionNth(0),
          range: optionNth(1),
          status: optionNth(2),
          page,
        };
      }
    } else if (providerId == 'kakuyomu') {
      filters = { genre: optionNth(0), range: optionNth(1) };
    }
    return runCatching(
      Locator.webNovelRepository.listRank(providerId, filters),
    );
  };
});
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ descriptior.title }}</n-h1>

    <novel-page
      :page="page"
      :selected="selected"
      :loader="loader"
      :search="
        descriptior.search
          ? {
              suggestions: [],
              tags: [],
            }
          : undefined
      "
      :options="descriptior.options"
      v-slot="{ items }"
    >
      <novel-list-web :items="items" />
    </novel-page>
  </div>
</template>
