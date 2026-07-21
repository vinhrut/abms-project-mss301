import { Outlet } from 'react-router-dom'

export function AuthLayout() {
  return (
    <div className="auth-shell auth-shell--sketch">
      <div className="auth-shell__content auth-shell__content--sketch">
        <section className="auth-shell__form auth-shell__form--sketch">
          <Outlet />
        </section>
      </div>
    </div>
  )
}