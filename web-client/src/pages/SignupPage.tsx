import { useNavigate } from 'react-router-dom'
import { AuthForm } from '../components/AuthForm'
import { AuthLayout } from '../components/AuthLayout'
import { useAuth } from '../context/AuthContext'

export function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()

  return (
    <AuthLayout
      title="Create account"
      alternateLabel="Already have an account? Sign in"
      alternateTo="/login"
    >
      <AuthForm
        submitLabel="Sign up"
        onSubmit={async (email, password) => {
          await signup({ email, password })
          navigate('/')
        }}
      />
    </AuthLayout>
  )
}
