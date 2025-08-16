<script lang="ts" setup>
import { useIsWideScreen } from '@/pages/util';

defineProps<{ menuKey: string }>();
const isWideScreen = useIsWideScreen();
</script>

<template>
  <c-layout :sidebar="isWideScreen" :sidebar-width="320" class="layout-content">
    <div style="flex: auto">
      <n-h1>My favorites</n-h1>
      <slot />
    </div>
    <template #sidebar>
      <section-header title="Favorites">
        <bookshelf-add-button />
      </section-header>
      <bookshelf-menu :value="menuKey" />
    </template>
  </c-layout>
</template>
