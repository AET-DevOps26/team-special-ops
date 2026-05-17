import { useAuth } from '../context/AuthContext'

export function HomePage() {
  const { user, logout } = useAuth()

  return (
    <main className="min-h-screen flex items-center justify-center bg-slate-50">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-slate-900">TV Q&amp;A</h1>
        <p className="mt-2 text-slate-600">
          Logged in as <span className="font-medium">{user?.email}</span>
        </p>
        <button
          type="button"
          onClick={logout}
          className="mt-6 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          Log out
        </button>
      </div>
    </main>
  )
}
