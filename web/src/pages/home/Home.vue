<script lang="ts" setup>
import {
  BookOutlined,
  ForumOutlined,
  LanguageOutlined,
  ReadMoreOutlined,
  StarBorderOutlined,
} from '@vicons/material';

import { Locator } from '@/data';
import { WebNovelRepository, WenkuNovelRepository } from '@/data/api';
import bannerUrl from '@/image/banner.webp';
import { WebNovelOutlineDto } from '@/model/WebNovel';
import { WenkuNovelOutlineDto } from '@/model/WenkuNovel';
import { useBreakPoints } from '@/pages/util';
import { Result, runCatching } from '@/util/result';
import { WebUtil } from '@/util/web';

const bp = useBreakPoints();
const showShortcut = bp.smaller('tablet');

const router = useRouter();
const vars = useThemeVars();

const { whoami } = Locator.authRepository();

const url = ref('');
const query = (url: string) => {
  if (url.length === 0) return;
  const parseResult = WebUtil.parseUrl(url);
  if (parseResult !== undefined) {
    const { providerId, novelId } = parseResult;
    router.push({ path: `/novel/${providerId}/${novelId}` });
  } else {
    router.push({ path: '/novel', query: { query: url } });
  }
};

const favoriteList = ref<Result<WebNovelOutlineDto[]>>();
const loadFavorite = async () => {
  favoriteList.value = await runCatching(
    Locator.favoredRepository()
      .listFavoredWebNovel('default', {
        page: 0,
        pageSize: 8,
        query: '',
        provider: 'kakuyomu,syosetu,novelup,hameln,pixiv,alphapolis',
        type: 0,
        level: 0,
        translate: 0,
        sort: 'update',
      })
      .then((it) => it.items),
  );
};
watch(
  () => whoami.value.isSignedIn,
  (isSignedIn) => {
    if (isSignedIn) {
      loadFavorite();
    }
  },
  { immediate: true },
);

const mostVisitedWeb = ref<Result<WebNovelOutlineDto[]>>();
const loadWeb = async () => {
  mostVisitedWeb.value = await runCatching(
    WebNovelRepository.listNovel({
      page: 0,
      pageSize: 8,
      provider: 'kakuyomu,syosetu,novelup,hameln,pixiv,alphapolis',
      sort: 1,
      level: 1,
    }).then((it) => it.items),
  );
};
loadWeb();

const latestUpdateWenku = ref<Result<WenkuNovelOutlineDto[]>>();
const loadWenku = async () => {
  latestUpdateWenku.value = await runCatching(
    WenkuNovelRepository.listNovel({
      page: 0,
      pageSize: 12,
      level: 1,
    }).then((it) => it.items),
  );
};
loadWenku();

const showHowToUseModal = ref(false);
const linkExample = [
  ['Kakuyomu', 'https://kakuyomu.jp/works/16817139555217983105'],
  [
    'Shōsetsuka ni Narō',
    'https://ncode.syosetu.com/n0833hi <br /> https://novel18.syosetu.com/n3192gh',
  ],
  ['Novelup', 'https://novelup.plus/story/206612087'],
  ['Hameln', 'https://syosetu.org/novel/297874/'],
  [
    'Pixiv Series/Short Stories',
    'https://www.pixiv.net/novel/series/9406879 <br/> https://www.pixiv.net/novel/show.php?id=18304868',
  ],
  ['Alphapolis', 'https://www.alphapolis.co.jp/novel/638978238/525733370'],
];

const showQQModal = ref(false);
const qqLink =
  'http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=Qa0SOMBYZoJZ4vuykz3MbPS0zbpeN0pW&authKey=q75E7fr5CIBSDhqX%2F4kuC%2B0mcPiDvj%2FSDfP%2FGZ8Rl8kDn6Z3M6XPSZ91yt4ZWonq&noverify=0&group_code=819513328';

const telegramLink = 'https://t.me/+Mphy0wV4LYZkNTI1';
const githubLink = 'https://github.com/auto-novel/auto-novel';
</script>

<template>
  <div
    :style="{ background: `rgba(0, 0, 0, .25) url(${bannerUrl})` }"
    style="background-blend-mode: darken"
  >
    <div id="banner" class="layout-content">
      <n-h1
        style="
          text-align: center;
          font-size: 3em;
          color: white;
          filter: drop-shadow(0.05em 0.05em black);
        "
      >
        Light Novel Machine Translation Robot
      </n-h1>
      <n-input-group>
        <n-input
          v-model:value="url"
          size="large"
          placeholder="Enter the web novel link to jump directly, or search the site's cache..."
          :input-props="{ spellcheck: false }"
          @keyup.enter="query(url)"
          :style="{ 'background-color': vars.bodyColor }"
        />
        <n-button size="large" type="primary" @click="query(url)">
          Search
        </n-button>
      </n-input-group>
    </div>
  </div>

  <div class="layout-content">
    <n-flex
      v-if="showShortcut"
      :size="0"
      justify="space-around"
      :wrap="false"
      style="margin: 8px 0px"
    >
      <router-link
        :to="whoami.isSignedIn ? '/favorite/web' : '/favorite/local'"
        style="flex: 1"
      >
        <n-button quaternary style="width: 100%; height: 64px">
          <n-flex align="center" vertical style="font-size: 12px">
            <n-icon size="24" :component="StarBorderOutlined" />
            My Favorites
          </n-flex>
        </n-button>
      </router-link>

      <router-link to="/novel" style="flex: 1">
        <n-button quaternary style="width: 100%; height: 64px">
          <n-flex align="center" vertical style="font-size: 12px">
            <n-icon size="24" :component="LanguageOutlined" />
            Web Novels
          </n-flex>
        </n-button>
      </router-link>

      <router-link to="/wenku" style="flex: 1">
        <n-button quaternary style="width: 100%; height: 64px">
          <n-flex align="center" vertical style="font-size: 12px">
            <n-icon size="24" :component="BookOutlined" />
            Wenku Novels
          </n-flex>
        </n-button>
      </router-link>

      <router-link to="/forum" style="flex: 1">
        <n-button quaternary style="width: 100%; height: 64px">
          <n-flex align="center" vertical style="font-size: 12px">
            <n-icon size="24" :component="ForumOutlined" />
            Forum
          </n-flex>
        </n-button>
      </router-link>
    </n-flex>
    <div v-else style="height: 16px" />

    <bulletin>
      <Migrate />
      <n-flex>
        <n-button text type="primary" @click="showHowToUseModal = true">
          Instructions
        </n-button>
        /
        <n-button text type="primary" @click="showQQModal = true">
          QQ Group
        </n-button>
        /
        <n-a :href="telegramLink" target="_blank">Telegram</n-a>
        /
        <n-a :href="githubLink" target="_blank">Github</n-a>
      </n-flex>
      <n-p>
        It is forbidden to use scripts to bypass the translator to submit translated text, even if you think you are submitting a proper translation.
      </n-p>
      <n-p>
        FishHawk has been working long hours, and the website development speed has dropped significantly. The forum feedback is currently not maintained. If you have any questions, please join the group and @.
      </n-p>
    </bulletin>

    <template v-if="whoami.isSignedIn">
      <section-header title="My Favorites">
        <router-link to="/favorite/web">
          <c-button label="More" :icon="ReadMoreOutlined" />
        </router-link>
      </section-header>
      <PanelWebNovel :list-result="favoriteList" />
      <n-divider />
    </template>

    <section-header title="Web Novels - Most Clicks">
      <router-link to="/novel">
        <c-button label="More" :icon="ReadMoreOutlined" />
      </router-link>
    </section-header>
    <PanelWebNovel :list-result="mostVisitedWeb" />
    <n-divider />

    <section-header title="Wenku Novels - Latest Updates">
      <router-link to="/wenku">
        <c-button label="More" :icon="ReadMoreOutlined" />
      </router-link>
    </section-header>
    <PanelWenkuNovel :list-result="latestUpdateWenku" />
    <n-divider />
  </div>

  <c-modal title="Instructions" v-model:show="showHowToUseModal">
    <n-p>
      Copy the novel link into the input box on the home page and click search. If the link is correct, it will jump to the novel page. For more advanced usage, such as generating machine translation, advanced search, etc., please refer to the
      <c-a to="/forum/64f3d63f794cbb1321145c07">Tutorial</c-a>
      . If you have any questions or suggestions, please post them in the
      <c-a to="/forum">Forum</c-a>
      .
    </n-p>
    <n-p>Supported novel sites are as follows:</n-p>
    <n-p v-for="[name, link] of linkExample" :key="name">
      <b>{{ name }}</b>
      <br />
      <!-- eslint-disable-next-line vue/no-v-html -->
      <span v-html="link" />
    </n-p>
  </c-modal>

  <c-modal title="QQ Group" v-model:show="showQQModal">
    <n-p>
      Communication group:
      <n-a :href="qqLink" target="_blank">819513328</n-a>
      , the verification answer is "green".
      <br />
      <n-qr-code :size="150" :value="qqLink" />
    </n-p>
  </c-modal>
</template>

<style scoped>
#banner {
  max-width: 800px;
  padding-top: 20px;
  padding-bottom: 50px;
}
@media only screen and (max-width: 600px) {
  #banner {
    padding-top: 10px;
    padding-bottom: 35px;
  }
}
</style>
