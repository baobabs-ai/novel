import { LocationQuery, createRouter, createWebHistory } from 'vue-router';

const parseSelected = (q: LocationQuery) => {
  const selected = <number[]>[];
  if (typeof q.selected === 'string') {
    selected[0] = Number(q.selected) || 0;
  } else if (q.selected) {
    q.selected.forEach((it, index) => {
      selected[index] = Number(it) || 0;
    });
  }
  return selected;
};

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/*',
      children: [
        {
          path: '/auth',
          name: 'auth',
          meta: { title: 'Unified Identity Authentication' },
          component: () => import('./pages/Auth.vue'),
          props: (route) => ({ from: route.query.from }),
        },
      ],
    },

    {
      path: '/*',
      meta: { isReader: true },
      component: () => import('./pages/reader/ReaderLayout.vue'),
      children: [
        {
          path: '/novel/:providerId/:novelId/:chapterId',
          components: {
            reader: () => import('./pages/reader/Reader.vue'),
          },
        },
        {
          path: '/workspace/reader/:novelId/:chapterId',
          components: {
            reader: () => import('./pages/reader/Reader.vue'),
          },
        },
      ],
    },

    {
      path: '/*',
      component: () => import('./pages/MainLayout.vue'),
      children: [
        {
          path: '/',
          meta: { title: 'Home' },
          component: () => import('./pages/home/Home.vue'),
        },

        {
          path: '/favorite',
          redirect: '/favorite/local/default',
          children: [
            {
              path: 'web/:favoredId?',
              meta: { title: 'My Favorites' },
              component: () => import('./pages/bookshelf/BookshelfWeb.vue'),
              props: (route) => ({
                page: Number(route.query.page) || 1,
                query: route.query.query || '',
                selected: parseSelected(route.query),
                favoredId: route.params.favoredId || 'default',
              }),
            },
            {
              path: 'wenku/:favoredId?',
              meta: { title: 'My Favorites' },
              component: () => import('./pages/bookshelf/BookshelfWenku.vue'),
              props: (route) => ({
                page: Number(route.query.page) || 1,
                selected: parseSelected(route.query),
                favoredId: route.params.favoredId || 'default',
              }),
            },
            {
              path: 'local/:favoredId?',
              meta: { title: 'My Favorites' },
              component: () => import('./pages/bookshelf/BookshelfLocal.vue'),
              props: (route) => ({
                favoredId: route.params.favoredId || 'default',
              }),
            },
          ],
        },

        {
          path: '/read-history',
          meta: { title: 'Read History' },
          component: () => import('./pages/list/ReadHistoryList.vue'),
          props: (route) => ({
            page: Number(route.query.page) || 1,
          }),
        },

        {
          path: '/novel',
          meta: { title: 'Web Novels' },
          component: () => import('./pages/list/WebNovelList.vue'),
          props: (route) => ({
            page: Number(route.query.page) || 1,
            query: route.query.query || '',
            selected: parseSelected(route.query),
          }),
        },
        {
          path: '/novel/:providerId/:novelId',
          component: () => import('./pages/novel/WebNovel.vue'),
          props: (route) => ({
            providerId: route.params.providerId,
            novelId: route.params.novelId,
            key: route.path,
          }),
        },
        {
          path: '/novel-edit/:providerId/:novelId',
          meta: { title: 'Edit Web Novel' },
          component: () => import('./pages/novel/WebNovelEdit.vue'),
          props: (route) => ({
            providerId: route.params.providerId,
            novelId: route.params.novelId,
            key: route.path,
          }),
        },

        {
          path: '/wenku',
          meta: { title: 'Wenku Novels' },
          component: () => import('./pages/list/WenkuNovelList.vue'),
          props: (route) => ({
            page: Number(route.query.page) || 1,
            query: route.query.query || '',
            selected: parseSelected(route.query),
          }),
        },
        {
          path: '/wenku/:novelId',
          component: () => import('./pages/novel/WenkuNovel.vue'),
          props: (route) => ({
            novelId: route.params.novelId,
            key: route.path,
          }),
        },
        {
          path: '/wenku-edit',
          meta: { title: 'New Wenku Novel' },
          component: () => import('./pages/novel/WenkuNovelEdit.vue'),
          props: (route) => ({
            key: route.path,
          }),
        },
        {
          path: '/wenku-edit/:novelId',
          meta: { title: 'Edit Wenku Novel' },
          component: () => import('./pages/novel/WenkuNovelEdit.vue'),
          props: (route) => ({
            novelId: route.params.novelId,
            key: route.path,
          }),
        },

        {
          path: '/rank/web/:providerId/:typeId',
          meta: { title: 'Novel Rankings' },
          component: () => import('./pages/list/WebNovelRank.vue'),
          props: (route) => ({
            providerId: route.params.providerId as string,
            typeId: route.params.typeId as string,
            page: Number(route.query.page) || 1,
            selected: parseSelected(route.query),
          }),
        },

        {
          path: '/workspace',
          redirect: '/workspace/sakura',
          children: [
            {
              path: 'gpt',
              meta: { title: 'GPT Workspace' },
              component: () => import('./pages/workspace/GptWorkspace.vue'),
            },
            {
              path: 'sakura',
              meta: { title: 'Sakura Workspace' },
              component: () => import('./pages/workspace/SakuraWorkspace.vue'),
            },
            {
              path: 'interactive',
              meta: { title: 'Interactive Translation' },
              component: () => import('./pages/workspace/Interactive.vue'),
            },
            {
              path: 'toolbox',
              meta: { title: 'Novel Toolbox' },
              component: () => import('./pages/workspace/Toolbox.vue'),
            },
          ],
        },

        {
          path: '/forum',
          meta: { title: 'Forum' },
          component: () => import('./pages/forum/Forum.vue'),
          props: (route) => ({
            page: Number(route.query.page) || 1,
            category: route.query.category || 'General',
          }),
        },
        {
          path: '/forum/:articleId',
          component: () => import('./pages/forum/ForumArticle.vue'),
          props: (route) => ({
            articleId: route.params.articleId,
            key: route.path,
          }),
        },
        {
          path: '/forum-edit',
          meta: { title: 'Publish Article' },
          component: () => import('./pages/forum/ForumArticleEdit.vue'),
          props: (route) => ({
            key: route.path,
          }),
        },
        {
          path: '/forum-edit/:articleId',
          meta: { title: 'Edit Article' },
          component: () => import('./pages/forum/ForumArticleEdit.vue'),
          props: (route) => ({
            articleId: route.params.articleId,
            key: route.path,
          }),
        },

        {
          path: '/setting',
          meta: { title: 'Settings' },
          component: () => import('./pages/other/Setting.vue'),
        },

        {
          path: '/admin',
          redirect: '/admin/user',
          component: () => import('./pages/admin/AdminLayout.vue'),
          children: [
            {
              path: 'user',
              component: () => import('./pages/admin/AdminUserManagement.vue'),
            },
            {
              path: 'operation',
              component: () =>
                import('./pages/admin/AdminOperationHistory.vue'),
            },
            {
              path: 'web-toc-merge-history',
              component: () =>
                import('./pages/admin/AdminWebTocMergeHistory.vue'),
            },
          ],
        },

// Compatible with old routes
        { path: '/novel-list', redirect: '/novel' }, // 2024-06-25
        { path: '/wenku-list', redirect: '/wenku' }, // 2024-06-25
        { path: '/sakura-workspace', redirect: '/workspace/sakura' },

        // 404
        {
          path: '/:pathMatch(.*)',
          component: () => import('./pages/other/NotFound.vue'),
        },
      ],
    },
  ],

  scrollBehavior(to, _from, savedPosition) {
    if (to.hash) {
      const decodedHash = encodeURIComponent(to.hash.substring(1));
      const element = document.getElementById(decodedHash);
      if (element) {
        const top = element.getBoundingClientRect().top + window.scrollY - 58; // 50 is the height of the navbar
        setTimeout(() => window.scrollTo({ top }));
        return { top };
      }
    }
    return { top: savedPosition?.top ?? 0 };
  },
});

router.afterEach((to, from) => {
  // The title between chapters depends on manual switching, so skip it here
  if (!(to.meta.isReader && from.meta.isReader)) {
    const defaultTitle = 'Light Novel Machine Translation Robot';
    const title = to.meta.title;
    if (title !== undefined) {
      document.title = title + ' | ' + defaultTitle;
    } else {
      document.title = defaultTitle;
    }
  }
});

export default router;
