<template>
  <v-main class="home-background">
    <v-container fluid>
      <v-row class="align-center justify-space-between mb-6 pa-4" style="position: sticky; top: 0; background: #f5f5f0; z-index: 1;">
        <v-col cols="auto">
          <v-btn class="toc-btn" variant="text" @click="goHome">
            TocTik
          </v-btn>
        </v-col>
        <v-col cols="auto">
          <v-btn class="upload-btn" variant="outlined" @click="goToUpload">
            Upload New Video
          </v-btn>
        </v-col>
      </v-row>

      <!-- Scrollable Video List Container -->
      <v-row class="video-list" dense>
        <v-col
          v-for="video in videos"
          :key="video.id"
          class="mb-4"
          cols="12"
        >
          <v-card class="video-row" outlined>
            <v-row align="center" no-gutters>
              <!-- Thumbnail Placeholder -->
              <v-col class="pa-2" cols="3">
                <v-img
                  class="thumbnail"
                  height="100"
                  :src="`https://via.placeholder.com/150`"
                  width="150"
                />
              </v-col>

              <!-- Video Details -->
              <v-col class="pa-2" cols="9">
                <v-card-title class="font-weight-bold">
                  {{ video.name }}
                </v-card-title>
                <v-card-text style="white-space: normal; word-wrap: break-word;">
                  {{ video.description }}
                </v-card-text>
                <v-card-actions>
                  <v-chip class="ma-1" color="brown lighten-4" text-color="brown darken-4">
                    {{ video.visibility }}
                  </v-chip>
                </v-card-actions>
              </v-col>
            </v-row>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'

  const router = useRouter()

  const goToUpload = () => router.push('/upload')
  const goHome = () => router.push('/')

  const videos = ref([
    { id: 1, name: 'Placeholder', description: 'temporarily here for visualizing purposes to test the layout and design and thumbnails will be on the left', visibility: 'Public' },
    { id: 2, name: 'Private', description: 'no one will see private videos', visibility: 'Private' },
  ])
</script>

<style scoped lang="scss">
.upload-btn {
  color: #2b2119 !important;
  border-color: #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.video-row {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.thumbnail {
  object-fit: cover;
}

.video-list {
  padding-top: 20px;
}

.home-background {
  background-color: #f5f5f0;
  min-height: 100vh;
}

</style>
<route lang="json5">
{
  meta: {
    requiresAuth: true
  }
}
</route>
