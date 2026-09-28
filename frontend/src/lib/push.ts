import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'

export interface PushConfig {
  enabled: boolean
  publicKey: string | null
  subscribed: boolean
}

/** Push needs a service worker, the Push API and Notification permission. */
export function pushSupported(): boolean {
  return (
    typeof window !== 'undefined' &&
    'serviceWorker' in navigator &&
    'PushManager' in window &&
    'Notification' in window
  )
}

/** VAPID keys travel as URL-safe base64; the browser wants raw bytes. */
function applicationServerKey(base64: string): ArrayBuffer {
  const padding = '='.repeat((4 - (base64.length % 4)) % 4)
  const normalized = (base64 + padding).replace(/-/g, '+').replace(/_/g, '/')
  const raw = atob(normalized)
  const buffer = new ArrayBuffer(raw.length)
  const view = new Uint8Array(buffer)
  for (let i = 0; i < raw.length; i++) view[i] = raw.charCodeAt(i)
  return buffer
}

export function usePushConfig() {
  return useQuery({
    queryKey: ['push', 'config'],
    queryFn: () => api<PushConfig>('/api/push/config'),
  })
}

export class PushPermissionDenied extends Error {}

export function useEnablePush() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (publicKey: string) => {
      const permission = await Notification.requestPermission()
      if (permission !== 'granted') throw new PushPermissionDenied('denied')
      const registration = await navigator.serviceWorker.ready
      const subscription =
        (await registration.pushManager.getSubscription()) ??
        (await registration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: applicationServerKey(publicKey),
        }))
      const json = subscription.toJSON()
      await api<void>('/api/push/subscriptions', {
        method: 'POST',
        body: {
          endpoint: json.endpoint,
          p256dh: json.keys?.p256dh,
          auth: json.keys?.auth,
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        },
      })
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['push'] }),
  })
}

export function useDisablePush() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.getSubscription()
      if (subscription) {
        await api<void>('/api/push/subscriptions', {
          method: 'DELETE',
          body: { endpoint: subscription.endpoint },
        })
        await subscription.unsubscribe()
      }
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['push'] }),
  })
}
