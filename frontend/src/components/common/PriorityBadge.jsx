import React from 'react'

export const PriorityBadge = ({ priority }) => {
  switch (priority) {
    case 'URGENT':
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-red-100 text-red-700 border border-red-300">
          🔥 URGENT
        </span>
      )
    case 'HIGH':
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-orange-100 text-orange-700 border border-orange-200">
          ⚡ HIGH
        </span>
      )
    case 'MEDIUM':
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-yellow-100 text-yellow-800 border border-yellow-200">
          MEDIUM
        </span>
      )
    case 'LOW':
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
          LOW
        </span>
      )
    default:
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700">
          {priority || 'NORMAL'}
        </span>
      )
  }
}
