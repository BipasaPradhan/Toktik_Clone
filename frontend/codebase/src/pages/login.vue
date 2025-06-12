<script setup lang="ts">
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import apiClient from '@/plugins/axios';
  import type { VForm } from 'vuetify/components'
  import { useAuthStore } from '@/stores/auth.ts'

  const router = useRouter()
  const authStore = useAuthStore();

  const valid = ref(true)
  const username = ref('admin')
  const password = ref('123456')
  const errorMessage = ref('')

  const usernameRules = [(v: string) => !!v || 'Username is required']
  const passwordRules = [(v: string) => !!v || 'Password is required']

  const form = ref<VForm | null>(null)

  const submit = async () => {
    if (form.value?.validate()) {
      const formData = new FormData();
      formData.append('username', username.value);
      formData.append('password', password.value);

      try {
        console.log('Attempting login for user:', username.value);
        const response = await apiClient.post('/api/login', formData);
        console.log('Login response:', response.data);

        if (response.data.success) {
          localStorage.setItem('jwtToken', response.data.data.token);
          await authStore.login(response.data.data.username, response.data.data.token, response.data.data.role);
          console.log('Navigating to /');
          await router.push({ path: '/' });
        } else {
          errorMessage.value = response.data.message;
          console.error('Login failed:', errorMessage.value);
          alert(errorMessage.value);
        }
      } catch (error) {
        errorMessage.value = 'Login failed. Please try again.';
        console.error('Login error:', error);
        alert(errorMessage.value);
      }
    }
  };
  const reset = () => {
    username.value = ''
    password.value = ''
    form.value?.resetValidation()
  }
</script>

<template>
  <v-main class="auth-background">
    <v-container class="d-flex align-center justify-center" style="min-height: 100vh;">
      <v-card class="auth-card" width="800">
        <v-row no-gutters>
          <v-col class="login-section pa-8" cols="12" md="6">
            <v-card-title class="text-center mb-6" style="color: #800020; font-size: 1.5rem;">
              Login to Your Account
            </v-card-title>

            <v-form ref="form" v-model="valid">
              <v-text-field
                v-model="username"
                class="mb-4"
                label="Username"
                :rules="usernameRules"
                variant="outlined"
              />

              <v-text-field
                v-model="password"
                class="mb-6"
                label="Password"
                :rules="passwordRules"
                type="password"
                variant="outlined"
              />

              <v-btn
                block
                class="beige-signin-btn mb-2"
                size="large"
                @click="submit"
              >
                SIGN IN
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

          <v-col class="signup-section pa-8 d-flex align-center" cols="12" md="6">
            <div class="text-center w-100">
              <v-card-title class="text-white mb-4">New Here?</v-card-title>
              <p class="text-white mb-6">
                Sign up and discover a great amount of new opportunities!
              </p>
              <v-btn
                block
                class="beige-signup-btn"
                size="large"
                to="/register"
                variant="outlined"
              >
                SIGN UP
              </v-btn>
            </div>
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

.signup-section {
  background-color: #800020;
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

.beige-signup-btn {
  color: #2b2119 !important;
  background-color: #e8d8c5 !important;
  border: 1px solid #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;

  &:hover {
    background-color: #d4c4b1 !important;
  }
}
</style>

<route lang="json5">
{
meta: {
requiresAuth: false
}
}
</route>
