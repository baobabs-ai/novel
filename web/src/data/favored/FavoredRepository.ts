import { v4 as uuidv4 } from 'uuid';

import { useLocalStorage } from '@/util';

import { FavoredList } from './Favored';
import { FavoredApi } from './FavoredApi';
import { LSKey } from '../LocalStorage';

export const createFavoredRepository = () => {
  const favoreds = useLocalStorage<FavoredList>(LSKey.Favored, {
    web: [{ id: 'default', title: 'Default Favorites' }],
    wenku: [{ id: 'default', title: 'Default Favorites' }],
    local: [{ id: 'default', title: 'Default Favorites' }],
  });

  let remoteFetched = false;
  const loadRemoteFavoreds = async () => {
    if (remoteFetched) return;
    const favoredList = await FavoredApi.listFavored();
    favoreds.value.web = favoredList.favoredWeb;
    favoreds.value.wenku = favoredList.favoredWenku;
    remoteFetched = true;
  };

  const createFavored = async (
    type: 'web' | 'wenku' | 'local',
    title: string,
  ) => {
    const specificFavoreds = favoreds.value[type];
    if (specificFavoreds.length >= 20) {
      throw new Error('You can only create up to 20 favorites');
    }
    let id: string;
    if (type === 'web') {
      id = await FavoredApi.createFavoredWeb({ title });
    } else if (type === 'wenku') {
      id = await FavoredApi.createFavoredWenku({ title });
    } else {
      id = uuidv4();
    }
    specificFavoreds.push({ id, title });
  };

  const updateFavored = async (
    type: 'web' | 'wenku' | 'local',
    id: string,
    title: string,
  ) => {
    if (type === 'web') {
      await FavoredApi.updateFavoredWeb(id, { title });
    } else if (type === 'wenku') {
      await FavoredApi.updateFavoredWenku(id, { title });
    }
    const favored = favoreds.value[type].find((it: { id: string; }) => it.id === id);
    if (favored !== undefined) {
      favored.title = title;
    }
  };

  const deleteFavored = async (type: 'web' | 'wenku' | 'local', id: string) => {
    if (id === 'default') {
      throw new Error('Cannot delete default favorites');
    }
    if (type === 'web') {
      await FavoredApi.deleteFavoredWeb(id);
    } else if (type === 'wenku') {
      await FavoredApi.deleteFavoredWenku(id);
    }
    favoreds.value[type] = favoreds.value[type].filter((it: { id: string; }) => it.id !== id);
  };

  const favoriteNovel = async (
    favoredId: string,
    novel:
      | { type: 'web'; providerId: string; novelId: string }
      | { type: 'wenku'; novelId: string },
  ) => {
    if (novel.type === 'web') {
      await FavoredApi.favoriteWebNovel(
        favoredId,
        novel.providerId,
        novel.novelId,
      );
    } else {
      await FavoredApi.favoriteWenkuNovel(favoredId, novel.novelId);
    }
  };

  const unfavoriteNovel = async (
    favoredId: string,
    novel:
      | { type: 'web'; providerId: string; novelId: string }
      | { type: 'wenku'; novelId: string },
  ) => {
    if (novel.type === 'web') {
      await FavoredApi.unfavoriteWebNovel(
        favoredId,
        novel.providerId,
        novel.novelId,
      );
    } else {
      await FavoredApi.unfavoriteWenkuNovel(favoredId, novel.novelId);
    }
  };

  return {
    favoreds: readonly(favoreds),
    //
    loadRemoteFavoreds,
    createFavored,
    updateFavored,
    deleteFavored,
    //
    listFavoredWebNovel: FavoredApi.listFavoredWebNovel,
    listFavoredWenkuNovel: FavoredApi.listFavoredWenkuNovel,
    favoriteNovel,
    unfavoriteNovel,
  };
};
