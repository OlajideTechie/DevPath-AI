import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type {
  LearnerProfileResponse,
  LessonContentResponse,
  ProgressResponse,
  RecommendedLessonResponse,
} from './api/learnerApi'
import App from './App'

const {
  getLearnerMock,
  getRecommendedLessonMock,
  getRecallMock,
  getProgressMock,
  getLessonMock,
} = vi.hoisted(() => ({
  getLearnerMock: vi.fn(),
  getRecommendedLessonMock: vi.fn(),
  getRecallMock: vi.fn(),
  getProgressMock: vi.fn(),
  getLessonMock: vi.fn(),
}))

vi.mock('./api/learnerApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('./api/learnerApi')>()
  return {
    ...original,
    getLearner: getLearnerMock,
    getRecommendedLesson: getRecommendedLessonMock,
    getRecall: getRecallMock,
    getProgress: getProgressMock,
    getLesson: getLessonMock,
  }
})

const profile: LearnerProfileResponse = {
  id: 'd69d5da5-5652-41aa-b573-cc5b42f5a605',
  currentRole: 'QA Engineer',
  targetRole: 'Java Backend Developer',
  preferredLanguage: 'JAVA',
  programmingLevel: 'BEGINNER',
  profileStatus: 'CREATED',
  createdAt: '2026-10-10T15:00:00Z',
  experienceBand: 'FOUR_TO_FIVE',
  specializations: ['API Testing'],
  existingSkills: [],
  studyDays: ['MONDAY'],
  dailyMinutes: 30,
  learningStyle: 'PRACTICAL',
}

const recommendation: RecommendedLessonResponse = {
  lessonId: 'java-methods-input-validation',
  title: 'Java Methods and Input Validation',
  summary: 'Learn Java input validation.',
  learningObjective: 'Validate withdrawal requests.',
  estimatedMinutes: 20,
  recommendationReason: 'A focused first lesson.',
  qaScenario: 'Test a banking API.',
}

const lesson: LessonContentResponse = {
  ...recommendation,
  qaContext: 'Test banking API withdrawals.',
  sections: [{ heading: 'Methods', content: 'Keep rules focused.', codeExample: 'return true;' }],
  exercise: {
    title: 'Validate a banking withdrawal',
    prompt: 'Implement validation.',
    requirements: ['Use BigDecimal.'],
    testCases: ['Valid withdrawal → true'],
    starterCode: 'class WithdrawalValidator {}',
  },
}

const recall = {
  learnerId: profile.id,
  exercise: {
    lessonId: 'bank-transfer-validation-recall',
    title: 'Recall: Validate a Bank Transfer',
    summary: 'Apply validation in a transfer scenario.',
    learningObjective: 'Validate a transfer.',
    estimatedMinutes: 15,
    qaContext: 'Test transfers.',
    prompt: 'Implement the transfer checks.',
    requirements: ['Use BigDecimal.'],
    testCases: ['Exact balance → true'],
    starterCode: 'class TransferValidator {}',
  },
  competencies: [],
  assessmentPolicy: 'Require positive reviews on both exercises and learner confirmation.',
}

const progress: ProgressResponse = {
  learnerId: profile.id,
  lessonsAttempted: 0,
  exercisesSubmitted: 0,
  competenciesNeedingReview: [],
  competenciesInProgress: [],
  competenciesDemonstrated: [],
  latestFeedback: null,
  nextActivity: {
    type: 'LESSON',
    lessonId: recommendation.lessonId,
    title: recommendation.title,
    estimatedMinutes: recommendation.estimatedMinutes,
    reason: 'Start with the recommended lesson.',
    competencyTag: null,
  },
}

beforeEach(() => {
  getLearnerMock.mockReset().mockResolvedValue(profile)
  getRecommendedLessonMock.mockReset().mockResolvedValue(recommendation)
  getRecallMock.mockReset().mockResolvedValue(recall)
  getProgressMock.mockReset().mockResolvedValue(progress)
  getLessonMock.mockReset().mockResolvedValue(lesson)
  window.history.pushState({}, '', `/dashboard/${profile.id}`)
})

afterEach(() => {
  window.history.pushState({}, '', '/')
})

describe('application navigation', () => {
  it('opens the lesson workspace from the dashboard recommendation', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('link', { name: /Open lesson workspace/ }))

    expect(await screen.findByRole('heading', { name: lesson.title })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: lesson.exercise.title })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Java focus for your programming level' }))
      .toBeInTheDocument()
    expect(screen.getByText(recommendation.qaScenario)).toBeInTheDocument()
    expect(screen.getByText(recommendation.recommendationReason)).toBeInTheDocument()
    expect(getLessonMock).toHaveBeenCalledWith(lesson.lessonId)
  })
})
