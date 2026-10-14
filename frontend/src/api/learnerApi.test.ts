import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  createLearner,
  confirmDemonstratedCompetency,
  getLesson,
  getLearner,
  getProgress,
  getRecall,
  getRecommendedLesson,
  LearnerApiError,
  submitRecallCode,
  submitLessonCode,
  type CreateLearnerRequest,
} from './learnerApi'

const requestBody: CreateLearnerRequest = {
  experienceBand: 'FOUR_TO_FIVE',
  specializations: ['API Testing', 'Database Testing'],
  existingSkills: ['Postman', 'SQL'],
  programmingLevel: 'BEGINNER',
  studyDays: ['MONDAY', 'WEDNESDAY'],
  dailyMinutes: 45,
  learningStyle: 'PRACTICAL',
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('learner API client', () => {
  it('posts the typed learner request and returns the creation response', async () => {
    const created = {
      id: 'b92a23d0-89ab-45f4-b89a-f95f10aa0190',
      currentRole: 'QA Engineer',
      targetRole: 'Java Backend Developer',
      preferredLanguage: 'JAVA',
      programmingLevel: 'BEGINNER',
      profileStatus: 'CREATED',
      createdAt: '2026-10-10T15:00:00Z',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(created), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(createLearner(requestBody)).resolves.toEqual(created)

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toMatch(/\/api\/v1\/learners$/)
    expect(init.method).toBe('POST')
    expect(init.headers).toMatchObject({ 'Content-Type': 'application/json' })
    expect(JSON.parse(init.body as string)).toEqual(requestBody)
  })

  it('retrieves a saved learner by encoded UUID', async () => {
    const savedProfile = {
      id: 'learner/id',
      currentRole: 'QA Engineer',
      experienceBand: 'FOUR_TO_FIVE',
      specializations: ['API Testing', 'Database Testing'],
      existingSkills: ['SQL'],
      targetRole: 'Java Backend Developer',
      preferredLanguage: 'JAVA',
      programmingLevel: 'BEGINNER',
      studyDays: ['MONDAY'],
      dailyMinutes: 45,
      learningStyle: 'PRACTICAL',
      profileStatus: 'CREATED',
      createdAt: '2026-10-10T15:00:00Z',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(savedProfile), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getLearner(savedProfile.id)).resolves.toEqual(savedProfile)
    expect(fetchMock.mock.calls[0]?.[0]).toMatch(/\/api\/v1\/learners\/learner%2Fid$/)
  })

  it('retrieves the deterministic lesson recommendation for a learner', async () => {
    const recommendation = {
      lessonId: 'java-methods-input-validation',
      title: 'Java Methods and Input Validation',
      summary: 'Learn how Java methods organize business rules.',
      learningObjective: 'Validate withdrawal requests before changing a balance.',
      estimatedMinutes: 20,
      recommendationReason: 'This is a suitable first lesson.',
      qaScenario: 'Test a banking withdrawal endpoint.',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(recommendation), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getRecommendedLesson('learner/id')).resolves.toEqual(recommendation)
    expect(fetchMock.mock.calls[0]?.[0]).toMatch(
      /\/api\/v1\/learners\/learner%2Fid\/recommended-lesson$/,
    )
  })

  it('retrieves curated lesson content by encoded lesson ID', async () => {
    const lesson = {
      lessonId: 'java-methods-input-validation',
      title: 'Java Methods and Input Validation',
      summary: 'Learn focused Java validation.',
      learningObjective: 'Reject invalid withdrawal requests.',
      estimatedMinutes: 20,
      qaContext: 'Validate banking API withdrawals.',
      sections: [{ heading: 'Method boundary', content: 'Keep one rule together.', codeExample: 'return true;' }],
      exercise: {
        title: 'Validate a banking withdrawal',
        prompt: 'Implement canWithdraw.',
        requirements: ['Use BigDecimal.'],
        testCases: ['Amount above balance → false'],
        starterCode: 'class WithdrawalValidator {}',
      },
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(lesson), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getLesson(lesson.lessonId)).resolves.toEqual(lesson)
    expect(fetchMock.mock.calls[0]?.[0]).toMatch(
      /\/api\/v1\/lessons\/java-methods-input-validation$/,
    )
  })

  it('posts only the code to the learner-scoped lesson submission endpoint', async () => {
    const submission = {
      attemptId: 'd693de29-79d3-4c7f-a31d-8e8fd9ddfca3',
      learnerId: 'learner/id',
      lessonId: 'java-methods-input-validation',
      feedback: {
        summary: 'Review summary.',
        strengths: ['Focused method.'],
        issues: ['Missing null guard.'],
        improvements: ['Validate inputs.'],
        findings: [{
          category: 'FUNCTIONAL_DEFECT',
          competency: 'INPUT_VALIDATION',
          description: 'Return false for null inputs.',
        }],
        assessmentConfidence: 'HIGH',
        competencyTags: ['INPUT_VALIDATION'],
        nextActionType: 'TARGETED_CORRECTION',
        nextAction: 'Add null checks.',
        reviewRecommendation: 'NEEDS_REVIEW',
        feedbackSource: 'LIVE_PROVIDER',
      },
      advisoryOnly: true,
      codeExecutionPerformed: false,
      submittedAt: '2026-10-12T15:00:00Z',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(submission), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(submitLessonCode(
      'learner/id',
      'java-methods-input-validation',
      'class Draft {}',
    )).resolves.toEqual(submission)

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toMatch(/\/api\/v1\/learners\/learner%2Fid\/lessons\/java-methods-input-validation\/submissions$/)
    expect(init.method).toBe('POST')
    expect(JSON.parse(init.body as string)).toEqual({ code: 'class Draft {}' })
  })

  it('retrieves persisted competency recall and its curated transfer exercise', async () => {
    const recall = {
      learnerId: 'learner/id',
      exercise: {
        lessonId: 'bank-transfer-validation-recall',
        title: 'Recall: Validate a Bank Transfer',
        summary: 'Transfer validation practice.',
        learningObjective: 'Validate a transfer.',
        estimatedMinutes: 15,
        qaContext: 'Test a transfer API.',
        prompt: 'Complete canTransfer.',
        requirements: ['Use BigDecimal.'],
        testCases: ['Exact balance → true'],
        starterCode: 'class TransferValidator {}',
      },
      competencies: [{
        competencyTag: 'INPUT_VALIDATION',
        status: 'NEEDS_REVIEW',
        gapAttemptId: 'attempt-1',
        baseReadyAttemptId: null,
        recallReadyAttemptId: null,
        learnerConfirmed: false,
        updatedAt: '2026-10-12T15:00:00Z',
      }],
      assessmentPolicy: 'Two positive exercise reviews plus learner confirmation.',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(recall), { status: 200 }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getRecall('learner/id')).resolves.toEqual(recall)
    expect(fetchMock.mock.calls[0]?.[0]).toMatch(/\/api\/v1\/learners\/learner%2Fid\/recall$/)
  })

  it('retrieves persisted progress for an encoded learner ID', async () => {
    const progress = {
      learnerId: 'learner/id',
      lessonsAttempted: 2,
      exercisesSubmitted: 3,
      competenciesNeedingReview: [],
      competenciesInProgress: [],
      competenciesDemonstrated: [],
      latestFeedback: null,
      nextActivity: {
        type: 'LESSON',
        lessonId: 'java-methods-input-validation',
        title: 'Java Methods and Input Validation',
        estimatedMinutes: 20,
        reason: 'Continue lesson practice.',
        competencyTag: null,
      },
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(progress), { status: 200 }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getProgress('learner/id')).resolves.toEqual(progress)
    expect(fetchMock.mock.calls[0]?.[0]).toMatch(
      /\/api\/v1\/learners\/learner%2Fid\/progress$/,
    )
  })

  it('submits a recall attempt and confirms demonstrated status using dedicated endpoints', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ attemptId: 'attempt-2' }), { status: 201 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        competencyTag: 'INPUT_VALIDATION',
        status: 'DEMONSTRATED',
        baseReadyAttemptId: 'attempt-1',
        recallReadyAttemptId: 'attempt-2',
        learnerConfirmed: true,
      }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await submitRecallCode('learner/id', 'INPUT_VALIDATION', 'class TransferValidator {}')
    await confirmDemonstratedCompetency('learner/id', 'INPUT_VALIDATION')

    const [submissionUrl, submissionInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    const [confirmationUrl, confirmationInit] = fetchMock.mock.calls[1] as [string, RequestInit]
    expect(submissionUrl).toMatch(/\/learners\/learner%2Fid\/recall\/submissions$/)
    expect(JSON.parse(submissionInit.body as string))
      .toEqual({ competencyTag: 'INPUT_VALIDATION', code: 'class TransferValidator {}' })
    expect(confirmationUrl).toMatch(
      /\/learners\/learner%2Fid\/recall\/INPUT_VALIDATION\/demonstration$/,
    )
    expect(JSON.parse(confirmationInit.body as string))
      .toEqual({ confirmed: true })
  })

  it('surfaces API validation details for the wizard', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({
          code: 'VALIDATION_ERROR',
          message: 'Invalid learner profile',
          fieldErrors: { dailyMinutes: 'must be between 10 and 240' },
        }), { status: 400 }),
      ),
    )

    await expect(createLearner(requestBody)).rejects.toMatchObject({
      name: 'LearnerApiError',
      status: 400,
      code: 'VALIDATION_ERROR',
      fieldErrors: { dailyMinutes: 'must be between 10 and 240' },
      message: 'Invalid learner profile',
    })
  })

  it('reports a clear connection error when the API is unreachable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('network error')))

    await expect(createLearner(requestBody)).rejects.toEqual(
      expect.objectContaining<LearnerApiError>({
        name: 'LearnerApiError',
        status: 0,
        message: "Could not reach the API. Check that the backend is running and that this site's origin is allowed by FRONTEND_ORIGINS.",
        fieldErrors: {},
      }),
    )
  })
})
