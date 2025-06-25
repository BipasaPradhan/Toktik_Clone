<template>
  <div class="notification-bell">
    <v-menu
      v-model="showNotifications"
      :close-on-content-click="false"
      location="bottom"
      offset="10"
      @update:model-value="onNotificationToggle"
    >
      <!-- Activator (bell icon with badge) -->
      <template #activator="{ props }">
        <v-badge
          v-if="unreadCount > 0"
          color="red"
          :content="unreadCount"
          overlap
        >
          <v-btn v-bind="props" icon variant="outlined">
            <v-icon>mdi-bell</v-icon>
          </v-btn>
        </v-badge>
        <v-btn
          v-else
          v-bind="props"
          icon
          variant="outlined"
        >
          <v-icon>mdi-bell</v-icon>
        </v-btn>
      </template>

      <!-- Notifications dropdown -->
      <v-card class="notification-panel" max-width="300" min-width="250">
        <v-card-title class="notification-title">Notifications</v-card-title>
        <v-divider />
        <v-list v-if="notifications.length > 0" class="notification-list">
          <v-list-item
            v-for="(notification, index) in notifications"
            :key="index"
            :class="{ 'unread-item': !notification.read }"
            @click="markAsRead(index)"
          >
            <v-list-item-title>{{ notification.message }}</v-list-item-title>
            <v-list-item-subtitle>{{ formatDate(notification.timestamp) }}</v-list-item-subtitle>
          </v-list-item>
        </v-list>
        <v-card-text v-else class="no-notifications">
          No new notifications.
        </v-card-text>
      </v-card>
    </v-menu>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref, watch } from 'vue';
  import { Client } from '@stomp/stompjs';
  import SockJS from 'sockjs-client';
  import { useAuthStore } from '@/stores/auth';

  const authStore = useAuthStore();
  const userId = authStore.username;

  const showNotifications = ref(false);
  const notifications = ref<{ message: string; timestamp: string; read: boolean }[]>([]);
  const unreadCount = ref(0);
  let stompClient: Client | null = null;

  const onNotificationToggle = (val: boolean) => {
    showNotifications.value = val;
    if (val) markAllAsRead();
  };

  const markAsRead = (index: number) => {
    notifications.value[index].read = true;
    unreadCount.value = notifications.value.filter(n => !n.read).length;
  };

  const markAllAsRead = () => {
    notifications.value.forEach(n => (n.read = true));
    unreadCount.value = 0;
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return isNaN(date.getTime()) ? 'Unknown Date' : date.toLocaleString();
  };

  const connectWebSocket = () => {
    if (!userId) return;

    const socket = new SockJS('/ws');
    stompClient = new Client({
      'webSocketFactory': () => socket,
      'reconnectDelay': 5000,
      'debug': str => console.log('STOMP (NotificationBell):', str),
    });

    stompClient.onConnect = () => {
      console.log(`Connected to WebSocket as user ${userId}`);
      stompClient!.subscribe(`/user/${userId}/notifications`, message => {
        const notification = {
          message: message.body,
          timestamp: new Date().toISOString(),
          read: false,
        };
        notifications.value.unshift(notification);
        unreadCount.value = notifications.value.filter(n => !n.read).length;
        console.log('Received notification:', notification);
      });
    };

    stompClient.onStompError = frame => {
      console.error('STOMP error in NotificationBell:', frame);
    };

    stompClient.activate();
  };

  const disconnectWebSocket = () => {
    if (stompClient) {
      stompClient.deactivate();
      stompClient = null;
      console.log('Disconnected from WebSocket');
    }
  };

  onMounted(() => {
    connectWebSocket();
  });

  onUnmounted(() => {
    disconnectWebSocket();
  });

  watch(() => authStore.username, newUserId => {
    disconnectWebSocket();
    if (newUserId) connectWebSocket();
  });
</script>

<style scoped lang="scss">
.notification-bell {
  position: relative;
}

.notification-panel {
  padding: 0;
  background-color: white;
  border: 1px solid #ccc;
}

.notification-title {
  font-size: 1.1rem;
  font-weight: bold;
  color: #2b2119;
  padding: 8px 16px;
}

.notification-list {
  max-height: 300px;
  overflow-y: auto;
}

.unread-item {
  background-color: #f5f5f0;
  font-weight: bold;
}

.no-notifications {
  padding: 16px;
  color: #666;
  font-style: italic;
}
</style>
