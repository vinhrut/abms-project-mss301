export function PageIntro({ eyebrow, title, description, actions }) {
  return (
    <section className="page-header-card">
      <div className="page-header-split">
        <div>
          {eyebrow ? <span className="eyebrow">{eyebrow}</span> : null}
          <h1>{title}</h1>
          {description ? <p>{description}</p> : null}
        </div>
        {actions ? <div className="hero-panel__actions">{actions}</div> : null}
      </div>
    </section>
  )
}