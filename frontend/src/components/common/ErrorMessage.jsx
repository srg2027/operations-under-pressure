import React from 'react'
import { AlertCircle, RefreshCw } from 'lucide-react'

export const ErrorMessage = ({ error, onRetry }) => {
  const message = typeof error === 'string' ? error : error?.message || 'An unexpected error occurred'

  return (
    <div className="bg-red-50 border border-red-200 rounded-lg p-4 my-4 flex items-start space-x-3 text-red-800">
      <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
      <div className="flex-1">
        <h4 className="text-sm font-semibold text-red-900">Request Error</h4>
        <p className="text-xs text-red-700 mt-1">{message}</p>
        {onRetry && (
          <button
            onClick={onRetry}
            className="mt-3 inline-flex items-center text-xs font-semibold text-red-700 hover:text-red-900 underline gap-1"
          >
            <RefreshCw className="w-3 h-3" /> Retry
          </button>
        )}
      </div>
    </div>
  )
}
