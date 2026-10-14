import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { RecallResponse } from '../api/learnerApi'
import RecallPage from './RecallPage'

const { getRecallMock, submitRecallCodeMock, confirmDemonstratedCompetencyMock } = vi.hoisted(() => ({
  getRecallMock: vi.fn(),
  submitRecallCodeMock: vi.fn(),
  confirmDemonstratedCompetencyMock: vi.fn(),
}))

vi.mock('../api/learnerApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/learnerApi')>()
  return {
    ...original,
    getRecall: getRecallMock,
    submitRecallCode: submitRecallCodeMock,
    confirmDemonstratedCompetency: confirmDemonstratedCompetencyMock,
  }
})

const baseRecall: RecallResponse = {
  learnerId: 'demo-learner',
  exercise: {
    lessonId: 'bank-transfer-validation-recall',
    title: 'Recall: Validate a Bank Transfer',
    summary: 'Apply validation in a related transfer context.',
    learningObjective: 'Validate a transfer before changing balances.',
    estimatedMinutes: 15,
    qaContext: 'Test both source and destination account boundaries.',
    prompt: 'Complete canTransfer using the requirements.',
    requirements: ['Use BigDecimal.', 'Do not mutate balances.'],
    testCases: ['Exact balance → true', 'Above balance → false'],
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
      sourceAttempt: {
        attemptId: 'gap-attempt',
        lessonId: 'java-methods-input-validation',
        submittedCode: 'class WithdrawalDraft {}',
        feedback: {
          summary: 'The original withdrawal needs another null check.',
          strengths: ['Uses a focused validation method.'],
          issues: ['The available balance can be null.'],
          improvements: ['Check null inputs before comparing.'],
          findings: [{
            category: 'FUNCTIONAL_DEFECT',
            competency: 'INPUT_VALIDATION',
            description: 'Return false for null arguments.',
          }],
          assessmentConfidence: 'HIGH',
          competencyTags: ['INPUT_VALIDATION'],
          nextActionType: 'TARGETED_CORRECTION',
          nextAction: 'Add a null check.',
          reviewRecommendation: 'NEEDS_REVIEW',
          feedbackSource: 'LIVE_PROVIDER',
        },
        submittedAt: '2026-10-12T14:00:00Z',
      },
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
  assessmentPolicy: 'Positive advisory feedback on both exercises plus your confirmation is required.',
}

function renderRecall(initialEntry = '/recall/demo-learner') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/recall/:learnerId" element={<RecallPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  getRecallMock.mockReset()
  submitRecallCodeMock.mockReset()
  confirmDemonstratedCompetencyMock.mockReset()
})

describe('Learning Recall', () => {
  it('selects the competency requested by a progress next-activity link', async () => {
    const withMultipleGaps: RecallResponse = {
      ...baseRecall,
      competencies: [
        ...baseRecall.competencies,
        {
          competencyTag: 'NULL_HANDLING',
          status: 'NEEDS_REVIEW',
          gapAttemptId: 'null-gap-attempt',
          baseReadyAttemptId: null,
          recallReadyAttemptId: null,
          learnerConfirmed: false,
          updatedAt: '2026-10-12T15:00:00Z',
          reason: 'COMPETENCY_GAP',
          sourceAttempt: baseRecall.competencies[0].sourceAttempt,
        },
      ],
    }
    getRecallMock.mockResolvedValue(withMultipleGaps)
    renderRecall('/recall/demo-learner?competencyTag=NULL_HANDLING')

    expect(await screen.findByRole('button', { name: /Null Handling/ }))
      .toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: /Input Validation/ }))
      .toHaveAttribute('aria-pressed', 'false')
  })

  it('loads the transfer exercise and submits code for an active competency gap', async () => {
    const user = userEvent.setup()
    getRecallMock
      .mockResolvedValueOnce(baseRecall)
      .mockResolvedValueOnce({
        ...baseRecall,
        competencies: baseRecall.competencies.map((competency) => (
          competency.competencyTag === 'INPUT_VALIDATION'
            ? { ...competency, status: 'PRACTICING' as const, recallReadyAttemptId: 'recall-attempt' }
            : competency
        )),
      })
    submitRecallCodeMock.mockResolvedValue({
      attemptId: 'recall-attempt',
      learnerId: 'demo-learner',
      lessonId: baseRecall.exercise.lessonId,
      feedback: {
        summary: 'The transfer is checked.',
        strengths: ['Uses BigDecimal.'],
        issues: ['Check null balances.'],
        improvements: ['Add guards.'],
        findings: [],
        assessmentConfidence: 'HIGH',
        competencyTags: ['INPUT_VALIDATION'],
        nextActionType: 'NEXT_LESSON',
        nextAction: 'Check all arguments.',
        reviewRecommendation: 'READY_FOR_REVIEW',
        feedbackSource: 'LIVE_PROVIDER',
      },
      advisoryOnly: true,
      codeExecutionPerformed: false,
      submittedAt: '2026-10-12T15:00:00Z',
    })
    renderRecall()

    expect(await screen.findByRole('heading', { name: baseRecall.exercise.title })).toBeInTheDocument()
    expect(screen.getByText(baseRecall.exercise.qaContext)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Input Validation/ })).toHaveAttribute('aria-pressed', 'true')
    await user.click(screen.getByText('Review your original withdrawal submission'))
    expect(screen.getByText('class WithdrawalDraft {}')).toBeInTheDocument()
    expect(screen.getByText('The original withdrawal needs another null check.')).toBeInTheDocument()
    const editor = screen.getByRole('textbox', { name: 'Your Java transfer draft' })
    expect(editor).toHaveValue(baseRecall.exercise.starterCode)
    fireEvent.change(editor, { target: { value: 'class ImprovedTransferValidator {}' } })
    await user.click(screen.getByRole('button', { name: 'Submit recall exercise' }))

    expect(await screen.findByRole('heading', { name: 'Review of your transfer exercise' }))
      .toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(/positive advisory feedback, not a verified result/i)
    expect(submitRecallCodeMock).toHaveBeenCalledWith(
      'demo-learner',
      'INPUT_VALIDATION',
      'class ImprovedTransferValidator {}',
    )
    expect(editor).toHaveValue('class ImprovedTransferValidator {}')
    expect(screen.getByText(/Attempt recall-a was saved/)).toBeInTheDocument()
  })

  it('requires learner confirmation after positive feedback on both exercises', async () => {
    const user = userEvent.setup()
    const withBothEvidence: RecallResponse = {
      ...baseRecall,
      competencies: baseRecall.competencies.map((competency) => (
        competency.competencyTag === 'INPUT_VALIDATION'
          ? {
              ...competency,
              status: 'PRACTICING',
              baseReadyAttemptId: 'base-ready',
              recallReadyAttemptId: 'recall-ready',
            }
          : competency
      )),
    }
    getRecallMock
      .mockResolvedValueOnce(withBothEvidence)
      .mockResolvedValueOnce({
        ...baseRecall,
        competencies: baseRecall.competencies.map((competency) => (
          competency.competencyTag === 'INPUT_VALIDATION'
            ? { ...competency, status: 'DEMONSTRATED' as const, learnerConfirmed: true }
            : competency
        )),
      })
    confirmDemonstratedCompetencyMock.mockResolvedValue({
      competencyTag: 'INPUT_VALIDATION',
      status: 'DEMONSTRATED',
      baseReadyAttemptId: 'base-ready',
      recallReadyAttemptId: 'recall-ready',
      learnerConfirmed: true,
    })
    renderRecall()

    const confirmButton = await screen.findByRole('button', {
      name: 'Confirm demonstrated competency',
    })
    expect(confirmButton).toBeDisabled()
    await user.click(screen.getByRole('checkbox', {
      name: 'I can explain and apply this rule in both exercise contexts.',
    }))
    await user.click(confirmButton)

    expect(confirmDemonstratedCompetencyMock).toHaveBeenCalledWith(
      'demo-learner',
      'INPUT_VALIDATION',
    )
    expect(await screen.findByText('Demonstrated')).toBeInTheDocument()
  })
})
