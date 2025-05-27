<template>
  <v-container fluid>
    <v-row class="justify-space-between mb-6 pa-4" style="position: sticky; top: 0; background: white; z-index: 1;">
      <v-col cols="auto">
        <v-btn class="toc-btn" variant="text" @click="goHome">
          TocTik
        </v-btn>
      </v-col>
      <v-col cols="auto">
      </v-col>
    </v-row>

    <v-row justify="center" class="mt-6">
      <v-col cols="12" sm="8" md="6">
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

            <v-btn
              class="mt-4"
              color="brown"
              :disabled="!valid"
              type="submit"
              variant="flat"
            >
              Upload
            </v-btn>
          </v-form>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const goHome = () => router.push('/')

const name = ref('')
const description = ref('')
const file = ref<File | null>(null)
const visibility = ref('')
const valid = ref(false)
const formRef = ref()

const handleSubmit = () => {
  if (!formRef.value?.validate()) return

  // TODO: Replace this with actual upload API
  console.log('Uploading:', {
    name: name.value,
    description: description.value,
    file: file.value,
    visibility: visibility.value,
  })

  // Redirect or show success message
  router.push('/manage')
}
</script>

<style scoped>
.toc-btn {
  font-weight: bold;
  font-size: 18px;
  text-transform: none;
  color: #2b2119 !important;
}
</style>
