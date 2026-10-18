import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getActivityHistory, addNote } from '../../api/workItems'
import { LoadingSpinner } from '../common/LoadingSpinner'
import { ErrorMessage } from '../common/ErrorMessage'
import { EmptyState } from '../common/EmptyState'
import {
  MessageSquare,
  Clock,
  CheckCircle2,
  UserCheck,
  RotateCcw,
  UserX,
  AlertCircle,
  PlusCircle,
  Send,
} from 'lucide-react'
import toast from 'react-hot-toast'

export const ActivityTimeline = ({ workItemId, currentUser }) => {
  const queryClient = useQueryClient()
  const [noteText, setNoteText] = useState('')
  const [noteError, setNoteError] = useState(null)

  const {
    data: history = [],
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['workItem', workItemId, 'history'],
    queryFn: () => getActivityHistory(workItemId),
    enabled: !!workItemId,
  })

  const addNoteMutation = useMutation({
    mutationFn: ({ note, actorId }) => addNote(workItemId, note, actorId),
    onSuccess: () => {
      toast.success('Note recorded on timeline')
      setNoteText('')
      setNoteError(null)
      queryClient.invalidateQueries({ queryKey: ['workItem', workItemId, 'history'] })
    },
    onError: (err) => {
      const msg = err?.response?.data?.message || err.message || 'Failed to add note'
      setNoteError(msg)
      toast.error(msg)
    },
  })

  const handleAddNote = (e) => {
    e.preventDefault()
    if (!noteText.trim()) return
    addNoteMutation.mutate({ note: noteText.trim(), actorId: currentUser.id })
  }

  const renderIcon = (type) => {
    switch (type) {
      case 'CREATED':
        return <PlusCircle className="w-4 h-4 text-blue-500" />
      case 'CLAIMED':
        return <UserCheck className="w-4 h-4 text-indigo-500" />
      case 'RELEASED':
        return <UserX className="w-4 h-4 text-amber-500" />
      case 'REASSIGNED':
        return <RotateCcw className="w-4 h-4 text-purple-500" />
      case 'COMPLETED':
        return <CheckCircle2 className="w-4 h-4 text-emerald-500" />
      case 'PRIORITY_CHANGED':
        return <AlertCircle className="w-4 h-4 text-orange-500" />
      case 'NOTE_ADDED':
        return <MessageSquare className="w-4 h-4 text-teal-500" />
      default:
        return <Clock className="w-4 h-4 text-slate-400" />
    }
  }

  const formatTimestamp = (isoString) => {
    if (!isoString) return ''
    try {
      const date = new Date(isoString)
      return date.toLocaleString(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
      })
    } catch {
      return isoString
    }
  }

  return (
    <div className="space-y-6">
      {/* Collaboration / Add Note Input */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs">
        <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-2 flex items-center space-x-1.5">
          <MessageSquare className="w-4 h-4 text-indigo-600" />
          <span>Add Operational Note</span>
        </h3>

        {noteError && <ErrorMessage error={noteError} />}

        <form onSubmit={handleAddNote} className="space-y-3">
          <textarea
            rows={2}
            value={noteText}
            onChange={(e) => setNoteText(e.target.value)}
            placeholder="Record team handover, triage observation, or status update..."
            className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <div className="flex items-center justify-between">
            <span className="text-[11px] text-slate-400">
              Posting as <strong>{currentUser?.displayName}</strong>
            </span>
            <button
              type="submit"
              disabled={addNoteMutation.isPending || !noteText.trim()}
              className="inline-flex items-center px-3 py-1.5 text-xs font-semibold rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white shadow-xs disabled:opacity-50 transition"
            >
              <Send className="w-3.5 h-3.5 mr-1" />
              {addNoteMutation.isPending ? 'Saving...' : 'Post Note'}
            </button>
          </div>
        </form>
      </div>

      {/* Activity Timeline List */}
      <div>
        <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-4 flex items-center justify-between">
          <span>Audit & Activity History</span>
          <span className="text-[11px] font-normal text-slate-400">
            {history.length} {history.length === 1 ? 'event' : 'events'} recorded
          </span>
        </h3>

        {isLoading && <LoadingSpinner message="Loading activity timeline..." />}
        {error && <ErrorMessage error={error} onRetry={refetch} />}

        {!isLoading && !error && (
          <>
            {history.length === 0 ? (
              <EmptyState
                title="No activity recorded"
                description="Events such as claims, state transitions, and notes will appear here."
              />
            ) : (
              <div className="relative border-l-2 border-slate-200 ml-4 space-y-6 py-2">
                {history.map((log) => (
                  <div key={log.id} className="relative pl-6">
                    {/* Circle marker */}
                    <div className="absolute -left-[17px] top-0.5 bg-white border-2 border-slate-300 rounded-full p-1 shadow-xs">
                      {renderIcon(log.activityType)}
                    </div>

                    <div className="bg-white border border-slate-200 rounded-xl p-3.5 shadow-xs">
                      <div className="flex flex-wrap items-center justify-between gap-1 mb-1">
                        <span className="text-xs font-bold text-slate-800">
                          {log.activityType.replace('_', ' ')}
                        </span>
                        <span className="text-[11px] text-slate-400 flex items-center">
                          <Clock className="w-3 h-3 mr-1 text-slate-400" />
                          {formatTimestamp(log.createdAt)}
                        </span>
                      </div>

                      <div className="text-xs text-slate-600">
                        {log.details || 'Operation completed.'}
                      </div>

                      <div className="mt-2 pt-2 border-t border-slate-100 text-[10px] text-slate-500 flex items-center space-x-1">
                        <span>Operator:</span>
                        <span className="font-semibold text-slate-700">
                          {log.actorDisplayName || `User #${log.actorId}`}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
