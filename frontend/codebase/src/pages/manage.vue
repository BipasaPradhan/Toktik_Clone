<template>
  <v-main class="home-background">
    <!-- Top Navigation Bar -->
    <div class="top-bar px-4 py-4">
      <!-- Left: Toctik logo -->
      <Logo @click="goHome" />

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
                    v-if="video.thumbnailUrl"
                    alt="Video Thumbnail"
                    class="thumbnail"
                    :src="video.thumbnailUrl"
                  />
                  <div v-else class="thumbnail placeholder-bg" />
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
                  <span>{{ video.userId }}</span> • <span>{{ formatDate(video.uploadTime) }}</span> • <span>Views: {{ video.viewCount }}</span>
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
                    <v-list-item @click="() => confirmDelete(video.id)">
                      <v-list-item-title>Delete</v-list-item-title>
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

      <!-- Delete Confirmation Dialog -->
      <v-dialog v-model="deleteDialog" max-width="400px">
        <v-card>
          <v-card-title>Confirm Delete</v-card-title>
          <v-card-text>Are you sure you want to delete this video?</v-card-text>
          <v-card-actions>
            <v-btn color="secondary" @click="deleteDialog = false">Cancel</v-btn>
            <v-btn color="error" @click="deleteVideo">Delete</v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </v-container>
  </v-main>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import axios, { AxiosError } from 'axios'

  const router = useRouter()
  const authStore = useAuthStore()

  // Define Video interface to match API response
  interface Video {
    id: number;
    hlsUrl: string | null;
    hlsKey: string | null;
    thumbnailUrl: string | null;
    convertedUrl: string | null;
    title: string;
    description: string;
    visibility: string;
    uploadTime: string;
    userId: string;
    status: string;
    viewCount: number;
    objectKey: string;
  }

  // Reactive state
  const videos = ref<Video[]>([])
  const loading = ref(true)
  const page = ref(1)
  const editDialog = ref(false)
  const deleteDialog = ref(false)
  const editedVideo = ref<Partial<Video>>({ id: 0, title: '', description: '', visibility: 'Public' })
  const videoToDelete = ref<number | null>(null)
  let pollInterval: number | null = null
  const isPolling = ref(false)

  // Fetch videos uploaded by the user
  const fetchMyVideos = async () => {
    try {
      loading.value = true;
      const userId = authStore.username || '';
      const response = await axios.get(`/api/videos/my`, {
        params: { page: page.value, size: 20 },
        headers: { 'X-User-Id': userId },
      });
      videos.value = response.data.videos || [];
      console.log('Fetched videos with ids:', videos.value.map(v => v.id));
      const hasProcessing = videos.value.some(video => video.status === 'PROCESSING');
      updatePollingState(hasProcessing);
    } catch (error) {
      const axiosError = error as AxiosError;
      console.error('Error fetching my videos:', axiosError.message, axiosError.response?.data);
    } finally {
      loading.value = false;
    }
  };

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
      await fetchMyVideos()
      editDialog.value = false
    } catch (error) {
      const axiosError = error as AxiosError
      console.error('Error updating video:', axiosError.message, axiosError.response?.data)
    }
  }

  // Confirm delete action
  const confirmDelete = (videoId: number | null) => {
    console.log('Received videoId in confirmDelete:', videoId);
    if (videoId === null || videoId === undefined || isNaN(videoId)) {
      console.error('Invalid videoId:', videoId);
      alert('Invalid video selected for deletion.');
      return;
    }
    videoToDelete.value = videoId;
    deleteDialog.value = true;
  };

  // Delete video
  const deleteVideo = async () => {
    console.log('Current videoToDelete.value:', videoToDelete.value);
    if (videoToDelete.value === null || videoToDelete.value === undefined) {
      console.error('No video selected for deletion');
      alert('Please select a video to delete.');
      deleteDialog.value = false;
      return;
    }
    try {
      const userId = authStore.username || '';
      await axios.delete(`/api/videos/${videoToDelete.value}`, {
        headers: { 'X-User-Id': userId },
      });
      await fetchMyVideos();
    } catch (error) {
      const axiosError = error as AxiosError;
      console.error('Error deleting video:', axiosError.message, axiosError.response?.data);
    } finally {
      deleteDialog.value = false;
      videoToDelete.value = null;
    }
  };


  // Navigation methods
  const goToUpload = () => router.push('/upload')
  const goHome = () => router.push('/')

  // Centralized polling state management
  const updatePollingState = (hasProcessing: boolean) => {
    if (hasProcessing && !isPolling.value) {
      isPolling.value = true
      pollInterval = setInterval(fetchMyVideos, 30000)
    } else if (!hasProcessing && isPolling.value) {
      clearInterval(pollInterval!)
      pollInterval = null
      isPolling.value = false
    }
  }

  // Trigger refresh after navigation
  const refreshMyVideos = () => {
    page.value = 1
    videos.value = []
    fetchMyVideos()
  }

  // Fetch videos on mount
  onMounted(() => {
    fetchMyVideos();
    window.addEventListener('refreshMyVideos', refreshMyVideos);
    window.manageRefresh = refreshMyVideos;
  });

  onUnmounted(() => {
    window.removeEventListener('refreshMyVideos', refreshMyVideos);
    delete window.manageRefresh;
    if (pollInterval) {
      clearInterval(pollInterval);
      pollInterval = null;
      isPolling.value = false;
    }
  });
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

.placeholder-bg {
  background-color: #ccc;
  width: 100%;
  height: 100%;
  border-radius: 8px 0 0 8px;
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
