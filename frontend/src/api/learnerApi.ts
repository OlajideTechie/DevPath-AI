export const studyDays = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const

export type StudyDay = (typeof studyDays)[number]
export type ProgrammingLevel = 'BEGINNER' | 'INTERMEDIATE' | 'EXPERIENCED'
export type LearningStyle = 'PRACTICAL' | 'READING' | 'MIXED'
export type PreferredLanguage = 'JAVA'
export type ProfileStatus = 'CREATED'

export interface CreateLearnerRequest {
  currentRole: string
  yearsOfExperience: number
  specialization: string
  existingSkills: string[]
  programmingLevel: ProgrammingLevel
  studyDays: StudyDay[]
  dailyMinutes: number
  learningStyle: LearningStyle
}

export interface LearnerCreatedResponse {
  id: string
  currentRole: string
  targetRole: string
  preferredLanguage: PreferredLanguage
  programmingLevel: ProgrammingLevel
  profileStatus: ProfileStatus
  createdAt: string
}

export interface LearnerProfileResponse extends LearnerCreatedResponse {
  yearsOfExperience: number
  specialization: string
  existingSkills: string[]
  studyDays: StudyDay[]
  dailyMinutes: number
  learningStyle: LearningStyle
}

interface ApiErrorResponse {
  code: string
  message: string
  fieldErrors?: Record<string, string>
}

export class LearnerApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly fieldErrors: Record<string, string>

  constructor(
    message: string,
    status: number,
    code?: string,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'LearnerApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const apiBaseUrl = configuredBaseUrl.replace(/\/+$/, '')

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
    })
  } catch {
    throw new LearnerApiError(
      "Could not reach the API. Check that the backend is running and that this site's origin is allowed by FRONTEND_ORIGINS.",
      0,
    )
  }

  const body: unknown = await response.json().catch(() => undefined)
  if (!response.ok) {
    const error = isApiErrorResponse(body) ? body : undefined
    throw new LearnerApiError(
      error?.message ?? `Request failed (${response.status}). Please try again.`,
      response.status,
      error?.code,
      error?.fieldErrors,
    )
  }

  if (body === undefined) {
    throw new LearnerApiError('The server returned an unreadable response.', response.status)
  }
  return body as T
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (typeof value !== 'object' || value === null) return false
  const candidate = value as Record<string, unknown>
  return typeof candidate.code === 'string' && typeof candidate.message === 'string'
}

export function createLearner(
  requestBody: CreateLearnerRequest,
): Promise<LearnerCreatedResponse> {
  return request<LearnerCreatedResponse>('/api/v1/learners', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

export function getLearner(learnerId: string): Promise<LearnerProfileResponse> {
  return request<LearnerProfileResponse>(
    `/api/v1/learners/${encodeURIComponent(learnerId)}`,
  )
}
