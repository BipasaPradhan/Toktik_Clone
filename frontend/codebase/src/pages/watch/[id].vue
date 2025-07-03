<template>
  <v-main class="watch-background">
    <!-- Top Navigation Bar -->
    <div class="top-bar px-4 py-4">
      <!-- Left: Toctik logo -->
      <Logo @click="goHome" />

      <!-- Right: Manage + Logout buttons + Notification Bell -->
      <div class="top-buttons">
        <v-btn class="manage-btn" variant="outlined" @click="goToManage">
          Manage
        </v-btn>
        <v-btn class="logout-btn" variant="outlined" @click="logout">
          Logout
        </v-btn>
        <NotificationBell />
      </div>
    </div>

    <v-container class="watch-container">
      <!-- Loading State -->
      <v-row v-if="loading" justify="center">
        <v-progress-circular class="my-8" color="#800020" indeterminate />
      </v-row>

      <!-- Video Player and Details -->
      <v-row v-else>
        <v-col cols="12">
          <div class="video-player-wrapper">
            <video
              ref="videoPlayer"
              class="video-player"
              controls
              @loadedmetadata="onLoadedMetadata"
              @play="handleVideoPlay"
            >
              <source :src="hlsBlobUrl" type="application/x-mpegURL">
              Your browser does not support the video tag.
            </video>
            <p v-if="videoError" class="video-error">{{ videoError }}</p>
          </div>
          <div class="video-details">
            <h2 class="video-title">{{ videoDetails.title || 'No Title Available' }}</h2>
            <p class="video-description">{{ videoDetails.description || 'No Description Available' }}</p>
            <p class="video-meta">{{ videoDetails.userId || 'Unknown User' }} • {{ formatDate(videoDetails.uploadTime) }}</p>
            <div class="interaction-section">
              <div class="stats-group">
                <p class="video-meta">
                  <v-icon>mdi-eye</v-icon>
                  {{ videoDetails.viewCount }}
                </p>
                <span class="separator">|</span>
                <p class="like-count-show">
                  <v-icon>mdi-heart</v-icon>
                  <span class="like-count">{{ likeCount }}</span>
                </p>
              </div>
              <v-btn
                class="like-btn"
                :class="{ 'liked': isLiked }"
                :color="isLiked ? '#800020' : 'grey'"
                :disabled="toggleLikeLoading"
                variant="outlined"
                @click="toggleLike"
              >
                <v-icon>{{ isLiked ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>
                Like
              </v-btn>
            </div>
            <v-alert v-if="likeError" class="mt-2" :text="likeError" type="error" />
          </div>

          <!-- Comments Section -->
          <div class="comments-section">
            <h3 class="comments-title">Comments</h3>
            <!-- Comment Form -->
            <v-form v-if="authStore.isLoggedIn" class="comment-form" @submit.prevent="submitComment">
              <v-textarea
                v-model="newComment"
                class="comment-textarea"
                :error-messages="commentError ? [commentError] : []"
                label="Add a comment..."
                maxlength="500"
                rows="3"
                variant="outlined"
              />
              <div class="d-flex justify-end gap-10">
                <v-btn
                  class="mr-4"
                  color="grey-darken-1"
                  :disabled="submittingComment"
                  variant="outlined"
                  @click="cancelComment"
                >
                  Cancel
                </v-btn>
                <v-btn
                  class="comment-submit-btn"
                  color="#800020"
                  :disabled="!newComment.trim() || submittingComment"
                  :loading="submittingComment"
                  type="submit"
                >
                  Post Comment
                </v-btn>
              </div>
            </v-form>
            <v-alert
              v-else
              class="mb-4"
              icon="mdi-information"
              text="Please sign in to comment."
              type="info"
            />

            <!-- Comment Error Alert -->
            <v-alert
              v-if="commentError && authStore.isLoggedIn"
              class="mb-4 comment-error"
              icon="mdi-alert"
              :text="commentError"
              type="error"
            />

            <!-- Comments List -->
            <div v-if="comments.length" class="comments-list">
              <v-card
                v-for="comment in comments"
                :key="comment.id"
                class="comment-card mb-2"
                variant="outlined"
              >
                <v-card-text>
                  <p class="comment-meta">
                    <strong>{{ comment.userId }}</strong> • {{ formatDate(comment.createdAt) }}
                  </p>
                  <p class="comment-content">{{ comment.content }}</p>
                </v-card-text>
              </v-card>
            </div>
            <p v-else class="no-comments">No comments yet.</p>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</template>

<script setup lang="ts">
  import { nextTick, onMounted, onUnmounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useAuthStore } from '@/stores/auth';
  import axios from 'axios';
  import Hls from 'hls.js';
  import type { ErrorData } from 'hls.js';
  import { Client } from '@stomp/stompjs';
  import SockJS from 'sockjs-client';
  import NotificationBell from '@/components/NotificationBell.vue';
  import apiClient from '@/plugins/axios.ts';

  // Define the route params type
  interface RouteParams {
    id?: string;
  }

  interface Comment {
    id: number;
    videoId: number;
    userId: string;
    content: string;
    createdAt: string;
  }

  // Initialize STOMP client
  const stompClient = ref<Client | null>(null);

  const route = useRoute() as { params: RouteParams };
  const router = useRouter();
  const authStore = useAuthStore();

  const videoPlayer = ref<HTMLVideoElement | null>(null);
  const hlsBlobUrl = ref<string>('');
  const videoError = ref<string>('');
  const loading = ref(true);
  const currentVideoId = ref<number | null>(null);

  let hlsInstance: Hls | null = null;

  const videoDetails = ref<{
    hlsUrl: string;
    hlsKey: string;
    thumbnailUrl: string;
    convertedUrl: string | null;
    title: string;
    description: string | null;
    userId: string;
    uploadTime: string;
    duration: number | null;
    viewCount: number;
  }>({
    hlsUrl: '',
    hlsKey: '',
    thumbnailUrl: '',
    convertedUrl: null,
    title: '',
    description: null,
    userId: '',
    uploadTime: '',
    duration: null,
    viewCount: 0,
  });

  const videoDuration = ref<number | null>(null)
  const videoPlayed = ref(false)

  //Comments state
  const comments = ref<Comment[]>([]);
  const newComment = ref('');
  const commentError = ref('');
  const submittingComment = ref(false);

  // Like state
  const isLiked = ref(false);
  const likeCount = ref(0);
  const toggleLikeLoading = ref(false);
  const likeError = ref('');

  const fetchVideoDetails = async (videoId: number, retries = 5) => {
    loading.value = true;
    videoError.value = '';
    const userId = authStore.username || 'default';
    try {
      const [detailsResponse, commentsResponse, likeResponse, viewCountResponse] = await Promise.all([
        axios.get(`/videos/details`, {
          params: { videoId, userId },
          headers: { 'X-User-Id': userId },
        }),
        axios.get(`/videos/${videoId}/comments`, {
          headers: { 'X-User-Id': userId },
        }),
        axios.get(`/videos/${videoId}/is-liked`, {
          headers: { 'X-User-Id': userId },
        }),
        axios.get(`/videos/${videoId}/view-count-total`, {
          headers: { 'X-User-Id': userId },
        }),
      ]);
      videoDetails.value = {
        hlsUrl: detailsResponse.data.hlsUrl || '',
        hlsKey: detailsResponse.data.hlsKey || '',
        thumbnailUrl: detailsResponse.data.thumbnailUrl || '',
        convertedUrl: detailsResponse.data.convertedUrl || null,
        title: detailsResponse.data.title || '',
        description: detailsResponse.data.description || null,
        userId: detailsResponse.data.userId || '',
        uploadTime: detailsResponse.data.uploadTime || '',
        duration: detailsResponse.data.duration || null,
        viewCount: viewCountResponse.data.view_count || 0,
      };

      const likeData = likeResponse.data;
      isLiked.value = likeData.isLiked;
      likeCount.value = likeData.likeCount;
      console.log('Updated like state after fetch:', { isLiked: isLiked.value, likeCount: likeCount.value });

      comments.value = commentsResponse.data || [];
      console.log(`Fetched ${comments.value.length} comments for videoId=${videoId}`);

      if (!videoDetails.value.hlsUrl) {
        videoError.value = 'No HLS URL provided.';
        loading.value = false;
        return;
      }

      const blob = new Blob([videoDetails.value.hlsUrl], { type: 'application/x-mpegURL' });

      if (hlsBlobUrl.value) URL.revokeObjectURL(hlsBlobUrl.value);

      hlsBlobUrl.value = URL.createObjectURL(blob);

      loading.value = false;
      await nextTick();

      if (!videoPlayer.value) {
        videoError.value = 'Video player element not found.';
        return;
      }

      if (hlsInstance) {
        hlsInstance.destroy();
        hlsInstance = null;
      }

      if (Hls.isSupported()) {
        hlsInstance = new Hls();
        hlsInstance.loadSource(hlsBlobUrl.value);
        hlsInstance.attachMedia(videoPlayer.value);

        hlsInstance.on(Hls.Events.MANIFEST_PARSED, () => {
          loading.value = false;
        });

        hlsInstance.on(Hls.Events.ERROR, (event, data: ErrorData) => {
          if (data.fatal) {
            if (data.details.includes('networkError') && retries > 0) {
              fetchVideoDetails(videoId, retries - 1);
            } else {
              videoError.value = `HLS fatal error: ${data.details}`;
              loading.value = false;
            }
          }
        });
      } else if (videoPlayer.value.canPlayType('application/vnd.apple.mpegurl')) {
        videoPlayer.value.src = hlsBlobUrl.value;
        videoPlayer.value.load();
        videoPlayer.value.play().catch(console.error);
        loading.value = false;
      } else {
        videoError.value = 'Your browser does not support HLS playback.';
        loading.value = false;
      }

      videoPlayer.value.onerror = () => {
        videoError.value = 'Failed to load video. Check the HLS URL in the console.';
        loading.value = false;
      };
    }
    catch (error: unknown) {
      console.error(`Error fetching video details or comments for videoId=${videoId}:`, error);
      videoError.value = 'Failed to load video details or comments.';
      loading.value = false;
    }
  };

  const toggleLike = async () => {
    console.log(`Toggling like for videoId=${route.params.id}`);
    if (!authStore.isLoggedIn) {
      likeError.value = 'Please sign in to like.';
      console.log('Toggle like failed: User not authenticated');
      return;
    }
    toggleLikeLoading.value = true;
    likeError.value = '';
    try {
      const response = await axios.post(`/videos/${route.params.id}/like`, {}, {
        headers: { 'X-User-Id': authStore.username },
      });
      const { isLiked: newIsLiked, likeCount: newLikeCount, success, error } = response.data;
      console.log(`Toggle like response for videoId=${route.params.id}:`, { newIsLiked, newLikeCount, success, error });
      if (success) {
        isLiked.value = newIsLiked;
        likeCount.value = newLikeCount;
      } else {
        likeError.value = error || 'Failed to toggle like.';
      }
    } catch (error) {
      console.error(`Error toggling like for videoId=${route.params.id}:`, error);
      likeError.value = 'Failed to toggle like. Please try again.';
    } finally {
      toggleLikeLoading.value = false;
    }
  };

  const handleVideoPlay = () => {
    if (videoPlayer.value) {
      console.log(`Video playback started for videoId=${route.params.id}`);
      // Increment view count immediately on play (including replays)
      if (!videoPlayed.value) {
        incrementViewCount();
      }
      // Reset videoPlayed on ended to allow replay counting
      videoPlayer.value.addEventListener(
        'ended',
        () => {
          videoPlayed.value = false; // Allow replay to increment view
        },
        { once: true }
      );
    }
  };

  const incrementViewCount = async () => {
    try {
      console.log(`Incrementing view count for videoId=${route.params.id}`);
      await axios.post(`/videos/${route.params.id}/views`, {}, {
        headers: { 'X-User-Id': authStore.username },
      });
      console.log(`Successfully incremented view for videoId=${route.params.id}`);
      videoPlayed.value = true; // Mark as played for this play session
    } catch (error) {
      console.error(`Error incrementing view count for videoId=${route.params.id}:`, error);
    }
  };

  const onLoadedMetadata = () => {
    if (videoPlayer.value) {
      videoDuration.value = videoPlayer.value.duration
    }
  }
  const submitComment = async () => {
    if (!authStore.isLoggedIn) {
      commentError.value = 'Please sign in to comment.';
      console.error('Comment submission failed: User not authenticated');
      return;
    }
    if (!newComment.value.trim()) {
      commentError.value = 'Comment cannot be empty.';
      console.error('Comment submission failed: Empty comment');
      return;
    }
    submittingComment.value = true;
    commentError.value = '';

    try {
      console.log(`Submitting comment for videoId=${route.params.id}, userId=${authStore.username}`);
      await axios.post(`/videos/${route.params.id}/comments`, {
        content: newComment.value,
      }, {
        headers: { 'X-User-Id': authStore.username },
      });
      console.log(`Successfully posted comment for videoId=${route.params.id}`);
      newComment.value = '';
    } catch (error: unknown) {
      console.error(`Error posting comment for videoId=${route.params.id}:`, error);
      if (axios.isAxiosError(error)) {
        commentError.value = error.response?.data?.error || 'Failed to post comment.';
      } else {
        commentError.value = 'Failed to post comment.';
      }
    } finally {
      submittingComment.value = false;
    }
  };

  const cancelComment = () => {
    newComment.value = '';
    commentError.value = '';
  };

  const subscribeToVideoTopics = (videoId: number) => {
    if (!stompClient.value) return;

    stompClient.value?.subscribe(`/topic/views/${videoId}`, message => {
      const viewCount = parseInt(message.body);
      console.log(`Received WebSocket view count for videoId=${videoId}: ${viewCount}`);
      videoDetails.value.viewCount = viewCount;
    });

    stompClient.value?.subscribe(`/topic/comments/${videoId}`, message => {
      const comment = JSON.parse(message.body);
      console.log(`Received WebSocket comment for videoId=${videoId}:`, comment);
      comments.value.push({
        id: Number(comment.id),
        videoId: Number(comment.video_id),
        userId: comment.user_id,
        content: comment.content,
        createdAt: comment.created_at,
      });
    });

    stompClient.value?.subscribe(`/topic/likes/${videoId}`, message => {
      try {
        const payload = JSON.parse(message.body);
        if (payload.videoId == videoId.toString()) {
          likeCount.value = parseInt(payload.likeCount);
          // isLiked.value = payload.isLiked === 'true';
          console.log('Updated like state from WebSocket:', {
            likeCount: likeCount.value,
            isLiked: isLiked.value,
          });
        }
      } catch (err) {
        console.error('Failed to parse like payload:', err, message.body);
      }
    });
  };


  const connectWebSocket = (videoId: number) => {
    const rawWsToken = sessionStorage.getItem('wsToken');

    if (!rawWsToken) {
      console.warn('No JWT token found — skipping WebSocket connection.');
      return;
    }

    const encodedToken = encodeURIComponent(rawWsToken);
    const socket = new SockJS(`/ws?wsToken=${encodedToken}`);

    stompClient.value = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: str => console.log('STOMP:', str),
    })

    stompClient.value.onConnect = () => {
      console.log('Connected to WebSocket');
      subscribeToVideoTopics(videoId);
    };

    stompClient.value.onStompError = frame => {
      console.error('STOMP error:', frame);
      videoError.value = 'Failed to connect to real-time updates. Please refresh.';
    };

    stompClient.value.activate();
  }

  let wsTokenRefreshInterval: number | null = null;

  const refreshWsToken = async () => {
    try {
      const response = await apiClient.get('/api/ws-token');
      if (response.data.success) {
        const newWsToken = response.data.data.wsToken;
        sessionStorage.setItem('wsToken', newWsToken);
        console.log('Refreshed wsToken, now reconnecting WebSocket...')
        reconnectWebSocket()
      } else {
        console.warn('Failed to refresh wsToken:', response.data.message);
      }
    } catch (err) {
      console.error('Error refreshing wsToken:', err);
    }
  };

  const reconnectWebSocket = () => {
    const rawWsToken = sessionStorage.getItem('wsToken')
    if (!rawWsToken) {
      console.warn('No JWT token found — skipping WebSocket reconnection.')
      return
    }
    // Unsubscribe old client
    disconnectWebSocket()

    const encodedToken = encodeURIComponent(rawWsToken)
    const newSocket = new SockJS(`/ws?wsToken=${encodedToken}`)

    const newClient = new Client({
      webSocketFactory: () => newSocket,
      reconnectDelay: 5000,
      debug: str => console.log('STOMP:', str),
    })

    newClient.onConnect = () => {
      console.log('Reconnected to WebSocket')

      // Replace with new connection
      stompClient.value = newClient
      if (currentVideoId.value !== null) {
        subscribeToVideoTopics(currentVideoId.value);
      }
    }

    newClient.onStompError = frame => {
      console.error('STOMP error:', frame)
    }

    newClient.activate()
  }

  // Clean up WebSocket connection
  const disconnectWebSocket = () => {
    console.log('Disconnected from WebSocket')
    console.log('WebSocket active:', stompClient.value?.active);
    if (stompClient.value && stompClient.value.active) {
      stompClient.value.deactivate();
      stompClient.value = null;
    }
  }


  onMounted(() => {
    const videoId = (route.params as { id?: string }).id;
    if (videoId) {
      currentVideoId.value = parseInt(videoId);
      fetchVideoDetails(parseInt(videoId));
      connectWebSocket(parseInt(videoId));

      // Wait for videoPlayer element
      nextTick(() => {
        if (videoPlayer.value) {
          videoPlayer.value.addEventListener('play', handleVideoPlay);
          videoPlayer.value.addEventListener('loadedmetadata', onLoadedMetadata);
        }
      });
    } else {
      loading.value = false;
      videoError.value = 'No video ID provided.';
    }

    // Refresh every 4.5 min
    refreshWsToken();
    wsTokenRefreshInterval = setInterval(refreshWsToken, 270000);
  });

  onUnmounted(() => {
    if (hlsBlobUrl.value) {
      URL.revokeObjectURL(hlsBlobUrl.value);
    }
    if (videoPlayer.value) {
      videoPlayer.value.removeEventListener('play', handleVideoPlay);
      videoPlayer.value.removeEventListener('loadedmetadata', onLoadedMetadata);
    }
    if (videoPlayer.value) {
      videoPlayer.value.pause();
      videoPlayer.value.src = '';
    }
    if (hlsInstance) {
      hlsInstance.destroy();
      hlsInstance = null;
    }

    if (wsTokenRefreshInterval) {
      clearInterval(wsTokenRefreshInterval);
      wsTokenRefreshInterval = null;
    }
    disconnectWebSocket();
  });

  const formatDate = (dateString: string): string => {
    if (!dateString) return 'Unknown Date';
    const date = new Date(dateString);
    return isNaN(date.getTime()) ? 'Invalid Date Format' : date.toLocaleString();
  };

  const goHome = () => router.push('/');
  const goToManage = () => router.push('/manage');
  const logout = async () => {
    try {
      await axios.get('/api/logout');
      await authStore.logout();
      router.push('/login');
    } catch {
      await authStore.logout();
      router.push('/login');
    }
  };
</script>


<style scoped lang="scss">
.watch-background {
  background-color: #f5f5f0;
  min-height: 100vh;
}

.watch-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 16px;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.top-buttons {
  display: flex;
  gap: 12px;
  align-items: center;
}

.logout-btn,
.manage-btn {
  color: #2b2119 !important;
  background-color: transparent !important;
  border: 1px solid #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;

  &:hover {
    background-color: rgba(212, 196, 177, 0.1) !important;
  }
}

.video-player-wrapper {
  position: relative;
  width: 100%;
  max-width: 1280px;
  aspect-ratio: 16 / 9;
  margin-bottom: 20px;
}

.video-player {
  width: 100%;
  height: 100%;
  border-radius: 8px;
}

.video-error {
  color: #d32f2f;
  font-size: 0.875rem;
  margin-top: 8px;
}

.video-details {
  padding-left: 16px;
}

.video-title {
  font-size: 2rem;
  font-weight: bold;
  color: #2b2119;
  margin-bottom: 10px;
}

.video-meta {
  font-size: 1rem;
  color: #666;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.video-description {
  font-size: 1rem;
  color: #333;
  line-height: 1.5;
}

.video-meta .v-icon,
.like-count-show .v-icon {
  color: #2b2119;
}

.interaction-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.stats-group {
  display: flex;
  align-items: center;
  gap: 4px;
  line-height: 1.5;
  height: 24px;
}

.video-meta,
.like-count-show {
  display: inline-flex;
  align-items: center;
  margin: 0;
  line-height: 1.5;
  height: 24px;
  vertical-align: middle;
}

.video-meta .v-icon,
.like-count-show .v-icon {
  font-size: 1.2rem;
  line-height: 1.5;
  vertical-align: middle;
}

.separator {
  font-size: 1rem;
  color: #666;
  margin: 0 4px;
  line-height: 1.5;
  height: 24px;
  vertical-align: middle;
}

.like-count-show {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 0;
}

.like-count {
  font-size: 1rem;
  color: #333;
}

.like-btn {
  color: #2b2119 !important;
  background-color: transparent !important;
  border: 1px solid #c4b5a3 !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;

  &:hover {
    background-color: rgba(212, 196, 177, 0.1) !important;
  }

  &.liked {
    color: #800020 !important;
    .v-icon {
      color: #800020 !important;
    }
  }
}

.comments-section {
  margin-top: 24px;
  padding-left: 16px;
}

.comments-title {
  font-size: 1.5rem;
  font-weight: bold;
  color: #2b2119;
  margin-bottom: 16px;
}

.comment-form {
  margin-bottom: 16px;
}

.comment-textarea {
  margin-bottom: 12px;
}

.comment-submit-btn {
  color: white !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.comment-error {
  font-size: 0.875rem;
}

.comments-list {
  margin-top: 16px;
}

.comment-card {
  border: 1px solid #c4b5a3;
  border-radius: 8px;
}

.comment-meta {
  font-size: 0.875rem;
  color: #666;
  margin-bottom: 8px;
}

.comment-content {
  font-size: 1rem;
  color: #333;
  line-height: 1.5;
  word-break: break-word;
}

.no-comments {
  font-size: 1rem;
  color: #666;
  font-style: italic;
}

.toc-logo:hover {
  color: #800020 !important;
}
</style>

<route lang="json5">
{
meta: {
requiresAuth: true
}
}
</route>
