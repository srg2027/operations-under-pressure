import React, { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSession } from '../context/SessionContext'
import {
  getWorkItem,
  claimWorkItem,
  releaseWorkItem,
  completeWorkItem,
  reassignWorkItem,
  changePriority,
} from '../api/workItems'
import { getTeamMembers } from '../api/teams'
import { WORK_ITEM_PRIORITY } from '../constants'
import { StatusBadge } from '../components/common/StatusBadge'
import { PriorityBadge } from '../components/common/PriorityBadge'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { ConflictBanner } from '../components/common/ConflictBanner'
import { ActivityTimeline } from '../components/workitem/ActivityTimeline'
import {
  ArrowLeft,
  UserCheck,
  UserX,
  CheckCircle2,
  Users,
  Calendar,
  Layers,
  ArrowRightLeft,
  AlertTriangle,
} from 'lucide-react'
import toast from 'react-hot-toast'

export const WorkItemDetailPage = () => {
  const { id } = useParams()
  const { currentUser } = useSession()
  const queryClient = useQueryClient()

  const [conflictMessage, setConflictMessage] = useState(null)
  const [selectedPriority, setSelectedPriority] = useState('')
  const [selectedTargetUserId, setSelectedTargetUserId] = useState('')
  const [actionError, setActionError] = useState(null)

  // 1. Fetch work item details
  const {
    data: item,
    isLoading: itemLoading,
    error: itemError,
    refetch: refetchItem,
  } = useQuery({
    queryKey: ['workItem', id],
    queryFn: () => getWorkItem(id),
    enabled: !!id,
  })

  const activePriority = selectedPriority || item?.priority || ''

  // 2. Fetch team members for reassign dropdown
  const { data: teamMembers = [] } = useQuery({
    queryKey: ['teamMembers', item?.teamId],
    queryFn: () => getTeamMembers(item.teamId),
    enabled: !!item?.teamId,
  })

  // Helper error handler for 409 conflict detection
  const handleMutationError = (err, defaultMsg) => {
    const status = err?.response?.status
    const msg = err?.response?.data?.message || err.message || defaultMsg

    if (status === 409) {
      setConflictMessage(`Conflict (HTTP 409): ${msg}`)
      toast.error('Conflict: Work item was modified or claimed by another user!')
    } else {
      setActionError(msg)
      toast.error(msg)
    }
    // Always invalidate to pull fresh version from backend
    queryClient.invalidateQueries({ queryKey: ['workItem', id] })
    queryClient.invalidateQueries({ queryKey: ['workItem', id, 'history'] })
  }

  // Claim Mutation
  const claimMutation = useMutation({
    mutationFn: () => claimWorkItem(id, currentUser.id),
    onSuccess: () => {
      toast.success('Work item claimed!')
      setConflictMessage(null)
      setActionError(null)
      queryClient.invalidateQueries({ queryKey: ['workItem', id] })
      queryClient.invalidateQueries({ queryKey: ['workItem', id, 'history'] })
    },
    onError: (err) => handleMutationError(err, 'Failed to claim work item'),
  })

  // Release Mutation
  const releaseMutation = useMutation({
    mutationFn: () => releaseWorkItem(id, currentUser.id),
    onSuccess: () => {
      toast.success('Work item released back to open queue')
      setConflictMessage(null)
      setActionError(null)
      queryClient.invalidateQueries({ queryKey: ['workItem', id] })
      queryClient.invalidateQueries({ queryKey: ['workItem', id, 'history'] })
    },
    onError: (err) => handleMutationError(err, 'Failed to release work item'),
  })

  // Complete Mutation
  const completeMutation = useMutation({
    mutationFn: () => completeWorkItem(id, currentUser.id),
    onSuccess: () => {
      toast.success('Work item marked as COMPLETED')
      setConflictMessage(null)
      setActionError(null)
      queryClient.invalidateQueries({ queryKey: ['workItem', id] })
      queryClient.invalidateQueries({ queryKey: ['workItem', id, 'history'] })
    },
    onError: (err) => handleMutationError(err, 'Failed to complete work item'),
  })

  // Reassign Mutation
  const reassignMutation = useMutation({
    mutationFn: (targetUserId) => reassignWorkItem(id, targetUserId, currentUser.id),
    onSuccess: () => {
      toast.success('Work item reassigned')
      setConflictMessage(null)
      setActionError(null)
      setSelectedTargetUserId('')
      queryClient.invalidateQueries({ queryKey: ['workItem', id] })
      queryClient.invalidateQueries({ queryKey: ['workItem', id, 'history'] })
    },
    onError: (err) => handleMutationError(err, 'Failed to reassign work item'),
  })

  // Priority Change Mutation
  const priorityMutation = useMutation({
    mutationFn: (newPriority) => changePriority(id, newPriority, currentUser.id),
    onSuccess: () => {
      toast.success('Priority updated')
      setConflictMessage(null)
      setActionError(null)
      queryClient.invalidateQueries({ queryKey: ['workItem', id] })
      queryClient.invalidateQueries({ queryKey: ['workItem', id, 'history'] })
    },
    onError: (err) => handleMutationError(err, 'Failed to update priority'),
  })

  const handlePrioritySubmit = (e) => {
    e.preventDefault()
    if (!activePriority || activePriority === item?.priority) return
    priorityMutation.mutate(activePriority)
  }

  const handleReassignSubmit = (e) => {
    e.preventDefault()
    if (!selectedTargetUserId) return
    reassignMutation.mutate(Number(selectedTargetUserId))
  }

  const handleManualRefresh = () => {
    setConflictMessage(null)
    setActionError(null)
    refetchItem()
    queryClient.invalidateQueries({ queryKey: ['workItem', id, 'history'] })
  }

  const isAssignedToCurrent = currentUser?.id && item?.assigneeId === currentUser.id
  const isAnyMutationPending =
    claimMutation.isPending ||
    releaseMutation.isPending ||
    completeMutation.isPending ||
    reassignMutation.isPending ||
    priorityMutation.isPending

  if (itemLoading) {
    return <LoadingSpinner message={`Loading Work Item #${id}...`} />
  }

  if (itemError || !item) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <ErrorMessage
          error={itemError || 'Work item not found or could not be loaded.'}
          onRetry={refetchItem}
        />
        <Link
          to="/work-items"
          className="inline-flex items-center text-xs font-semibold text-indigo-600 hover:text-indigo-800 mt-4"
        >
          <ArrowLeft className="w-4 h-4 mr-1" /> Back to Work Items
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Back button & item ID */}
      <div className="flex items-center justify-between">
        <Link
          to="/work-items"
          className="inline-flex items-center text-xs font-semibold text-slate-600 hover:text-slate-900"
        >
          <ArrowLeft className="w-4 h-4 mr-1" /> Back to Work Items
        </Link>
        <span className="text-xs font-mono text-slate-400">
          ID: #{item.id} · Version: v{item.version}
        </span>
      </div>

      {/* Conflict Banner for HTTP 409 */}
      <ConflictBanner conflictMessage={conflictMessage} onRefresh={handleManualRefresh} />

      {/* General Action Error Banner */}
      {actionError && <ErrorMessage error={actionError} />}

      {/* Main Grid: Details (Left 7 cols) & Timeline (Right 5 cols) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Work Item Details and Controls */}
        <div className="lg:col-span-7 space-y-6">
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs">
            {/* Badges & Meta */}
            <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
              <div className="flex items-center space-x-2">
                <StatusBadge status={item.status} />
                <PriorityBadge priority={item.priority} />
              </div>
              <span className="text-xs text-slate-400 flex items-center">
                <Calendar className="w-3.5 h-3.5 mr-1" />
                Created {new Date(item.createdAt).toLocaleDateString()}
              </span>
            </div>

            {/* Title & Description */}
            <h1 className="text-xl font-bold text-slate-900 tracking-tight">{item.title}</h1>
            <div className="mt-3 text-sm text-slate-600 leading-relaxed whitespace-pre-wrap bg-slate-50 p-4 rounded-xl border border-slate-100">
              {item.description || 'No detailed description provided.'}
            </div>

            {/* Entity Attributes Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mt-6 pt-6 border-t border-slate-100 text-xs">
              <div>
                <span className="text-slate-400 block font-medium">Assigned Team</span>
                <span className="font-semibold text-slate-800 flex items-center gap-1 mt-0.5">
                  <Users className="w-3.5 h-3.5 text-slate-400" />
                  {item.teamName || `Team #${item.teamId}`}
                </span>
              </div>

              <div>
                <span className="text-slate-400 block font-medium">Assignee</span>
                <span className="font-semibold text-slate-800 block mt-0.5">
                  {item.assigneeDisplayName ? (
                    <span className="text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded font-bold inline-flex items-center gap-1">
                      {item.assigneeDisplayName}
                      {isAssignedToCurrent && (
                        <span className="bg-indigo-600 text-white text-[9px] px-1 py-0.2 rounded font-semibold uppercase">
                          You
                        </span>
                      )}
                    </span>
                  ) : (
                    <span className="text-slate-400 italic">Unassigned</span>
                  )}
                </span>
              </div>

              <div>
                <span className="text-slate-400 block font-medium">Created By</span>
                <span className="font-semibold text-slate-800 block mt-0.5">
                  {item.creatorDisplayName || `User #${item.createdById}`}
                </span>
              </div>

              <div>
                <span className="text-slate-400 block font-medium">Claimed At</span>
                <span className="text-slate-600 block mt-0.5">
                  {item.claimedAt ? new Date(item.claimedAt).toLocaleString() : 'Not claimed'}
                </span>
              </div>

              <div>
                <span className="text-slate-400 block font-medium">Last Modified</span>
                <span className="text-slate-600 block mt-0.5">
                  {item.updatedAt ? new Date(item.updatedAt).toLocaleString() : '—'}
                </span>
              </div>

              <div>
                <span className="text-slate-400 block font-medium">Optimistic Lock</span>
                <span className="text-slate-600 font-mono block mt-0.5">v{item.version}</span>
              </div>
            </div>
          </div>

          {/* Workflow Action Panel */}
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs space-y-6">
            <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
              <Layers className="w-4 h-4 text-indigo-600" />
              <span>Lifecycle & Operational Controls</span>
            </h2>

            {/* Primary Status Transitions */}
            <div className="flex flex-wrap gap-3">
              {item.status === 'OPEN' && (
                <button
                  onClick={() => claimMutation.mutate()}
                  disabled={isAnyMutationPending}
                  className="inline-flex items-center px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold shadow-xs disabled:opacity-50 transition"
                >
                  <UserCheck className="w-4 h-4 mr-1.5" />
                  {claimMutation.isPending ? 'Claiming...' : 'Claim Work Item'}
                </button>
              )}

              {item.status === 'IN_PROGRESS' && (
                <>
                  <button
                    onClick={() => completeMutation.mutate()}
                    disabled={isAnyMutationPending}
                    className="inline-flex items-center px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold shadow-xs disabled:opacity-50 transition"
                  >
                    <CheckCircle2 className="w-4 h-4 mr-1.5" />
                    {completeMutation.isPending ? 'Completing...' : 'Mark as Complete'}
                  </button>

                  <button
                    onClick={() => releaseMutation.mutate()}
                    disabled={isAnyMutationPending}
                    className="inline-flex items-center px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-xs font-semibold shadow-xs disabled:opacity-50 transition"
                  >
                    <UserX className="w-4 h-4 mr-1.5" />
                    {releaseMutation.isPending ? 'Releasing...' : 'Release to Queue'}
                  </button>
                </>
              )}

              {item.status === 'DONE' && (
                <div className="inline-flex items-center px-4 py-2 bg-slate-100 text-slate-600 rounded-xl text-xs font-semibold">
                  <CheckCircle2 className="w-4 h-4 mr-1.5 text-emerald-600" />
                  Item is completed and archived
                </div>
              )}
            </div>

            {/* Reassign Operator Form */}
            {item.status !== 'DONE' && (
              <form
                onSubmit={handleReassignSubmit}
                className="pt-4 border-t border-slate-100 space-y-2"
              >
                <label className="block text-xs font-semibold text-slate-700">
                  Reassign to Team Member
                </label>
                <div className="flex flex-col sm:flex-row gap-2">
                  <select
                    value={selectedTargetUserId}
                    onChange={(e) => setSelectedTargetUserId(e.target.value)}
                    className="text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white flex-1"
                  >
                    <option value="">Select team member...</option>
                    {teamMembers.map((m) => (
                      <option key={m.userId} value={m.userId}>
                        {m.displayName} ({m.role})
                      </option>
                    ))}
                  </select>
                  <button
                    type="submit"
                    disabled={isAnyMutationPending || !selectedTargetUserId}
                    className="inline-flex items-center justify-center px-4 py-2 text-xs font-semibold rounded-lg bg-slate-800 hover:bg-slate-900 text-white disabled:opacity-50 transition"
                  >
                    <ArrowRightLeft className="w-3.5 h-3.5 mr-1.5" />
                    {reassignMutation.isPending ? 'Reassigning...' : 'Reassign'}
                  </button>
                </div>
              </form>
            )}

            {/* Change Priority Form */}
            {item.status !== 'DONE' && (
              <form
                onSubmit={handlePrioritySubmit}
                className="pt-4 border-t border-slate-100 space-y-2"
              >
                <label className="block text-xs font-semibold text-slate-700">
                  Adjust Item Priority
                </label>
                <div className="flex flex-col sm:flex-row gap-2">
                  <select
                    value={activePriority}
                    onChange={(e) => setSelectedPriority(e.target.value)}
                    className="text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white flex-1"
                  >
                    {Object.values(WORK_ITEM_PRIORITY).map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                  </select>
                  <button
                    type="submit"
                    disabled={isAnyMutationPending || activePriority === item.priority}
                    className="inline-flex items-center justify-center px-4 py-2 text-xs font-semibold rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white disabled:opacity-50 transition"
                  >
                    <AlertTriangle className="w-3.5 h-3.5 mr-1.5" />
                    {priorityMutation.isPending ? 'Updating...' : 'Update Priority'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>

        {/* Right Column: Collaboration Notes & Audit Timeline */}
        <div className="lg:col-span-5">
          <ActivityTimeline workItemId={item.id} currentUser={currentUser} />
        </div>
      </div>
    </div>
  )
}
