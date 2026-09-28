import Link from 'next/link'
import { Button } from '@/components/ui/button'

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24 bg-gradient-to-br from-blue-50 to-indigo-50">
      <div className="text-center space-y-6 max-w-3xl">
        <h1 className="text-6xl font-bold text-gray-900">
          Clausify
        </h1>
        <p className="text-2xl text-gray-600">
          AI-Powered Contract Analysis for Freelancers
        </p>
        <p className="text-lg text-gray-500">
          Upload any contract and get instant AI analysis highlighting risky clauses,
          unfavorable terms, and negotiation strategies.
        </p>
        <div className="flex gap-4 justify-center pt-8">
          <Link href="/signup">
            <Button size="lg" className="text-lg px-8">
              Get Started
            </Button>
          </Link>
          <Link href="/login">
            <Button size="lg" variant="outline" className="text-lg px-8">
              Sign In
            </Button>
          </Link>
        </div>
      </div>
    </main>
  )
}