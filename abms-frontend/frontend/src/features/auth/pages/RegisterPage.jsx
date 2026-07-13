export function RegisterPage() {
  return (
    <div className="auth-card">
      <div className="auth-card__header">
        <span className="eyebrow">Account policy</span>
        <h2>Tài khoản không đăng ký công khai</h2>
        <p>ABMS hiện áp dụng mô hình cấp tài khoản nội bộ: Admin cấp Manager, Manager cấp Resident/Staff/Technician.</p>
      </div>

      <div className="form-grid">
        <div className="auth-inline-note">
          <strong>Ai được cấp tài khoản?</strong>
          <span>
            Admin cấp tài khoản cho Manager. Manager cấp tài khoản cho Resident, Staff và Technician.
          </span>
        </div>

        <div className="auth-inline-note">
          <strong>Nếu bạn chưa có tài khoản</strong>
          <span>Vui lòng liên hệ Admin hệ thống hoặc Ban quản lý tòa nhà để được cấp quyền truy cập.</span>
        </div>
      </div>
    </div>
  )
}