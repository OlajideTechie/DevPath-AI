import { useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  createLearner,
  getLearner,
  LearnerApiError,
  studyDays,
  type CreateLearnerRequest,
  type LearnerCreatedResponse,
  type LearnerProfileResponse,
  type LearningStyle,
  type ProgrammingLevel,
  type StudyDay,
} from '../api/learnerApi'
import './OnboardingPage.css'

const steps = ['Background', 'Java goal', 'Availability', 'Review'] as const

interface FormValues {
  currentRole: string
  yearsOfExperience: string
  specialization: string
  skills: string[]
  skillInput: string
  programmingLevel: ProgrammingLevel | ''
  selectedDays: StudyDay[]
  dailyMinutes: string
  learningStyle: LearningStyle | ''
}

type FieldErrors = Record<string, string>

const initialValues: FormValues = {
  currentRole: '',
  yearsOfExperience: '',
  specialization: '',
  skills: [],
  skillInput: '',
  programmingLevel: '',
  selectedDays: [],
  dailyMinutes: '30',
  learningStyle: '',
}

const programmingLevels: { value: ProgrammingLevel; label: string; hint: string }[] = [
  { value: 'BEGINNER', label: 'Beginner', hint: 'I am new to programming' },
  { value: 'INTERMEDIATE', label: 'Intermediate', hint: 'I know some programming basics' },
  { value: 'EXPERIENCED', label: 'Experienced', hint: 'I have built software before' },
]

const learningStyles: { value: LearningStyle; label: string; hint: string }[] = [
  { value: 'PRACTICAL', label: 'Hands-on', hint: 'Learn by trying things' },
  { value: 'READING', label: 'Reading', hint: 'Learn through written explanations' },
  { value: 'MIXED', label: 'A mix of both', hint: 'Combine reading and practice' },
]

function validateStep(step: number, values: FormValues): FieldErrors {
  const errors: FieldErrors = {}
  if (step === 0) {
    if (!values.currentRole.trim()) errors.currentRole = 'Enter your current role.'
    else if (values.currentRole.trim().length > 100) {
      errors.currentRole = 'Use 100 characters or fewer.'
    }

    const years = Number(values.yearsOfExperience)
    if (values.yearsOfExperience === '') {
      errors.yearsOfExperience = 'Enter your years of experience.'
    } else if (!Number.isInteger(years) || years < 0 || years > 50) {
      errors.yearsOfExperience = 'Enter a whole number from 0 to 50.'
    }

    if (!values.specialization.trim()) {
      errors.specialization = 'Enter your QA specialization.'
    } else if (values.specialization.trim().length > 100) {
      errors.specialization = 'Use 100 characters or fewer.'
    }
    if (values.skills.length > 20) errors.skills = 'Add no more than 20 skills.'
  }
  if (step === 1 && !values.programmingLevel) {
    errors.programmingLevel = 'Choose the option that best describes your experience.'
  }
  if (step === 2) {
    if (values.selectedDays.length === 0) {
      errors.selectedDays = 'Choose at least one study day.'
    }
    const minutes = Number(values.dailyMinutes)
    if (
      values.dailyMinutes === '' ||
      !Number.isInteger(minutes) ||
      minutes < 10 ||
      minutes > 240
    ) {
      errors.dailyMinutes = 'Enter a whole number from 10 to 240 minutes.'
    }
    if (!values.learningStyle) errors.learningStyle = 'Choose a learning style.'
  }
  return errors
}

function toRequest(values: FormValues): CreateLearnerRequest {
  return {
    currentRole: values.currentRole.trim(),
    yearsOfExperience: Number(values.yearsOfExperience),
    specialization: values.specialization.trim(),
    existingSkills: values.skills,
    programmingLevel: values.programmingLevel as ProgrammingLevel,
    studyDays: values.selectedDays,
    dailyMinutes: Number(values.dailyMinutes),
    learningStyle: values.learningStyle as LearningStyle,
  }
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return 'Something went wrong. Please try again.'
}

function OnboardingPage() {
  const [values, setValues] = useState<FormValues>(initialValues)
  const [step, setStep] = useState(0)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [createdLearner, setCreatedLearner] = useState<LearnerCreatedResponse | null>(null)
  const [savedProfile, setSavedProfile] = useState<LearnerProfileResponse | null>(null)
  const [profileError, setProfileError] = useState('')
  const [isLoadingProfile, setIsLoadingProfile] = useState(false)
  const submittingRef = useRef(false)

  function updateValue<K extends keyof FormValues>(field: K, value: FormValues[K]) {
    setValues((current) => ({ ...current, [field]: value }))
    setErrors((current) => {
      const next = { ...current }
      delete next[field]
      return next
    })
    setSubmitError('')
  }

  function addSkill(event?: FormEvent | KeyboardEvent<HTMLInputElement>) {
    event?.preventDefault()
    const skill = values.skillInput.trim()
    if (!skill) return
    if (values.skills.some((current) => current.toLowerCase() === skill.toLowerCase())) {
      setErrors((current) => ({ ...current, skills: 'That skill is already on your list.' }))
      return
    }
    if (values.skills.length >= 20) {
      setErrors((current) => ({ ...current, skills: 'Add no more than 20 skills.' }))
      return
    }
    setValues((current) => ({
      ...current,
      skills: [...current.skills, skill],
      skillInput: '',
    }))
    setErrors((current) => {
      const next = { ...current }
      delete next.skills
      return next
    })
  }

  function removeSkill(skillToRemove: string) {
    updateValue('skills', values.skills.filter((skill) => skill !== skillToRemove))
  }

  function toggleDay(day: StudyDay) {
    const selectedDays = values.selectedDays.includes(day)
      ? values.selectedDays.filter((selectedDay) => selectedDay !== day)
      : [...values.selectedDays, day]
    updateValue('selectedDays', selectedDays)
  }

  function continueStep() {
    const stepErrors = validateStep(step, values)
    setErrors(stepErrors)
    if (Object.keys(stepErrors).length === 0) {
      setStep((current) => Math.min(current + 1, steps.length - 1))
      setSubmitError('')
    }
  }

  async function submitProfile() {
    if (submittingRef.current) return
    const allErrors = [0, 1, 2].reduce<FieldErrors>(
      (collected, stepIndex) => ({ ...collected, ...validateStep(stepIndex, values) }),
      {},
    )
    setErrors(allErrors)
    if (Object.keys(allErrors).length > 0) {
      const firstInvalidStep = [0, 1, 2].find(
        (stepIndex) => Object.keys(validateStep(stepIndex, values)).length > 0,
      )
      if (firstInvalidStep !== undefined) setStep(firstInvalidStep)
      return
    }

    submittingRef.current = true
    setIsSubmitting(true)
    setSubmitError('')
    try {
      const learner = await createLearner(toRequest(values))
      setCreatedLearner(learner)
      setSavedProfile(null)
      setProfileError('')
    } catch (error) {
      if (error instanceof LearnerApiError) {
        setErrors((current) => ({ ...current, ...error.fieldErrors }))
      }
      setSubmitError(getErrorMessage(error))
    } finally {
      submittingRef.current = false
      setIsSubmitting(false)
    }
  }

  async function loadSavedProfile() {
    if (!createdLearner || isLoadingProfile) return
    setIsLoadingProfile(true)
    setProfileError('')
    try {
      const profile = await getLearner(createdLearner.id)
      setSavedProfile(profile)
    } catch (error) {
      setProfileError(getErrorMessage(error))
    } finally {
      setIsLoadingProfile(false)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (step < steps.length - 1) {
      continueStep()
      return
    }
    void submitProfile()
  }

  return (
    <div className="onboarding-shell">
      <header className="onboarding-header">
        <Link className="onboarding-brand" to="/" aria-label="DevPath AI home">
          <span className="onboarding-brand-mark" aria-hidden="true">D</span>
          <span>DevPath <span>AI</span></span>
        </Link>
        <span className="demo-label">Early release demo</span>
      </header>

      <main className="onboarding-main">
        <div className="onboarding-intro">
          <p className="onboarding-eyebrow">YOUR NEXT CHAPTER STARTS HERE</p>
          <h1>Let’s find your starting point.</h1>
          <p>Tell us a little about your experience and how you like to learn.</p>
        </div>

        {createdLearner ? (
          <section className="onboarding-card success-card" aria-labelledby="success-title">
            <div className="success-mark" aria-hidden="true">✓</div>
            <p className="onboarding-eyebrow">PROFILE SAVED</p>
            <h2 id="success-title">Your profile is saved.</h2>
            <p className="success-description">
              Your onboarding details have been saved. You can retrieve the saved
              profile from the server below.
            </p>
            <button
              className="wizard-button wizard-button-primary"
              type="button"
              onClick={() => void loadSavedProfile()}
              disabled={isLoadingProfile}
            >
              {isLoadingProfile ? 'Loading saved profile…' : 'View saved profile'}
            </button>
            {profileError && (
              <div className="form-error-banner" role="alert">
                <p>{profileError}</p>
                <button type="button" className="text-button" onClick={() => void loadSavedProfile()}>
                  Try again
                </button>
              </div>
            )}
            {savedProfile && <SavedProfile profile={savedProfile} />}
          </section>
        ) : (
          <>
            <nav className="step-navigation" aria-label="Onboarding steps">
              {steps.map((label, index) => (
                <div
                  className={`step-indicator${index === step ? ' is-current' : ''}${index < step ? ' is-complete' : ''}`}
                  key={label}
                  aria-current={index === step ? 'step' : undefined}
                >
                  <span className="step-number" aria-hidden="true">
                    {index < step ? '✓' : index + 1}
                  </span>
                  <span>{label}</span>
                </div>
              ))}
            </nav>

            <form className="onboarding-card" onSubmit={handleSubmit} noValidate>
              <div className="step-heading">
                <p className="step-count">STEP {step + 1} OF {steps.length}</p>
                <h2>{steps[step]}</h2>
                <p>{getStepDescription(step)}</p>
              </div>

              {step === 0 && (
                <BackgroundStep
                  values={values}
                  errors={errors}
                  onChange={updateValue}
                  onAddSkill={addSkill}
                  onRemoveSkill={removeSkill}
                />
              )}
              {step === 1 && (
                <GoalStep
                  value={values.programmingLevel}
                  error={errors.programmingLevel}
                  onChange={(value) => updateValue('programmingLevel', value)}
                />
              )}
              {step === 2 && (
                <AvailabilityStep
                  values={values}
                  errors={errors}
                  onChange={updateValue}
                  onToggleDay={toggleDay}
                />
              )}
              {step === 3 && (
                <ReviewStep values={values} errors={errors} onEdit={setStep} />
              )}

              {submitError && (
                <div className="form-error-banner" role="alert">
                  <p>{submitError}</p>
                  <p>Your details are still here. You can retry when you’re ready.</p>
                </div>
              )}

              <div className="wizard-actions">
                {step > 0 ? (
                  <button
                    className="wizard-button wizard-button-secondary"
                    type="button"
                    onClick={() => setStep((current) => current - 1)}
                    disabled={isSubmitting}
                  >
                    Back
                  </button>
                ) : (
                  <Link className="wizard-button wizard-button-secondary" to="/">
                    Back to home
                  </Link>
                )}
                {step < steps.length - 1 ? (
                  <button
                    className="wizard-button wizard-button-primary"
                    type="button"
                    onClick={continueStep}
                  >
                    Continue
                    <span aria-hidden="true">→</span>
                  </button>
                ) : (
                  <button
                    className="wizard-button wizard-button-primary"
                    type="submit"
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? 'Saving profile…' : 'Save profile'}
                    {!isSubmitting && <span aria-hidden="true">→</span>}
                  </button>
                )}
              </div>
            </form>
            <p className="privacy-note">
              This demo stores only the profile details you provide. Please use
              synthetic information; account sign-in is not part of this release.
            </p>
          </>
        )}
      </main>
    </div>
  )
}

function getStepDescription(step: number): string {
  switch (step) {
    case 0:
      return 'We’ll use your QA background as the foundation.'
    case 1:
      return 'Java is your destination. Tell us where you are starting.'
    case 2:
      return 'Choose a realistic rhythm that works for you.'
    default:
      return 'Check your details before saving your profile.'
  }
}

interface BackgroundStepProps {
  values: FormValues
  errors: FieldErrors
  onChange: <K extends keyof FormValues>(field: K, value: FormValues[K]) => void
  onAddSkill: (event?: FormEvent | KeyboardEvent<HTMLInputElement>) => void
  onRemoveSkill: (skill: string) => void
}

function BackgroundStep({ values, errors, onChange, onAddSkill, onRemoveSkill }: BackgroundStepProps) {
  return (
    <div className="wizard-fields">
      <TextField
        id="currentRole"
        label="Current role"
        placeholder="e.g. QA Engineer"
        value={values.currentRole}
        error={errors.currentRole}
        onChange={(value) => onChange('currentRole', value)}
      />
      <TextField
        id="yearsOfExperience"
        label="Years of experience"
        type="number"
        min={0}
        max={50}
        placeholder="e.g. 5"
        value={values.yearsOfExperience}
        error={errors.yearsOfExperience}
        onChange={(value) => onChange('yearsOfExperience', value)}
      />
      <TextField
        id="specialization"
        label="QA specialization"
        placeholder="e.g. API Testing"
        value={values.specialization}
        error={errors.specialization}
        onChange={(value) => onChange('specialization', value)}
      />
      <div className="field-group">
        <label className="field-label" htmlFor="skillInput">Skills you already use <span>(optional)</span></label>
        <p className="field-hint">Add up to 20 skills. Press Enter or select Add skill.</p>
        <div className="skill-entry">
          <input
            id="skillInput"
            value={values.skillInput}
            maxLength={100}
            onChange={(event) => onChange('skillInput', event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') onAddSkill(event)
            }}
            aria-describedby={errors.skills ? 'skills-error' : undefined}
          />
          <button type="button" className="add-skill-button" onClick={() => onAddSkill()}>
            Add skill
          </button>
        </div>
        {errors.skills && <FieldError id="skills-error" message={errors.skills} />}
        {values.skills.length > 0 && (
          <ul className="skill-list" aria-label="Added skills">
            {values.skills.map((skill) => (
              <li key={skill}>
                <span>{skill}</span>
                <button
                  type="button"
                  aria-label={`Remove ${skill}`}
                  onClick={() => onRemoveSkill(skill)}
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

interface GoalStepProps {
  value: ProgrammingLevel | ''
  error?: string
  onChange: (value: ProgrammingLevel) => void
}

function GoalStep({ value, error, onChange }: GoalStepProps) {
  return (
    <div className="wizard-fields">
      <div className="fixed-goal">
        <span className="fixed-goal-icon" aria-hidden="true">↗</span>
        <span>
          <span className="fixed-goal-label">YOUR GOAL</span>
          <strong>Java Backend Developer</strong>
        </span>
        <span className="fixed-goal-lock" aria-label="Goal is fixed for this demo">Fixed</span>
      </div>
      <fieldset className="choice-fieldset" aria-describedby={error ? 'programmingLevel-error' : undefined}>
        <legend className="field-label">How would you describe your programming experience?</legend>
        <div className="choice-list">
          {programmingLevels.map((level) => (
            <label className={`choice-card${value === level.value ? ' is-selected' : ''}`} key={level.value}>
              <input
                type="radio"
                name="programmingLevel"
                value={level.value}
                checked={value === level.value}
                onChange={() => onChange(level.value)}
              />
              <span className="custom-radio" aria-hidden="true" />
              <span className="choice-copy">
                <strong>{level.label}</strong>
                <span>{level.hint}</span>
              </span>
            </label>
          ))}
        </div>
        {error && <FieldError id="programmingLevel-error" message={error} />}
      </fieldset>
    </div>
  )
}

interface AvailabilityStepProps {
  values: FormValues
  errors: FieldErrors
  onChange: <K extends keyof FormValues>(field: K, value: FormValues[K]) => void
  onToggleDay: (day: StudyDay) => void
}

function AvailabilityStep({ values, errors, onChange, onToggleDay }: AvailabilityStepProps) {
  return (
    <div className="wizard-fields">
      <fieldset
        className="choice-fieldset"
        aria-describedby={errors.selectedDays ? 'selectedDays-error' : undefined}
      >
        <legend className="field-label">Which days work for you?</legend>
        <p className="field-hint">Select all the days you could usually study.</p>
        <div className="day-grid">
          {studyDays.map((day) => (
            <label className={`day-choice${values.selectedDays.includes(day) ? ' is-selected' : ''}`} key={day}>
              <input
                type="checkbox"
                checked={values.selectedDays.includes(day)}
                onChange={() => onToggleDay(day)}
              />
              <span aria-hidden="true">{day.slice(0, 1) + day.slice(1, 2).toLowerCase()}</span>
              <span className="sr-only">{day.charAt(0) + day.slice(1).toLowerCase()}</span>
            </label>
          ))}
        </div>
        {errors.selectedDays && <FieldError id="selectedDays-error" message={errors.selectedDays} />}
      </fieldset>
      <TextField
        id="dailyMinutes"
        label="Minutes you can study per day"
        type="number"
        min={10}
        max={240}
        value={values.dailyMinutes}
        error={errors.dailyMinutes}
        onChange={(value) => onChange('dailyMinutes', value)}
        hint="Choose between 10 and 240 minutes."
      />
      <fieldset
        className="choice-fieldset"
        aria-describedby={errors.learningStyle ? 'learningStyle-error' : undefined}
      >
        <legend className="field-label">How do you prefer to learn?</legend>
        <div className="style-grid">
          {learningStyles.map((style) => (
            <label className={`style-choice${values.learningStyle === style.value ? ' is-selected' : ''}`} key={style.value}>
              <input
                type="radio"
                name="learningStyle"
                value={style.value}
                checked={values.learningStyle === style.value}
                onChange={() => onChange('learningStyle', style.value)}
              />
              <strong>{style.label}</strong>
              <span>{style.hint}</span>
            </label>
          ))}
        </div>
        {errors.learningStyle && <FieldError id="learningStyle-error" message={errors.learningStyle} />}
      </fieldset>
    </div>
  )
}

interface ReviewStepProps {
  values: FormValues
  errors: FieldErrors
  onEdit: (step: number) => void
}

function ReviewStep({ values, errors, onEdit }: ReviewStepProps) {
  return (
    <div className="review-list">
      {Object.keys(errors).length > 0 && (
        <div className="inline-error" role="alert">
          Review the highlighted information before saving.
        </div>
      )}
      <ReviewGroup title="Your background" step={0} onEdit={onEdit}>
        <ReviewRow label="Current role" value={values.currentRole || 'Not provided'} />
        <ReviewRow label="Experience" value={`${values.yearsOfExperience || '—'} years`} />
        <ReviewRow label="Specialization" value={values.specialization || 'Not provided'} />
        <ReviewRow label="Existing skills" value={values.skills.join(', ') || 'None added'} />
      </ReviewGroup>
      <ReviewGroup title="Your Java goal" step={1} onEdit={onEdit}>
        <ReviewRow label="Target role" value="Java Backend Developer" />
        <ReviewRow
          label="Programming experience"
          value={programmingLevels.find((level) => level.value === values.programmingLevel)?.label ?? 'Not selected'}
        />
      </ReviewGroup>
      <ReviewGroup title="Your schedule" step={2} onEdit={onEdit}>
        <ReviewRow
          label="Study days"
          value={values.selectedDays.map(formatStudyDay).join(', ') || 'Not selected'}
        />
        <ReviewRow label="Daily time" value={`${values.dailyMinutes || '—'} minutes`} />
        <ReviewRow
          label="Learning style"
          value={learningStyles.find((style) => style.value === values.learningStyle)?.label ?? 'Not selected'}
        />
      </ReviewGroup>
    </div>
  )
}

function ReviewGroup({
  title,
  step,
  onEdit,
  children,
}: {
  title: string
  step: number
  onEdit: (step: number) => void
  children: React.ReactNode
}) {
  return (
    <section className="review-group" aria-labelledby={`review-title-${step}`}>
      <div className="review-group-heading">
        <h3 id={`review-title-${step}`}>{title}</h3>
        <button type="button" className="text-button" onClick={() => onEdit(step)}>
          Edit
        </button>
      </div>
      <dl>{children}</dl>
    </section>
  )
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="review-row">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

function SavedProfile({ profile }: { profile: LearnerProfileResponse }) {
  return (
    <section className="saved-profile" aria-labelledby="saved-profile-title">
      <h3 id="saved-profile-title">Retrieved profile</h3>
      <dl>
        <ReviewRow label="Current role" value={profile.currentRole} />
        <ReviewRow label="Target role" value={profile.targetRole} />
        <ReviewRow label="Experience" value={`${profile.yearsOfExperience} years`} />
        <ReviewRow label="Specialization" value={profile.specialization} />
        <ReviewRow label="Skills" value={profile.existingSkills.join(', ') || 'None'} />
        <ReviewRow label="Study days" value={profile.studyDays.map(formatStudyDay).join(', ')} />
        <ReviewRow label="Daily time" value={`${profile.dailyMinutes} minutes`} />
        <ReviewRow label="Learning style" value={formatEnum(profile.learningStyle)} />
        <ReviewRow label="Programming level" value={formatEnum(profile.programmingLevel)} />
      </dl>
    </section>
  )
}

function TextField({
  id,
  label,
  value,
  onChange,
  error,
  hint,
  ...inputProps
}: {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  hint?: string
  type?: string
  min?: number
  max?: number
  placeholder?: string
}) {
  const descriptionIds = [hint ? `${id}-hint` : '', error ? `${id}-error` : '']
    .filter(Boolean)
    .join(' ') || undefined
  return (
    <div className="field-group">
      <label className="field-label" htmlFor={id}>{label}</label>
      {hint && <p className="field-hint" id={`${id}-hint`}>{hint}</p>}
      <input
        className={`text-input${error ? ' has-error' : ''}`}
        id={id}
        name={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={Boolean(error)}
        aria-describedby={descriptionIds}
        {...inputProps}
      />
      {error && <FieldError id={`${id}-error`} message={error} />}
    </div>
  )
}

function FieldError({ id, message }: { id: string; message: string }) {
  return <p className="field-error" id={id}>{message}</p>
}

function formatStudyDay(day: StudyDay): string {
  return day.charAt(0) + day.slice(1).toLowerCase()
}

function formatEnum(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export default OnboardingPage
