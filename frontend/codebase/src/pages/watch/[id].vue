<template>
  <v-main class="watch-background">
    <!-- Top Navigation Bar -->
    <div class="top-bar px-4 py-4">
      <!-- Left: Toctik logo -->
      <v-btn class="toc-btn" size="large" variant="text" @click="goHome">
        TocTik
      </v-btn>

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
  import { onMounted, ref } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import axios, { AxiosError } from 'axios'
  import Hls from 'hls.js'
  import type { ErrorData } from 'hls.js'

  const route = useRoute()
  const router = useRouter()
  const authStore = useAuthStore()
  const videoPlayer = ref<HTMLVideoElement | null>(null)
  const videoError = ref<string>('')
  const loading = ref(true)
  const videoDetails = ref<{
    hlsUrl: string
    thumbnailUrl: string
    convertedUrl: string | null
    title: string
    description: string
    userId: string
    uploadTime: string
    duration: number | null
  }>({
    hlsUrl: '',
    thumbnailUrl: '',
    convertedUrl: null,
    title: '',
    description: '',
    userId: '',
    uploadTime: '',
    duration: null,
  })
  const hlsBlobUrl = ref<string>('') // New ref for the Blob URL

  // Fetch video details (hlsUrl as raw playlist, metadata, etc.) with retry
  const fetchVideoDetails = async (videoId: number, retries = 2) => {
    try {
      const userId = authStore.username || ''
      console.log('Fetching video details for videoId:', videoId, 'userId:', userId)

      // Fetch detailed info from /videos/details
      console.log('Sending request to /api/videos/details with params:', { videoId, userId })
      const response = await axios.get(`/api/videos/details`, {
        params: { videoId, userId },
        headers: {
          'X-User-Id': userId,
        },
      })
      console.log('Video Details Response:', JSON.stringify(response.data, null, 2))
      console.log('Raw HLS Playlist Content:', response.data.hlsUrl)
      const data = response.data

      // Set video details
      videoDetails.value = {
        hlsUrl: data.hlsUrl || '',
        thumbnailUrl: data.thumbnailUrl || '',
        convertedUrl: data.convertedUrl || null,
        title: data.title || '',
        description: data.description || '',
        userId: data.userId || '',
        uploadTime: data.uploadTime || '',
        duration: data.duration || null,
      }

      // Create a Blob URL from the raw m3u8 content
      if (videoDetails.value.hlsUrl) {
        const blob = new Blob([videoDetails.value.hlsUrl], { type: 'application/x-mpegURL' })
        hlsBlobUrl.value = URL.createObjectURL(blob)
        console.log('Generated Blob URL for HLS playlist:', hlsBlobUrl.value)
      }

      // Load video with HLS.js or native playback
      if (videoPlayer.value && hlsBlobUrl.value) {
        if (Hls.isSupported()) {
          const hls = new Hls()
          hls.loadSource(hlsBlobUrl.value)
          hls.attachMedia(videoPlayer.value)
          hls.on(Hls.Events.ERROR, (event, data: ErrorData) => {
            if (typeof data.details === 'string' && data.details.includes('networkError') && retries > 0) {
              console.log('Retrying due to network error...', retries)
              fetchVideoDetails(videoId, retries - 1)
            } else {
              videoError.value = `HLS Error: ${data.details}`
              console.error('HLS Error:', event, data)
            }
          })
          hls.on(Hls.Events.MANIFEST_PARSED, () => {
            videoPlayer.value?.play().catch(e => console.error('Play error:', e))
          })
        } else if (videoPlayer.value.canPlayType('application/vnd.apple.mpegurl')) {
          videoPlayer.value.src = hlsBlobUrl.value
          videoPlayer.value.load()
          videoPlayer.value.play().catch(e => console.error('Play error:', e))
        } else {
          videoError.value = 'Your browser does not support HLS playback.'
        }
        videoPlayer.value.onerror = () => {
          videoError.value = 'Failed to load video. The URL may be invalid or inaccessible. Check the console for the Blob URL.'
        }
      }
    } catch (error) {
      const axiosError = error as AxiosError
      console.error('Error fetching video details:', axiosError.message, axiosError.response?.data)
      if (axiosError.response?.status === 403 && retries > 0) {
        console.log('Retrying due to 403 error...', retries)
        await new Promise(resolve => setTimeout(resolve, 1000))
        await fetchVideoDetails(videoId, retries - 1)
      } else {
        videoError.value = 'Failed to fetch video details.'
      }
    } finally {
      loading.value = false
    }
  }

  // Cleanup Blob URL on unmount
  onUnmounted(() => {
    if (hlsBlobUrl.value) {
      URL.revokeObjectURL(hlsBlobUrl.value)
    }
  })

  // Format upload time
  const formatDate = (dateString: string): string => {
    if (!dateString) return 'Unknown Date'
    console.log('Raw uploadTime:', dateString)
    const date = new Date(dateString)
    return isNaN(date.getTime()) ? 'Invalid Date Format' : date.toLocaleDateString()
  }

  // Navigation methods
  const goHome = () => router.push('/')
  const goToManage = () => router.push('/manage')
  const logout = async () => {
    try {
      await axios.get('/api/logout')
      await authStore.logout()
      router.push('/login')
    } catch (error) {
      console.error('Logout failed:', error)
      await authStore.logout()
      router.push('/login')
    }
  }

  // Fetch details on mount
  onMounted(() => {
    const videoId = (route.params as { id?: string }).id
    if (videoId) fetchVideoDetails(parseInt(videoId))
  })
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

.toc-btn {
  font-size: 1.5rem !important;
  padding: 8px 16px !important;
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
</style>

<route lang="json5">
{
meta: {
requiresAuth: true
}
}
</route>
