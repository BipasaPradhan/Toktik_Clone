<template>
  <v-main class="home-background">
    <!-- Top Navigation Bar -->
    <div class="top-bar px-4 py-4">
      <!-- Left: Toctik logo -->
      <v-btn class="toc-btn" variant="text" @click="goHome">
        TocTik
      </v-btn>

      <!-- Right: Upload + Logout buttons -->
      <div class="top-buttons">
        <v-btn class="manage-btn" variant="outlined" @click="goToManage">
          Manage
        </v-btn>
        <v-btn class="logout-btn" variant="outlined" @click="logout">
          Logout
        </v-btn>
      </div>
    </div>
  </v-main>
</template>

<script setup lang="ts">
  import { useAuthStore } from '@/stores/auth'
  import { useRouter } from 'vue-router'
  import axios from 'axios'

  const authStore = useAuthStore()
  const router = useRouter()

  const logout = async () => {
    await axios.get('/api/logout');
    await authStore.logout();
    await router.push({ path: '/login' });
  }

  const goHome = () => {
    router.push('/')
  }

  const goToManage = () => {
    router.push('/manage')
  }
</script>

<style scoped lang="scss">
.home-background {
  background-color: #f5f5f0;
  min-height: 100vh;
}

.upload-btn {
  color: #2b2119 !important;
  background-color: #e8d8c5 !important;
  border: 1px solid #c4b5a3 !important;

  &:hover {
    background-color: #d4c4b1 !important;
  }
}

.logout-btn {
  color: #2b2119 !important;
  background-color: transparent !important;
  border: 1px solid #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;

  &:hover {
    background-color: rgba(212, 196, 177, 0.1) !important;
  }
}

.welcome-text {
  font-size: 1.5rem;
  color: #800020;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.top-buttons {
  display: flex;
  gap: 12px;
}

.logout-btn,
.upload-btn {
  color: #2b2119 !important;
  background-color: transparent !important;
  border: 1px solid #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;

  &:hover {
    background-color: rgba(212, 196, 177, 0.1) !important;
  }
}

</style>

<route lang="json5">
{
meta: {
requiresAuth: true
}
}
</route>
