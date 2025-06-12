/**
 * router/index.ts
 *
 * Automatic routes for `./src/pages/*.vue`
 */

// Composables
import { createRouter, createWebHashHistory } from 'vue-router/auto'
import { setupLayouts } from 'virtual:generated-layouts'
import { routes } from 'vue-router/auto-routes'
import { useAuthStore } from '@/stores/auth.ts';
import apiClient from '@/plugins/axios';

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: setupLayouts(routes),
})

// Workaround for https://github.com/vitejs/vite/issues/11804
router.onError((err, to) => {
  if (err?.message?.includes?.('Failed to fetch dynamically imported module')) {
    if (!localStorage.getItem('vuetify:dynamic-reload')) {
      console.log('Reloading page to fix dynamic import error')
      localStorage.setItem('vuetify:dynamic-reload', 'true')
      location.assign(to.fullPath)
    } else {
      console.error('Dynamic import error, reloading page did not fix it', err)
    }
  } else {
    console.error(err)
  }
})

router.isReady().then(() => {
  localStorage.removeItem('vuetify:dynamic-reload')
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  if (to.path === '/login') {
    next();
    return;
  }

  try {
    console.log('Fetching /api/whoami');
    const response = await apiClient.get('/api/whoami', {
      headers: { 'Content-Type': 'application/json' },
    });
    console.log('Whoami response:', response.data);

    if (response.data.loggedIn) {
      const token = localStorage.getItem('jwtToken') || '';
      await authStore.login(response.data.username, token, response.data.role);
    } else {
      await authStore.logout();
    }
  } catch (error) {
    console.error('Error fetching /api/whoami:', error);
    await authStore.logout();
  }

  if (to.matched.some(record => record.meta.requiresAuth) && !authStore.isLoggedIn) {
    console.log('Redirecting to /login: User not authenticated');
    next('/login');
  } else {
    next();
  }
});
export default router
