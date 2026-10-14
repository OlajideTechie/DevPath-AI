import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import {
  confirmDemonstratedCompetency,
  getRecall,
  LearnerApiError,
  submitRecallCode,
  type RecallResponse,
  type SubmissionResponse,
} from '../api/learnerApi'
import './RecallPage.css'

function RecallPage() {
  const { learnerId } = useParams()
  const [searchParams] = useSearchParams()
  const requestedCompetencyTag = searchParams.get('competencyTag')
  const [data, setData] = useState<RecallResponse | null>(null)
  const [selectedTag, setSelectedTag] = useState('')
  const [draft, setDraft] = useState<string | null>(null)
  const [submission, setSubmission] = useState<SubmissionResponse | null>(null)
  const [error, setError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isConfirming, setIsConfirming] = useState(false)
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    let isCurrent = true

    async function loadRecall() {
      if (!learnerId) {
        setError('A learner profile ID is required to load Learning Recall.')
        setIsLoading(false)
        return
      }
      setIsLoading(true)
      setError('')
      try {
        const response = await getRecall(learnerId)
        if (isCurrent) {
          setData(response)
          setSelectedTag((current) => {
            const activeCompetencies = response.competencies.filter((item) => (
              item.status === 'NEEDS_REVIEW' || item.status === 'PRACTICING'
            ))
            if (activeCompetencies.some((item) => item.competencyTag === current)) return current
            return activeCompetencies.find((item) => (
              item.competencyTag === requestedCompetencyTag
            ))?.competencyTag ?? activeCompetencies[0]?.competencyTag ?? ''
          })
        }
      } catch (loadError) {
        if (isCurrent) {
          setError(loadError instanceof Error
            ? loadError.message
            : 'Could not load Learning Recall. Please try again.')
        }
      } finally {
        if (isCurrent) setIsLoading(false)
      }
    }

    void loadRecall()
    return () => {
      isCurrent = false
    }
  }, [learnerId, requestedCompetencyTag, retryKey])

  const activeCompetencies = data?.competencies.filter((item) => (
    item.status === 'NEEDS_REVIEW' || item.status === 'PRACTICING'
  )) ?? []
  const selectedCompetency = activeCompetencies.find((item) => item.competencyTag === selectedTag)
  const canConfirm = Boolean(
    selectedCompetency?.baseReadyAttemptId && selectedCompetency.recallReadyAttemptId,
  )

  async function submitDraft() {
    if (!learnerId || !selectedTag || isSubmitting) return
    setIsSubmitting(true)
    setSubmitError('')
    setSubmission(null)
    try {
      const result = await submitRecallCode(learnerId, selectedTag, draft ?? data?.exercise.starterCode ?? '')
      setSubmission(result)
      setConfirmed(false)
      setRetryKey((current) => current + 1)
    } catch (submitFailure) {
      const message = submitFailure instanceof LearnerApiError &&
        submitFailure.code === 'AI_FEEDBACK_UNAVAILABLE'
        ? `${submitFailure.message} Your draft is preserved; you can retry.`
        : submitFailure instanceof Error
          ? submitFailure.message
          : 'Could not submit the recall exercise. Please try again.'
      setSubmitError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function confirmDemonstrated() {
    if (!learnerId || !selectedTag || !confirmed || isConfirming) return
    setIsConfirming(true)
    setSubmitError('')
    try {
      await confirmDemonstratedCompetency(learnerId, selectedTag)
      setConfirmed(false)
      setSubmission(null)
      setRetryKey((current) => current + 1)
    } catch (confirmError) {
      setSubmitError(confirmError instanceof Error
        ? confirmError.message
        : 'Could not save the confirmation. Please try again.')
    } finally {
      setIsConfirming(false)
    }
  }

  function selectCompetency(tag: string) {
    setSelectedTag(tag)
    setSubmission(null)
    setSubmitError('')
    setConfirmed(false)
  }

  return (
    <div className="recall-shell">
      <a className="skip-link" href="#recall-main">Skip to content</a>
      <header className="recall-header">
        <Link className="onboarding-brand" to="/" aria-label="DevPath AI home">
          <span className="onboarding-brand-mark" aria-hidden="true">D</span>
          <span>DevPath <span>AI</span></span>
        </Link>
        {learnerId && (
          <Link className="recall-back-link" to={`/dashboard/${encodeURIComponent(learnerId)}`}>
            Back to dashboard
          </Link>
        )}
      </header>

      <main className="recall-main" id="recall-main">
        <div className="recall-heading">
          <p className="onboarding-eyebrow">LEARNING RECALL</p>
          <h1>Revisit a competency in a new context.</h1>
          <p>Use a bank-transfer exercise to practise the validation rules from your withdrawal review.</p>
        </div>

        {isLoading && <div className="recall-state" role="status">Loading your recall activity…</div>}
        {!isLoading && error && (
          <section className="recall-state recall-error" role="alert">
            <h2>Learning Recall could not be loaded.</h2>
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

        {!isLoading && !error && data && (
          <>
            <section className="recall-exercise-panel" aria-labelledby="recall-exercise-title">
              <div className="recall-exercise-topline">
                <span className="recommendation-kicker">RELATED TRANSFER EXERCISE</span>
                <span className="lesson-duration">{data.exercise.estimatedMinutes} min</span>
              </div>
              <h2 id="recall-exercise-title">{data.exercise.title}</h2>
              <p>{data.exercise.summary}</p>
              <div className="recall-context">
                <strong>New QA context</strong>
                <span>{data.exercise.qaContext}</span>
              </div>

              {activeCompetencies.length === 0 ? (
                <div className="recall-empty" role="status">
                  <h3>No active competency gaps</h3>
                  <p>
                    Recall starts when an advisory review identifies a competency
                    needing practice. No competency is marked demonstrated from a
                    single AI review.
                  </p>
                </div>
              ) : (
                <div className="recall-workspace">
                  <section className="recall-competencies" aria-labelledby="competency-title">
                    <h3 id="competency-title">Choose a competency to practise</h3>
                    <div className="recall-competency-list">
                      {activeCompetencies.map((competency) => (
                        <button
                          className={`recall-competency${selectedTag === competency.competencyTag ? ' is-selected' : ''}`}
                          type="button"
                          key={competency.competencyTag}
                          aria-pressed={selectedTag === competency.competencyTag}
                          onClick={() => selectCompetency(competency.competencyTag)}
                        >
                          <span>{formatLabel(competency.competencyTag)}</span>
                          <span className="recall-status">{formatLabel(competency.status)}</span>
                          <span className="recall-status">
                            {competency.reason === 'PLANNED_REINFORCEMENT'
                              ? 'Planned reinforcement'
                              : 'Identified competency gap'}
                          </span>
                        </button>
                      ))}
                    </div>
                  </section>

                  {selectedCompetency && (
                    <section className="recall-practice" aria-labelledby="recall-prompt-title">
                      <p className="onboarding-eyebrow">PRACTISE IN A NEW CONTEXT</p>
                      <h3 id="recall-prompt-title">{data.exercise.prompt}</h3>
                      <div className="recall-instructions">
                        <h4>Requirements</h4>
                        <ul>{data.exercise.requirements.map((item) => <li key={item}>{item}</li>)}</ul>
                        <h4>Cases to consider</h4>
                        <ul>{data.exercise.testCases.map((item) => <li key={item}><code>{item}</code></li>)}</ul>
                      </div>
                      {selectedCompetency.sourceAttempt && (
                        <OriginalReview attempt={selectedCompetency.sourceAttempt} />
                      )}
                      <label className="draft-label" htmlFor="recall-draft">Your Java transfer draft</label>
                      <textarea
                        id="recall-draft"
                        className="java-draft recall-draft"
                        spellCheck={false}
                        maxLength={20000}
                        value={draft ?? data.exercise.starterCode}
                        onChange={(event) => {
                          setDraft(event.target.value)
                          setSubmission(null)
                          setSubmitError('')
                        }}
                      />
                      <p className="draft-note">
                        Maximum 20,000 characters. The backend does not compile or execute this code.
                      </p>
                      {submitError && <p className="submission-error" role="alert">{submitError}</p>}
                      <button
                        className="wizard-button wizard-button-primary"
                        type="button"
                        disabled={isSubmitting || !(draft ?? data.exercise.starterCode).trim()}
                        onClick={() => void submitDraft()}
                      >
                        {isSubmitting ? 'Requesting advisory feedback…' : 'Submit recall exercise'}
                      </button>
                      {submission && (
                        <RecallFeedback result={submission} competencyTag={selectedTag} />
                      )}

                      {canConfirm ? (
                        <div className="recall-demonstration">
                          <p>
                            Positive advisory feedback was recorded on both the original
                            and transfer exercises. Demonstration also requires your
                            confirmation; AI feedback alone never establishes mastery.
                          </p>
                          <label>
                            <input
                              type="checkbox"
                              checked={confirmed}
                              onChange={(event) => setConfirmed(event.target.checked)}
                            />
                            I can explain and apply this rule in both exercise contexts.
                          </label>
                          <button
                            className="wizard-button wizard-button-primary"
                            type="button"
                            disabled={!confirmed || isConfirming}
                            onClick={() => void confirmDemonstrated()}
                          >
                            {isConfirming ? 'Saving confirmation…' : 'Confirm demonstrated competency'}
                          </button>
                        </div>
                      ) : (
                        <div className="recall-policy">
                          <p>{data.assessmentPolicy}</p>
                          {!selectedCompetency.baseReadyAttemptId && (
                            <Link
                              className="text-button"
                              to={`/lessons/java-methods-input-validation?learnerId=${encodeURIComponent(data.learnerId)}`}
                            >
                              Return to the withdrawal exercise for another review
                            </Link>
                          )}
                        </div>
                      )}
                    </section>
                  )}
                </div>
              )}
            </section>

            <section className="recall-history" aria-labelledby="recall-history-title">
              <h2 id="recall-history-title">Competency status</h2>
              <ul>
                {data.competencies.map((competency) => (
                  <li key={competency.competencyTag}>
                    <span>{formatLabel(competency.competencyTag)}</span>
                    <span className={`recall-status recall-status-${competency.status.toLowerCase()}`}>
                      {formatLabel(competency.status)}
                    </span>
                  </li>
                ))}
              </ul>
              <p className="recall-trust-note">
                No exercise is compiled or executed. Demonstrated status requires
                separate positive reviews and your explicit confirmation.
              </p>
            </section>
          </>
        )}
      </main>
    </div>
  )
}

function RecallFeedback({
  result,
  competencyTag,
}: {
  result: SubmissionResponse
  competencyTag: string
}) {
  const sections = [
    { title: 'Strengths', items: result.feedback.strengths },
    { title: 'Issues to consider', items: result.feedback.issues },
    { title: 'Ways to improve', items: result.feedback.improvements },
  ]

  return (
    <section className="feedback-result recall-feedback" aria-live="polite">
      <div className="feedback-result-heading">
        <div>
          <p className="onboarding-eyebrow">
            {result.feedback.feedbackSource === 'DEMO_SAMPLE'
              ? 'DEMO SAMPLE · NOT AN ASSESSMENT'
              : 'ADVISORY FEEDBACK'}
          </p>
          <h3>Review of your transfer exercise</h3>
        </div>
        <span className="feedback-status">{formatLabel(result.feedback.reviewRecommendation)}</span>
      </div>
      <p className="feedback-summary">{result.feedback.summary}</p>
      <AssessmentFindings feedback={result.feedback} />
      <div className="feedback-columns">
        {sections.map((section) => (
          <div className="feedback-list" key={section.title}>
            <h4>{section.title}</h4>
            <ul>{section.items.map((item) => <li key={item}>{item}</li>)}</ul>
          </div>
        ))}
      </div>
      <div className="feedback-next-action">
        <strong>Next action · {formatLabel(result.feedback.nextActionType)}</strong>
        <p>{result.feedback.nextAction}</p>
      </div>
      <p className="feedback-trust-note">
        Advisory only. The code was not compiled or executed. Attempt {result.attemptId.slice(0, 8)} was saved.
      </p>
      {result.feedback.reviewRecommendation === 'READY_FOR_REVIEW'
        && result.feedback.competencyTags.includes(competencyTag) && (
        <p className="recall-positive-note" role="status">
          This is positive advisory feedback, not a verified result. Demonstrated status still
          requires positive feedback on both exercises and your confirmation.
        </p>
      )}
    </section>
  )
}

function OriginalReview({ attempt }: { attempt: NonNullable<RecallResponse['competencies'][number]['sourceAttempt']> }) {
  const sections = [
    { title: 'Strengths', items: attempt.feedback.strengths },
    { title: 'Issues to consider', items: attempt.feedback.issues },
    { title: 'Ways to improve', items: attempt.feedback.improvements },
  ]

  return (
    <details className="recall-original-review">
      <summary>Review your original withdrawal submission</summary>
      <div className="recall-original-content">
        {attempt.feedback.feedbackSource === 'DEMO_SAMPLE' && (
          <p className="recall-demo-notice" role="note">
            Demo sample only. This response did not assess the submitted code.
          </p>
        )}
        <p>{attempt.feedback.summary}</p>
        <AssessmentFindings feedback={attempt.feedback} />
        <span className="feedback-status">
          {formatLabel(attempt.feedback.reviewRecommendation)}
        </span>
        <pre><code>{attempt.submittedCode}</code></pre>
        <div className="feedback-columns">
          {sections.map((section) => (
            <div className="feedback-list" key={section.title}>
              <h4>{section.title}</h4>
              <ul>{section.items.map((item) => <li key={item}>{item}</li>)}</ul>
            </div>
          ))}
        </div>
        <div className="feedback-next-action">
          <strong>Next action · {formatLabel(attempt.feedback.nextActionType)}</strong>
          <p>{attempt.feedback.nextAction}</p>
        </div>
        <p className="feedback-trust-note">
          Saved from your original submission. It was not compiled or executed.
        </p>
      </div>
    </details>
  )
}

function AssessmentFindings({ feedback }: { feedback: SubmissionResponse['feedback'] }) {
  return (
    <div className="feedback-findings">
      <h4>Assessment findings ({formatLabel(feedback.assessmentConfidence)} confidence)</h4>
      {feedback.findings.length === 0 ? (
        <p>No defects or follow-up findings identified.</p>
      ) : (
        <ul>
          {feedback.findings.map((finding, index) => (
            <li key={`${finding.competency}-${finding.category}-${index}`}>
              <strong>{formatLabel(finding.category)} · {formatLabel(finding.competency)}:</strong>
              {' '}{finding.description}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function formatLabel(value: string): string {
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

export default RecallPage
