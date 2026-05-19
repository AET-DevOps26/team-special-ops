import { useNavigate } from 'react-router-dom'
import { AuthForm } from '../components/AuthForm'
import { AuthLayout } from '../components/AuthLayout'
import { useAuth } from '../context'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  return (
    <AuthLayout
      title="Sign in"
      alternateLabel="Create an account"
      alternateTo="/signup"
    >
      <AuthForm
        submitLabel="Sign in"
        onSubmit={async (email, password) => {
          await login({ email, password })
          navigate('/')
        }}
      />
    </AuthLayout>
  )
}
