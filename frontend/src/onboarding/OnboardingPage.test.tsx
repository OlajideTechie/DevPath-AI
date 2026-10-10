import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { LearnerCreatedResponse, LearnerProfileResponse } from '../api/learnerApi'
import OnboardingPage from './OnboardingPage'

const { createLearnerMock, getLearnerMock } = vi.hoisted(() => ({
  createLearnerMock: vi.fn(),
  getLearnerMock: vi.fn(),
}))

vi.mock('../api/learnerApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/learnerApi')>()
  return {
    ...original,
    createLearner: createLearnerMock,
    getLearner: getLearnerMock,
  }
})

const createdProfile: LearnerCreatedResponse = {
  id: 'd69d5da5-5652-41aa-b573-cc5b42f5a605',
  currentRole: 'QA Engineer',
  targetRole: 'Java Backend Developer',
  preferredLanguage: 'JAVA',
  programmingLevel: 'BEGINNER',
  profileStatus: 'CREATED',
  createdAt: '2026-10-10T15:00:00Z',
}

const savedProfile: LearnerProfileResponse = {
  ...createdProfile,
  yearsOfExperience: 5,
  specialization: 'API Testing',
  existingSkills: ['Postman', 'SQL'],
  studyDays: ['MONDAY', 'WEDNESDAY'],
  dailyMinutes: 45,
  learningStyle: 'PRACTICAL',
}

function renderWizard() {
  return render(
    <MemoryRouter initialEntries={['/onboarding']}>
      <OnboardingPage />
    </MemoryRouter>,
  )
}

async function completeBackgroundStep(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Current role'), 'QA Engineer')
  await user.type(screen.getByLabelText('Years of experience'), '5')
  await user.type(screen.getByLabelText('QA specialization'), 'API Testing')
  await user.type(screen.getByLabelText(/Skills you already use/), 'Postman')
  await user.click(screen.getByRole('button', { name: 'Add skill' }))
  await user.type(screen.getByLabelText(/Skills you already use/), 'SQL')
  await user.click(screen.getByRole('button', { name: 'Add skill' }))
  await user.click(screen.getByRole('button', { name: 'Continue' }))
}

async function completeGoalStep(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('radio', { name: /Beginner/ }))
  await user.click(screen.getByRole('button', { name: 'Continue' }))
}

async function completeAvailabilityStep(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('checkbox', { name: /Monday/ }))
  await user.click(screen.getByRole('checkbox', { name: /Wednesday/ }))
  await user.clear(screen.getByLabelText('Minutes you can study per day'))
  await user.type(screen.getByLabelText('Minutes you can study per day'), '45')
  await user.click(screen.getByRole('radio', { name: /Hands-on/ }))
  await user.click(screen.getByRole('button', { name: 'Continue' }))
}

beforeEach(() => {
  createLearnerMock.mockReset()
  getLearnerMock.mockReset()
})

describe('onboarding wizard', () => {
  it('validates required background fields inline before continuing', async () => {
    const user = userEvent.setup()
    renderWizard()

    await user.click(screen.getByRole('button', { name: 'Continue' }))

    expect(screen.getByText('Enter your current role.')).toBeInTheDocument()
    expect(screen.getByText('Enter your years of experience.')).toBeInTheDocument()
    expect(screen.getByText('Enter your QA specialization.')).toBeInTheDocument()
    expect(createLearnerMock).not.toHaveBeenCalled()
  })

  it('reviews, posts the profile, and retrieves the saved profile through GET', async () => {
    const user = userEvent.setup()
    createLearnerMock.mockResolvedValue(createdProfile)
    getLearnerMock.mockResolvedValue(savedProfile)
    renderWizard()

    await completeBackgroundStep(user)
    await completeGoalStep(user)
    await completeAvailabilityStep(user)

    expect(screen.getByRole('heading', { name: 'Review' })).toBeInTheDocument()
    expect(screen.getByText('Postman, SQL')).toBeInTheDocument()
    expect(screen.getByText('Java Backend Developer')).toBeInTheDocument()
    expect(screen.getByText('Monday, Wednesday')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Save profile' }))

    expect(await screen.findByRole('heading', { name: 'Your profile is saved.' })).toBeInTheDocument()
    expect(createLearnerMock).toHaveBeenCalledWith({
      currentRole: 'QA Engineer',
      yearsOfExperience: 5,
      specialization: 'API Testing',
      existingSkills: ['Postman', 'SQL'],
      programmingLevel: 'BEGINNER',
      studyDays: ['MONDAY', 'WEDNESDAY'],
      dailyMinutes: 45,
      learningStyle: 'PRACTICAL',
    })
    expect(getLearnerMock).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'View saved profile' }))

    expect(await screen.findByRole('heading', { name: 'Retrieved profile' })).toBeInTheDocument()
    expect(getLearnerMock).toHaveBeenCalledWith(createdProfile.id)
    expect(screen.getByText('API Testing')).toBeInTheDocument()
  })

  it('allows returning to prior steps to edit review values', async () => {
    const user = userEvent.setup()
    renderWizard()

    await completeBackgroundStep(user)
    await completeGoalStep(user)
    await completeAvailabilityStep(user)
    await user.click(screen.getAllByRole('button', { name: 'Edit' })[0])

    expect(screen.getByLabelText('Current role')).toHaveValue('QA Engineer')
    expect(screen.getByLabelText(/Skills you already use/)).toHaveValue('')
  })

  it('shows API failures and permits retry without losing answers', async () => {
    const user = userEvent.setup()
    createLearnerMock
      .mockRejectedValueOnce(new Error('Service unavailable. Try again.'))
      .mockResolvedValueOnce(createdProfile)
    renderWizard()
    await completeBackgroundStep(user)
    await completeGoalStep(user)
    await completeAvailabilityStep(user)

    await user.click(screen.getByRole('button', { name: 'Save profile' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Service unavailable. Try again.')
    await user.click(screen.getByRole('button', { name: 'Save profile' }))

    expect(await screen.findByRole('heading', { name: 'Your profile is saved.' })).toBeInTheDocument()
    expect(createLearnerMock).toHaveBeenCalledTimes(2)
  })

  it('prevents duplicate POST submissions while a request is in flight', async () => {
    const user = userEvent.setup()
    let resolveCreate: ((value: LearnerCreatedResponse) => void) | undefined
    createLearnerMock.mockImplementation(
      () => new Promise((resolve) => { resolveCreate = resolve }),
    )
    renderWizard()
    await completeBackgroundStep(user)
    await completeGoalStep(user)
    await completeAvailabilityStep(user)

    const submitButton = screen.getByRole('button', { name: 'Save profile' })
    fireEvent.click(submitButton)
    fireEvent.click(submitButton)

    expect(createLearnerMock).toHaveBeenCalledTimes(1)
    expect(await screen.findByRole('button', { name: 'Saving profile…' })).toBeDisabled()
    resolveCreate?.(createdProfile)
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Your profile is saved.' })).toBeInTheDocument()
    })
  })

  it('allows retrying a failed GET instead of presenting a false saved-profile result', async () => {
    const user = userEvent.setup()
    createLearnerMock.mockResolvedValue(createdProfile)
    getLearnerMock
      .mockRejectedValueOnce(new Error('Could not connect to the server.'))
      .mockResolvedValueOnce(savedProfile)
    renderWizard()
    await completeBackgroundStep(user)
    await completeGoalStep(user)
    await completeAvailabilityStep(user)
    await user.click(screen.getByRole('button', { name: 'Save profile' }))
    await screen.findByRole('heading', { name: 'Your profile is saved.' })

    await user.click(screen.getByRole('button', { name: 'View saved profile' }))
    expect(await screen.findByText('Could not connect to the server.')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Retrieved profile' })).not.toBeInTheDocument()

    const retry = screen.getByRole('button', { name: 'Try again' })
    await user.click(retry)

    expect(await screen.findByRole('heading', { name: 'Retrieved profile' })).toBeInTheDocument()
    expect(getLearnerMock).toHaveBeenCalledTimes(2)
  })
})
