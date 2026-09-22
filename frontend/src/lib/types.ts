// Mirrors the backend DTOs (see backend/src/main/java/com/culitostracker/api/dto).

export type RelationshipType =
  | 'CASUAL'
  | 'DATING'
  | 'SERIOUS_RELATIONSHIP'
  | 'MONOGAMOUS'
  | 'POLYAMOROUS'
  | 'ENGAGED'
  | 'MARRIED'
  | 'FRIENDS_WITH_BENEFITS'
  | 'OTHER'

export type RelationshipStatus = 'ACTIVE' | 'PAUSED' | 'INACTIVE' | 'ENDED'

export type RelationshipSituation =
  | 'SINGLE'
  | 'IN_RELATIONSHIP'
  | 'MARRIED'
  | 'POLYAMOROUS'
  | 'OTHER'

export type MilestoneType =
  | 'FIRST_DATE'
  | 'STARTED_DATING'
  | 'RELATIONSHIP_STARTED'
  | 'ENGAGEMENT'
  | 'MARRIAGE'
  | 'MOVED_IN_TOGETHER'
  | 'TRIP'
  | 'CUSTOM'

export type Mood =
  | 'VERY_HAPPY'
  | 'HAPPY'
  | 'CALM'
  | 'NEUTRAL'
  | 'TIRED'
  | 'STRESSED'
  | 'SAD'
  | 'ANGRY'
  | 'ANXIOUS'
  | 'OVERWHELMED'
  | 'AFFECTIONATE'
  | 'CUSTOM'

export type ObservationType =
  | 'VERY_HAPPY'
  | 'HAPPY'
  | 'NEUTRAL'
  | 'TIRED'
  | 'STRESSED'
  | 'SAD'
  | 'UPSET'
  | 'DISTANT'
  | 'AFFECTIONATE'
  | 'NEEDS_SPACE'
  | 'NOT_SURE'
  | 'OTHER'

export type CyclePhase = 'MENSTRUATION' | 'FOLLICULAR' | 'OVULATION' | 'LUTEAL'

export type AdviceCategory =
  | 'COMMUNICATION'
  | 'AFFECTION'
  | 'SUPPORT'
  | 'SPACE'
  | 'DATE_IDEA'
  | 'ANNIVERSARY'
  | 'CONFLICT'
  | 'SELF_CARE'
  | 'INTIMACY'
  | 'GENERAL'

export type AdviceSource = 'SELF_REPORT' | 'OBSERVATION' | 'CYCLE' | 'RELATIONSHIP'

export type LeaderboardWindow = 'GLOBAL' | 'MONTH' | 'YEAR'

export interface User {
  id: string
  username: string
  email: string
  displayName: string
  preferredLanguage: string
  avatarEmoji: string | null
  hasAvatar: boolean
  relationshipSituation: RelationshipSituation
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
}

export interface Duration {
  years: number
  months: number
  days: number
  totalDays: number
}

export interface Anniversary {
  date: string
  daysUntil: number
  years: number
}

export interface Relationship {
  id: string
  type: RelationshipType
  status: RelationshipStatus
  datingStartDate: string | null
  relationshipStartDate: string | null
  engagementDate: string | null
  marriageDate: string | null
  relationshipEndDate: string | null
  togetherSince: string | null
  duration: Duration | null
  nextAnniversary: Anniversary | null
}

export interface Partner {
  id: string
  name: string
  nickname: string | null
  notes: string | null
  avatarEmoji: string | null
  linked: boolean
  deleted: boolean
  relationship: Relationship | null
  createdAt: string
}

export interface Milestone {
  id: string
  partnerId: string
  type: MilestoneType
  title: string
  description: string | null
  date: string
}

export interface CycleProfile {
  id: string
  partnerId: string
  averageCycleLength: number
  averagePeriodLength: number
  lastPeriodStartDate: string | null
  trackingEnabled: boolean
}

export interface PeriodRecord {
  id: string
  startDate: string
  endDate: string | null
  notes: string | null
}

export interface CycleDay {
  date: string
  cycleDay: number | null
  phase: CyclePhase | null
  actualPeriod: boolean
  predictedPeriod: boolean
  fertile: boolean
  ovulation: boolean
}

export interface Predictions {
  insufficientData: boolean
  trackingEnabled: boolean
  cycleLength: number | null
  periodLength: number | null
  lastPeriodStart: string | null
  nextPeriodStart: string | null
  ovulationDate: string | null
  fertileWindowStart: string | null
  fertileWindowEnd: string | null
  currentCycleDay: number | null
  currentPhase: CyclePhase | null
  days: CycleDay[]
  disclaimers: string[]
}

export interface CheckIn {
  id: string
  partnerId: string
  mine: boolean
  mood: Mood
  energyLevel: number
  stressLevel: number
  affectionLevel: number | null
  relationshipSatisfaction: number | null
  note: string | null
  date: string
}

export interface Observation {
  id: string
  partnerId: string
  observationType: ObservationType
  note: string | null
  date: string
}

export interface AdviceItem {
  category: AdviceCategory
  messageKey: string
  params: Record<string, string | number>
  source: AdviceSource
}

export interface DashboardCycleSummary {
  trackingEnabled: boolean
  insufficientData: boolean
  currentPhase: CyclePhase | null
  currentCycleDay: number | null
  nextPeriodStart: string | null
  ovulationDate: string | null
}

export interface DashboardPartner {
  partnerId: string
  name: string
  nickname: string | null
  avatarEmoji: string | null
  relationshipType: RelationshipType
  relationshipStatus: RelationshipStatus
  togetherSince: string | null
  duration: Duration | null
  nextAnniversary: Anniversary | null
  cycle: DashboardCycleSummary
  myCheckInToday: CheckIn | null
  partnerCheckInToday: CheckIn | null
  latestObservation: Observation | null
  adviceOfTheDay: AdviceItem | null
}

export interface Dashboard {
  user: User
  partners: DashboardPartner[]
}

export interface LeaderboardEntry {
  rank: number
  alias: string
  avatarEmoji: string | null
  score: number
  me: boolean
}

export interface Leaderboard {
  window: LeaderboardWindow
  entries: LeaderboardEntry[]
}

export interface LeaderboardMe {
  enabled: boolean
  publicAlias: string | null
  showAvatar: boolean
  score: number
  rank: number | null
}

export interface Stats {
  partnersRegistered: number
  uniquePartners: number
  activeRelationships: number
  seriousRelationships: number
  casualRelationships: number
  longestRelationship: Duration | null
  longestRelationshipPartnerName: string | null
  milestonesCount: number
  situation: RelationshipSituation
  leaderboardRank: number | null
}

/** RFC 7807 problem detail extended with our business-rule code. */
export interface ApiProblem {
  status: number
  title?: string
  code?: string
  fields?: Record<string, string>
}
