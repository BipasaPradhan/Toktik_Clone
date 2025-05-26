// Utilities
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    loggedIn: false as boolean,
    username: null as string | null,
    name: null as string | null,
    role: null as string | null,
  }),
  getters: {
    isLoggedIn: state => state.loggedIn,
    getUsername: state => state.username,
    getName: state => state.name,
    getRole: state => state.role,
  },
  actions: {
    async login (username: string, name: string, role: string) {
      this.loggedIn = true
      this.username = username
      this.name = name
      this.role = role
    },
    async logout () {
      this.loggedIn = false
      this.username = null
      this.name = null
      this.role = null
    },
  },
})
