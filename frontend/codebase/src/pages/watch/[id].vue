<template>
  <v-main class="watch-background">
    <v-container class="watch-container">
      <!-- Video Player -->
      <div class="video-player-wrapper">
        <video class="video-player" controls>
          <source :src="videoDetails.hlsUrl" type="application/x-mpegURL">
          Your browser does not support the video tag.
        </video>
      </div>

      <!-- Video Details -->
      <div class="video-details">
        <h2 class="video-title">{{ videoDetails.title }}</h2>
        <p class="video-meta">{{ videoDetails.userId }} • {{ formatDate(videoDetails.uploadTime) }}</p>
        <p class="video-description">{{ videoDetails.description }}</p>
      </div>
    </v-container>
  </v-main>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue'
  import { useRoute } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import axios, { AxiosError } from 'axios'

  const route = useRoute()
  const authStore = useAuthStore()

  // Define VideoDetails interface to match API response
  interface VideoDetails {
    hlsUrl: string
    thumbnailUrl: string
    convertedUrl: string | null
    title: string
    description: string
    userId: string
    uploadTime: string
    duration: number | null
  }

  // Reactive state
  const videoDetails = ref<VideoDetails>({
    hlsUrl: '',
    thumbnailUrl: '',
    convertedUrl: null,
    title: '',
    description: '',
    userId: '',
    uploadTime: '',
    duration: null,
  })

  // Fetch video details
  const fetchVideoDetails = async () => {
    try {
      // Type assertion for route.params.id
      const videoId = (route.params as { id?: string }).id
      if (!videoId) throw new Error('No video ID provided in route')
      const userId = authStore.username || ''
      const response = await axios.get(`/videos/details`, {
        params: { videoId: parseInt(videoId), userId },
      })
      videoDetails.value = response.data
    } catch (error) {
      const axiosError = error as AxiosError
      console.error('Error fetching video details:', axiosError.message, axiosError.response?.data)
    }
  }

  // Format upload time
  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString()
  }

  // Fetch details on mount
  onMounted(() => {
    fetchVideoDetails()
  })
</script>

<style scoped lang="scss">
.watch-background {
  background-color: #f5f5f0;
  min-height: 100vh;
}

.watch-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}

.video-player-wrapper {
  position: relative;
  width: 100%;
  max-width: 800px; // Adjustable size
  margin-bottom: 20px;
}

.video-player {
  width: 100%;
  height: auto;
  border-radius: 8px;
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
