import { useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import {
  getLesson,
  getRecommendedLesson,
  LearnerApiError,
  submitLessonCode,
  type LessonContentResponse,
  type SubmissionResponse,
} from '../api/learnerApi'
import './LessonLabPage.css'

function LessonLabPage() {
  const { lessonId } = useParams()
  const [searchParams] = useSearchParams()
  const learnerId = searchParams.get('learnerId')
  const [lesson, setLesson] = useState<LessonContentResponse | null>(null)
  const [qaScenario, setQaScenario] = useState<string | null>(null)
  const [recommendationReason, setRecommendationReason] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [submission, setSubmission] = useState<SubmissionResponse | null>(null)
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const submittingRef = useRef(false)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    let isCurrent = true

    async function loadLesson() {
      if (!lessonId) {
        setError('A lesson ID is required to open the lesson workspace.')
        setIsLoading(false)
        return
      }

      setIsLoading(true)
      setError('')
      try {
        const [content, recommendation] = await Promise.all([
          getLesson(lessonId),
          learnerId ? getRecommendedLesson(learnerId) : Promise.resolve(null),
        ])
        if (isCurrent) {
          setLesson(content)
          setQaScenario(recommendation?.qaScenario ?? null)
          setRecommendationReason(recommendation?.recommendationReason ?? null)
          setDraft(content.exercise.starterCode)
        }
      } catch (loadError) {
        if (isCurrent) {
          setError(loadError instanceof Error
            ? loadError.message
            : 'Could not load this lesson. Please try again.')
        }
      } finally {
        if (isCurrent) setIsLoading(false)
      }
    }

    void loadLesson()
    return () => {
      isCurrent = false
    }
  }, [learnerId, lessonId, retryKey])

  async function submitDraft() {
    if (!learnerId || !lessonId || submittingRef.current) return
    submittingRef.current = true
    setIsSubmitting(true)
    setSubmitError('')
    setSubmission(null)
    try {
      const result = await submitLessonCode(learnerId, lessonId, draft)
      setSubmission(result)
    } catch (submitFailure) {
      const message = submitFailure instanceof LearnerApiError &&
        submitFailure.code === 'AI_FEEDBACK_UNAVAILABLE'
        ? `${submitFailure.message} You can retry without losing your draft.`
        : submitFailure instanceof Error
          ? submitFailure.message
          : 'Could not submit your code. Please try again.'
      setSubmitError(message)
    } finally {
      submittingRef.current = false
      setIsSubmitting(false)
    }
  }

  function updateDraft(code: string) {
    setDraft(code)
    setSubmission(null)
    setSubmitError('')
  }

  return (
    <div className="lesson-shell">
      <a className="skip-link" href="#lesson-main">Skip to content</a>
      <header className="lesson-header">
        <Link className="onboarding-brand" to="/" aria-label="DevPath AI home">
          <span className="onboarding-brand-mark" aria-hidden="true">D</span>
          <span>DevPath <span>AI</span></span>
        </Link>
        <div className="lesson-header-actions">
          <span className="demo-label">Early release demo</span>
          {learnerId && (
            <Link className="lesson-back-link" to={`/dashboard/${encodeURIComponent(learnerId)}`}>
              Back to dashboard
            </Link>
          )}
        </div>
      </header>

      <main className="lesson-main" id="lesson-main">
        {isLoading && (
          <div className="lesson-state" role="status">Loading curated lesson…</div>
        )}
        {!isLoading && error && (
          <section className="lesson-state lesson-error" role="alert">
            <h1>Lesson unavailable</h1>
            <p>{error}</p>
            <button
              className="wizard-button wizard-button-primary"
              type="button"
              onClick={() => setRetryKey((current) => current + 1)}
            >
              Try again
            </button>
          </section>
        )}
        {!isLoading && !error && lesson && (
          <>
            <div className="lesson-heading">
              <p className="onboarding-eyebrow">ASSISTED LEARNING LAB · LESSON 1</p>
              <h1>{lesson.title}</h1>
              <p>{lesson.summary}</p>
              <div className="lesson-meta">
                <span>{lesson.estimatedMinutes} min lesson</span>
                <span>Java</span>
                <span>Curated learning content</span>
              </div>
            </div>

            <div className="lesson-workspace">
              <section className="lesson-content-panel" aria-labelledby="learn-title">
                <div className="lesson-panel-heading">
                  <p className="onboarding-eyebrow">UNDERSTAND</p>
                  <h2 id="learn-title">Methods and validation</h2>
                </div>
                <div className="qa-context-card">
                  <span className="qa-context-mark" aria-hidden="true">QA</span>
                  <div>
                    <h3>{qaScenario ? 'Your QA perspective' : 'Banking API context'}</h3>
                    <p>{qaScenario ?? lesson.qaContext}</p>
                  </div>
                </div>
                {recommendationReason && (
                  <div className="qa-context-card java-level-context">
                    <span className="qa-context-mark" aria-hidden="true">Java</span>
                    <div>
                      <h3>Java focus for your programming level</h3>
                      <p>{recommendationReason}</p>
                    </div>
                  </div>
                )}
                <div className="lesson-sections">
                  {lesson.sections.map((section, index) => (
                    <article className="lesson-section" key={section.heading}>
                      <span className="lesson-section-number" aria-hidden="true">
                        {String(index + 1).padStart(2, '0')}
                      </span>
                      <div>
                        <h3>{section.heading}</h3>
                        <p>{section.content}</p>
                        <pre><code>{section.codeExample}</code></pre>
                      </div>
                    </article>
                  ))}
                </div>
                <div className="lesson-goal">
                  <span aria-hidden="true">→</span>
                  <p><strong>Lesson objective</strong>{lesson.learningObjective}</p>
                </div>
              </section>

              <section className="practice-panel" aria-labelledby="practice-title">
                <div className="lesson-panel-heading practice-panel-heading">
                  <p className="onboarding-eyebrow">PRACTICE</p>
                  <h2 id="practice-title">{lesson.exercise.title}</h2>
                  <p>{lesson.exercise.prompt}</p>
                </div>
                <div className="practice-instructions">
                  <h3>Requirements</h3>
                  <ul>
                    {lesson.exercise.requirements.map((requirement) => (
                      <li key={requirement}>{requirement}</li>
                    ))}
                  </ul>
                </div>
                <div className="practice-instructions test-cases">
                  <h3>Cases to consider</h3>
                  <ul>
                    {lesson.exercise.testCases.map((testCase) => (
                      <li key={testCase}><code>{testCase}</code></li>
                    ))}
                  </ul>
                </div>
                <label className="draft-label" htmlFor="java-draft">Your Java draft</label>
                <textarea
                  id="java-draft"
                  className="java-draft"
                  spellCheck={false}
                  maxLength={20000}
                  value={draft}
                  onChange={(event) => updateDraft(event.target.value)}
                  aria-describedby="draft-note"
                />
                <p className="draft-note" id="draft-note">
                  Maximum 20,000 characters. Your draft remains in this page until
                  you submit; it is not compiled or executed.
                </p>
                {!learnerId && (
                  <p className="submission-error" role="alert">
                    A learner profile is required to submit this exercise. Return to
                    your dashboard and open the lesson from there.
                  </p>
                )}
                {submitError && (
                  <div className="submission-error" role="alert">
                    <p>{submitError}</p>
                    <button
                      className="text-button"
                      type="button"
                      onClick={() => void submitDraft()}
                      disabled={isSubmitting}
                    >
                      Retry feedback
                    </button>
                  </div>
                )}
                <button
                  className="wizard-button wizard-button-primary submit-code-button"
                  type="button"
                  onClick={() => void submitDraft()}
                  disabled={!learnerId || isSubmitting || !draft.trim() || draft.length > 20000}
                >
                  {isSubmitting ? 'Requesting feedback…' : 'Submit for advisory feedback'}
                </button>
                {submission && <FeedbackResult result={submission} />}
                <div className="upcoming-feedback" role="note">
                  <strong>Advisory review, not code execution</strong>
                  <span>
                    The code is sent securely from the backend to the configured
                    feedback provider. It is not compiled or run.
                  </span>
                </div>
              </section>
            </div>
          </>
        )}
      </main>
    </div>
  )
}

function FeedbackResult({ result }: { result: SubmissionResponse }) {
  const feedback = result.feedback
  return (
    <section className="feedback-result" aria-labelledby="feedback-title" aria-live="polite">
      <div className="feedback-result-heading">
        <div>
          <p className="onboarding-eyebrow">
            {feedback.feedbackSource === 'DEMO_SAMPLE' ? 'DEMO SAMPLE · NOT AN ASSESSMENT' : 'ADVISORY FEEDBACK'}
          </p>
          <h3 id="feedback-title">Review of your submission</h3>
        </div>
        <span className="feedback-status">{formatRecommendation(feedback.reviewRecommendation)}</span>
      </div>
      <p className="feedback-summary">{feedback.summary}</p>
      <div className="feedback-findings">
        <h4>Assessment findings ({formatRecommendation(feedback.assessmentConfidence)} confidence)</h4>
        {feedback.findings.length === 0 ? (
          <p>No defects or follow-up findings identified.</p>
        ) : (
          <ul>
            {feedback.findings.map((finding, index) => (
              <li key={`${finding.competency}-${finding.category}-${index}`}>
                <strong>{formatRecommendation(finding.category)} · {formatRecommendation(finding.competency)}:</strong>
                {' '}{finding.description}
              </li>
            ))}
          </ul>
        )}
      </div>
      <div className="feedback-columns">
        <FeedbackList title="Strengths" values={feedback.strengths} />
        <FeedbackList title="Issues to consider" values={feedback.issues} />
        <FeedbackList title="Ways to improve" values={feedback.improvements} />
      </div>
      <div className="feedback-next-action">
        <strong>Next action · {formatRecommendation(feedback.nextActionType)}</strong>
        <p>{feedback.nextAction}</p>
      </div>
      <div className="feedback-tags" aria-label="Competency tags">
        {feedback.competencyTags.map((tag) => (
          <span className="dashboard-chip" key={tag}>{formatRecommendation(tag)}</span>
        ))}
      </div>
      <Link
        className="text-button"
        to={`/recall/${encodeURIComponent(result.learnerId)}`}
      >
        View competency status and Learning Recall
      </Link>
      <p className="feedback-trust-note">
        This is advisory feedback only. Your code was not compiled or executed,
        and this recommendation does not establish mastery.
        {result.attemptId && <span> Attempt {result.attemptId.slice(0, 8)} was saved.</span>}
      </p>
    </section>
  )
}

function FeedbackList({ title, values }: { title: string; values: string[] }) {
  return (
    <div className="feedback-list">
      <h4>{title}</h4>
      <ul>
        {values.map((value, index) => <li key={`${index}-${value}`}>{value}</li>)}
      </ul>
    </div>
  )
}

function formatRecommendation(value: string): string {
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

export default LessonLabPage
