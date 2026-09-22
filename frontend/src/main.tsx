import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './styles/index.css'
import './i18n'
import { initTheme } from './lib/theme'
import { AuthProvider } from './lib/auth'
import { ToastProvider } from './components/ui/Toast'
import { AppShell } from './app/AppShell'
import { RedirectIfAuthed, RequireAuth } from './app/guards'
import { LandingPage } from './features/landing/LandingPage'
import { LoginPage } from './features/auth/LoginPage'
import { RegisterPage } from './features/auth/RegisterPage'
import { DashboardPage } from './features/dashboard/DashboardPage'
import { PartnersPage } from './features/partners/PartnersPage'
import { PartnerDetailPage } from './features/partners/PartnerDetailPage'
import { TimelinePage } from './features/partners/TimelinePage'
import { PartnerCalendarPage } from './features/calendar/CalendarPage'
import { AgendaPage } from './features/calendar/AgendaPage'
import { CheckInPage } from './features/checkin/CheckInPage'
import { StatsPage } from './features/stats/StatsPage'
import { LeaderboardPage } from './features/leaderboard/LeaderboardPage'
import { HistoryPage } from './features/history/HistoryPage'
import { SettingsPage } from './features/settings/SettingsPage'
import { PrivacyPage } from './features/settings/PrivacyPage'
import { NotFoundPage } from './app/NotFoundPage'

initTheme()

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <BrowserRouter>
            <Routes>
              <Route element={<RedirectIfAuthed />}>
                <Route path="/" element={<LandingPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
              </Route>
              <Route element={<RequireAuth />}>
                <Route element={<AppShell />}>
                  <Route path="/dashboard" element={<DashboardPage />} />
                  <Route path="/partners" element={<PartnersPage />} />
                  <Route path="/partners/:id" element={<PartnerDetailPage />} />
                  <Route path="/partners/:id/calendar" element={<PartnerCalendarPage />} />
                  <Route path="/partners/:id/timeline" element={<TimelinePage />} />
                  <Route path="/calendar" element={<AgendaPage />} />
                  <Route path="/check-in" element={<CheckInPage />} />
                  <Route path="/stats" element={<StatsPage />} />
                  <Route path="/leaderboard" element={<LeaderboardPage />} />
                  <Route path="/history" element={<HistoryPage />} />
                  <Route path="/settings" element={<SettingsPage />} />
                  <Route path="/privacy" element={<PrivacyPage />} />
                </Route>
              </Route>
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </BrowserRouter>
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
)
