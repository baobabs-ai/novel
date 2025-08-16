<script lang="ts" setup>
import { MoreVertOutlined } from '@vicons/material';
import { FormInst, FormItemRule, FormRules } from 'naive-ui';

import { Locator } from '@/data';

import { doAction } from '@/pages/util';
import { useBookshelfLocalStore } from '../BookshelfLocalStore';

const { id, type, title } = defineProps<{
  id: string;
  title: string;
  type: 'web' | 'wenku' | 'local';
}>();

const favoredRepository = Locator.favoredRepository();
const store = useBookshelfLocalStore();

const message = useMessage();

const getOptions = () => {
  if (id === 'all') {
    return [];
  } else if (id === 'default') {
    return [{ label: 'Edit information', key: 'edit' }];
  } else {
    return [
      { label: 'Edit information', key: 'edit' },
      { label: 'Delete', key: 'delete' },
    ];
  }
};

const options = getOptions();

const onSelect = (key: string) => {
  if (key === 'edit') {
    showEditModal.value = true;
  } else if (key === 'delete') {
    showDeleteModal.value = true;
  }
};

const showEditModal = ref(false);
const formRef = ref<FormInst | null>(null);
const formValue = ref({ title });
const formRules: FormRules = {
  title: [
    {
      validator: (_rule: FormItemRule, value: string) => value.length > 0,
      message: 'Bookshelf title cannot be empty',
      trigger: 'input',
    },
  ],
};
const updateFavored = async () => {
  if (formRef.value == null) {
    return;
  } else {
    try {
      await formRef.value.validate();
    } catch (e) {
      return;
    }
  }

  const title = formValue.value.title;

  await doAction(
    favoredRepository.updateFavored(type, id, title).then(() => {
      showEditModal.value = false;
    }),
    'Bookshelf updated',
    message,
  );
};

const deleteFavoredNovels = async () => {
  if (type === 'local') {
    const { failed } = await store.deleteVolumes(
      store.volumes.filter((it) => it.favoredId === id).map(({ id }) => id),
    );
    if (failed > 0) {
      throw new Error(`Failed to clear bookshelf, ${failed} items were not deleted`);
    }
  }
};

const showDeleteModal = ref(false);
const deleteFavored = () =>
  doAction(
    deleteFavoredNovels()
      .then(() => favoredRepository.deleteFavored(type, id))
      .then(() => (showDeleteModal.value = false)),
    'Bookshelf deleted',
    message,
  );
</script>

<template>
  <router-link :to="`/favorite/${type}/${id}`">
    <n-flex align="center" justify="space-between">
      {{ title }}
      <n-dropdown
        v-if="options.length > 0"
        trigger="hover"
        :options="options"
        :keyboard="false"
        @select="onSelect"
      >
        <n-button quaternary circle>
          <n-icon :component="MoreVertOutlined" />
        </n-button>
      </n-dropdown>
    </n-flex>
  </router-link>

  <c-modal v-model:show="showEditModal" title="Edit bookshelf">
    <n-form
      ref="formRef"
      :model="formValue"
      :rules="formRules"
      label-placement="left"
      label-width="auto"
    >
      <n-form-item-row label="Title" path="title">
        <n-input
          v-model:value="formValue.title"
          placeholder="Bookshelf title"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>
    </n-form>

    <template #action>
      <c-button
        label="Confirm"
        require-login
        type="primary"
        @action="updateFavored"
      />
    </template>
  </c-modal>

  <c-modal v-model:show="showDeleteModal" title="Delete bookshelf">
    Are you sure you want to delete the bookshelf [{{ title }}]?
    <n-text v-if="type === 'local'">
      <br />
      Note that deleting a local bookshelf will also clear all the novels in it.
    </n-text>

    <template #action>
      <c-button
        label="Confirm"
        require-login
        type="primary"
        @action="deleteFavored"
      />
    </template>
  </c-modal>
</template>
