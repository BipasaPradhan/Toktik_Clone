<template>
  <v-main class="home-background">
    <!-- Top Navigation Bar -->
    <div class="top-bar px-4 py-4">
      <!-- Left: Toctik logo -->
      <v-btn class="toc-btn" size="large" variant="text" @click="goHome">
        TocTik
      </v-btn>

      <!-- Right: Upload button -->
      <div class="top-buttons">
        <v-btn class="upload-btn" variant="outlined" @click="goToUpload">
          Upload New Video
        </v-btn>
      </div>
    </div>

    <v-container class="max-width-container">
      <!-- Loading State -->
      <v-row v-if="loading" justify="center">
        <v-progress-circular class="my-8" color="#800020" indeterminate />
      </v-row>

      <!-- Video List -->
      <v-row v-else class="video-list" dense>
        <v-col
          v-for="video in videos"
          :key="video.id"
          class="mb-4"
          cols="12"
        >
          <v-card class="video-row" outlined>
            <v-row align="center" no-gutters>
              <!-- Thumbnail -->
              <v-col class="pa-2" cols="3">
                <div class="thumbnail-wrapper">
                  <v-img
                    alt="Video Thumbnail"
                    class="thumbnail"
                    :src="video.thumbnailUrl || 'https://via.placeholder.com/150'"
                  />
                </div>
              </v-col>

              <!-- Video Details -->
              <v-col class="pa-2" cols="8">
                <v-card-title class="font-weight-bold">
                  {{ video.title }}
                </v-card-title>
                <v-card-text style="white-space: normal; word-wrap: break-word;">
                  {{ video.description }}
                </v-card-text>
                <v-card-text class="video-meta">
                  <span>{{ video.userId }}</span> • <span>{{ formatDate(video.uploadTime) }}</span>
                </v-card-text>
                <v-card-actions>
                  <v-chip class="ma-1" color="brown lighten-4" text-color="brown darken-4">
                    {{ video.visibility }}
                  </v-chip>
                </v-card-actions>
              </v-col>

              <!-- Triple Dot Menu -->
              <v-col align-self="center" class="pa-2" cols="1">
                <v-menu>
                  <template #activator="{ props }">
                    <v-btn icon v-bind="props">
                      <v-icon>mdi-dots-vertical</v-icon>
                    </v-btn>
                  </template>
                  <v-list>
                    <v-list-item @click="openEditDialog(video)">
                      <v-list-item-title>Edit</v-list-item-title>
                    </v-list-item>
                  </v-list>
                </v-menu>
              </v-col>
            </v-row>
          </v-card>
        </v-col>
      </v-row>

      <!-- No Videos Message -->
      <v-row v-if="!loading && videos.length === 0" justify="center">
        <v-alert class="my-4" type="info">No videos uploaded yet.</v-alert>
      </v-row>

      <!-- Edit Dialog -->
      <v-dialog v-model="editDialog" max-width="500px">
        <v-card>
          <v-card-title>Edit Video</v-card-title>
          <v-card-text>
            <v-text-field v-model="editedVideo.title" label="Title" />
            <v-text-field v-model="editedVideo.description" label="Description" />
            <v-select
              v-model="editedVideo.visibility"
              :items="['Public', 'Private']"
              label="Visibility"
            />
          </v-card-text>
          <v-card-actions>
            <v-btn color="error" @click="editDialog = false">Cancel</v-btn>
            <v-btn color="primary" @click="saveEdit">Save</v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </v-container>
  </v-main>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import axios, { AxiosError } from 'axios'

  const router = useRouter()
  const authStore = useAuthStore()

  // Define Video interface to match API response
  interface Video {
    id: number
    title: string
    description: string
    visibility: string
    thumbnailUrl: string
    uploadTime: string
    userId: string
  }

  // Reactive state
  const videos = ref<Video[]>([])
  const loading = ref(true)
  const page = ref(1)
  const editDialog = ref(false)
  const editedVideo = ref<Partial<Video>>({ id: 0, title: '', description: '', visibility: 'Public' })

  // Fetch videos uploaded by the user
  const fetchMyVideos = async () => {
    try {
      loading.value = true
      const userId = authStore.username || ''
      const response = await axios.get(`/api/videos/my`, {
        params: { page: page.value, size: 20 },
        headers: { 'X-User-Id': userId },
      })
      console.log('My Videos Response:', JSON.stringify(response.data, null, 2))
      videos.value = response.data.videos || []
    } catch (error) {
      const axiosError = error as AxiosError
      console.error('Error fetching my videos:', axiosError.message, axiosError.response?.data)
    } finally {
      loading.value = false
    }
  }

  // Format upload time
  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString()
  }

  // Open edit dialog with video data
  const openEditDialog = (video: Video) => {
    editedVideo.value = { ...video }
    editDialog.value = true
  }

  // Save edited video metadata
  const saveEdit = async () => {
    try {
      const userId = authStore.username || ''
      await axios.put(`/api/videos/${editedVideo.value.id}`, {
        title: editedVideo.value.title,
        description: editedVideo.value.description,
        visibility: editedVideo.value.visibility,
      }, {
        headers: { 'X-User-Id': userId },
      })
      await fetchMyVideos() // Refresh the list
      editDialog.value = false
    } catch (error) {
      const axiosError = error as AxiosError
      console.error('Error updating video:', axiosError.message, axiosError.response?.data)
    }
  }

  // Navigation methods
  const goToUpload = () => router.push('/upload')
  const goHome = () => router.push('/')

  // Fetch videos on mount
  onMounted(() => {
    fetchMyVideos()
  })
</script>

<style scoped lang="scss">
.home-background {
  background-color: #f5f5f0;
  min-height: 100vh;
}

.max-width-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
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

.upload-btn {
  color: #2b2119 !important;
  border-color: #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;

  &:hover {
    background-color: rgba(212, 196, 177, 0.1) !important;
  }
}

.video-row {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.thumbnail-wrapper {
  position: relative;
  width: 100%;
  padding-top: 56.25%; // 16:9 aspect ratio
  background-color: #000;
}

.thumbnail {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px 0 0 8px;
}

.video-list {
  padding-top: 20px;
}

.video-meta {
  font-size: 0.875rem;
  color: #666;
  padding: 0 16px 8px;
}
</style>

<route lang="json5">
{
meta: {
requiresAuth: true
}
}
</route>
