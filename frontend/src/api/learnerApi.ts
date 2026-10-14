export const studyDays = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const

export const experienceBands = [
  { value: 'ZERO_TO_ONE', label: '0–1 years' },
  { value: 'TWO_TO_THREE', label: '2–3 years' },
  { value: 'FOUR_TO_FIVE', label: '4–5 years' },
  { value: 'SIX_TO_EIGHT', label: '6–8 years' },
  { value: 'NINE_PLUS', label: '9+ years' },
] as const

export type StudyDay = (typeof studyDays)[number]
export type ExperienceBand = (typeof experienceBands)[number]['value']
export type ProgrammingLevel = 'BEGINNER' | 'INTERMEDIATE' | 'EXPERIENCED'
export type LearningStyle = 'PRACTICAL' | 'READING' | 'MIXED'
export type PreferredLanguage = 'JAVA'
export type ProfileStatus = 'CREATED'

export interface CreateLearnerRequest {
  experienceBand: ExperienceBand
  specializations: string[]
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
  experienceBand: ExperienceBand
  specializations: string[]
  existingSkills: string[]
  studyDays: StudyDay[]
  dailyMinutes: number
  learningStyle: LearningStyle
}

export interface RecommendedLessonResponse {
  lessonId: string
  title: string
  summary: string
  learningObjective: string
  estimatedMinutes: number
  recommendationReason: string
  qaScenario: string
}

export interface LessonSection {
  heading: string
  content: string
  codeExample: string
}

export interface PracticeExercise {
  title: string
  prompt: string
  requirements: string[]
  testCases: string[]
  starterCode: string
}

export interface LessonContentResponse {
  lessonId: string
  title: string
  summary: string
  learningObjective: string
  estimatedMinutes: number
  qaContext: string
  sections: LessonSection[]
  exercise: PracticeExercise
}

export interface AdvisoryFeedback {
  summary: string
  strengths: string[]
  issues: string[]
  improvements: string[]
  findings: FeedbackFinding[]
  assessmentConfidence: 'HIGH' | 'MEDIUM' | 'LOW'
  competencyTags: string[]
  nextActionType: 'TARGETED_CORRECTION' | 'REINFORCEMENT' | 'NEXT_LESSON' | 'CLARIFICATION'
  nextAction: string
  reviewRecommendation: 'NEEDS_REVIEW' | 'CONTINUE_PRACTICE' | 'READY_FOR_REVIEW' | 'UNCERTAIN'
  feedbackSource: 'LIVE_PROVIDER' | 'DEMO_SAMPLE'
}

export interface FeedbackFinding {
  category: 'FUNCTIONAL_DEFECT' | 'OPTIONAL_IMPROVEMENT' | 'ADDITIONAL_DEFENSIVE_CHECK' | 'PLANNED_REINFORCEMENT'
  competency: string
  description: string
}

export interface SubmissionResponse {
  attemptId: string
  learnerId: string
  lessonId: string
  feedback: AdvisoryFeedback
  advisoryOnly: boolean
  codeExecutionPerformed: boolean
  submittedAt: string
}

export type RecallStatus = 'NOT_STARTED' | 'NEEDS_REVIEW' | 'PRACTICING' | 'DEMONSTRATED'

export interface CompetencyRecall {
  competencyTag: string
  status: RecallStatus
  gapAttemptId: string | null
  baseReadyAttemptId: string | null
  recallReadyAttemptId: string | null
  learnerConfirmed: boolean
  updatedAt: string | null
  reason: 'COMPETENCY_GAP' | 'PLANNED_REINFORCEMENT' | null
  sourceAttempt: RecallAttempt | null
}

export interface RecallAttempt {
  attemptId: string
  lessonId: string
  submittedCode: string
  feedback: AdvisoryFeedback
  submittedAt: string
}

export interface RecallExercise {
  lessonId: string
  title: string
  summary: string
  learningObjective: string
  estimatedMinutes: number
  qaContext: string
  prompt: string
  requirements: string[]
  testCases: string[]
  starterCode: string
}

export interface RecallResponse {
  learnerId: string
  exercise: RecallExercise
  competencies: CompetencyRecall[]
  assessmentPolicy: string
}

export interface RecallStatusResponse {
  competencyTag: string
  status: RecallStatus
  baseReadyAttemptId: string | null
  recallReadyAttemptId: string | null
  learnerConfirmed: boolean
}

export interface ProgressCompetency {
  competencyTag: string
  status: RecallStatus
  updatedAt: string | null
}

export interface LatestFeedback {
  attemptId: string
  lessonId: string
  submittedAt: string
  summary: string
  nextAction: string
  competencyTags: string[]
  reviewRecommendation: AdvisoryFeedback['reviewRecommendation']
  feedbackSource: AdvisoryFeedback['feedbackSource']
}

export interface NextActivity {
  type: 'LESSON' | 'RECALL'
  lessonId: string
  title: string
  estimatedMinutes: number
  reason: string
  competencyTag: string | null
}

export interface ProgressResponse {
  learnerId: string
  lessonsAttempted: number
  exercisesSubmitted: number
  competenciesNeedingReview: ProgressCompetency[]
  competenciesInProgress: ProgressCompetency[]
  competenciesDemonstrated: ProgressCompetency[]
  latestFeedback: LatestFeedback | null
  nextActivity: NextActivity
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

export function getRecommendedLesson(
  learnerId: string,
): Promise<RecommendedLessonResponse> {
  return request<RecommendedLessonResponse>(
    `/api/v1/learners/${encodeURIComponent(learnerId)}/recommended-lesson`,
  )
}

export function getLesson(lessonId: string): Promise<LessonContentResponse> {
  return request<LessonContentResponse>(
    `/api/v1/lessons/${encodeURIComponent(lessonId)}`,
  )
}

export function submitLessonCode(
  learnerId: string,
  lessonId: string,
  code: string,
): Promise<SubmissionResponse> {
  return request<SubmissionResponse>(
    `/api/v1/learners/${encodeURIComponent(learnerId)}/lessons/${encodeURIComponent(lessonId)}/submissions`,
    {
      method: 'POST',
      body: JSON.stringify({ code }),
    },
  )
}

export function getRecall(learnerId: string): Promise<RecallResponse> {
  return request<RecallResponse>(
    `/api/v1/learners/${encodeURIComponent(learnerId)}/recall`,
  )
}

export function getProgress(learnerId: string): Promise<ProgressResponse> {
  return request<ProgressResponse>(
    `/api/v1/learners/${encodeURIComponent(learnerId)}/progress`,
  )
}

export function submitRecallCode(
  learnerId: string,
  competencyTag: string,
  code: string,
): Promise<SubmissionResponse> {
  return request<SubmissionResponse>(
    `/api/v1/learners/${encodeURIComponent(learnerId)}/recall/submissions`,
    {
      method: 'POST',
      body: JSON.stringify({ competencyTag, code }),
    },
  )
}

export function confirmDemonstratedCompetency(
  learnerId: string,
  competencyTag: string,
): Promise<RecallStatusResponse> {
  return request<RecallStatusResponse>(
    `/api/v1/learners/${encodeURIComponent(learnerId)}/recall/${encodeURIComponent(competencyTag)}/demonstration`,
    {
      method: 'POST',
      body: JSON.stringify({ confirmed: true }),
    },
  )
}
