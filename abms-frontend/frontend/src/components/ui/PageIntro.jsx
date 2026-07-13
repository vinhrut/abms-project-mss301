export function PageIntro({ eyebrow, title, description, actions }) {
  return (
    <section className="page-header-card">
      <div className="page-header-split">
        <div>
          <span className="eyebrow">{eyebrow}</span>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
        {actions ? <div className="hero-panel__actions">{actions}</div> : null}
      </div>
    </section>
  )
}