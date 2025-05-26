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

  // const reset = () => {
  //   form.value?.reset()
  // }

</script>

<template>
  <v-main class="auth-background">
    <v-container
      class="fill-height"
      fluid
    >
      <v-row
        align="center"
        justify="center"
      >
        <v-col
          cols="12"
          lg="5"
          md="6"
          sm="8"
          xl="4"
        >
          <v-card
            class="auth-card"
            elevation="4"
          >
            <!-- Login Section -->
            <div class="login-section">
              <v-card-title class="text-center py-6">
                <h2 class="text-h4 font-weight-bold">Login to Your Account</h2>
              </v-card-title>

              <v-card-text class="px-8 pb-2">
                <v-form
                  ref="form"
                  v-model="valid"
                  lazy-validation
                >
                  <v-text-field
                    v-model="username"
                    bg-color="beige-lighten-5"
                    class="mb-4"
                    label="Username"
                    required
                    :rules="usernameRules"
                    variant="outlined"
                  />

                  <v-text-field
                    v-model="password"
                    bg-color="beige-lighten-5"
                    class="mb-2"
                    label="Password"
                    required
                    :rules="passwordRules"
                    type="password"
                    variant="outlined"
                  />

                  <v-btn
                    block
                    class="mt-4 mb-6 beige-signin-btn"
                    color="burgundy"
                    size="large"
                    @click="submit"
                  >
                    SIGN IN
                  </v-btn>
                </v-form>
              </v-card-text>
            </div>

            <!-- Signup Section -->
            <div class="signup-section">
              <v-divider class="my-4" />
              <v-card-text class="text-center px-8 py-6 dark-burgundy-section">
                <p class="text-body-1 mb-2 text-white">New Here?</p>
                <p class="text-body-2 mb-4 text-white">Sign up and discover new opportunities!</p>
                <v-btn
                  class="beige-signup-btn"
                  variant="outlined"
                  @click="router.push('/register')"
                >
                  SIGN UP
                </v-btn>
              </v-card-text>
            </div>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</template>

<style scoped lang="scss">
.auth-background {
  background-color: #f5f5f0;
  min-height: 100vh;
}

.auth-card {
  background-color: #ffffff;
  border-radius: 16px;
  overflow: hidden;

  .v-card-title {
    color: #800020;
  }
}

.login-section {
  background-color: white;

  .beige-signin-btn {
    color: #2b2119 !important;
    background-color: #e8d8c5 !important;
    border: 1px solid #c4b5a3 !important;

    &:hover {
      background-color: #d4c4b1 !important;
      border-color: #a89a88 !important;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

  }
}


.signup-section {
  background-color: #4a0c1a;
  color: white;

  .v-divider {
    border-color: rgba(255, 255, 255, 0.1) !important;
  }

  .beige-signup-btn {
    color: #2b2119 !important;
    background-color: #e8d8c5 !important;
    border-color: #c4b5a3 !important;

    &:hover {
      background-color: #d4c4b1 !important;
      border-color: #a89a88 !important;
    }
  }
}

:deep() {
  .v-theme--light {
    --v-theme-burgundy: #800020;
    --v-theme-magenta: #8b008b;
    --v-theme-beige-lighten-5: #f5f5f0;
    --v-theme-burgundy-darken-4: #4a0c1a;
  }

  .v-btn--variant-outlined {
    border-color: rgba(139, 0, 139, 0.5);
  }

  .v-field--outlined {
    --v-field-border-opacity: 0.2;
  }
}
</style>


<route lang="yaml">
meta:
layout: login
requiresAuth: false
</route>
