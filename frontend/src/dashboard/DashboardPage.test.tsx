import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type {
  LearnerProfileResponse,
  ProgressResponse,
  RecallResponse,
  RecommendedLessonResponse,
} from '../api/learnerApi'
import DashboardPage from './DashboardPage'

const { getLearnerMock, getRecommendedLessonMock, getRecallMock, getProgressMock } = vi.hoisted(() => ({
  getLearnerMock: vi.fn(),
  getRecommendedLessonMock: vi.fn(),
  getRecallMock: vi.fn(),
  getProgressMock: vi.fn(),
}))

vi.mock('../api/learnerApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/learnerApi')>()
  return {
    ...original,
    getLearner: getLearnerMock,
    getRecommendedLesson: getRecommendedLessonMock,
    getRecall: getRecallMock,
    getProgress: getProgressMock,
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
  existingSkills: ['Postman'],
  studyDays: ['MONDAY', 'WEDNESDAY'],
  dailyMinutes: 45,
  learningStyle: 'PRACTICAL',
}

const recommendation: RecommendedLessonResponse = {
  lessonId: 'java-methods-input-validation',
  title: 'Java Methods and Input Validation',
  summary: 'Learn how Java methods organize business rules.',
  learningObjective: 'Validate withdrawal requests before changing a balance.',
  estimatedMinutes: 20,
  recommendationReason: 'Recommended as a focused first lesson.',
  qaScenario: 'Test a banking withdrawal endpoint.',
}

function renderDashboard() {
  return render(
    <MemoryRouter initialEntries={[`/dashboard/${profile.id}`]}>
      <Routes>
        <Route path="/dashboard/:learnerId" element={<DashboardPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

const recall: RecallResponse = {
  learnerId: profile.id,
  exercise: {
    lessonId: 'bank-transfer-validation-recall',
    title: 'Recall: Validate a Bank Transfer',
    summary: 'Apply input validation to transfers.',
    learningObjective: 'Validate a transfer.',
    estimatedMinutes: 15,
    qaContext: 'Check both account boundaries.',
    prompt: 'Implement the transfer rules.',
    requirements: ['Use BigDecimal.'],
    testCases: ['Exact-balance transfer → true'],
    starterCode: 'class TransferValidator {}',
  },
  competencies: [
    {
      competencyTag: 'INPUT_VALIDATION',
      status: 'NEEDS_REVIEW',
      gapAttemptId: 'gap-attempt',
      baseReadyAttemptId: null,
      recallReadyAttemptId: null,
      learnerConfirmed: false,
      updatedAt: '2026-10-12T15:00:00Z',
      reason: 'COMPETENCY_GAP',
      sourceAttempt: null,
    },
    {
      competencyTag: 'JAVA_METHODS',
      status: 'NOT_STARTED',
      gapAttemptId: null,
      baseReadyAttemptId: null,
      recallReadyAttemptId: null,
      learnerConfirmed: false,
      updatedAt: null,
      reason: null,
      sourceAttempt: null,
    },
  ],
  assessmentPolicy: 'Both exercises need positive reviews and learner confirmation.',
}

const progress: ProgressResponse = {
  learnerId: profile.id,
  lessonsAttempted: 2,
  exercisesSubmitted: 3,
  competenciesNeedingReview: [{
    competencyTag: 'NULL_HANDLING',
    status: 'NEEDS_REVIEW',
    updatedAt: '2026-10-12T15:00:00Z',
  }],
  competenciesInProgress: [],
  competenciesDemonstrated: [{
    competencyTag: 'INPUT_VALIDATION',
    status: 'DEMONSTRATED',
    updatedAt: '2026-10-12T15:30:00Z',
  }],
  latestFeedback: {
    attemptId: 'latest-attempt',
    lessonId: 'bank-transfer-validation-recall',
    submittedAt: '2026-10-12T15:30:00Z',
    summary: 'Transfer validation was reviewed.',
    nextAction: 'Check the null cases.',
    competencyTags: ['INPUT_VALIDATION'],
    reviewRecommendation: 'READY_FOR_REVIEW',
    feedbackSource: 'LIVE_PROVIDER',
  },
  nextActivity: {
    type: 'RECALL',
    lessonId: 'bank-transfer-validation-recall',
    title: 'Recall: Validate a Bank Transfer',
    estimatedMinutes: 15,
    reason: 'Practise the transfer exercise for NULL_HANDLING.',
    competencyTag: 'NULL_HANDLING',
  },
}

beforeEach(() => {
  getLearnerMock.mockReset()
  getRecommendedLessonMock.mockReset()
  getRecallMock.mockReset()
  getProgressMock.mockReset()
})

describe('learning dashboard', () => {
  it('shows the persisted learner context, curated recommendation, and honest phase status', async () => {
    getLearnerMock.mockResolvedValue(profile)
    getRecommendedLessonMock.mockResolvedValue(recommendation)
    getRecallMock.mockResolvedValue(recall)
    getProgressMock.mockResolvedValue(progress)
    renderDashboard()

    expect(await screen.findByRole('heading', { name: recommendation.title })).toBeInTheDocument()
    expect(screen.getByText('QA Engineer')).toBeInTheDocument()
    expect(screen.getByText('Java Backend Developer')).toBeInTheDocument()
    expect(screen.getByText('4–5 years')).toBeInTheDocument()
    expect(screen.getByText('API Testing')).toBeInTheDocument()
    expect(screen.getByText('Monday, Wednesday')).toBeInTheDocument()
    expect(screen.getByText('Practical')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Skip to content' }))
      .toHaveAttribute('href', '#dashboard-main')
    expect(screen.getByText(recommendation.recommendationReason)).toBeInTheDocument()
    expect(screen.getByText(recommendation.qaScenario)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Open lesson workspace/ }))
      .toHaveAttribute(
        'href',
        `/lessons/${recommendation.lessonId}?learnerId=${profile.id}`,
      )
    expect(screen.getByRole('heading', { name: 'Submissions are recorded' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Learning progress' })).toBeInTheDocument()
    expect(screen.getByText('Lessons attempted')).toBeInTheDocument()
    expect(screen.getByText('Exercises submitted')).toBeInTheDocument()
    expect(screen.getByText('Transfer validation was reviewed.')).toBeInTheDocument()
    expect(screen.getByText('Null Handling')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open next activity' }))
      .toHaveAttribute('href', `/recall/${profile.id}?competencyTag=NULL_HANDLING`)
    expect(screen.getByRole('heading', { name: '1 competency to practise' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open Learning Recall' }))
      .toHaveAttribute('href', `/recall/${profile.id}`)
    expect(getLearnerMock).toHaveBeenCalledWith(profile.id)
    expect(getRecommendedLessonMock).toHaveBeenCalledWith(profile.id)
    expect(getRecallMock).toHaveBeenCalledWith(profile.id)
    expect(getProgressMock).toHaveBeenCalledWith(profile.id)
  })

  it('labels persisted demo feedback as a sample rather than an assessment', async () => {
    getLearnerMock.mockResolvedValue(profile)
    getRecommendedLessonMock.mockResolvedValue(recommendation)
    getRecallMock.mockResolvedValue(recall)
    getProgressMock.mockResolvedValue({
      ...progress,
      latestFeedback: {
        ...progress.latestFeedback!,
        feedbackSource: 'DEMO_SAMPLE',
        reviewRecommendation: 'UNCERTAIN',
      },
    })
    renderDashboard()

    expect(await screen.findByText('Demo sample only · not an assessment')).toBeInTheDocument()
    expect(screen.getByText('Transfer validation was reviewed.')).toBeInTheDocument()
  })

  it('shows a load error and retries all dashboard requests', async () => {
    const user = userEvent.setup()
    getLearnerMock
      .mockRejectedValueOnce(new Error('Learner not found'))
      .mockResolvedValueOnce(profile)
    getRecommendedLessonMock.mockResolvedValue(recommendation)
    getRecallMock.mockResolvedValue(recall)
    getProgressMock.mockResolvedValue(progress)
    renderDashboard()

    expect(await screen.findByRole('alert')).toHaveTextContent('Learner not found')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { name: recommendation.title })).toBeInTheDocument()
    expect(getLearnerMock).toHaveBeenCalledTimes(2)
    expect(getRecommendedLessonMock).toHaveBeenCalledTimes(2)
    expect(getRecallMock).toHaveBeenCalledTimes(2)
    expect(getProgressMock).toHaveBeenCalledTimes(2)
  })
})
