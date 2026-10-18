import React from 'react'
import { Inbox } from 'lucide-react'

export const EmptyState = ({ title = 'No items found', description = 'There are no items matching this criteria.', action }) => {
  return (
    <div className="text-center py-12 px-4 bg-slate-50 border border-dashed border-slate-200 rounded-xl my-4">
      <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center mx-auto shadow-sm border border-slate-200 text-slate-400 mb-3">
        <Inbox className="w-6 h-6" />
      </div>
      <h3 className="text-sm font-semibold text-slate-800">{title}</h3>
      <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">{description}</p>
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
