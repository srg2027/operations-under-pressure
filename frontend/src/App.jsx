import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'react-hot-toast'
import { SessionProvider } from './context/SessionContext'
import { Navbar } from './components/layout/Navbar'
import { SessionSelectPage } from './pages/SessionSelectPage'
import { DashboardPage } from './pages/DashboardPage'
import { WorkItemsPage } from './pages/WorkItemsPage'
import { WorkItemDetailPage } from './pages/WorkItemDetailPage'
import { TeamsPage } from './pages/TeamsPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5000,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SessionProvider>
        <BrowserRouter>
          <div className="min-h-screen bg-slate-100/70 text-slate-900 flex flex-col font-sans antialiased">
            <Toaster
              position="top-right"
              toastOptions={{
                duration: 4000,
                style: {
                  fontSize: '12px',
                  borderRadius: '10px',
                  background: '#1e293b',
                  color: '#f8fafc',
                  padding: '10px 14px',
                },
                success: {
                  iconTheme: {
                    primary: '#10b981',
                    secondary: '#ffffff',
                  },
                },
                error: {
                  iconTheme: {
                    primary: '#ef4444',
                    secondary: '#ffffff',
                  },
                },
              }}
            />

            <Navbar />

            <main className="flex-1">
              <Routes>
                <Route path="/" element={<SessionSelectPage />} />
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/work-items" element={<WorkItemsPage />} />
                <Route path="/work-items/:id" element={<WorkItemDetailPage />} />
                <Route path="/teams" element={<TeamsPage />} />
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
              </Routes>
            </main>

            <footer className="border-t border-slate-200 bg-white py-4 px-6 text-center text-xs text-slate-400">
              <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2">
                <span>
                  <strong>Newtonite</strong> Operations Under Pressure — Full-Stack Verification
                </span>
                <span className="text-[11px] text-slate-400">
                  Backend API: <code className="bg-slate-100 px-1 py-0.5 rounded text-indigo-600 font-mono">/api</code> · Live State Active
                </span>
              </div>
            </footer>
          </div>
        </BrowserRouter>
      </SessionProvider>
    </QueryClientProvider>
  )
}
