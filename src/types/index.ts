export interface User {
  id: number
  username: string
  email: string
  firstName: string
  lastName: string
  role: 'USER' | 'MANAGER' | 'LEADER' | 'ADMIN'
  isActive: boolean
  manager?: User
  createdAt: string
  updatedAt: string
}

export interface WorkSummary {
  id: number
  userId: number
  summaryDate: string
  summaryType: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  githubActivity: string
  meetingParticipation: string
  documentationWork: string
  customerSupport: string
  aiGeneratedSummary: string
  productivityScore: number
  collaborationScore: number
  createdAt: string
  updatedAt: string
}

export interface MeetingMinutes {
  id: number
  meetingTitle: string
  meetingDate: string
  meetingPlatform: 'ZOOM' | 'MICROSOFT_TEAMS' | 'GOOGLE_MEET' | 'SLACK_HUDDLE' | 'OTHER'
  meetingId: string
  durationMinutes: number
  transcript: string
  aiSummary: string
  keyPoints: string
  actionItems: string
  decisionsMade: string
  attendees: string
  recordingUrl?: string
  processed: boolean
  createdAt: string
  updatedAt: string
}

export interface Team {
  id: number
  name: string
  description: string
  managerId: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  updateUser: (userData: Partial<User>) => void
}

export interface ApiResponse<T> {
  data?: T
  message?: string
  error?: string
  totalCount?: number
}

export interface TimeFilter {
  label: string
  value: string
  startDate: Date
  endDate: Date
}
