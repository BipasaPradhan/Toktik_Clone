<script setup lang="ts">
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import axios from 'axios'
  import type { VForm } from 'vuetify/components'

  const router = useRouter()
  const form = ref<VForm | null>(null)

  const username = ref('')
  const password = ref('')
  const confirmPassword = ref('')

  const usernameRules = [
    (v: string) => !!v || 'Username is required',
  ]

  const passwordRules = [
    (v: string) => !!v || 'Password is required',
  ]

  const confirmPasswordRules = [
    (v: string) => !!v || 'Please confirm password',
    (v: string) => v === password.value || 'Passwords must match',
  ]

  const submit = async () => {
    const valid = await form.value?.validate()
    if (!valid) return

    try {
      const res = await axios.post('/api/register', {
        username: username.value,
        password: password.value,
      })
      if (res.data.success) {
        router.push('/login')
      }
    } catch (error) {
      console.error('Registration failed:', error)
    }
  }

  const reset = () => {
    username.value = ''
    password.value = ''
    confirmPassword.value = ''
    form.value?.resetValidation()
  }
</script>

<template>
  <v-main class="auth-background">
    <v-container class="d-flex align-center justify-center" style="min-height: 100vh;">
      <v-card class="auth-card" width="600">
        <v-row no-gutters>
          <!-- Registration Form Section -->
          <v-col class="login-section pa-8" cols="12">
            <v-card-title class="text-center mb-6" style="color: #800020; font-size: 1.5rem;">
              Create Your Account
            </v-card-title>

            <v-form ref="form">
              <v-text-field
                v-model="username"
                class="mb-4"
                label="Username"
                :rules="usernameRules"
                variant="outlined"
              />

              <v-text-field
                v-model="password"
                class="mb-4"
                label="Password"
                :rules="passwordRules"
                type="password"
                variant="outlined"
              />

              <v-text-field
                v-model="confirmPassword"
                class="mb-6"
                label="Confirm Password"
                :rules="confirmPasswordRules"
                type="password"
                variant="outlined"
              />

              <v-btn
                block
                class="beige-signin-btn mb-2"
                size="large"
                @click="submit"
              >
                REGISTER
              </v-btn>
              <v-btn
                block
                class="beige-cancel-btn"
                size="large"
                variant="outlined"
                @click="reset"
              >
                CANCEL
              </v-btn>
            </v-form>
          </v-col>
        </v-row>
      </v-card>
    </v-container>
  </v-main>
</template>

<style scoped lang="scss">
.auth-background {
  background-color: #f5f5f0;
}

.auth-card {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.login-section {
  background-color: white;
  padding: 48px;
}

.beige-signin-btn {
  color: #2b2119 !important;
  background-color: #e8d8c5 !important;
  border: 1px solid #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: none !important;

  &:hover {
    background-color: #d4c4b1 !important;
  }
}

.beige-cancel-btn {
  color: #2b2119 !important;
  background-color: transparent !important;
  border: 1px solid #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;

  &:hover {
    background-color: rgba(212, 196, 177, 0.1) !important;
  }
}
</style>

<route lang="yaml">
meta:
layout: login
requiresAuth: false
</route>
