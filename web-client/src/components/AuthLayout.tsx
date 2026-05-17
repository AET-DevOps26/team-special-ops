import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'

interface AuthLayoutProps {
  title: string
  alternateLabel: string
  alternateTo: string
  children: ReactNode
}

export function AuthLayout({
  title,
  alternateLabel,
  alternateTo,
  children,
}: AuthLayoutProps) {
  return (
    <main className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-2xl font-bold text-slate-900 text-center">{title}</h1>
        {children}
        <p className="mt-6 text-center text-sm text-slate-600">
          <Link
            to={alternateTo}
            className="font-medium text-slate-900 hover:underline"
          >
            {alternateLabel}
          </Link>
        </p>
      </div>
    </main>
  )
}
