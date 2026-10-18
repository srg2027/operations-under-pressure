import React, { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSession } from '../context/SessionContext'
import { getAssignedWorkItems, getWorkItems, claimWorkItem } from '../api/workItems'
import { getTeams } from '../api/teams'
import { WorkItemCard } from '../components/workitem/WorkItemCard'
import { CreateWorkItemModal } from '../components/workitem/CreateWorkItemModal'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'
import { ConflictBanner } from '../components/common/ConflictBanner'
import {
  Flame,
  UserCheck,
  CheckCircle2,
  Clock,
  Plus,
  ShieldAlert,
  ArrowRight,
  ListTodo,
} from 'lucide-react'
import toast from 'react-hot-toast'

export const DashboardPage = () => {
  const { currentUser, isLoading: sessionLoading } = useSession()
  const queryClient = useQueryClient()

  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [conflictMessage, setConflictMessage] = useState(null)
  const [claimingId, setClaimingId] = useState(null)

  // 1. Teams Query
  const { data: teams = [] } = useQuery({
    queryKey: ['teams'],
    queryFn: getTeams,
    enabled: !!currentUser,
  })

  // 2. Assigned to Me Query
  const {
    data: assignedItems = [],
    isLoading: assignedLoading,
    error: assignedError,
    refetch: refetchAssigned,
  } = useQuery({
    queryKey: ['workItems', 'assigned', currentUser?.id],
    queryFn: () => getAssignedWorkItems(currentUser.id),
    enabled: !!currentUser?.id,
  })

  // 3. Unassigned Urgent Query (Scan teams for OPEN + URGENT/HIGH items without assignee)
  const {
    data: urgentItems = [],
    isLoading: urgentLoading,
    error: urgentError,
    refetch: refetchUrgent,
  } = useQuery({
    queryKey: ['workItems', 'unassigned-urgent', teams],
    queryFn: async () => {
      if (!teams || teams.length === 0) return []
      const promises = teams.map((team) => getWorkItems(team.id, 'OPEN'))
      const results = await Promise.all(promises)
      const allOpen = results.flat()
      // Filter unassigned and priority is URGENT or HIGH
      return allOpen.filter(
        (item) =>
          !item.assigneeId &&
          (item.priority === 'URGENT' || item.priority === 'HIGH')
      )
    },
    enabled: !!currentUser && teams.length > 0,
  })

  // Claim mutation with HTTP 409 conflict detection
  const claimMutation = useMutation({
    mutationFn: ({ itemId, userId }) => claimWorkItem(itemId, userId),
    onMutate: ({ itemId }) => {
      setClaimingId(itemId)
      setConflictMessage(null)
    },
    onSuccess: (data) => {
      toast.success(`Work item #${data.id} successfully claimed!`)
      queryClient.invalidateQueries({ queryKey: ['workItems'] })
    },
    onError: (err) => {
      const status = err?.response?.status
      const msg = err?.response?.data?.message || err.message || 'Claim operation failed'

      if (status === 409) {
        setConflictMessage(`Concurrent Claim Conflict: ${msg}`)
        toast.error(`Conflict: Work item was already claimed or updated!`)
      } else {
        toast.error(`Failed to claim: ${msg}`)
      }
      queryClient.invalidateQueries({ queryKey: ['workItems'] })
    },
    onSettled: () => {
      setClaimingId(null)
    },
  })

  const handleClaim = (itemId) => {
    claimMutation.mutate({ itemId, userId: currentUser.id })
  }

  const handleRefreshAll = () => {
    setConflictMessage(null)
    refetchAssigned()
    refetchUrgent()
  }

  if (sessionLoading) {
    return <LoadingSpinner message="Checking operator session..." />
  }

  if (!currentUser) {
    return <Navigate to="/" replace />
  }

  const inProgressCount = assignedItems.filter((i) => i.status === 'IN_PROGRESS').length
  const completedCount = assignedItems.filter((i) => i.status === 'DONE').length

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Operations Dashboard</h1>
          <p className="text-xs text-slate-500 mt-1">
            Real-time status overview for operator{' '}
            <span className="font-semibold text-slate-700">{currentUser.displayName}</span>
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={() => setCreateModalOpen(true)}
            className="inline-flex items-center px-4 py-2 rounded-xl text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm transition"
          >
            <Plus className="w-4 h-4 mr-1.5" /> Create Work Item
          </button>
        </div>
      </div>

      {/* Conflict Banner if 409 happens */}
      <ConflictBanner conflictMessage={conflictMessage} onRefresh={handleRefreshAll} />

      {/* Top Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Assigned to Me</span>
            <UserCheck className="w-4 h-4 text-indigo-600" />
          </div>
          <div className="text-2xl font-extrabold text-slate-900">{assignedItems.length}</div>
          <div className="text-[11px] text-slate-400 mt-1">
            {inProgressCount} active, {completedCount} completed
          </div>
        </div>

        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Unassigned Urgent</span>
            <Flame className="w-4 h-4 text-red-500" />
          </div>
          <div className="text-2xl font-extrabold text-red-600">{urgentItems.length}</div>
          <div className="text-[11px] text-slate-400 mt-1">Requiring immediate claim</div>
        </div>

        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Active Teams</span>
            <ListTodo className="w-4 h-4 text-slate-500" />
          </div>
          <div className="text-2xl font-extrabold text-slate-900">{teams.length}</div>
          <div className="text-[11px] text-slate-400 mt-1">Operational divisions</div>
        </div>

        <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Completed by Me</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-extrabold text-emerald-600">{completedCount}</div>
          <div className="text-[11px] text-slate-400 mt-1">Resolved operations</div>
        </div>
      </div>

      {/* Main 3 Dashboard Panels */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* PANEL 1: Assigned to Me */}
        <section className="bg-slate-50/70 border border-slate-200 rounded-2xl p-5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-2">
                <div className="w-7 h-7 rounded-lg bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs">
                  <UserCheck className="w-4 h-4" />
                </div>
                <h2 className="text-sm font-bold text-slate-900">Assigned to Me</h2>
              </div>
              <span className="text-xs font-semibold text-slate-500 bg-white px-2 py-0.5 rounded-full border border-slate-200">
                {assignedItems.length}
              </span>
            </div>

            {assignedLoading && <LoadingSpinner message="Loading assigned items..." />}
            {assignedError && <ErrorMessage error={assignedError} onRetry={refetchAssigned} />}

            {!assignedLoading && !assignedError && (
              <>
                {assignedItems.length === 0 ? (
                  <EmptyState
                    title="No work assigned"
                    description="You currently have no active work items assigned to your profile."
                    action={
                      <Link
                        to="/work-items"
                        className="inline-flex items-center text-xs font-semibold text-indigo-600 hover:text-indigo-800"
                      >
                        Browse all items <ArrowRight className="w-3 h-3 ml-1" />
                      </Link>
                    }
                  />
                ) : (
                  <div className="space-y-3">
                    {assignedItems.map((item) => (
                      <WorkItemCard key={item.id} item={item} />
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        </section>

        {/* PANEL 2: Unassigned Urgent */}
        <section className="bg-slate-50/70 border border-slate-200 rounded-2xl p-5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-2">
                <div className="w-7 h-7 rounded-lg bg-red-100 text-red-600 flex items-center justify-center font-bold text-xs">
                  <Flame className="w-4 h-4" />
                </div>
                <h2 className="text-sm font-bold text-slate-900">Unassigned Urgent</h2>
              </div>
              <span className="text-xs font-semibold text-red-700 bg-red-50 px-2 py-0.5 rounded-full border border-red-200">
                {urgentItems.length}
              </span>
            </div>

            {urgentLoading && <LoadingSpinner message="Scanning queue for urgent tasks..." />}
            {urgentError && <ErrorMessage error={urgentError} onRetry={refetchUrgent} />}

            {!urgentLoading && !urgentError && (
              <>
                {urgentItems.length === 0 ? (
                  <EmptyState
                    title="Queue is clear"
                    description="No urgent or high priority items are awaiting claim."
                  />
                ) : (
                  <div className="space-y-3">
                    {urgentItems.map((item) => (
                      <WorkItemCard
                        key={item.id}
                        item={item}
                        onClaim={handleClaim}
                        claimingId={claimingId}
                      />
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        </section>

        {/* PANEL 3: Awaiting My Approval (Polished Unavailable State) */}
        <section className="bg-slate-50/70 border border-slate-200 rounded-2xl p-5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-2">
                <div className="w-7 h-7 rounded-lg bg-slate-200 text-slate-600 flex items-center justify-center font-bold text-xs">
                  <Clock className="w-4 h-4" />
                </div>
                <h2 className="text-sm font-bold text-slate-900">Awaiting My Approval</h2>
              </div>
              <span className="text-[10px] font-bold text-slate-500 bg-slate-200/80 px-2 py-0.5 rounded uppercase">
                Unavailable
              </span>
            </div>

            {/* Polished Architectural Unavailable State Card */}
            <div className="bg-white border border-dashed border-slate-300 rounded-xl p-5 text-center my-2 space-y-3 shadow-xs">
              <div className="w-10 h-10 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400">
                <ShieldAlert className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wide">
                  Approval Workflow Unavailable
                </h3>
                <p className="text-xs text-slate-500 mt-1.5 leading-relaxed">
                  The backend domain model implements direct state transitions:{' '}
                  <code className="text-indigo-600 bg-indigo-50 px-1 py-0.5 rounded font-mono text-[10px]">
                    OPEN → IN_PROGRESS → DONE
                  </code>
                  .
                </p>
                <p className="text-[11px] text-slate-400 mt-2 leading-relaxed">
                  No multi-tier approval gates or <code className="font-mono text-[10px]">PENDING_APPROVAL</code> states exist in the frozen schema. Actions like Claim, Release, Reassign, and Complete are immediately effective and logged to the audit history.
                </p>
              </div>

              <div className="pt-2 border-t border-slate-100 text-[10px] text-slate-400 font-medium">
                Complies with frozen Newtonite specification
              </div>
            </div>
          </div>
        </section>
      </div>

      {/* Modal for creating work item */}
      <CreateWorkItemModal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        teams={teams}
        currentUser={currentUser}
        onSuccess={() => {
          queryClient.invalidateQueries({ queryKey: ['workItems'] })
        }}
      />
    </div>
  )
}
