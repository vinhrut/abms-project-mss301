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
              <strong>Role-based workspace</strong>
              <span>Điều hướng khác nhau cho admin, manager, staff, technician và resident.</span>
            </div>
            <div>
              <strong>Operations-ready base UI</strong>
              <span>Khung sẵn cho building, apartment, billing, maintenance, notification và reports.</span>
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