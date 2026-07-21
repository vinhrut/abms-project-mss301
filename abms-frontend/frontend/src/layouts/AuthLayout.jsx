import { Outlet } from 'react-router-dom'

export function AuthLayout() {
  return (
    <div className="auth-shell">
      <div className="auth-shell__content">
        <section className="auth-shell__brand">
          <span className="eyebrow">ABMS</span>
          <h1>Hệ thống quản lý tòa nhà</h1>
          <p>
            Giao diện quản lý căn hộ, cư dân và vận hành tòa nhà theo từng vai trò.
          </p>

          <div className="feature-points">
            <div>
              <strong>Không gian theo vai trò</strong>
              <span>Điều hướng khác nhau cho admin, manager, staff, technician và resident.</span>
            </div>
            <div>
              <strong>Quản lý vận hành rõ ràng</strong>
              <span>Các phần tòa nhà, căn hộ, hóa đơn, bảo trì, xe và thông báo được tách gọn.</span>
            </div>
            <div>
              <strong>Dễ sử dụng</strong>
              <span>Thông tin quan trọng được trình bày ngắn gọn, hạn chế hiển thị dữ liệu kỹ thuật.</span>
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