import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import DashboardPage from './dashboard/DashboardPage'
import LessonLabPage from './lesson/LessonLabPage'
import OnboardingPage from './onboarding/OnboardingPage'
import RecallPage from './recall/RecallPage'

const benefits = [
  {
    number: '01',
    title: 'Build on what you know',
    description:
      'Connect your QA instincts to the systems, APIs, and testing practices behind backend engineering.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M4 18.5V5.5h16v13H4Z" />
        <path d="m8 10 2 2-2 2m4 0h4" />
      </svg>
    ),
  },
  {
    number: '02',
    title: 'Learn at your pace',
    description:
      'Shape a study rhythm around your real schedule, with small steps that fit alongside work and life.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="12" cy="12" r="8.25" />
        <path d="M12 7v5l3.5 2" />
      </svg>
    ),
  },
  {
    number: '03',
    title: 'Show what you can do',
    description:
      'Focus on understanding and practice so your next step is grounded in skills you can demonstrate.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M12 3.75 19 7v4.75c0 4.1-2.85 7.1-7 8.5-4.15-1.4-7-4.4-7-8.5V7l7-3.25Z" />
        <path d="m9 12 2 2 4-4" />
      </svg>
    ),
  },
]

function LandingPage() {
  return (
    <>
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>

      <div className="site-shell">
        <header className="site-header">
          <a className="brand" href="/" aria-label="DevPath AI home">
            <span className="brand-mark" aria-hidden="true">
              <svg viewBox="0 0 32 32" fill="none">
                <path d="M16 3.5 28 10.25v11.5L16 28.5 4 21.75v-11.5L16 3.5Z" />
                <path d="m11 16 3.2 3.2L21.5 12" />
              </svg>
            </span>
            <span>DevPath <span className="brand-ai">AI</span></span>
          </a>

          <nav className="header-nav" aria-label="Main navigation">
            <a href="#why-devpath">Why DevPath</a>
            <Link className="nav-cta" to="/onboarding">
              Start your journey
              <span aria-hidden="true">↗</span>
            </Link>
          </nav>
        </header>

        <main id="main-content">
          <section className="hero" aria-labelledby="hero-title">
            <div className="hero-copy">
              <p className="eyebrow">
                <span className="eyebrow-dot" aria-hidden="true" />
                A new direction, built on your experience
              </p>
              <h1 id="hero-title">
                Your QA Experience Is Your <span>Starting Point.</span>
              </h1>
              <p className="hero-description">
                Move into Java backend engineering with a learning journey
                that adapts to your skills, schedule, and progress.
              </p>
              <div className="hero-actions">
                <Link className="primary-cta" to="/onboarding">
                  Start Your Journey
                  <span className="cta-arrow" aria-hidden="true">→</span>
                </Link>
                <p className="cta-note">
                  Begin with your experience. Find your next step.
                </p>
              </div>
              <div className="demo-note">
                <span className="demo-icon" aria-hidden="true">✳</span>
                <span>Early release demo</span>
                <span className="demo-divider" aria-hidden="true" />
                <span>Onboarding foundation</span>
              </div>
            </div>

            <div className="hero-art" aria-label="Illustration connecting quality assurance to backend engineering">
              <div className="art-orbit art-orbit-one" />
              <div className="art-orbit art-orbit-two" />
              <div className="art-glow" />
              <div className="path-chip path-chip-top">
                <span className="chip-dot chip-dot-violet" />
                Quality mindset
              </div>
              <div className="path-chip path-chip-bottom">
                <span className="chip-dot chip-dot-green" />
                Java backend
              </div>

              <div className="journey-card">
                <div className="journey-card-top">
                  <div className="journey-avatar" aria-hidden="true">
                    <svg viewBox="0 0 40 40" fill="none">
                      <path d="M8 27.5 15.5 20l5 5L32 13.5" />
                      <path d="M24 13.5h8v8" />
                    </svg>
                  </div>
                  <span className="card-label">YOUR NEXT CHAPTER</span>
                  <span className="card-menu" aria-hidden="true">···</span>
                </div>
                <div className="journey-card-copy">
                  <span className="journey-overline">A path that starts with you</span>
                  <p>From quality assurance</p>
                  <div className="connector" aria-hidden="true">
                    <span />
                    <span />
                    <span />
                  </div>
                  <p className="journey-destination">To Java backend engineering</p>
                </div>
                <div className="journey-card-footer">
                  <span className="footer-icon" aria-hidden="true">✦</span>
                  <span>Made for your starting point</span>
                </div>
              </div>

              <div className="floating-symbol symbol-code" aria-hidden="true">
                <span>&lt;/&gt;</span>
              </div>
              <div className="floating-symbol symbol-check" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="m5 12 4.5 4.5L19 7" />
                </svg>
              </div>
              <div className="art-caption">
                <span className="caption-line" />
                Your experience is an advantage
              </div>
            </div>

            <div className="hero-index" aria-hidden="true">
              <span>01</span>
              <span className="index-rule" />
              <span>03</span>
            </div>
          </section>

          <section className="benefits-section" id="why-devpath" aria-labelledby="benefits-title">
            <div className="benefits-heading">
              <p className="section-kicker">A more considered way forward</p>
              <h2 id="benefits-title">Your next chapter starts with what you bring.</h2>
            </div>
            <div className="benefit-grid">
              {benefits.map((benefit) => (
                <article className="benefit-card" key={benefit.number}>
                  <div className="benefit-card-top">
                    <span className="benefit-icon">{benefit.icon}</span>
                    <span className="benefit-number">{benefit.number}</span>
                  </div>
                  <h3>{benefit.title}</h3>
                  <p>{benefit.description}</p>
                </article>
              ))}
            </div>
          </section>
        </main>

        <footer className="site-footer">
          <a className="footer-brand" href="/" aria-label="DevPath AI home">
            <span className="footer-brand-mark" aria-hidden="true">D</span>
            DevPath AI
          </a>
          <p>Built for the next step in your engineering journey.</p>
          <span className="footer-demo">Early release demo</span>
        </footer>
      </div>
    </>
  )
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/dashboard/:learnerId" element={<DashboardPage />} />
        <Route path="/lessons/:lessonId" element={<LessonLabPage />} />
        <Route path="/recall/:learnerId" element={<RecallPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
