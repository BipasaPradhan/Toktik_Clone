<script setup lang="ts">
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import axios from 'axios'
  import type { VForm } from 'vuetify/components'
  import { useAuthStore } from '@/stores/auth.ts';

  const router = useRouter()

  const valid = ref(true)
  const username = ref('admin')
  const password = ref('123456')
  const errorMessage = ref('')

  const usernameRules = [(v: string) => !!v || 'Username is required']
  const passwordRules = [(v: string) => !!v || 'Password is required']

  const form = ref<VForm | null>(null)

  const submit = async () => {
    if (form.value?.validate()) {
      const formData = new FormData()
      formData.append('username', username.value)
      formData.append('password', password.value)

      const response = await axios.post('/api/login', formData)

      if (response.data.success) {
        const authStore = useAuthStore();
        await authStore.login(response.data.username, response.data.name, response.data.role)
        await router.push({ path: '/' })
      } else {
        errorMessage.value = response.data.message
        alert(errorMessage.value)
      }
    }
  }

  const reset = () => {
    form.value?.reset()
  }

</script>

<template>
  <v-main>
    <v-container>
      <v-form ref="form" v-model="valid" lazy-validation>
        <v-row class="pt-8">
          <v-col
            lg="6"
            md="8"
            offset-lg="3"
            offset-md="2"
            offset-sm="1"
            offset-xl="4"
            offset-xs="0"
            sm="10"
            xl="4"
            xs="12"
          >
            <v-card class="pa-4" elevation="2">
              <v-container>
                <v-row>
                  <v-col>
                    <v-text-field
                      v-model="username"
                      label="Username"
                      required
                      :rules="usernameRules"
                    />
                  </v-col>
                </v-row>
                <v-row>
                  <v-col>
                    <v-text-field
                      v-model="password"
                      label="Password"
                      required
                      :rules="passwordRules"
                      type="password"
                    />
                  </v-col>
                </v-row>
              </v-container>

              <v-card-actions>
                <v-spacer />
                <v-btn
                  class="mr-4"
                  color="success"
                  :disabled="!valid"
                  text
                  @click="submit"
                >
                  Login
                </v-btn>
                <v-btn class="mr-4" color="error" text @click="reset">
                  Reset
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-col>
        </v-row>
      </v-form>
    </v-container>
  </v-main>
</template>

<style scoped lang="sass">

</style>

<route lang="yaml">
meta:
  layout: login
  requiresAuth: false
</route>
