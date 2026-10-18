import React from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'

export const ConflictBanner = ({ conflictMessage, onRefresh }) => {
  if (!conflictMessage) return null

  return (
    <div className="bg-amber-50 border-l-4 border-amber-500 p-4 rounded-r-lg my-4 shadow-sm animate-pulse">
      <div className="flex items-start">
        <div className="shrink-0">
          <AlertTriangle className="h-5 h-5 text-amber-600" />
        </div>
        <div className="ml-3 flex-1">
          <h3 className="text-sm font-semibold text-amber-800">
            Work Item Conflict Detected (HTTP 409)
          </h3>
          <div className="mt-1 text-xs text-amber-700">
            {conflictMessage}
          </div>
          <p className="mt-2 text-xs text-amber-600">
            Another team member or concurrent process has modified or claimed this item. Please refresh to load the latest state.
          </p>
          {onRefresh && (
            <div className="mt-3">
              <button
                type="button"
                onClick={onRefresh}
                className="inline-flex items-center px-3 py-1.5 border border-amber-600 text-xs font-medium rounded-md text-amber-800 bg-white hover:bg-amber-50 shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-amber-500"
              >
                <RefreshCw className="w-3.5 h-3.5 mr-1.5" />
                Reload Current State
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
