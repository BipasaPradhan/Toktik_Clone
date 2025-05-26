<template>
  <v-app>
    <v-app-bar app color="teal darken-3">
      <v-spacer/>

      <span class="mr-4">{{ authStore.name }}</span>
      <v-btn
        class="px-0 rounded-circle"
        href=""
        min-width="36"
        target="_blank"
        text
      >
        <v-icon>mdi-dots-vertical</v-icon>
      </v-btn>
    </v-app-bar>
    <v-navigation-drawer app class="teal darken-4">
      <v-list>
        <v-list-item
          title="Home"
          to="/"
        >
          <template #prepend>
            <v-icon>mdi-home</v-icon>
          </template>
        </v-list-item>
        <v-list-item title="About" to="about">
          <template #prepend>
            <v-icon>mdi-information-outline</v-icon>
          </template>
        </v-list-item>
      </v-list>

      <template #append>
        <div class="pa-2">
          <v-btn block @click="logout"> Logout</v-btn>
        </div>
      </template>
    </v-navigation-drawer>
    <v-main>
      <router-view/>
    </v-main>
  </v-app>
</template>
<script lang="ts" setup>

import {useAuthStore} from '@/stores/auth.ts';
import axios from 'axios';
import {useRouter} from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();
const logout = async () => {
  await axios.get('/api/logout');
  await authStore.logout();
  await router.push({path: '/login'});
}

</script>
