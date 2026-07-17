import { Link } from 'react-router-dom'
import { useAuth } from '../context'

export function AppHeader() {
  const { user, logout } = useAuth()

  return (
    <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <div className="flex items-center gap-4">
          <Link to="/" className="text-lg font-bold tracking-tight">
            SceneIt
          </Link>
          <Link to="/" className="text-sm text-slate-600 hover:text-slate-900">
            Browse
          </Link>
          <Link to="/my-shows" className="text-sm text-slate-600 hover:text-slate-900">
            My Shows
          </Link>
        </div>
        <div className="flex items-center gap-3 text-sm">
          <span className="text-slate-500">{user?.email}</span>
          <button
            type="button"
            onClick={logout}
            className="rounded-md bg-slate-900 px-3 py-1.5 font-medium text-white hover:bg-slate-800"
          >
            Log out
          </button>
        </div>
      </div>
    </header>
  )
}
