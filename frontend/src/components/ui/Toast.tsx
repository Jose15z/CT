import { createContext, useCallback, useContext, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { CheckCircle2, AlertCircle } from 'lucide-react'

interface ToastItem {
  id: number
  message: string
  kind: 'success' | 'error'
}

const ToastContext = createContext<(message: string, kind?: 'success' | 'error') => void>(() => {})

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const nextId = useRef(1)

  const push = useCallback((message: string, kind: 'success' | 'error' = 'success') => {
    const id = nextId.current++
    setToasts((prev) => [...prev, { id, message, kind }])
    setTimeout(() => setToasts((prev) => prev.filter((toast) => toast.id !== id)), 3500)
  }, [])

  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-20 z-[60] flex flex-col items-center gap-2 px-4 sm:bottom-6">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role="status"
            className="flex items-center gap-2 rounded-lg border border-border bg-surface px-3.5 py-2.5 text-[13.5px] text-ink shadow-md"
          >
            {toast.kind === 'success' ? (
              <CheckCircle2 size={16} className="text-success" />
            ) : (
              <AlertCircle size={16} className="text-danger" />
            )}
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
