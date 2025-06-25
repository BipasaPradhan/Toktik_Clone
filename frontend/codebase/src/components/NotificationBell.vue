<template>
  <div class="notification-bell">
    <v-menu
      v-model="showNotifications"
      :close-on-content-click="false"
      location="bottom"
      offset="10"
      @update:model-value="onNotificationToggle"
    >
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
        <v-btn v-else v-bind="props" icon variant="outlined">
          <v-icon>mdi-bell</v-icon>
        </v-btn>
      </template>

      <v-card class="notification-panel" max-width="320" min-width="260">
        <v-card-title class="notification-title">Notifications</v-card-title>
        <v-divider />
        <v-list v-if="notifications.length > 0" class="notification-list">
          <v-list-item
            v-for="(notification, index) in notifications"
            :key="index"
            :class="{ 'unread-item': !notification.read }"
            @click="handleNotificationClick(notification, index)"
          >
            <v-list-item-title>{{ notification.message }}</v-list-item-title>
            <v-list-item-subtitle>
              {{ formatDate(notification.timestamp) }} —
              <span :class="notification.read ? 'read-label' : 'unread-label'">
                {{ notification.read ? 'Read' : 'Unread' }}
              </span>
            </v-list-item-subtitle>
          </v-list-item>
        </v-list>
        <v-card-text v-else class="no-notifications">
          No notifications yet.
        </v-card-text>
      </v-card>
    </v-menu>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import { useAuthStore } from '@/stores/auth';
  import { Client } from '@stomp/stompjs';
  import SockJS from 'sockjs-client';
  import axios from 'axios';

  type Notification = {
    message: string;
    timestamp: string;
    read: boolean;
    videoId?: string;
  };

  const authStore = useAuthStore();
  const userId = authStore.username;
  const router = useRouter();

  const showNotifications = ref(false);
  const notifications = ref<Notification[]>([]);
  const unreadCount = ref(0);
  let stompClient: Client | null = null;

  const extractVideoId = (msg: string): string | undefined => {
    const match = msg.match(/video '.*?' \(ID: (\d+)\)/);
    return match ? match[1] : undefined;
  };

  const fetchNotifications = async () => {
    if (!userId) return;
    try {
      const response = await axios.get('/api/videos/notifications', {
        headers: { 'X-User-Id': userId },
      });

      notifications.value = (response.data as Notification[])
        .map(n => ({
          message: n.message,
          timestamp: n.timestamp,
          read: String(n.read) === 'true',
          videoId: n.videoId || extractVideoId(n.message),
        }))
        .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
        .slice(0, 10);

      unreadCount.value = notifications.value.filter(n => !n.read).length;
    } catch (error) {
      console.error('Error fetching notifications:', error);
    }
  };

  const handleNotificationClick = async (notification: Notification, index: number) => {
    await markAsRead(index);
    if (notification.videoId) {
      router.push(`/watch/${notification.videoId}`);
    } else {
      console.warn('Notification does not contain a videoId');
    }
  };

  const markAsRead = async (index: number) => {
    const notif = notifications.value[index];
    if (notif.read) return;

    notifications.value[index].read = true;
    unreadCount.value = notifications.value.filter(n => !n.read).length;
    try {
      await axios.post(`/api/videos/notifications/${index}/read`, {}, {
        headers: { 'X-User-Id': userId },
      });
    } catch (error) {
      console.error('Error marking notification as read:', error);
      // revert on error
      notifications.value[index].read = false;
      unreadCount.value = notifications.value.filter(n => !n.read).length;
    }
  };

  const onNotificationToggle = (val: boolean) => {
    showNotifications.value = val;
    if (val) {
      unreadCount.value = notifications.value.filter(n => !n.read).length;
    }
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return isNaN(date.getTime()) ? 'Unknown Date' : date.toLocaleString();
  };

  const connectWebSocket = () => {
    if (!userId) return;

    const socket = new SockJS('/ws');
    stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: str => console.log('STOMP (NotificationBell):', str),
    });

    stompClient.onConnect = () => {
      console.log(`Connected to WebSocket as user ${userId}`);
      stompClient!.subscribe(`/user/${userId}/notifications`, message => {
        const payload = JSON.parse(message.body);
        const newNotification: Notification = {
          message: payload.message,
          timestamp: payload.timestamp,
          read: String(payload.read) === 'true',
          videoId: payload.videoId || extractVideoId(payload.message),
        };
        notifications.value.unshift(newNotification);
        notifications.value = notifications.value
          .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
          .slice(0, 10);
        unreadCount.value = notifications.value.filter(n => !n.read).length;
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
    }
  };

  onMounted(() => {
    fetchNotifications();
    connectWebSocket();
  });

  onUnmounted(() => {
    disconnectWebSocket();
  });

  watch(() => authStore.username, newUserId => {
    disconnectWebSocket();
    notifications.value = [];
    unreadCount.value = 0;
    if (newUserId) {
      fetchNotifications();
      connectWebSocket();
    }
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

.read-label {
  color: #999;
  font-style: italic;
}

.unread-label {
  color: #d50000;
  font-weight: bold;
}

.no-notifications {
  padding: 16px;
  color: #666;
  font-style: italic;
}
</style>
