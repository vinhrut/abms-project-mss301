import { Link } from 'react-router-dom'

const highlights = [
  {
    title: 'Trang giao diện rõ ràng',
    description:
      'Người dùng vào localhost sẽ thấy ngay landing page giới thiệu hệ thống thay vì bị chuyển sang màn hình trống.',
  },
  {
    title: 'Luồng xác thực đầy đủ',
    description:
      'Hỗ trợ điều hướng mượt mà giữa Home, Login và Sign up để bắt đầu sử dụng hệ thống nhanh hơn.',
  },
  {
    title: 'Quản lý phương tiện',
    description:
      'Cư dân có thể đăng ký xe và bộ phận quản lý có thể theo dõi, duyệt hoặc từ chối yêu cầu.',
  },
]

const steps = [
  'Khách truy cập vào trang chủ để xem tổng quan hệ thống',
  'Người dùng đăng ký hoặc đăng nhập tài khoản',
  'Sau khi xác thực, hệ thống chuyển đến dashboard và các màn hình vehicle',
]

export function HomePage() {
  return (
    <main className="landing-page">
      <section className="landing-hero">
        <div className="landing-hero__content">
          <span className="eyebrow">ABMS Frontend</span>
          <h1>Hệ thống giao diện quản lý căn hộ và phương tiện</h1>
          <p>
            Giao diện đầu vào cho Apartment Building Management System, tối ưu cho luồng
            từ trang chủ, đăng nhập, đăng ký đến quản lý vehicle trong một trải nghiệm
            nhất quán.
          </p>

          <div className="landing-actions">
            <Link to="/login" className="btn btn-primary">
              Đăng nhập ngay
            </Link>
            <Link to="/register" className="btn btn-secondary">
              Tạo tài khoản
            </Link>
            <Link to="/dashboard" className="btn btn-ghost">
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