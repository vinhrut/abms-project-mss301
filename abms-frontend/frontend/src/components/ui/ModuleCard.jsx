import { Link } from 'react-router-dom'

export function ModuleCard({ title, description, cta, to, meta }) {
  return (
    <article className="module-card">
      <div className="module-card__body">
        <h3>{title}</h3>
        <p>{description}</p>
        {meta ? <small>{meta}</small> : null}
      </div>
      {to ? <Link to={to} className="btn btn-secondary">{cta || 'Open'}</Link> : null}
    </article>
  )
}