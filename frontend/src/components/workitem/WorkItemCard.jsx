import React from 'react'
import { Link } from 'react-router-dom'
import { StatusBadge } from '../common/StatusBadge'
import { PriorityBadge } from '../common/PriorityBadge'
import { Users, ArrowRight } from 'lucide-react'

export const WorkItemCard = ({ item, onClaim, claimingId }) => {
  const isClaiming = claimingId === item.id

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm hover:shadow transition-shadow flex flex-col justify-between">
      <div>
        <div className="flex items-start justify-between gap-2 mb-2">
          <div className="flex flex-wrap items-center gap-1.5">
            <StatusBadge status={item.status} />
            <PriorityBadge priority={item.priority} />
          </div>
          <span className="text-[11px] font-mono text-slate-400">#{item.id}</span>
        </div>

        <Link
          to={`/work-items/${item.id}`}
          className="text-sm font-semibold text-slate-900 hover:text-indigo-600 line-clamp-1 transition-colors"
        >
          {item.title}
        </Link>

        {item.description && (
          <p className="text-xs text-slate-500 mt-1 line-clamp-2">{item.description}</p>
        )}
      </div>

      <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
        <div className="flex items-center space-x-1.5">
          <Users className="w-3.5 h-3.5 text-slate-400" />
          <span className="truncate max-w-[120px]">{item.teamName || `Team #${item.teamId}`}</span>
        </div>

        <div className="flex items-center space-x-2">
          {item.status === 'OPEN' && !item.assigneeId && onClaim ? (
            <button
              onClick={() => onClaim(item.id)}
              disabled={isClaiming}
              className="px-2.5 py-1 bg-indigo-600 hover:bg-indigo-700 text-white rounded text-xs font-semibold shadow-sm transition disabled:opacity-50"
            >
              {isClaiming ? 'Claiming...' : 'Claim'}
            </button>
          ) : (
            <Link
              to={`/work-items/${item.id}`}
              className="inline-flex items-center text-xs font-semibold text-indigo-600 hover:text-indigo-800"
            >
              Details <ArrowRight className="w-3 h-3 ml-0.5" />
            </Link>
          )}
        </div>
      </div>
    </div>
  )
}
