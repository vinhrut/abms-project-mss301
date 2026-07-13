import { Link } from 'react-router-dom'

const highlights = [
  {
    title: 'Unified building operations UI',
    description:
      'Trang đầu vào giới thiệu rõ hệ thống và dẫn người dùng tới workspace phù hợp theo vai trò.',
  },
  {
    title: 'Role-based navigation',
    description:
      'Sidebar, header và dashboard thay đổi theo role để phù hợp vận hành thực tế của tòa nhà.',
  },
  {
    title: 'API-friendly module shells',
    description:
      'Các module billing, maintenance, reports và notifications đã có khung giao diện để nối backend sau.',
  },
]

const steps = [
  'Khách truy cập vào trang chủ để xem tổng quan hệ thống',
  'Người dùng đăng ký hoặc đăng nhập tài khoản',
  'Sau khi xác thực, hệ thống chuyển vào workspace và menu phù hợp theo role',
]

export function HomePage() {
  return (
    <main className="landing-page">
      <section className="landing-hero">
        <div className="landing-hero__content">
          <span className="eyebrow">ABMS Frontend</span>
          <h1>Hệ thống giao diện quản lý căn hộ và phương tiện</h1>
          <p>
            Base frontend cho Apartment Building Management System với giao diện quản trị
            đồng nhất, định hướng quản lý tòa nhà và sẵn sàng tích hợp thêm các service
            backend trong các phase tiếp theo.
          </p>

          <div className="landing-actions">
            <Link to="/login" className="btn btn-primary">
              Đăng nhập ngay
            </Link>
            <Link to="/app/dashboard" className="btn btn-ghost">
              Đi đến dashboard
            </Link>
          </div>
        </div>

        <div className="landing-hero__card">
          <span className="eyebrow">Core flow</span>
          <h3>Luồng sử dụng cơ bản</h3>
          <ol className="landing-step-list">
            {steps.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
        </div>
      </section>

      <section className="landing-grid">
        {highlights.map((item) => (
          <article key={item.title} className="landing-card">
            <h3>{item.title}</h3>
            <p>{item.description}</p>
          </article>
        ))}
      </section>
    </main>
  )
}