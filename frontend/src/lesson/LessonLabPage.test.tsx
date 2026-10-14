import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  LearnerApiError,
  type LessonContentResponse,
  type SubmissionResponse,
} from '../api/learnerApi'
import LessonLabPage from './LessonLabPage'

const { getLessonMock, getRecommendedLessonMock, submitLessonCodeMock } = vi.hoisted(() => ({
  getLessonMock: vi.fn(),
  getRecommendedLessonMock: vi.fn(),
  submitLessonCodeMock: vi.fn(),
}))

vi.mock('../api/learnerApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/learnerApi')>()
  return {
    ...original,
    getLesson: getLessonMock,
    getRecommendedLesson: getRecommendedLessonMock,
    submitLessonCode: submitLessonCodeMock,
  }
})

const lesson: LessonContentResponse = {
  lessonId: 'java-methods-input-validation',
  title: 'Java Methods and Input Validation',
  summary: 'Learn focused Java validation.',
  learningObjective: 'Reject invalid withdrawal requests.',
  estimatedMinutes: 20,
  qaContext: 'Validate banking API withdrawals.',
  sections: [
    {
      heading: 'A method gives a rule a clear boundary',
      content: 'Keep related rules in a small method.',
      codeExample: 'boolean canWithdraw() { return true; }',
    },
    {
      heading: 'Validate before comparing',
      content: 'Reject missing and non-positive values.',
      codeExample: 'amount.compareTo(BigDecimal.ZERO) > 0',
    },
  ],
  exercise: {
    title: 'Validate a banking withdrawal',
    prompt: 'Implement the method.',
    requirements: ['Use BigDecimal.', 'Reject non-positive values.'],
    testCases: ['100.00, 25.00 → true', '100.00, 100.01 → false'],
    starterCode: 'public class WithdrawalValidator {}',
  },
}

const submission: SubmissionResponse = {
  attemptId: 'd693de29-79d3-4c7f-a31d-8e8fd9ddfca3',
  learnerId: 'demo-learner',
  lessonId: lesson.lessonId,
  feedback: {
    summary: 'Add defensive checks before comparing amounts.',
    strengths: ['Uses a clear method boundary.'],
    issues: ['Null values are not guarded.'],
    improvements: ['Check for null and non-positive requests.'],
    findings: [{
      category: 'FUNCTIONAL_DEFECT',
      competency: 'NULL_HANDLING',
      description: 'Return false when either argument is null.',
    }],
    assessmentConfidence: 'HIGH',
    competencyTags: ['INPUT_VALIDATION', 'NULL_HANDLING'],
    nextActionType: 'TARGETED_CORRECTION',
    nextAction: 'Add null and boundary guards.',
    reviewRecommendation: 'NEEDS_REVIEW',
    feedbackSource: 'LIVE_PROVIDER',
  },
  advisoryOnly: true,
  codeExecutionPerformed: false,
  submittedAt: '2026-10-12T15:00:00Z',
}

function renderLesson(path = `/lessons/${lesson.lessonId}?learnerId=demo-learner`) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/lessons/:lessonId" element={<LessonLabPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  getLessonMock.mockReset()
  getRecommendedLessonMock.mockReset()
  submitLessonCodeMock.mockReset()
})

describe('assisted learning lab', () => {
  it('loads curated lesson content and allows editing an explicitly unsaved draft', async () => {
    const user = userEvent.setup()
    getLessonMock.mockResolvedValue(lesson)
    getRecommendedLessonMock.mockResolvedValue({
      lessonId: lesson.lessonId,
      title: lesson.title,
      summary: lesson.summary,
      learningObjective: lesson.learningObjective,
      estimatedMinutes: lesson.estimatedMinutes,
      recommendationReason: 'Start with one focused Java method and validate its inputs.',
      qaScenario: 'Start with three straightforward examples.',
    })
    renderLesson()

    expect(await screen.findByRole('heading', { name: lesson.title })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Your QA perspective' })).toBeInTheDocument()
    expect(screen.getByText('Start with three straightforward examples.')).toBeInTheDocument()
    expect(screen.getByText('Start with one focused Java method and validate its inputs.'))
      .toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Java focus for your programming level' }))
      .toBeInTheDocument()
    expect(screen.getByRole('heading', { name: lesson.sections[0].heading })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: lesson.exercise.title })).toBeInTheDocument()
    expect(screen.getByText('100.00, 100.01 → false')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to dashboard' }))
      .toHaveAttribute('href', '/dashboard/demo-learner')

    const editor = screen.getByRole('textbox', { name: 'Your Java draft' })
    expect(editor).toHaveValue(lesson.exercise.starterCode)
    await user.clear(editor)
    fireEvent.change(editor, { target: { value: 'class MyValidator {}' } })
    expect(editor).toHaveValue('class MyValidator {}')
    expect(screen.getByText(/Maximum 20,000 characters/)).toBeInTheDocument()
    expect(screen.getByText(/not compiled or executed/)).toBeInTheDocument()
    expect(getLessonMock).toHaveBeenCalledWith(lesson.lessonId)
    expect(getRecommendedLessonMock).toHaveBeenCalledWith('demo-learner')
  })

  it('reports a missing lesson and supports retry', async () => {
    const user = userEvent.setup()
    getLessonMock
      .mockRejectedValueOnce(new Error('Lesson not found'))
      .mockResolvedValueOnce(lesson)
    renderLesson()

    expect(await screen.findByRole('alert')).toHaveTextContent('Lesson not found')
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('heading', { name: lesson.title })).toBeInTheDocument()
    expect(getLessonMock).toHaveBeenCalledTimes(2)
  })

  it('submits code and renders the structured advisory feedback without execution claims', async () => {
    const user = userEvent.setup()
    getLessonMock.mockResolvedValue(lesson)
    submitLessonCodeMock.mockResolvedValue(submission)
    renderLesson()

    const editor = await screen.findByRole('textbox', { name: 'Your Java draft' })
    await user.click(screen.getByRole('button', { name: 'Submit for advisory feedback' }))

    expect(await screen.findByRole('heading', { name: 'Review of your submission' }))
      .toBeInTheDocument()
    expect(screen.getByText(submission.feedback.summary)).toBeInTheDocument()
    expect(screen.getByText('Null values are not guarded.')).toBeInTheDocument()
    expect(screen.getByText(/Functional Defect · Null Handling/)).toBeInTheDocument()
    expect(screen.getByText(/code was not compiled or executed/)).toBeInTheDocument()
    expect(screen.getByText(/Attempt d693de29 was saved/)).toBeInTheDocument()
    expect(submitLessonCodeMock).toHaveBeenCalledWith(
      'demo-learner',
      lesson.lessonId,
      lesson.exercise.starterCode,
    )
    expect(editor).toHaveValue(lesson.exercise.starterCode)
  })

  it('labels sample feedback as a demo and not an assessment', async () => {
    const user = userEvent.setup()
    getLessonMock.mockResolvedValue(lesson)
    submitLessonCodeMock.mockResolvedValue({
      ...submission,
      feedback: {
        ...submission.feedback,
        summary: 'DEMO SAMPLE ONLY: This response does not assess your submitted code.',
        findings: [],
        assessmentConfidence: 'LOW',
        competencyTags: ['INPUT_VALIDATION'],
        nextActionType: 'CLARIFICATION',
        nextAction: 'Review the exercise requirements manually.',
        reviewRecommendation: 'UNCERTAIN',
        feedbackSource: 'DEMO_SAMPLE',
      },
    })
    renderLesson()

    await screen.findByRole('textbox', { name: 'Your Java draft' })
    await user.click(screen.getByRole('button', { name: 'Submit for advisory feedback' }))

    expect(await screen.findByText('DEMO SAMPLE · NOT AN ASSESSMENT')).toBeInTheDocument()
    expect(screen.getByText(/does not assess your submitted code/)).toBeInTheDocument()
    expect(screen.getByText('Review the exercise requirements manually.')).toBeInTheDocument()
  })

  it('preserves code and allows retry when advisory feedback is unavailable', async () => {
    const user = userEvent.setup()
    getLessonMock.mockResolvedValue(lesson)
    submitLessonCodeMock
      .mockRejectedValueOnce(new LearnerApiError(
        'Advisory feedback is temporarily unavailable.',
        503,
        'AI_FEEDBACK_UNAVAILABLE',
      ))
      .mockResolvedValueOnce(submission)
    renderLesson()

    const editor = await screen.findByRole('textbox', { name: 'Your Java draft' })
    fireEvent.change(editor, { target: { value: 'class ImprovedValidator {}' } })
    await user.click(screen.getByRole('button', { name: 'Submit for advisory feedback' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('You can retry without losing your draft.')
    expect(editor).toHaveValue('class ImprovedValidator {}')
    await user.click(screen.getByRole('button', { name: 'Retry feedback' }))

    expect(await screen.findByRole('heading', { name: 'Review of your submission' }))
      .toBeInTheDocument()
    expect(submitLessonCodeMock).toHaveBeenNthCalledWith(
      2,
      'demo-learner',
      lesson.lessonId,
      'class ImprovedValidator {}',
    )
  })

  it('does not make a lesson request for an unsupported lesson ID', async () => {
    getLessonMock.mockRejectedValue(new Error('Lesson not found'))
    renderLesson('/lessons/other-lesson')

    expect(await screen.findByRole('alert')).toHaveTextContent('Lesson not found')
    expect(getLessonMock).toHaveBeenCalledWith('other-lesson')
  })
})
