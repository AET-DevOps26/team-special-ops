import { execSync } from 'node:child_process'

async function globalSetup() {
  console.log('🚀 Starting Docker Compose stack...')

  try {
    execSync('docker compose -f ../infra/docker-compose.yml up -d --build', {
      cwd: process.cwd(),
      stdio: 'inherit',
    })

    // Wait for services to be healthy
    const maxRetries = 60
    let retries = 0
    const requiredEndpoints = [
      'http://localhost:8080',
      'http://localhost:8080/api/catalog/health',
      'http://localhost:8080/api/user-progress/health',
    ]

    console.log('⏳ Waiting for services to be healthy...')

    while (retries < maxRetries) {
      try {
        const results = await Promise.allSettled(
          requiredEndpoints.map((url) =>
            fetch(url, { signal: AbortSignal.timeout(5000) }).then((r) => {
              if (!r.ok) throw new Error(`${url} returned ${r.status}`)
              return true
            }),
          ),
        )

        if (results.every((r) => r.status === 'fulfilled')) {
          console.log('✅ All services are healthy')

          // 👇 Return the teardown function directly! Playwright will automatically
          // run this function after all tests have finished.
          return async () => {
            console.log('🧹 Stopping Docker Compose stack...')
            try {
              execSync('docker compose -f ../infra/docker-compose.yml down -v', {
                cwd: process.cwd(),
                stdio: 'inherit',
              })
              console.log('✅ Global teardown complete')
            } catch (error) {
              console.error('❌ Global teardown failed:', error)
            }
          }
        }
      } catch  {
        // Service not ready yet
      }

      await new Promise((resolve) => setTimeout(resolve, 2000))
      retries++

      if (retries % 5 === 0) {
        console.log(`⏳ Waiting... (${retries}/${maxRetries})`)
      }
    }

    throw new Error('Services failed to become healthy within timeout')
  } catch (error) {
    console.error('❌ Global setup failed:', error)
    // Dump container logs so an opaque startup failure (e.g. a service exiting
    // during `up`) is diagnosable from the CI log instead of just "exit 1".
    try {
      execSync('docker compose -f ../infra/docker-compose.yml logs --no-color --tail=80', {
        cwd: process.cwd(),
        stdio: 'inherit',
      })
    } catch {
      // best-effort; surface the original error regardless
    }
    throw error
  }
}

export default globalSetup
