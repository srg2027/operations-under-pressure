import React, { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useSession } from '../context/SessionContext'
import { getWorkItems } from '../api/workItems'
import { getTeams } from '../api/teams'
import { StatusBadge } from '../components/common/StatusBadge'
import { PriorityBadge } from '../components/common/PriorityBadge'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'
import { CreateWorkItemModal } from '../components/workitem/CreateWorkItemModal'
import {
  Search,
  Plus,
  ArrowRight,
  RotateCcw,
} from 'lucide-react'

export const WorkItemsPage = () => {
  const { currentUser, isLoading: sessionLoading } = useSession()
  const queryClient = useQueryClient()

  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [selectedTeamId, setSelectedTeamId] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [searchQuery, setSearchQuery] = useState('')

  // 1. Teams query
  const { data: teams = [] } = useQuery({
    queryKey: ['teams'],
    queryFn: getTeams,
    enabled: !!currentUser,
  })

  const effectiveTeamId = selectedTeamId || (teams.length > 0 ? String(teams[0].id) : '')

  // 2. Work Items query for selected team
  const {
    data: rawItems = [],
    isLoading: itemsLoading,
    error: itemsError,
    refetch,
  } = useQuery({
    queryKey: ['workItems', 'team', effectiveTeamId, statusFilter],
    queryFn: () => getWorkItems(effectiveTeamId, statusFilter || undefined),
    enabled: !!effectiveTeamId,
  })

  if (sessionLoading) {
    return <LoadingSpinner message="Checking operator session..." />
  }

  if (!currentUser) {
    return <Navigate to="/" replace />
  }

  // Filter in memory for priority & search query
  const filteredItems = rawItems.filter((item) => {
    if (priorityFilter && item.priority !== priorityFilter) return false
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase()
      const inTitle = item.title?.toLowerCase().includes(q)
      const inDesc = item.description?.toLowerCase().includes(q)
      const inAssignee = item.assigneeDisplayName?.toLowerCase().includes(q)
      if (!inTitle && !inDesc && !inAssignee) return false
    }
    return true
  })

  const resetFilters = () => {
    setStatusFilter('')
    setPriorityFilter('')
    setSearchQuery('')
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Work Items Repository</h1>
          <p className="text-xs text-slate-500 mt-1">
            Browse, filter, and triage operational work items across teams
          </p>
        </div>
        <button
          onClick={() => setCreateModalOpen(true)}
          className="inline-flex items-center px-4 py-2 rounded-xl text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm transition"
        >
          <Plus className="w-4 h-4 mr-1.5" /> Create Work Item
        </button>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Team Selector */}
          <div>
            <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
              Select Team
            </label>
            <select
              value={effectiveTeamId}
              onChange={(e) => setSelectedTeamId(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
            >
              {teams.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
              Lifecycle Status
            </label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
            >
              <option value="">All Statuses</option>
              <option value="OPEN">OPEN</option>
              <option value="IN_PROGRESS">IN PROGRESS</option>
              <option value="DONE">DONE</option>
            </select>
          </div>

          {/* Priority Filter */}
          <div>
            <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
              Priority Level
            </label>
            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
            >
              <option value="">All Priorities</option>
              <option value="URGENT">URGENT</option>
              <option value="HIGH">HIGH</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="LOW">LOW</option>
            </select>
          </div>

          {/* Search Query */}
          <div>
            <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
              Search Keywords
            </label>
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search title, desc, assignee..."
                className="w-full text-xs pl-8 pr-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
        </div>

        {(statusFilter || priorityFilter || searchQuery) && (
          <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs text-slate-500">
            <span>
              Showing {filteredItems.length} of {rawItems.length} items
            </span>
            <button
              onClick={resetFilters}
              className="inline-flex items-center text-indigo-600 hover:text-indigo-800 font-semibold"
            >
              <RotateCcw className="w-3 h-3 mr-1" /> Reset Filters
            </button>
          </div>
        )}
      </div>

      {/* Main Content: Table on desktop, Cards on mobile */}
      <div>
        {itemsLoading && <LoadingSpinner message="Querying team work items..." />}
        {itemsError && <ErrorMessage error={itemsError} onRetry={refetch} />}

        {!itemsLoading && !itemsError && (
          <>
            {filteredItems.length === 0 ? (
              <EmptyState
                title="No work items match"
                description={
                  rawItems.length === 0
                    ? 'No items currently exist for this team. Click "Create Work Item" to log one.'
                    : 'Try clearing your filters or search keywords.'
                }
              />
            ) : (
              <>
                {/* Desktop Table View */}
                <div className="hidden md:block bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs">
                  <table className="min-w-full divide-y divide-slate-200 text-xs text-left">
                    <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider">
                      <tr>
                        <th className="py-3 px-4">ID</th>
                        <th className="py-3 px-4">Title & Details</th>
                        <th className="py-3 px-4">Status</th>
                        <th className="py-3 px-4">Priority</th>
                        <th className="py-3 px-4">Assignee</th>
                        <th className="py-3 px-4">Created</th>
                        <th className="py-3 px-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 text-slate-700">
                      {filteredItems.map((item) => (
                        <tr key={item.id} className="hover:bg-slate-50/70 transition-colors">
                          <td className="py-3 px-4 font-mono font-medium text-slate-400">
                            #{item.id}
                          </td>
                          <td className="py-3 px-4">
                            <Link
                              to={`/work-items/${item.id}`}
                              className="font-semibold text-slate-900 hover:text-indigo-600 block line-clamp-1"
                            >
                              {item.title}
                            </Link>
                            {item.description && (
                              <p className="text-[11px] text-slate-400 line-clamp-1 mt-0.5">
                                {item.description}
                              </p>
                            )}
                          </td>
                          <td className="py-3 px-4 whitespace-nowrap">
                            <StatusBadge status={item.status} />
                          </td>
                          <td className="py-3 px-4 whitespace-nowrap">
                            <PriorityBadge priority={item.priority} />
                          </td>
                          <td className="py-3 px-4 whitespace-nowrap">
                            {item.assigneeDisplayName ? (
                              <span className="font-semibold text-slate-800">
                                {item.assigneeDisplayName}
                              </span>
                            ) : (
                              <span className="text-slate-400 italic">Unassigned</span>
                            )}
                          </td>
                          <td className="py-3 px-4 whitespace-nowrap text-slate-400">
                            {new Date(item.createdAt).toLocaleDateString()}
                          </td>
                          <td className="py-3 px-4 text-right whitespace-nowrap">
                            <Link
                              to={`/work-items/${item.id}`}
                              className="inline-flex items-center text-xs font-semibold text-indigo-600 hover:text-indigo-800"
                            >
                              View <ArrowRight className="w-3 h-3 ml-0.5" />
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Mobile Cards View */}
                <div className="grid grid-cols-1 gap-3 md:hidden">
                  {filteredItems.map((item) => (
                    <div
                      key={item.id}
                      className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs space-y-3"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex items-center space-x-1.5">
                          <StatusBadge status={item.status} />
                          <PriorityBadge priority={item.priority} />
                        </div>
                        <span className="text-xs font-mono text-slate-400">#{item.id}</span>
                      </div>

                      <Link
                        to={`/work-items/${item.id}`}
                        className="font-semibold text-sm text-slate-900 hover:text-indigo-600 block"
                      >
                        {item.title}
                      </Link>

                      {item.description && (
                        <p className="text-xs text-slate-500 line-clamp-2">{item.description}</p>
                      )}

                      <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                        <span>
                          Assignee:{' '}
                          <strong className="text-slate-700">
                            {item.assigneeDisplayName || 'None'}
                          </strong>
                        </span>
                        <Link
                          to={`/work-items/${item.id}`}
                          className="font-semibold text-indigo-600"
                        >
                          Details →
                        </Link>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </>
        )}
      </div>

      {/* Create Modal */}
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
