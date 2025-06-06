<template>
  <v-main class="home-background">
    <v-container fluid>
      <v-row class="justify-space-between mb-6 pa-4" style="position: sticky; top: 0; background: #f5f5f0; z-index: 1;">
        <v-col cols="auto">
          <v-btn class="toc-btn" variant="text" @click="goHome">
            TocTik
          </v-btn>
        </v-col>
        <v-col cols="auto" />
      </v-row>

      <v-row class="mt-6" justify="center">
        <v-col cols="12" md="6" sm="8">
          <v-card class="pa-6">
            <v-card-title class="text-h5 font-weight-bold mb-4">Upload New Video</v-card-title>

            <v-form ref="formRef" v-model="valid" @submit.prevent="handleSubmit">
              <v-text-field
                v-model="name"
                label="Video Name"
                required
                :rules="[v => !!v || 'Name is required']"
              />

              <v-textarea
                v-model="description"
                label="Description"
                required
                :rules="[v => !!v || 'Description is required']"
              />

              <v-file-input
                v-model="file"
                accept="video/*"
                label="Select Video File"
                required
                :rules="[v => !!v || 'File is required']"
              />

              <v-select
                v-model="visibility"
                :items="['Public', 'Private']"
                label="Visibility"
                required
                :rules="[v => !!v || 'Visibility is required']"
              />

              <div class="mt-4">
                <v-btn
                  color="brown"
                  :disabled="!valid"
                  type="submit"
                  variant="flat"
                >
                  Upload
                </v-btn>
                <v-btn
                  class="ml-2"
                  color="grey"
                  variant="outlined"
                  @click="cancelUpload"
                >
                  Cancel
                </v-btn>
              </div>
            </v-form>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'

  const router = useRouter()
  const authStore = useAuthStore()
  const goHome = () => router.push('/')

  const name = ref('')
  const description = ref('')
  const file = ref<File | null>(null)
  const visibility = ref('')
  const valid = ref(false)
  const formRef = ref()

  const checkDuration = (file: File): Promise<boolean> => {
    return new Promise((resolve, reject) => {
      const url = URL.createObjectURL(file);
      const video = document.createElement('video');
      video.preload = 'metadata';
      video.src = url;
      video.onloadedmetadata = () => {
        URL.revokeObjectURL(url);
        resolve(video.duration <= 60);
      };
      video.onerror = () => reject(new Error('Invalid video file'));
    });
  };

  const getPresignedUrl = async (filename: string) => {
    if (!authStore.isLoggedIn) {
      router.push('/login');
      throw new Error('User not authenticated');
    }
    const userId = authStore.getUsername || 'default-user';
    console.log('Requesting presigned URL for userId:', userId, 'filename:', filename);
    const res = await fetch(
      `/api/videos/presign-upload?videoFileName=${encodeURIComponent(filename)}&userId=${encodeURIComponent(userId)}`,
      { credentials: 'include' }
    );
    if (!res.ok) {
      console.error('Presign response:', res.status, await res.text());
      if (res.status === 400) {
        throw new Error('Invalid request: missing userId or videoFileName');
      }
      throw new Error('Failed to get presigned URL');
    }
    const response = await res.json();
    console.log('Received presigned URL:', response.presignedUrl);
    console.log('Received objectKey:', response.objectKey);
    return { presignedUrl: response.presignedUrl, objectKey: response.objectKey };
  };

  const uploadToPresignedUrl = async (presignedUrl: string, file: File) => {
    const res = await fetch(presignedUrl, {
      method: 'PUT',
      headers: {
        'Content-Type': file.type,
      },
      body: file,
    });
    if (!res.ok) {
      console.error('Upload response:', res.status, res.statusText);
      throw new Error(`Upload failed with status ${res.status}: ${res.statusText}`);
    }
  };

  const saveMetadata = async (objectKey: string, title: string, description: string, visibility: string) => {
    if (!authStore.isLoggedIn) {
      router.push('/login');
      throw new Error('User not authenticated');
    }
    const userId = authStore.getUsername || 'default-user';
    const params = new URLSearchParams({
      objectKey,
      title,
      description,
      visibility,
      userId,
    }).toString();
    const res = await fetch(
      `/api/videos/metadata?${params}`,
      {
        method: 'POST',
        credentials: 'include',
      }
    );
    if (!res.ok) {
      console.error('Metadata save response:', res.status, await res.text());
      if (res.status === 400) {
        throw new Error('Invalid request: missing userId or other parameters');
      }
      throw new Error('Failed to save metadata');
    }
    return await res.text();
  };

  const handleSubmit = async () => {
    if (!formRef.value?.validate()) return;
    if (!file.value) return;

    try {
      const isValidDuration = await checkDuration(file.value);
      if (!isValidDuration) {
        alert('Video duration exceeds 60 seconds. Please select a shorter video.');
        return;
      }

      const { presignedUrl, objectKey } = await getPresignedUrl(file.value.name);
      await uploadToPresignedUrl(presignedUrl, file.value);
      await saveMetadata(objectKey, name.value, description.value, visibility.value);

      console.log('Upload and metadata save success:', {
        name: name.value,
        description: description.value,
        fileName: file.value.name,
        visibility: visibility.value,
        objectKey,
      });

      router.push('/manage').then(() => {
        setTimeout(() => {
          console.log('Triggering manageRefresh after delay');
          window.manageRefresh?.();
        }, 2000); // 2-second delay to ensure component is mounted
      });
    } catch (err) {
      console.error('Upload failed:', err);
      alert('Failed to upload video or save metadata. Please try again.');
    }
  };

  const cancelUpload = () => {
    formRef.value?.reset();
    router.push('/manage');
  };
</script>

<style scoped lang="scss">
.toc-btn {
  font-weight: bold;
  font-size: 18px;
  text-transform: none;
  color: #2b2119 !important;
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
