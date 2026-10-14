import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  experienceBands,
  getLearner,
  getProgress,
  getRecall,
  getRecommendedLesson,
  type LearnerProfileResponse,
  type ProgressResponse,
  type RecallResponse,
  type RecommendedLessonResponse,
} from '../api/learnerApi'
import './DashboardPage.css'

interface DashboardData {
  profile: LearnerProfileResponse
  recommendation: RecommendedLessonResponse
  recall: RecallResponse
  progress: ProgressResponse
}

function DashboardPage() {
  const { learnerId } = useParams()
  const [data, setData] = useState<DashboardData | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    let isCurrent = true

    async function loadDashboard() {
      if (!learnerId) {
        setError('A learner profile ID is required to load this dashboard.')
        setIsLoading(false)
        return
      }

      setIsLoading(true)
      setError('')
      try {
        const [profile, recommendation, recall, progress] = await Promise.all([
          getLearner(learnerId),
          getRecommendedLesson(learnerId),
          getRecall(learnerId),
          getProgress(learnerId),
        ])
        if (isCurrent) setData({ profile, recommendation, recall, progress })
      } catch (loadError) {
        if (isCurrent) {
          setError(loadError instanceof Error
            ? loadError.message
            : 'Could not load your dashboard. Please try again.')
        }
      } finally {
        if (isCurrent) setIsLoading(false)
      }
    }

    void loadDashboard()
    return () => {
      isCurrent = false
    }
  }, [learnerId, retryKey])

  const experienceLabel = data
    ? experienceBands.find((band) => band.value === data.profile.experienceBand)?.label
    : undefined
  const activeRecallCount = data?.recall.competencies.filter((competency) => (
    competency.status === 'NEEDS_REVIEW' || competency.status === 'PRACTICING'
  )).length ?? 0
  const demonstratedCount = data?.recall.competencies.filter(
    (competency) => competency.status === 'DEMONSTRATED',
  ).length ?? 0

  return (
    <div className="dashboard-shell">
      <a className="skip-link" href="#dashboard-main">Skip to content</a>
      <header className="dashboard-header">
        <Link className="onboarding-brand" to="/" aria-label="DevPath AI home">
          <span className="onboarding-brand-mark" aria-hidden="true">D</span>
          <span>DevPath <span>AI</span></span>
        </Link>
        <span className="demo-label">Early release demo</span>
      </header>

      <main className="dashboard-main" id="dashboard-main">
        <div className="dashboard-heading">
          <p className="onboarding-eyebrow">YOUR LEARNING PATH</p>
          <h1>Your learning path, grounded in your QA experience.</h1>
          <p>Your persisted activity and recommended next step are below.</p>
        </div>

        {isLoading && (
          <div className="dashboard-state" role="status">
            Loading your saved profile and lesson recommendation…
          </div>
        )}

        {!isLoading && error && (
          <section className="dashboard-state dashboard-error" role="alert">
            <h2>Your dashboard could not be loaded.</h2>
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
            <section className="dashboard-context dashboard-panel" aria-labelledby="context-title">
              <div className="dashboard-section-heading">
                <div>
                  <p className="onboarding-eyebrow">YOUR STARTING POINT</p>
                  <h2 id="context-title">Profile context</h2>
                </div>
                <span className="dashboard-status-pill">Saved profile</span>
              </div>
              <dl className="context-grid">
                <div>
                  <dt>Current role</dt>
                  <dd>{data.profile.currentRole}</dd>
                </div>
                <div>
                  <dt>Next role</dt>
                  <dd>{data.profile.targetRole}</dd>
                </div>
                <div>
                  <dt>QA experience</dt>
                  <dd>{experienceLabel ?? 'Not available'}</dd>
                </div>
                <div>
                  <dt>Programming level</dt>
                  <dd>{formatLabel(data.profile.programmingLevel)}</dd>
                </div>
                <div>
                  <dt>Daily study goal</dt>
                  <dd>{data.profile.dailyMinutes} minutes per day</dd>
                </div>
                <div>
                  <dt>Study days</dt>
                  <dd>{data.profile.studyDays.map(formatLabel).join(', ')}</dd>
                </div>
                <div>
                  <dt>Learning preference</dt>
                  <dd>{formatLabel(data.profile.learningStyle)}</dd>
                </div>
                <div className="context-specializations">
                  <dt>QA specializations</dt>
                  <dd className="dashboard-chips">
                    {data.profile.specializations.map((specialization) => (
                      <span className="dashboard-chip" key={specialization}>{specialization}</span>
                    ))}
                  </dd>
                </div>
              </dl>
            </section>

            <section className="recommendation-panel" aria-labelledby="recommendation-title">
              <div className="recommendation-topline">
                <span className="recommendation-kicker">CURATED STARTER LESSON</span>
                <span className="lesson-duration">{data.recommendation.estimatedMinutes} min</span>
              </div>
              <h2 id="recommendation-title">{data.recommendation.title}</h2>
              <p className="recommendation-summary">{data.recommendation.summary}</p>
              <div className="recommendation-detail">
                <h3>Java focus for your programming level</h3>
                <p>{data.recommendation.recommendationReason}</p>
              </div>
              <div className="recommendation-detail qa-example">
                <h3>Connected to your QA practice</h3>
                <p>{data.recommendation.qaScenario}</p>
              </div>
              <div className="lesson-objective">
                <span className="objective-mark" aria-hidden="true">→</span>
                <p><strong>Lesson goal</strong>{data.recommendation.learningObjective}</p>
              </div>
              <Link
                className="wizard-button wizard-button-primary dashboard-lesson-link"
                to={`/lessons/${encodeURIComponent(data.recommendation.lessonId)}?learnerId=${encodeURIComponent(data.profile.id)}`}
              >
                Open lesson workspace
                <span aria-hidden="true">→</span>
              </Link>
              <p className="upcoming-note">Other lessons remain upcoming. Your attempts and feedback are tracked below.</p>
            </section>

            <section className="dashboard-status-grid" aria-label="Learning status">
              <article className="dashboard-panel status-card">
                <p className="onboarding-eyebrow">CURRENT MILESTONE</p>
                <h2>Profile saved</h2>
                <p>Your saved profile grounds the lesson recommendation and persisted activity summary below.</p>
                <span className="dashboard-status-pill">Onboarding complete</span>
              </article>
              <article className="dashboard-panel status-card">
                <p className="onboarding-eyebrow">LEARNING ACTIVITY</p>
                <h2>Submissions are recorded</h2>
                <p>
                  {data.progress.exercisesSubmitted} {data.progress.exercisesSubmitted === 1 ? 'exercise has' : 'exercises have'} been submitted across {data.progress.lessonsAttempted} {data.progress.lessonsAttempted === 1 ? 'lesson' : 'lessons'}.
                </p>
              </article>
              <article className="dashboard-panel status-card">
                <p className="onboarding-eyebrow">LEARNING RECALL</p>
                <h2>
                  {activeRecallCount > 0
                    ? activeRecallCount === 1
                      ? '1 competency to practise'
                      : `${activeRecallCount} competencies to practise`
                    : demonstratedCount > 0
                      ? 'No active gaps'
                      : 'Ready when a gap is found'}
                </h2>
                <p>
                  {activeRecallCount > 0
                    ? 'A persisted advisory review identified a related practice opportunity.'
                    : demonstratedCount > 0
                      ? `${demonstratedCount} ${demonstratedCount === 1 ? 'competency is' : 'competencies are'} marked demonstrated under the review policy.`
                      : 'A related transfer exercise becomes available after a review identifies a competency gap.'}
                </p>
                <Link
                  className="text-button"
                  to={`/recall/${encodeURIComponent(data.profile.id)}`}
                >
                  Open Learning Recall
                </Link>
              </article>
            </section>

            <section className="dashboard-progress dashboard-panel" aria-labelledby="progress-title">
              <div className="dashboard-section-heading">
                <div>
                  <p className="onboarding-eyebrow">PERSISTED ACTIVITY</p>
                  <h2 id="progress-title">Learning progress</h2>
                </div>
              </div>

              <div className="progress-metrics" aria-label="Persisted progress counts">
                <ProgressMetric label="Lessons attempted" value={data.progress.lessonsAttempted} />
                <ProgressMetric label="Exercises submitted" value={data.progress.exercisesSubmitted} />
                <ProgressMetric label="Competencies demonstrated" value={data.progress.competenciesDemonstrated.length} />
              </div>

              <div className="progress-detail-grid">
                <ProgressCompetencyList
                  title="Competencies needing review"
                  competencies={data.progress.competenciesNeedingReview}
                  emptyMessage="No competencies are currently marked as needing review."
                />
                <ProgressCompetencyList
                  title="Competencies in practice"
                  competencies={data.progress.competenciesInProgress}
                  emptyMessage="No competencies are currently in practice."
                />
                <ProgressCompetencyList
                  title="Demonstrated under the review policy"
                  competencies={data.progress.competenciesDemonstrated}
                  emptyMessage="No competencies have met the two-exercise evidence and confirmation policy yet."
                />
              </div>

              <section className="latest-feedback" aria-labelledby="latest-feedback-title">
                <h3 id="latest-feedback-title">Latest available advisory feedback</h3>
                {data.progress.latestFeedback ? (
                  <>
                    {data.progress.latestFeedback.feedbackSource === 'DEMO_SAMPLE' && (
                      <p className="dashboard-status-pill" role="note">
                        Demo sample only · not an assessment
                      </p>
                    )}
                    <p className="latest-feedback-context">
                      {formatLabel(data.progress.latestFeedback.lessonId)} ·{' '}
                      <time dateTime={data.progress.latestFeedback.submittedAt}>
                        {new Date(data.progress.latestFeedback.submittedAt).toLocaleString()}
                      </time>
                    </p>
                    <p>{data.progress.latestFeedback.summary}</p>
                    <p><strong>Next action:</strong> {data.progress.latestFeedback.nextAction}</p>
                    <p>
                      <strong>Recommendation:</strong>{' '}
                      {formatLabel(data.progress.latestFeedback.reviewRecommendation)}
                    </p>
                    <p>
                      <strong>Competencies mentioned:</strong>{' '}
                      {data.progress.latestFeedback.competencyTags.map(formatLabel).join(', ')}
                    </p>
                  </>
                ) : (
                  <p>No advisory feedback is available yet. A saved attempt will appear here after feedback is received.</p>
                )}
                <p className="progress-trust-note">
                  Advisory feedback is not a compilation or test result. Demonstrated status follows the separate review and confirmation policy.
                </p>
              </section>

              <section className="next-activity" aria-labelledby="next-activity-title">
                <div>
                  <p className="onboarding-eyebrow">RECOMMENDED NEXT ACTIVITY</p>
                  <h3 id="next-activity-title">{data.progress.nextActivity.title}</h3>
                  <p>{data.progress.nextActivity.reason}</p>
                  {data.progress.nextActivity.competencyTag && (
                    <p>Focus competency: {formatLabel(data.progress.nextActivity.competencyTag)}</p>
                  )}
                </div>
                <Link
                  className="wizard-button wizard-button-primary"
                  to={nextActivityHref(data.profile.id, data.progress)}
                >
                  Open next activity
                  <span aria-hidden="true">→</span>
                </Link>
              </section>
            </section>
          </>
        )}
      </main>
    </div>
  )
}

function ProgressMetric({ label, value }: { label: string; value: number }) {
  return (
    <div className="progress-metric">
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  )
}

function ProgressCompetencyList({
  title,
  competencies,
  emptyMessage,
}: {
  title: string
  competencies: ProgressResponse['competenciesNeedingReview']
  emptyMessage: string
}) {
  return (
    <section className="progress-competency-group">
      <h3>{title}</h3>
      {competencies.length > 0 ? (
        <ul>
          {competencies.map((competency) => (
            <li key={competency.competencyTag}>{formatLabel(competency.competencyTag)}</li>
          ))}
        </ul>
      ) : (
        <p>{emptyMessage}</p>
      )}
    </section>
  )
}

function nextActivityHref(learnerId: string, progress: ProgressResponse): string {
  if (progress.nextActivity.type === 'RECALL') {
    const competencyTag = progress.nextActivity.competencyTag
    const tagQuery = competencyTag
      ? `?competencyTag=${encodeURIComponent(competencyTag)}`
      : ''
    return `/recall/${encodeURIComponent(learnerId)}${tagQuery}`
  }
  return `/lessons/${encodeURIComponent(progress.nextActivity.lessonId)}?learnerId=${encodeURIComponent(learnerId)}`
}

function formatLabel(value: string): string {
  return value.toLowerCase().replace(/[_-]/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

export default DashboardPage
