import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  createLearner,
  getLearner,
  LearnerApiError,
  type CreateLearnerRequest,
} from './learnerApi'

const requestBody: CreateLearnerRequest = {
  currentRole: 'QA Engineer',
  yearsOfExperience: 5,
  specialization: 'API Testing',
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
      yearsOfExperience: 5,
      specialization: 'API Testing',
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
