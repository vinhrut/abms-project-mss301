import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/context/useAuth.js'

const dashboardCards = [
  {
    title: 'Authentication',
    value: 'Ready',
    description: 'Đã hỗ trợ login, register, protected route và lưu phiên đăng nhập.',
  },
  {
    title: 'Vehicle workflow',
    value: 'Integrated',
    description: 'Có form đăng ký xe, danh sách xe theo vai trò, và action approve/reject.',
  },
  {
    title: 'Architecture',
    value: 'Domain-based',
    description: 'Cấu trúc chia theo feature để mở rộng thêm service/module dễ dàng.',
  },
]

export function DashboardPage() {
  const { auth, isElevatedRole } = useAuth()

  return (
    <div className="page-stack">
      <section className="hero-panel">
        <div>
          <span className="eyebrow">Dashboard overview</span>
          <h1>Xin chào, {auth?.email}</h1>
          <p>
            Đây là điểm vào sau đăng nhập để nối các luồng quan trọng của release hiện tại:
            đăng ký xe, xem xe và duyệt xe theo role.
          </p>
        </div>

        <div className="hero-panel__actions">
          <Link to="/vehicles/register" className="btn btn-primary">
            Đăng ký xe mới
          </Link>
          <Link to="/vehicles" className="btn btn-secondary">
            {isElevatedRole ? 'Duyệt yêu cầu xe' : 'Xem danh sách xe'}
          </Link>
        </div>
      </section>

      <section className="stats-grid">
        {dashboardCards.map((card) => (
          <article key={card.title} className="stat-card">
            <span className="stat-card__title">{card.title}</span>
            <strong>{card.value}</strong>
            <p>{card.description}</p>
          </article>
        ))}
      </section>

      <section className="content-grid">
        <article className="content-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Current scope</span>
              <h3>Phạm vi đã triển khai</h3>
            </div>
          </div>
          <ul className="feature-list">
            <li>Login / Register theo auth API contract</li>
            <li>Bearer token injection cho request đã xác thực</li>
            <li>Vehicle registration cho Resident</li>
            <li>Vehicle list tự tải theo owner với Resident</li>
            <li>Vehicle approval / rejection cho Manager và Staff</li>
            <li>Role-based navigation và redirect sau đăng nhập</li>
          </ul>
        </article>

        <article className="content-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Design note</span>
              <h3>Hướng microservice-friendly</h3>
            </div>
          </div>
          <p>
            Mỗi feature chính được tách theo domain (`auth`, `vehicles`, `dashboard`),
            kết hợp với service layer độc lập để map API từng microservice dễ hơn trong các phase tiếp theo.
          </p>
        </article>
      </section>
    </div>
  )
}