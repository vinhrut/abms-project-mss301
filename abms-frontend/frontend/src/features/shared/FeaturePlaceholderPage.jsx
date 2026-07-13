import { ModuleCard } from '../../components/ui/ModuleCard.jsx'
import { PageIntro } from '../../components/ui/PageIntro.jsx'

export function FeaturePlaceholderPage({ eyebrow, title, description, highlights = [] }) {
  return (
    <div className="page-stack">
      <PageIntro eyebrow={eyebrow} title={title} description={description} />

      <section className="content-card">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Base UI ready</span>
            <h3>Khung giao diện đã sẵn sàng để tích hợp API</h3>
          </div>
        </div>

        <div className="module-grid">
          {highlights.map((item) => (
            <ModuleCard key={item.title} title={item.title} description={item.description} meta={item.meta} />
          ))}
        </div>
      </section>
    </div>
  )
}