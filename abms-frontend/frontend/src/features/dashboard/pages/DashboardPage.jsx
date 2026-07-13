import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/context/useAuth.js'
import { APP_ROUTES } from '../../../config/navigation.js'
import { ModuleCard } from '../../../components/ui/ModuleCard.jsx'

const dashboardCards = [
  {
    title: 'Occupancy rate',
    value: '92%',
    description: 'Tỷ lệ lấp đầy minh họa cho dashboard tổng quan của tòa nhà.',
  },
  {
    title: 'Monthly revenue',
    value: '1.28B VND',
    description: 'Tổng doanh thu tháng hiện tại, sẵn để nối billing/report API.',
  },
  {
    title: 'Open requests',
    value: '18 tickets',
    description: 'Số maintenance requests đang mở hoặc chờ xử lý.',
  },
]

const quickModules = [
  { title: 'Buildings', description: 'Quản trị danh mục tòa nhà, block và thông tin vận hành.', to: APP_ROUTES.buildings },
  { title: 'Apartments', description: 'Theo dõi căn hộ, cư dân, tình trạng sử dụng và occupancy.', to: APP_ROUTES.apartments },
  { title: 'Residents', description: 'Quản lý hồ sơ cư dân, hợp đồng, trạng thái duyệt và lịch sử.', to: APP_ROUTES.residents },
  { title: 'Billing', description: 'Invoice list, payment list, ghi nhận thanh toán và VietQR flow.', to: APP_ROUTES.invoices },
  { title: 'Maintenance', description: 'Submit request, assign staff, update status và completion evidence.', to: APP_ROUTES.maintenance },
  { title: 'Notifications', description: 'Notification center, announcement history và inbox theo role.', to: APP_ROUTES.notifications },
]

export function DashboardPage() {
  const { auth, isElevatedRole, roleLabel } = useAuth()

  return (
    <div className="page-stack">
      <section className="hero-panel">
        <div>
          <span className="eyebrow">Dashboard overview</span>
          <h1>Xin chào, {auth?.email}</h1>
          <p>
            Đây là base dashboard cho ứng dụng quản lý tòa nhà. Giao diện này được dựng để
            làm điểm vào thống nhất cho các module building, apartment, resident, finance,
            maintenance, vehicle và notifications theo role <strong>{roleLabel}</strong>.
          </p>
        </div>

        <div className="hero-panel__actions">
          <Link to={APP_ROUTES.vehicles} className="btn btn-primary">
            {isElevatedRole ? 'Mở bảng duyệt vận hành' : 'Mở không gian cá nhân'}
          </Link>
          <Link to={APP_ROUTES.reports} className="btn btn-secondary">
            Xem báo cáo
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
              <span className="eyebrow">Operational focus</span>
              <h3>Khu vực trọng tâm</h3>
            </div>
          </div>
          <ul className="feature-list">
            <li>Sidebar / header / footer thống nhất cho toàn ứng dụng</li>
            <li>Role-based navigation cho admin, manager, staff, technician, resident</li>
            <li>Giữ tương thích các flow đã nối API thật: auth, vehicle, resident approval</li>
            <li>Page shell sẵn sàng để tích hợp billing, maintenance, notifications, reports</li>
            <li>Dashboard theo phong cách building operations command center</li>
          </ul>
        </article>

        <article className="content-card">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Alerts panel</span>
              <h3>Thông báo vận hành mẫu</h3>
            </div>
          </div>
          <ul className="feature-list">
            <li>03 hóa đơn quá hạn cần xử lý trước 17:00 hôm nay</li>
            <li>02 hợp đồng cư trú sắp hết hạn trong 7 ngày tới</li>
            <li>01 yêu cầu maintenance mức Emergency đang chờ kỹ thuật viên</li>
            <li>05 đăng ký xe PENDING cần manager/staff xem xét</li>
          </ul>
        </article>
      </section>

      <section className="content-card">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Module shortcuts</span>
            <h3>Điều hướng nhanh tới các module</h3>
          </div>
        </div>

        <div className="module-grid">
          {quickModules.map((module) => (
            <ModuleCard key={module.title} {...module} cta="Open module" />
          ))}
        </div>
      </section>
    </div>
  )
}