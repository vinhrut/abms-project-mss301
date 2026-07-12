import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <main className="not-found-page">
      <div className="not-found-card">
        <span className="eyebrow">404</span>
        <h1>Không tìm thấy trang</h1>
        <p>Trang bạn đang tìm không tồn tại hoặc đã được di chuyển.</p>
        <Link className="btn btn-primary" to="/">
          Quay về trang chủ
        </Link>
      </div>
    </main>
  )
}