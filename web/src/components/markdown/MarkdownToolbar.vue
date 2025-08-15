<script setup lang="ts">
import {
  FormatBoldOutlined,
  FormatItalicOutlined,
  HelpOutlineOutlined,
  LinkOutlined,
  MenuOpenOutlined,
  StarOutlineFilled,
  StrikethroughSOutlined,
  WarningAmberOutlined,
} from '@vicons/material';
import { DropdownOption } from 'naive-ui';

import { Draft } from '@/data/stores/DraftRepository';

const props = defineProps<{
  elTextarea?: HTMLTextAreaElement;
  drafts: Draft[];
}>();

const emit = defineEmits<{
  clearDraft: [];
}>();

// ==============================
// Draft
// ==============================

const draftOptions = ref<DropdownOption[]>([]);

watch(
  props.drafts,
  (drafts) => {
    const draftOptionsValue: DropdownOption[] = [];
    for (const draft of drafts.slice().reverse()) {
      draftOptionsValue.push({
        label: draft.createdAt.toLocaleString('en-US'),
        key: draft.createdAt.getTime(),
        draftText: draft.text,
      });
    }
    draftOptionsValue.push({ type: 'divider' }, { label: 'Clear', key: 'Clear' });
    draftOptions.value = draftOptionsValue;
  },
  { immediate: true },
);

const handleSelectDraft = (key: string, option: DropdownOption) => {
  if (key === 'Clear') {
    emit('clearDraft');
  } else {
    const { elTextarea } = props;
    if (!elTextarea) return;
    elTextarea.value = option.draftText as string;
    elTextarea.dispatchEvent(new Event('input'));
  }
};

// ==============================
// Edit
// ==============================

const showGuideModal = ref(false);

type TextSelection = {
  before: string;
  middle: string;
  after: string;
};

type Processor = (selection: TextSelection) => TextSelection;

const applyFormat = (formatter: Processor) => {
  const { elTextarea } = props;
  if (!elTextarea) return;
  elTextarea.focus();

  const { value, selectionStart, selectionEnd } = elTextarea;

  const { before, middle, after } = formatter({
    before: value.slice(0, selectionStart),
    middle: value.slice(selectionStart, selectionEnd),
    after: value.slice(selectionEnd),
  });

  const newValue = before + middle + after;
  const newSelectionStart = before.length;
  const newSelectionEnd = newSelectionStart + middle.length;

  elTextarea.value = newValue;
  elTextarea.setSelectionRange(newSelectionStart, newSelectionEnd);
  elTextarea.dispatchEvent(new Event('input'));
};

const warp = (
  prefix: string,
  suffix: string,
  placeholder: string,
  inline: boolean = true,
) => {
  applyFormat(({ before, middle, after }: TextSelection) => {
    if (before.endsWith(prefix) && after.startsWith(suffix)) {
      before = before.slice(0, -prefix.length);
      after = after.slice(suffix.length);
    } else if (
      middle.length >= prefix.length + suffix.length &&
      middle.startsWith(prefix) &&
      middle.endsWith(suffix)
    ) {
      middle = middle.slice(prefix.length, -suffix.length);
    } else {
      if (!inline) {
        if (!before.endsWith('\n') && before) before = before + '\n';
        if (!after.startsWith('\n')) after = '\n' + after;
      }
      before = before + prefix;
      after = suffix + after;
      if (middle.length === 0) {
        middle = placeholder;
      }
    }
    return { before, middle, after };
  });
};

const insert = (text: string) => {
  applyFormat(({ before, middle, after }: TextSelection) => {
    if (!before.endsWith('\n') && before) before = before + '\n';
    if (!after.startsWith('\n')) after = '\n' + after;
    middle = text;
    return { before, middle, after };
  });
};

const formatBold = () => warp('**', '**', 'Bold');
const formatItalic = () => warp('*', '*', 'Italic');
const formatStrikethrough = () => warp('~~', '~~', 'Strikethrough');
const formatLink = () => warp('[', '](Link)', '');
const formatSpoiler = () => warp('!!', '!!', 'Spoiler');

const formatStar = () => insert('::: star 5');
const formatCollapsibleBlock = () =>
  warp('::: details Click to expand\n', '\n:::', 'Collapsed content', false);
</script>

<template>
  <n-dropdown
    v-if="drafts.length"
    :options="draftOptions"
    trigger="click"
    @select="handleSelectDraft"
  >
    <n-button size="small" quaternary>
      <n-badge :value="drafts.length" dot :offset="[8, -4]">Draft</n-badge>
    </n-button>
  </n-dropdown>

  <MarkdownToolbarButton
    label="Bold"
    :icon="FormatBoldOutlined"
    @action="formatBold"
  />
  <MarkdownToolbarButton
    label="Italic"
    :icon="FormatItalicOutlined"
    @action="formatItalic"
  />
  <MarkdownToolbarButton
    label="Strikethrough"
    :icon="StrikethroughSOutlined"
    @action="formatStrikethrough"
  />
  <MarkdownToolbarButton
    label="Link"
    :icon="LinkOutlined"
    @action="formatLink"
  />
  <MarkdownToolbarButton
    label="Spoiler"
    :icon="WarningAmberOutlined"
    @action="formatSpoiler"
  />
  <n-divider vertical />
  <MarkdownToolbarButton
    label="Rating"
    :icon="StarOutlineFilled"
    @action="formatStar"
  />
  <MarkdownToolbarButton
    label="Collapse"
    :icon="MenuOpenOutlined"
    @action="formatCollapsibleBlock"
  />
  <MarkdownToolbarButton
    label="Formatting help"
    :icon="HelpOutlineOutlined"
    @action="() => (showGuideModal = true)"
  />
  <div style="width: 8px" />

  <MarkdownGuideModal v-model:show="showGuideModal" />
</template>
