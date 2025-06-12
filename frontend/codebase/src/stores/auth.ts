// Utilities
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    loggedIn: false as boolean,
    username: null as string | null,
    role: null as string | null,
    token: null as string | null,
  }),
  getters: {
    isLoggedIn: state => state.loggedIn,
    getUsername: state => state.username,
    getRole: state => state.role,
    getToken: state => state.token,
  },
  actions: {
    async login (username: string, token: string, role: string) {
      this.loggedIn = true
      this.username = username
      this.token = token
      this.role = role
      console.log('Auth store updated: User logged in', { username, role });
    },

    async logout () {
      this.loggedIn = false
      this.username = null
      this.role = null
      this.token = null
      localStorage.removeItem('jwtToken');
      console.log('Auth store cleared: User logged out');
    },
  },
})
