import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'
import type {
  CheckIn,
  CycleProfile,
  Dashboard,
  Leaderboard,
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
  User,
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

/** Invalidate everything derived from partner data after a write. */
export function useInvalidatePartnerData() {
  const queryClient = useQueryClient()
  return (partnerId?: string) => {
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    queryClient.invalidateQueries({ queryKey: ['partners'] })
    queryClient.invalidateQueries({ queryKey: ['stats'] })
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
