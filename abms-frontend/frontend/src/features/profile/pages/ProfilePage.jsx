import { PageIntro } from '../../../components/ui/PageIntro.jsx'
import { useAuth } from '../../auth/context/useAuth.js'

export function ProfilePage() {
  const { auth, roleLabel } = useAuth()

  return (
    <div className="page-stack">
      <PageIntro
        eyebrow="Account management"
        title="User Profile"
        description="Thông tin hồ sơ người dùng đăng nhập hiện tại, phục vụ cho luồng cập nhật profile và đổi mật khẩu ở phase tiếp theo."
      />

      <section className="content-card">
        <div className="info-grid">
          <article className="info-card">
            <strong>Email</strong>
            <p>{auth?.email || 'N/A'}</p>
          </article>
          <article className="info-card">
            <strong>Role</strong>
            <p>{roleLabel}</p>
          </article>
          <article className="info-card">
            <strong>Token state</strong>
            <p>{auth?.token ? 'Authenticated session is active' : 'No token found'}</p>
          </article>
          <article className="info-card">
            <strong>Integration note</strong>
            <p>Giữ chỗ cho API user profile/update profile/change password.</p>
          </article>
        </div>
      </section>
    </div>
  )
}