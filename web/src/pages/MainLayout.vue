<script lang="ts" setup>
import {
  BookOutlined,
  CandlestickChartOutlined,
  DarkModeOutlined,
  ForumOutlined,
  HistoryOutlined,
  HomeOutlined,
  LanguageOutlined,
  LocalFireDepartmentOutlined,
  LogOutOutlined,
  MenuOutlined,
  SettingsOutlined,
  StarBorderOutlined,
  WbSunnyOutlined,
  WorkspacesOutlined,
} from '@vicons/material';
import { MenuOption, NButton, NIcon, NText, NTime, useOsTheme } from 'naive-ui';
import { RouterLink } from 'vue-router';

import { Locator } from '@/data';
import { UserRole } from '@/model/User';
import { useBreakPoints } from '@/pages/util';

const bp = useBreakPoints();
const hasSider = bp.greater('tablet');
const menuShowTrigger = bp.greater('desktop');
const showMenuModal = ref(false);

watch(hasSider, () => (showMenuModal.value = false));

const route = useRoute();

const authRepository = Locator.authRepository();
const { whoami } = authRepository;

const { setting } = Locator.settingRepository();
const menuCollapsed = computed(() => {
  if (menuShowTrigger.value) {
    return setting.value.menuCollapsed;
  } else {
    return true;
  }
});

const renderLabel = (text: string, href: string) => () =>
  h(RouterLink, { to: href }, { default: () => text });
const renderIcon = (icon: Component) => () =>
  h(NIcon, null, { default: () => h(icon) });

const menuOptions = computed<MenuOption[]>(() => {
  const resolveTheme = () => {
    if (setting.value.theme === 'system') {
      const osTheme = useOsTheme();
      return osTheme.value ?? 'light';
    } else {
      return setting.value.theme;
    }
  };
  const theme = resolveTheme();

  return [
    {
      label: renderLabel('Home', '/'),
      icon: renderIcon(HomeOutlined),
      key: '/',
    },
    {
      label: renderLabel(
        'My Favorites',
        whoami.value.isSignedIn ? '/favorite/web' : '/favorite/local',
      ),
      icon: renderIcon(StarBorderOutlined),
      key: '/favorite',
    },
    {
      label: renderLabel('Read History', '/read-history'),
      icon: renderIcon(HistoryOutlined),
      key: '/read-history',
      show: whoami.value.isSignedIn,
    },
    {
      label: renderLabel('Web Novels', '/novel'),
      icon: renderIcon(LanguageOutlined),
      key: '/novel',
    },
    {
      label: renderLabel('Wenku Novels', '/wenku'),
      icon: renderIcon(BookOutlined),
      key: '/wenku',
    },
    {
      label: 'Novel Rankings',
      icon: renderIcon(LocalFireDepartmentOutlined),
      key: '/rank',
      children: [
        {
          label: renderLabel('Syosetu: Genre', '/rank/web/syosetu/1'),
          key: '/rank/web/syosetu/1',
        },
        {
          label: renderLabel('Syosetu: Comprehensive', '/rank/web/syosetu/2'),
          key: '/rank/web/syosetu/2',
        },
        {
          label: renderLabel(
            'Syosetu: Isekai Transfer/Tensei',
            '/rank/web/syosetu/3',
          ),
          key: '/rank/web/syosetu/3',
        },
        {
          label: renderLabel('Kakuyomu: Genre', '/rank/web/kakuyomu/1'),
          key: '/rank/web/kakuyomu/1',
        },
      ],
    },
    {
      type: 'divider',
      key: 'divider',
      props: { style: { marginTop: '16px', marginBottom: '16px' } },
    },
    {
      label: 'Workspace',
      icon: renderIcon(WorkspacesOutlined),
      key: '/workspace',
      children: [
        {
          label: renderLabel('Novel Toolbox', '/workspace/toolbox'),
          key: '/workspace/toolbox',
        },
        {
          label: renderLabel('GPT Workspace', '/workspace/gpt'),
          key: '/workspace/gpt',
        },
        {
          label: renderLabel('Sakura Workspace', '/workspace/sakura'),
          key: '/workspace/sakura',
        },
        {
          label: renderLabel('Interactive Translation', '/workspace/interactive'),
          key: '/workspace/interactive',
        },
      ],
    },
    {
      label: renderLabel('Forum', '/forum'),
      icon: renderIcon(ForumOutlined),
      key: '/forum',
    },
    {
      label: renderLabel('Settings', '/setting'),
      icon: renderIcon(SettingsOutlined),
      key: '/setting',
    },
    {
      label: () =>
        h(
          'a',
          {
            onClick: () => {
              if (theme === 'light') {
                setting.value.theme = 'dark';
              } else {
                setting.value.theme = 'light';
              }
            },
          },
          { default: () => 'Switch Theme' },
        ),
      icon: renderIcon(theme === 'light' ? WbSunnyOutlined : DarkModeOutlined),
      key: 'theme',
    },
    {
      label: renderLabel('Admin Console', '/admin'),
      icon: renderIcon(CandlestickChartOutlined),
      key: '/admin',
      show: whoami.value.asAdmin,
    },
  ];
});

const menuKey = computed(() => {
  const path = route.path;
  for (const key of ['/novel', '/wenku', '/favorite', '/forum']) {
    if (path.startsWith(key)) {
      return key;
    }
  }
  return path;
});

const userDropdownOptions = computed<MenuOption[]>(() => {
  const renderHeader = () =>
    h(
      'div',
      {
        onClick: () => {
          if (whoami.value.isAdmin) {
            authRepository.toggleManageMode();
          }
        },
        style: {
          'margin-left': '36px',
          'margin-right': '8px',
        },
      },
      [
        h('div', null, [
          h(
            NText,
            { depth: 2 },
            {
              default: () => whoami.value.user.role,
            },
          ),
        ]),
        h('div', null, [
          h(
            NText,
            { depth: 3, style: 'font-size: 12px;' },
            {
              default: () =>
                h(NTime, {
                  time: whoami.value.user.createAt * 1000,
                  type: 'date',
                }),
            },
          ),
        ]),
      ],
    );
  return [
    {
      key: 'header',
      type: 'render',
      render: renderHeader,
    },
    {
      key: 'header-divider',
      type: 'divider',
    },
    {
      label: 'Logout',
      key: 'logout',
      icon: renderIcon(LogOutOutlined),
    },
  ];
});
const handleUserDropdownSelect = (key: string | number) => {
  if (key === 'logout') {
    authRepository.logout();
  }
};

watch(
  () => route.path,
  () => (showMenuModal.value = false),
);
</script>

<template>
  <n-layout :has-sider="hasSider" style="width: 100%; min-height: 100vh">
    <n-layout-header bordered style="position: fixed; z-index: 2">
      <n-flex align="center" style="height: 50px" :size="0">
        <n-button
          v-if="!hasSider"
          size="large"
          quaternary
          circle
          :focusable="false"
          style="margin: 0 8px"
          @click="showMenuModal = true"
        >
          <n-icon size="24" :component="MenuOutlined" />
        </n-button>
        <div v-else style="padding: 0 16px">
          <robot-icon size="32" />
        </div>

        <div style="flex: 1" />

        <router-link
          v-if="!hasSider"
          :to="whoami.isSignedIn ? '/favorite/web' : '/favorite/local'"
        >
          <n-button size="large" quaternary circle :focusable="false">
            <n-icon size="20" :component="StarBorderOutlined" />
          </n-button>
        </router-link>

        <div style="margin-right: 8px">
          <n-dropdown
            v-if="whoami.isSignedIn"
            trigger="hover"
            placement="bottom-end"
            :keyboard="false"
            :options="userDropdownOptions"
            @select="handleUserDropdownSelect"
          >
            <n-button :focusable="false" quaternary>
              @{{ whoami.user.username }}
            </n-button>
          </n-dropdown>

          <router-link
            v-else
            :to="{ name: 'auth', query: { from: route.fullPath } }"
          >
            <n-button quaternary>Login/Register</n-button>
          </router-link>
        </div>
      </n-flex>
    </n-layout-header>

    <n-layout-sider
      v-if="hasSider"
      :show-trigger="menuShowTrigger"
      :trigger-style="{ position: 'fixed', top: '80%', left: '214px' }"
      :collapsed-trigger-style="{ position: 'fixed', top: '80%', left: '36px' }"
      bordered
      :width="240"
      :collapsed="menuCollapsed"
      :collapsed-width="64"
      collapse-mode="width"
      :native-scrollbar="false"
      style="z-index: 1"
      @collapse="setting.menuCollapsed = true"
      @expand="setting.menuCollapsed = false"
    >
      <n-scrollbar
        style="margin-top: 50px; position: fixed; top: 0"
        :style="{ width: menuCollapsed ? '64px' : '240px' }"
      >
        <n-menu
          :value="menuKey"
          :options="menuOptions"
          :width="240"
          :collapsed="menuCollapsed"
          :collapsed-width="64"
          :collapsed-icon-size="22"
          style="margin-bottom: 64px"
        />
      </n-scrollbar>
    </n-layout-sider>

    <n-layout-content
      style="
        margin-top: 50px;
        margin-bottom: 64px;
        z-index: 0;
        min-height: calc(100vh - 50px);
      "
    >
      <router-view v-slot="{ Component }">
        <keep-alive
          :include="[
            'Forum',
            'Index',
            'BookshelfWeb',
            'BookshelfWenku',
            'ReadHistoryList',
            'WebNovelList',
            'WebNovelRank',
            'WenkuNovelList',
          ]"
        >
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </n-layout-content>
  </n-layout>

  <c-drawer-left v-if="!hasSider" v-model:show="showMenuModal">
    <n-menu :value="menuKey" :options="menuOptions" />
  </c-drawer-left>
</template>

<style>
.layout-content {
  max-width: 1000px;
  margin: 0 auto;
  padding-left: 30px;
  padding-right: 30px;
}
@media only screen and (max-width: 600px) {
  .layout-content {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>
