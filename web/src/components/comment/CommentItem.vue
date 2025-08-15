<script lang="ts" setup>
import {
  CommentOutlined,
  MoreVertOutlined,
  DeleteOutlined,
} from '@vicons/material';

import { Locator } from '@/data';
import { Comment1 } from '@/model/Comment';

const { comment, topLevel } = defineProps<{
  comment: Comment1;
  topLevel?: boolean;
}>();

const emit = defineEmits<{
  copy: [Comment1];
  delete: [Comment1];
  hide: [Comment1];
  unhide: [Comment1];
  block: [Comment1];
  unblock: [Comment1];
  reply: [Comment1];
}>();

const { whoami } = Locator.authRepository();
const blockUserCommentRepository = Locator.blockUserCommentRepository();
const options = computed(() => {
  const options = [
    {
      label: 'Copy',
      key: 'copy',
    },
  ];
  if (whoami.value.asAdmin) {
    if (comment.hidden) {
      options.push({
        label: 'Unhide',
        key: 'unhide',
      });
    } else {
      options.push({
        label: 'Hide',
        key: 'hide',
      });
    }
  }
  if (
    blockUserCommentRepository.ref.value.usernames.includes(
      comment.user.username,
    )
  ) {
    options.push({
      label: 'Unblock',
      key: 'unblock',
    });
  } else {
    options.push({
      label: 'Block user',
      key: 'block',
    });
  }
  return options;
});

const handleSelect = (key: string) => {
  if (key === 'copy') {
    emit('copy', comment);
  } else if (key === 'delete') {
    emit('delete', comment);
  } else if (key === 'hide') {
    emit('hide', comment);
  } else if (key === 'unhide') {
    emit('unhide', comment);
  } else if (key === 'block') {
    emit('block', comment);
  } else if (key === 'unblock') {
    emit('unblock', comment);
  }
};

const isDeletable = computed(() => {
  return (
    whoami.value.asAdmin ||
    (whoami.value.isMe(comment.user.username) &&
      Date.now() / 1000 - comment.createAt < 3600 * 24)
  );
});

const isBlocked = computed(() => {
  return blockUserCommentRepository.ref.value.usernames.includes(
    comment.user.username,
  );
});
</script>

<template>
  <n-flex align="center" :size="0">
    <n-text>
      <b>{{ comment.user.username }}</b>
    </n-text>
    <n-text depth="3" style="font-size: 12px; margin-left: 12px">
      <n-time :time="comment.createAt * 1000" type="relative" />
    </n-text>

    <div style="flex: 1" />

    <c-button
      v-if="topLevel && whoami.allowAdvancedFeatures"
      label="Reply"
      :icon="CommentOutlined"
      require-login
      quaternary
      type="tertiary"
      size="tiny"
      style="margin-right: 4px"
      @action="emit('reply', comment)"
    />

    <c-button-confirm
      v-if="isDeletable"
      hint="Are you sure you want to delete this comment?"
      label="Delete"
      :icon="DeleteOutlined"
      require-login
      quaternary
      type="tertiary"
      size="tiny"
      style="margin-right: 4px"
      @action="emit('delete', comment)"
    />

    <n-dropdown trigger="click" :options="options" @select="handleSelect">
      <n-button circle quaternary type="tertiary" size="tiny">
        <n-icon :component="MoreVertOutlined" />
      </n-button>
    </n-dropdown>
  </n-flex>

  <n-card embedded :bordered="false" size="small" style="margin-top: 2px">
    <n-text v-if="comment.hidden" depth="3">[Hidden]</n-text>
    <n-text v-else-if="isBlocked" depth="3">[Blocked]</n-text>
    <MarkdownView
      v-else
      mode="comment"
      :source="comment.content"
      style="margin-top: -1em; margin-bottom: -1em"
    />
  </n-card>
</template>
