import { jwtDecode } from 'jwt-decode';

import { formatError } from '@/data/api';
import { updateToken } from '@/data/api/client';
import { UserRole } from '@/model/User';
import { useLocalStorage } from '@/util';

import { AuthApi } from './AuthApi';
import { LSKey } from '../LocalStorage';
import { AuthData, migrate } from './Auth';

export const createAuthRepository = () => {
  const authData = useLocalStorage<AuthData>(LSKey.Auth, {
    profile: undefined,
    adminMode: false,
  });
  migrate(authData.value);

  const whoami = computed(() => {
    const { profile, adminMode } = authData.value;

    const isAdmin = profile?.role === 'admin';
    const isSignedIn = profile !== undefined;

    const createAtLeast = (days: number) => {
      if (!profile) return false;
      return Date.now() / 1000 - profile.createdAt > days * 24 * 3600;
    };

    const buildRoleLabel = (auth: AuthData) => {
      if (!auth.profile) return '';

      const roleToString = {
        admin: 'Administrator',
        trusted: 'Trusted User',
        member: 'Normal User',
        restricted: 'Restricted User',
        banned: 'Banned User',
      };
      return (
        roleToString[auth.profile.role] ??
        'Unknown User' + (auth.adminMode ? '+' : '')
      );
    };

    return {
      user: {
        username: profile?.username ?? 'Not logged in',
        role: buildRoleLabel(authData.value),
        createAt: profile?.createdAt ?? Date.now() / 1000,
      },
      isSignedIn,
      isAdmin,
      asAdmin: isAdmin && adminMode,
      allowNsfw: createAtLeast(30),
      allowAdvancedFeatures: createAtLeast(30),
      isMe: (username: string) => profile?.username === username,
    };
  });

  const toggleManageMode = () => {
    authData.value.adminMode = !authData.value.adminMode;
  };

  let refreshTimer: number | undefined = undefined;

  const refresh = () =>
    AuthApi.refresh().then((token) => {
      updateToken(token);
      const { sub, exp, role, iat, crat } = jwtDecode<{
        sub: string;
        exp: number;
        iat: number;
        role: UserRole;
        crat: number;
      }>(token);
      authData.value.profile = {
        token,
        username: sub,
        role,
        issuedAt: iat,
        createdAt: crat,
        expiredAt: exp,
      };
    });

  const refreshIfNeeded = () => {
    // Clear expired Access Token
    if (
      authData.value.profile &&
      Date.now() > authData.value.profile.expiredAt * 1000
    ) {
      authData.value.profile = undefined;
    }

    // Refresh Access Token, cooldown is 1 hour
    const cooldown = 3600 * 1000;
    const sinceIssuedAt = Date.now() - (authData.value.profile?.issuedAt ?? 0);
    if (sinceIssuedAt < cooldown) {
      return;
    }
    return refresh().catch(async (e) => {
      console.warn('Failed to update authorization:' + (await formatError(e)));
    });
  };

  const startRefreshAuth = () => {
    watch(
      () => authData.value.profile?.token,
      (token) => updateToken(token),
      { immediate: true },
    );
    refreshIfNeeded();
    if (refreshTimer === undefined) {
      refreshTimer = window.setInterval(refreshIfNeeded, 15 * 60 * 1000);
    }
  };

  const logout = () => {
    updateToken();
    return AuthApi.logout().then(() => {
      authData.value.profile = undefined;
    });
  };

  return {
    whoami,
    toggleManageMode,
    refresh,
    startRefreshAuth,
    logout,
  };
};
