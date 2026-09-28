import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiBlob, tokenStore } from './api'
import type {
  AccessScope,
  CheckIn,
  CycleProfile,
  Dashboard,
  DatePlan,
  Encounter,
  GrantState,
  Invite,
  InvitePreview,
  Leaderboard,
  LinkedRelationship,
  LeaderboardMe,
  LeaderboardWindow,
  Milestone,
  Observation,
  ObservationType,
  Partner,
  PeriodRecord,
  Predictions,
  Relationship,
  Stats,
  Trends,
  User,
  Wish,
  XpSummary,
} from './types'

export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard'],
    queryFn: () => api<Dashboard>('/api/dashboard'),
  })
}

export function usePartners() {
  return useQuery({
    queryKey: ['partners'],
    queryFn: () => api<Partner[]>('/api/partners'),
  })
}

export function usePartnerHistory() {
  return useQuery({
    queryKey: ['partners', 'history'],
    queryFn: () => api<Partner[]>('/api/partners/history'),
  })
}

export function usePartner(id: string | undefined) {
  return useQuery({
    queryKey: ['partners', id],
    queryFn: () => api<Partner>(`/api/partners/${id}`),
    enabled: !!id,
  })
}

export function useMilestones(partnerId: string | undefined) {
  return useQuery({
    queryKey: ['milestones', partnerId],
    queryFn: () => api<Milestone[]>(`/api/partners/${partnerId}/milestones`),
    enabled: !!partnerId,
  })
}

export function useCycleProfile(partnerId: string | undefined) {
  return useQuery({
    queryKey: ['cycle', partnerId],
    queryFn: () => api<CycleProfile>(`/api/partners/${partnerId}/cycle`),
    enabled: !!partnerId,
  })
}

export function usePeriods(partnerId: string | undefined) {
  return useQuery({
    queryKey: ['periods', partnerId],
    queryFn: () => api<PeriodRecord[]>(`/api/partners/${partnerId}/periods`),
    enabled: !!partnerId,
  })
}

export function usePredictions(partnerId: string | undefined, from: string, to: string) {
  return useQuery({
    queryKey: ['predictions', partnerId, from, to],
    queryFn: () =>
      api<Predictions>(`/api/partners/${partnerId}/cycle/predictions?from=${from}&to=${to}`),
    enabled: !!partnerId,
  })
}

export function useCheckIns(partnerId: string | undefined) {
  return useQuery({
    queryKey: ['check-ins', partnerId],
    queryFn: () => api<CheckIn[]>(`/api/partners/${partnerId}/check-ins`),
    enabled: !!partnerId,
  })
}

export function useObservations(partnerId: string | undefined) {
  return useQuery({
    queryKey: ['observations', partnerId],
    queryFn: () => api<Observation[]>(`/api/partners/${partnerId}/observations`),
    enabled: !!partnerId,
  })
}

export function useLeaderboard(window: LeaderboardWindow) {
  return useQuery({
    queryKey: ['leaderboard', window],
    queryFn: () => api<Leaderboard>(`/api/leaderboard?window=${window}`),
  })
}

export function useLeaderboardMe() {
  return useQuery({
    queryKey: ['leaderboard', 'me'],
    queryFn: () => api<LeaderboardMe>('/api/leaderboard/me'),
  })
}

export function useStats() {
  return useQuery({
    queryKey: ['stats'],
    queryFn: () => api<Stats>('/api/stats/me'),
  })
}

export function useDatePlans(from: string, to: string) {
  return useQuery({
    queryKey: ['date-plans', from, to],
    queryFn: () => api<DatePlan[]>(`/api/date-plans?from=${from}&to=${to}`),
  })
}

export function useUpcomingDatePlans() {
  return useQuery({
    queryKey: ['date-plans', 'upcoming'],
    queryFn: () => api<DatePlan[]>('/api/date-plans/upcoming'),
  })
}

export function useEncounters(from: string, to: string) {
  return useQuery({
    queryKey: ['encounters', from, to],
    queryFn: () => api<Encounter[]>(`/api/encounters?from=${from}&to=${to}`),
  })
}

export function useXp() {
  return useQuery({
    queryKey: ['xp'],
    queryFn: () => api<XpSummary>('/api/xp/me'),
  })
}

export function useTrends(weeks: number) {
  return useQuery({
    queryKey: ['trends', weeks],
    queryFn: () => api<Trends>(`/api/trends?weeks=${weeks}`),
  })
}

// ---- Gift ideas ----

export function useWishlist(partnerId: string | undefined) {
  return useQuery({
    queryKey: ['wishlist', partnerId],
    queryFn: () => api<Wish[]>(`/api/partners/${partnerId}/wishlist`),
    enabled: !!partnerId,
  })
}

export function useCreateWish(partnerId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { title: string; note?: string; url?: string }) =>
      api<Wish>(`/api/partners/${partnerId}/wishlist`, { method: 'POST', body: payload }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wishlist', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useUpdateWish(partnerId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { id: string; done?: boolean; title?: string }) =>
      api<Wish>(`/api/wishlist/${payload.id}`, {
        method: 'PATCH',
        body: { done: payload.done, title: payload.title },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wishlist', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useDeleteWish(partnerId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/api/wishlist/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wishlist', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

// ---- Linked accounts (invites + consent grants) ----

export function useLinks() {
  return useQuery({
    queryKey: ['links'],
    queryFn: () => api<LinkedRelationship[]>('/api/links'),
  })
}

/** Public preview of an invite; works before logging in. */
export function useInvitePreview(token: string | undefined) {
  return useQuery({
    queryKey: ['invite', token],
    queryFn: () => api<InvitePreview>(`/api/invites/${token}`, { auth: false }),
    enabled: !!token,
    retry: false,
  })
}

export function useGrantsGiven(partnerId: string | undefined) {
  return useQuery({
    queryKey: ['grants', partnerId],
    queryFn: () => api<GrantState[]>(`/api/partners/${partnerId}/access`),
    enabled: !!partnerId,
  })
}

export function useCreateInvite(partnerId: string) {
  return useMutation({
    mutationFn: () => api<Invite>(`/api/partners/${partnerId}/invite`, { method: 'POST' }),
  })
}

export function useAcceptInvite() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (token: string) =>
      api<LinkedRelationship>(`/api/invites/${token}/accept`, { method: 'POST' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['links'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useSetGrant(partnerId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { scope: AccessScope; enabled: boolean }) =>
      api<GrantState>(`/api/partners/${partnerId}/access/${payload.scope}`, {
        method: 'PUT',
        body: { enabled: payload.enabled },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['grants', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['links'] })
      queryClient.invalidateQueries({ queryKey: ['check-ins', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useUnlink() {
  const invalidate = useInvalidatePartnerData()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (partnerId: string) => api<void>(`/api/links/${partnerId}`, { method: 'DELETE' }),
    onSuccess: (_, partnerId) => {
      invalidate(partnerId)
      queryClient.invalidateQueries({ queryKey: ['links'] })
      queryClient.invalidateQueries({ queryKey: ['grants', partnerId] })
    },
  })
}

/** Invalidate everything derived from partner data after a write. */
export function useInvalidatePartnerData() {
  const queryClient = useQueryClient()
  return (partnerId?: string) => {
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['partners'] })
    queryClient.invalidateQueries({ queryKey: ['stats'] })
    // Partner attributes (age, weight, relationship type) feed the XP engine.
    queryClient.invalidateQueries({ queryKey: ['xp'] })
    if (partnerId) {
      queryClient.invalidateQueries({ queryKey: ['cycle', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['periods', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['predictions', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['milestones', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['check-ins', partnerId] })
      queryClient.invalidateQueries({ queryKey: ['observations', partnerId] })
    } else {
      queryClient.invalidateQueries({ queryKey: ['predictions'] })
    }
  }
}

// ---- Mutations ----

export interface PartnerPayload {
  name: string
  nickname?: string | null
  notes?: string | null
  avatarEmoji?: string | null
  birthDate?: string | null
  weightKg?: number | null
  relationshipType: string
  datingStartDate?: string | null
  relationshipStartDate?: string | null
  engagementDate?: string | null
  marriageDate?: string | null
  consentConfirmed: boolean
}

export function useCreatePartner() {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (payload: PartnerPayload) =>
      api<Partner>('/api/partners', { method: 'POST', body: payload }),
    onSuccess: () => invalidate(),
  })
}

export function useUpdatePartner(partnerId: string) {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (payload: Partial<PartnerPayload>) =>
      api<Partner>(`/api/partners/${partnerId}`, { method: 'PATCH', body: payload }),
    onSuccess: () => invalidate(partnerId),
  })
}

export function useDeletePartner() {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (partnerId: string) =>
      api<void>(`/api/partners/${partnerId}`, { method: 'DELETE' }),
    onSuccess: () => invalidate(),
  })
}

export function useUpdateRelationship(partnerId: string) {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      api<Relationship>(`/api/partners/${partnerId}/relationship`, {
        method: 'PATCH',
        body: payload,
      }),
    onSuccess: () => invalidate(partnerId),
  })
}

export function useLogPeriod(partnerId: string) {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (payload: { startDate?: string; endDate?: string; notes?: string }) =>
      api<PeriodRecord>(`/api/partners/${partnerId}/periods`, { method: 'POST', body: payload }),
    onSuccess: () => invalidate(partnerId),
  })
}

export function useDeletePeriod(partnerId: string) {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (periodId: string) => api<void>(`/api/periods/${periodId}`, { method: 'DELETE' }),
    onSuccess: () => invalidate(partnerId),
  })
}

export function useUpdateCycleProfile(partnerId: string) {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      api<CycleProfile>(`/api/partners/${partnerId}/cycle`, { method: 'PATCH', body: payload }),
    onSuccess: () => invalidate(partnerId),
  })
}

export function useCreateCheckIn() {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (payload: {
      partnerId: string
      mood: string
      energyLevel: number
      stressLevel: number
      affectionLevel?: number
      relationshipSatisfaction?: number
      note?: string
    }) => api<CheckIn>('/api/check-ins', { method: 'POST', body: payload }),
    onSuccess: (_, variables) => invalidate(variables.partnerId),
  })
}

export function useCreateObservation() {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (payload: { partnerId: string; observationType: ObservationType; note?: string }) =>
      api<Observation>(`/api/partners/${payload.partnerId}/observations`, {
        method: 'POST',
        body: { observationType: payload.observationType, note: payload.note },
      }),
    onSuccess: (_, variables) => invalidate(variables.partnerId),
  })
}

export function useCreateMilestone(partnerId: string) {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (payload: { type: string; title: string; description?: string; date: string }) =>
      api<Milestone>(`/api/partners/${partnerId}/milestones`, { method: 'POST', body: payload }),
    onSuccess: () => invalidate(partnerId),
  })
}

export function useDeleteMilestone(partnerId: string) {
  const invalidate = useInvalidatePartnerData()
  return useMutation({
    mutationFn: (milestoneId: string) =>
      api<void>(`/api/milestones/${milestoneId}`, { method: 'DELETE' }),
    onSuccess: () => invalidate(partnerId),
  })
}

export function useCreateDatePlan() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: {
      partnerId: string
      title: string
      location?: string
      notes?: string
      date: string
      startTime?: string
    }) => api<DatePlan>('/api/date-plans', { method: 'POST', body: payload }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['date-plans'] }),
  })
}

export function useDeleteDatePlan() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (planId: string) => api<void>(`/api/date-plans/${planId}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['date-plans'] }),
  })
}

export function useLogEncounter() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { partnerId: string; date: string; notes?: string }) =>
      api<Encounter>('/api/encounters', { method: 'POST', body: payload }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['encounters'] })
      queryClient.invalidateQueries({ queryKey: ['xp'] })
    },
  })
}

export function useDeleteEncounter() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (encounterId: string) =>
      api<void>(`/api/encounters/${encounterId}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['encounters'] })
      queryClient.invalidateQueries({ queryKey: ['xp'] })
    },
  })
}

export function useUpdateLeaderboardSettings() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { enabled?: boolean; publicAlias?: string; showAvatar?: boolean }) =>
      api<LeaderboardMe>('/api/leaderboard/me/settings', { method: 'PATCH', body: payload }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leaderboard'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}

export function useUpdateProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: {
      displayName?: string
      preferredLanguage?: string
      avatarEmoji?: string
      relationshipSituation?: string
      timezone?: string
      remindersEnabled?: boolean
    }) => api<User>('/api/users/me', { method: 'PATCH', body: payload }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (payload: { currentPassword: string; newPassword: string }) =>
      api<void>('/api/users/me/password', { method: 'PATCH', body: payload }),
  })
}

/** Revokes every other session; this device keeps its refresh token. */
export function useLogoutEverywhereElse() {
  return useMutation({
    mutationFn: () =>
      api<void>('/api/auth/logout-all', {
        method: 'POST',
        body: { refreshToken: tokenStore.refresh },
      }),
  })
}

/** Downloads the JSON export and hands it to the browser as a file. */
export function useExportData() {
  return useMutation({
    mutationFn: async () => {
      const blob = await apiBlob('/api/users/me/export')
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'culitostracker-export.json'
      a.click()
      URL.revokeObjectURL(url)
    },
  })
}

export function useDeleteAccount() {
  return useMutation({
    mutationFn: (password: string) =>
      api<void>('/api/users/me', { method: 'DELETE', body: { password } }),
  })
}

// ---- Profile photo ----

/** Object URL of the user's own photo; enable only when user.hasAvatar. */
export function useMyAvatar(enabled: boolean) {
  return useQuery({
    queryKey: ['avatar', 'me'],
    enabled,
    staleTime: Infinity,
    queryFn: async () => {
      const blob = await apiBlob('/api/users/me/avatar')
      return URL.createObjectURL(blob)
    },
  })
}

export function useUploadAvatar() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData()
      form.append('file', file)
      return api<void>('/api/users/me/avatar', { method: 'PUT', body: form })
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['avatar', 'me'] }),
  })
}

export function useDeleteAvatar() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api<void>('/api/users/me/avatar', { method: 'DELETE' }),
    onSuccess: () => queryClient.removeQueries({ queryKey: ['avatar', 'me'] }),
  })
}
