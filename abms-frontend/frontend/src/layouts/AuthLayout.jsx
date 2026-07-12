import { Outlet } from 'react-router-dom'

export function AuthLayout() {
  return (
    <div className="auth-shell">
      <div className="auth-shell__content">
        <section className="auth-shell__brand">
          <span className="eyebrow">ABMS</span>
          <h1>Apartment Building Management System</h1>
          <p>
            Giao diện quản lý căn hộ theo hướng đơn giản, rõ ràng và dễ mở rộng cho
            kiến trúc microservice.
          </p>

          <div className="feature-points">
            <div>
              <strong>Authentication</strong>
              <span>Đăng nhập, đăng ký, lưu phiên và phân quyền giao diện.</span>
            </div>
            <div>
              <strong>Vehicle workflow</strong>
              <span>Đăng ký xe, duyệt yêu cầu, theo dõi trạng thái PENDING.</span>
            </div>
            <div>
              <strong>Maintainable UI</strong>
              <span>Cấu trúc tách domain rõ ràng để mở rộng thêm service sau này.</span>
            </div>
          </div>
        </section>

        <section className="auth-shell__form">
          <Outlet />
        </section>
      </div>
    </div>
  )
}