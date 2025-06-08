<template>
  <v-main class="watch-background">
    <!-- Top Navigation Bar -->
    <div class="top-bar px-4 py-4">
      <!-- Left: Toctik logo -->
      <Logo @click="goHome" />

      <!-- Right: Manage + Logout buttons -->
      <div class="top-buttons">
        <v-btn class="manage-btn" variant="outlined" @click="goToManage">
          Manage
        </v-btn>
        <v-btn class="logout-btn" variant="outlined" @click="logout">
          Logout
        </v-btn>
      </div>
    </div>

    <v-container class="watch-container">
      <!-- Loading State -->
      <v-row v-if="loading" justify="center">
        <v-progress-circular class="my-8" color="#800020" indeterminate />
      </v-row>

      <!-- Video Player and Details -->
      <v-row v-else>
        <v-col cols="12">
          <div class="video-player-wrapper">
            <video ref="videoPlayer" class="video-player" controls>
              <source :src="hlsBlobUrl" type="application/x-mpegURL">
              Your browser does not support the video tag.
            </video>
            <p v-if="videoError" class="video-error">{{ videoError }}</p>
          </div>
          <div class="video-details">
            <h2 class="video-title">{{ videoDetails.title || 'No Title Available' }}</h2>
            <p class="video-meta">{{ videoDetails.userId || 'Unknown User' }} • {{ formatDate(videoDetails.uploadTime) }}</p>
            <p class="video-description">{{ videoDetails.description || 'No Description Available' }}</p>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</template>

<script setup lang="ts">
  import { nextTick, onMounted, onUnmounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useAuthStore } from '@/stores/auth';
  import axios, { AxiosError } from 'axios';
  import Hls from 'hls.js';
  import type { ErrorData } from 'hls.js';

  const route = useRoute();
  const router = useRouter();
  const authStore = useAuthStore();

  const videoPlayer = ref<HTMLVideoElement | null>(null);
  const hlsBlobUrl = ref<string>('');
  const videoError = ref<string>('');
  const loading = ref(true);
  let hlsInstance: Hls | null = null;

  const videoDetails = ref<{
    hlsUrl: string;
    hlsKey: string;
    thumbnailUrl: string;
    convertedUrl: string | null;
    title: string;
    description: string | null;
    userId: string;
    uploadTime: string;
    duration: number | null;
  }>({
    hlsUrl: '',
    hlsKey: '',
    thumbnailUrl: '',
    convertedUrl: null,
    title: '',
    description: null,
    userId: '',
    uploadTime: '',
    duration: null,
  });

  // Fetch video details and setup HLS playback
  const fetchVideoDetails = async (videoId: number, retries = 5) => {
    try {
      loading.value = true;
      videoError.value = '';

      const userId = authStore.username || 'default';
      const response = await axios.get(`/videos/details`, {
        params: { videoId, userId },
        headers: { 'X-User-Id': userId },
      });

      // Update video details
      videoDetails.value = {
        hlsUrl: response.data.hlsUrl || '',
        hlsKey: response.data.hlsKey || '',
        thumbnailUrl: response.data.thumbnailUrl || '',
        convertedUrl: response.data.convertedUrl || null,
        title: response.data.title || '',
        description: response.data.description || null,
        userId: response.data.userId || '',
        uploadTime: response.data.uploadTime || '',
        duration: response.data.duration || null,
      };

      if (!videoDetails.value.hlsUrl) {
        videoError.value = 'No HLS URL provided.';
        loading.value = false;
        return;
      }

      // Create Blob URL from HLS playlist content
      const blob = new Blob([videoDetails.value.hlsUrl], { type: 'application/x-mpegURL' });

      // Revoke old blob URL if exists
      if (hlsBlobUrl.value) URL.revokeObjectURL(hlsBlobUrl.value);

      hlsBlobUrl.value = URL.createObjectURL(blob);

      loading.value = false;
      // Wait for DOM update so videoPlayer ref is ready
      await nextTick();

      if (!videoPlayer.value) {
        videoError.value = 'Video player element not found.';
        return;
      }

      // Clean up any previous hlsInstance
      if (hlsInstance) {
        hlsInstance.destroy();
        hlsInstance = null;
      }

      if (Hls.isSupported()) {
        hlsInstance = new Hls();
        hlsInstance.loadSource(hlsBlobUrl.value);
        hlsInstance.attachMedia(videoPlayer.value);

        hlsInstance.on(Hls.Events.MANIFEST_PARSED, () => {
          videoPlayer.value?.play().catch(console.error);
          loading.value = false;
        });

        hlsInstance.on(Hls.Events.ERROR, (event, data: ErrorData) => {
          if (data.fatal) {
            if (data.details.includes('networkError') && retries > 0) {
              fetchVideoDetails(videoId, retries - 1); // retry on network errors
            } else {
              videoError.value = `HLS fatal error: ${data.details}`;
              loading.value = false;
            }
          }
        });
      } else if (videoPlayer.value.canPlayType('application/vnd.apple.mpegurl')) {
        videoPlayer.value.src = hlsBlobUrl.value;
        videoPlayer.value.load();
        videoPlayer.value.play().catch(console.error);
        loading.value = false;
      } else {
        videoError.value = 'Your browser does not support HLS playback.';
        loading.value = false;
      }

      videoPlayer.value.onerror = () => {
        videoError.value = 'Failed to load video. Check the HLS URL in the console.';
        loading.value = false;
      };

    } catch (error) {
      const axiosError = error as AxiosError;
      if (axiosError.response?.status === 403 && retries > 0) {
        await new Promise(resolve => setTimeout(resolve, 1000));
        await fetchVideoDetails(videoId, retries - 1);
      } else {
        videoError.value = `Failed to fetch video details: ${axiosError.message}`;
        loading.value = false;
      }
    }
  };

  onMounted(() => {
    const videoId = (route.params as { id?: string }).id;
    if (videoId) {
      fetchVideoDetails(parseInt(videoId));
    } else {
      loading.value = false;
      videoError.value = 'No video ID provided.';
    }
  });

  onUnmounted(() => {
    if (hlsBlobUrl.value) {
      URL.revokeObjectURL(hlsBlobUrl.value);
    }
    if (videoPlayer.value) {
      videoPlayer.value.pause();
      videoPlayer.value.src = '';
    }
    if (hlsInstance) {
      hlsInstance.destroy();
      hlsInstance = null;
    }
  });

  const formatDate = (dateString: string): string => {
    if (!dateString) return 'Unknown Date';
    const date = new Date(dateString);
    return isNaN(date.getTime()) ? 'Invalid Date Format' : date.toLocaleString();
  };

  const goHome = () => router.push('/');
  const goToManage = () => router.push('/manage');
  const logout = async () => {
    try {
      await axios.get('/api/logout');
      await authStore.logout();
      router.push('/login');
    } catch {
      await authStore.logout();
      router.push('/login');
    }
  };
</script>

<style scoped lang="scss">
.watch-background {
  background-color: #f5f5f0;
  min-height: 100vh;
}

.watch-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 16px;
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
.manage-btn {
  color: #2b2119 !important;
  background-color: transparent !important;
  border: 1px solid #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;

  &:hover {
    background-color: rgba(212, 196, 177, 0.1) !important;
  }
}

.video-player-wrapper {
  position: relative;
  width: 100%;
  max-width: 1280px;
  aspect-ratio: 16 / 9;
  margin-bottom: 20px;
}

.video-player {
  width: 100%;
  height: 100%;
  border-radius: 8px;
}

.video-error {
  color: #d32f2f;
  font-size: 0.875rem;
  margin-top: 8px;
}

.video-details {
  padding-left: 16px;
}

.video-title {
  font-size: 2rem;
  font-weight: bold;
  color: #2b2119;
  margin-bottom: 10px;
}

.video-meta {
  font-size: 1rem;
  color: #666;
  margin-bottom: 10px;
}

.video-description {
  font-size: 1rem;
  color: #333;
  line-height: 1.5;
}

.toc-logo:hover {
  color: #800020 !important;
}

</style>

<route lang="json5">
{
meta: {
requiresAuth: true
}
}
</route>
