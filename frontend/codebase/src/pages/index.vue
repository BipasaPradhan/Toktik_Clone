<template>
  <v-main class="home-background">
    <!-- Top Navigation Bar -->
    <div class="top-bar px-4 py-4">
      <!-- Left: Toctik logo -->
      <v-btn class="toc-btn" size="large" variant="text" @click="goHome">
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

    <!-- Video Grid -->
    <v-container>
      <!-- Loading State -->
      <v-row v-if="loading" justify="center">
        <v-progress-circular class="my-8" color="#800020" indeterminate />
      </v-row>

      <!-- Video Grid -->
      <v-row v-else-if="videos.length > 0">
        <v-col
          v-for="video in videos"
          :key="video.id"
          cols="12"
          md="4"
          sm="6"
        >
          <v-card class="video-card" elevation="2" @click="goToWatch(video.id)">
            <div class="thumbnail-wrapper">
              <img alt="Video Thumbnail" class="thumbnail" :src="thumbnailUrls[video.id] || 'https://via.placeholder.com/150'">
            </div>
            <v-card-title class="video-title">{{ video.title }}</v-card-title>
            <v-card-text class="video-meta">
              <span>{{ video.userId }}</span> • <span>{{ formatDate(video.uploadTime) }}</span>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- No Videos Message -->
      <v-row v-else justify="center">
        <v-alert class="my-4" type="info">No videos available. Check console for details.</v-alert>
      </v-row>

      <!-- Load More Button -->
      <v-row v-if="!loading && videos.length > 0 && videos.length % 20 === 0" justify="center">
        <v-btn class="load-more-btn my-4" variant="outlined" @click="loadMore">
          Load More
        </v-btn>
      </v-row>
    </v-container>
  </v-main>
</template>

<script setup lang="ts">
  import { useAuthStore } from '@/stores/auth'
  import { useRouter } from 'vue-router'
  import { onMounted, ref } from 'vue'
  import axios, { AxiosError } from 'axios'

  const authStore = useAuthStore()
  const router = useRouter()

  // Define Video interface to match API response
  interface Video {
    id: number
    title: string
    thumbnailUrl: string
    userId: string
    uploadTime: string
    status: string
  }

  // Reactive state
  const videos = ref<Video[]>([])
  const loading = ref(true)
  const page = ref(1)
  const thumbnailUrls = ref<Record<number, string>>({})
  let pollInterval: number | null = null
  const isPolling = ref(false)

  // Fetch videos from the backend
  const fetchVideos = async () => {
    try {
      loading.value = true
      console.log('Fetching videos from:', `/api/videos/feed?page=${page.value}&size=20`)
      const response = await axios.get(`/api/videos/feed`, {
        params: { page: page.value, size: 20 },
      })
      console.log('Full API Response:', JSON.stringify(response.data, null, 2))
      const newVideos: Video[] = response.data.videos || []
      console.log('Parsed Videos:', JSON.stringify(newVideos, null, 2))
      videos.value = [...videos.value, ...newVideos]
      // Pre-fetch thumbnails
      await Promise.all(newVideos.map(video => fetchThumbnail(video.id)))

      // Check if any video is in PROCESSING state
      const hasProcessing = videos.value.some(video => video.status === 'PROCESSING')
      updatePollingState(hasProcessing)
    } catch (error) {
      const axiosError = error as AxiosError
      console.error('Error fetching videos:', axiosError.message, axiosError.response?.data || axiosError.response?.status)
    } finally {
      loading.value = false
    }
  }

  // Fetch and cache thumbnail URL
  const fetchThumbnail = async (videoId: number) => {
    try {
      const userId = authStore.username || ''
      console.log(`Fetching thumbnail for videoId ${videoId}, userId ${userId}`)
      const response = await axios.get(`/api/videos/details`, {
        params: { videoId, userId },
      })
      console.log('Thumbnail Response:', JSON.stringify(response.data, null, 2))
      thumbnailUrls.value[videoId] = response.data.thumbnailUrl || 'https://via.placeholder.com/150'
    } catch (error) {
      const axiosError = error as AxiosError
      console.error('Error fetching thumbnail for videoId', videoId, axiosError.message, axiosError.response?.data)
      thumbnailUrls.value[videoId] = 'https://via.placeholder.com/150'
    }
  }

  // Format upload time
  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString()
  }

  // Navigation methods
  const goToWatch = (videoId: number) => {
    router.push(`/watch/${videoId}`)
  }

  const logout = async () => {
    await axios.get('/api/logout')
    await authStore.logout()
    await router.push({ path: '/login' })
  }

  const goHome = () => {
    router.push('/')
  }

  const goToManage = () => {
    router.push('/manage')
  }

  // Load more videos
  const loadMore = () => {
    page.value += 1
    fetchVideos()
  }

  // Centralized polling state management
  const updatePollingState = (hasProcessing: boolean) => {
    if (hasProcessing && !isPolling.value) {
      console.log('Starting polling for PROCESSING videos')
      isPolling.value = true
      pollInterval = setInterval(fetchVideos, 5000) // Poll every 5 seconds
    } else if (!hasProcessing && isPolling.value) {
      console.log('Stopping polling: No PROCESSING videos')
      clearInterval(pollInterval!)
      pollInterval = null
      isPolling.value = false
    }
  }

  // Trigger refresh after navigation (e.g., from upload)
  const refreshVideos = () => {
    page.value = 1 // Reset to first page
    videos.value = [] // Clear current videos
    fetchVideos() // Fetch fresh data
  }

  // Fetch videos on mount
  onMounted(() => {
    fetchVideos()
    window.addEventListener('refreshVideos', refreshVideos)
  })

  onUnmounted(() => {
    window.removeEventListener('refreshVideos', refreshVideos)
    if (pollInterval) {
      clearInterval(pollInterval)
      pollInterval = null
      isPolling.value = false
    }
  })
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
    background-color: rgba(212, 191.2, 177, 0.1) !important;
  }
}

.video-card {
  cursor: pointer;
  transition: background-color 0.3s;

  &:hover {
    background-color: rgba(212, 196, 177, 0.1);
  }
}

.thumbnail-wrapper {
  position: relative;
  width: 100%;
  padding-top: 56.25%; // 16:9 aspect ratio (9 / 16 * 100%)
  background-color: #000; // Black bars
}

.thumbnail {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain; // Maintain aspect ratio with black bars
  border-radius: 8px 8px 0 0;
}

.video-title {
  font-size: 1rem;
  color: #2b2119;
  line-height: 1.2;
  padding: 8px 16px;
}

.video-meta {
  font-size: 0.875rem;
  color: #666;
  padding: 0 16px 8px;
}

.load-more-btn {
  color: #2b2119 !important;
  border: 1px solid #c4b5a3 !important;
  text-transform: uppercase;

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
