import React, { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSession } from '../context/SessionContext'
import { getTeams, createTeam, getTeamMembers, addTeamMember, removeMember } from '../api/teams'
import { TEAM_ROLE } from '../constants'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'
import {
  Users,
  UserPlus,
  Trash2,
  Plus,
  CheckSquare,
} from 'lucide-react'
import toast from 'react-hot-toast'

export const TeamsPage = () => {
  const { currentUser, users = [], isLoading: sessionLoading } = useSession()
  const queryClient = useQueryClient()

  const [selectedTeamId, setSelectedTeamId] = useState(null)
  const [createTeamModalOpen, setCreateTeamModalOpen] = useState(false)
  const [newTeamName, setNewTeamName] = useState('')
  const [newTeamDesc, setNewTeamDesc] = useState('')
  const [addMemberUserId, setAddMemberUserId] = useState('')
  const [addMemberRole, setAddMemberRole] = useState(TEAM_ROLE.MEMBER)

  // 1. Teams Query
  const {
    data: teams = [],
    isLoading: teamsLoading,
    error: teamsError,
  } = useQuery({
    queryKey: ['teams'],
    queryFn: getTeams,
    enabled: !!currentUser,
  })

  const activeTeamId = selectedTeamId ?? (teams.length > 0 ? teams[0].id : null)

  // 2. Selected Team Members Query
  const {
    data: members = [],
    isLoading: membersLoading,
    error: membersError,
    refetch: refetchMembers,
  } = useQuery({
    queryKey: ['teamMembers', activeTeamId],
    queryFn: () => getTeamMembers(activeTeamId),
    enabled: !!activeTeamId,
  })

  // Create Team Mutation
  const createTeamMutation = useMutation({
    mutationFn: ({ name, description }) => createTeam({ name, description }),
    onSuccess: (newTeam) => {
      toast.success(`Team "${newTeam.name}" created!`)
      setNewTeamName('')
      setNewTeamDesc('')
      setCreateTeamModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['teams'] })
      setSelectedTeamId(newTeam.id)
    },
    onError: (err) => {
      toast.error(err?.response?.data?.message || err.message || 'Failed to create team')
    },
  })

  // Add Member Mutation
  const addMemberMutation = useMutation({
    mutationFn: ({ userId, role }) =>
      addTeamMember(activeTeamId, { userId: Number(userId), role }, currentUser.id),
    onSuccess: () => {
      toast.success('Team membership updated')
      setAddMemberUserId('')
      queryClient.invalidateQueries({ queryKey: ['teamMembers', activeTeamId] })
    },
    onError: (err) => {
      toast.error(err?.response?.data?.message || err.message || 'Failed to add member')
    },
  })

  // Remove Member Mutation
  const removeMemberMutation = useMutation({
    mutationFn: (userId) => removeMember(activeTeamId, userId, currentUser.id),
    onSuccess: () => {
      toast.success('Member removed from team')
      queryClient.invalidateQueries({ queryKey: ['teamMembers', activeTeamId] })
    },
    onError: (err) => {
      toast.error(err?.response?.data?.message || err.message || 'Failed to remove member')
    },
  })

  if (sessionLoading) {
    return <LoadingSpinner message="Checking operator session..." />
  }

  if (!currentUser) {
    return <Navigate to="/" replace />
  }

  const selectedTeam = teams.find((t) => t.id === activeTeamId)

  // Filter users who are not yet members
  const memberUserIds = new Set(members.map((m) => m.userId))
  const eligibleUsers = users.filter((u) => !memberUserIds.has(u.id))

  const handleCreateTeamSubmit = (e) => {
    e.preventDefault()
    if (!newTeamName.trim()) return
    createTeamMutation.mutate({
      name: newTeamName.trim(),
      description: newTeamDesc.trim(),
    })
  }

  const handleAddMemberSubmit = (e) => {
    e.preventDefault()
    if (!addMemberUserId) return
    addMemberMutation.mutate({ userId: addMemberUserId, role: addMemberRole })
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Teams & Roster</h1>
          <p className="text-xs text-slate-500 mt-1">
            Manage operational division rosters, assign leads, and configure access
          </p>
        </div>
        <button
          onClick={() => setCreateTeamModalOpen(true)}
          className="inline-flex items-center px-4 py-2 rounded-xl text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm transition"
        >
          <Plus className="w-4 h-4 mr-1.5" /> New Team
        </button>
      </div>

      {teamsLoading && <LoadingSpinner message="Loading operational teams..." />}
      {teamsError && <ErrorMessage error={teamsError} />}

      {!teamsLoading && !teamsError && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Teams Navigation (Left 4 cols) */}
          <div className="lg:col-span-4 space-y-3">
            <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Operational Divisions ({teams.length})
            </h2>

            <div className="space-y-2">
              {teams.map((t) => {
                const isSelected = t.id === activeTeamId
                return (
                  <button
                    key={t.id}
                    onClick={() => setSelectedTeamId(t.id)}
                    className={`w-full text-left p-4 rounded-xl border transition flex flex-col justify-between ${
                      isSelected
                        ? 'bg-indigo-50/80 border-indigo-300 ring-2 ring-indigo-500/20'
                        : 'bg-white border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-bold text-slate-900">{t.name}</span>
                      <span className="text-[11px] font-mono text-slate-400">#{t.id}</span>
                    </div>
                    {t.description && (
                      <p className="text-xs text-slate-500 mt-1 line-clamp-2">{t.description}</p>
                    )}
                  </button>
                )
              })}
            </div>
          </div>

          {/* Team Members & Roster (Right 8 cols) */}
          <div className="lg:col-span-8 space-y-6">
            {selectedTeam ? (
              <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs space-y-6">
                {/* Team Info Header */}
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-100">
                  <div>
                    <div className="flex items-center space-x-2">
                      <Users className="w-5 h-5 text-indigo-600" />
                      <h2 className="text-lg font-bold text-slate-900">{selectedTeam.name}</h2>
                    </div>
                    <p className="text-xs text-slate-500 mt-1">
                      {selectedTeam.description || 'No description provided.'}
                    </p>
                  </div>
                  <Link
                    to="/work-items"
                    className="inline-flex items-center px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                  >
                    <CheckSquare className="w-3.5 h-3.5 mr-1 text-indigo-600" />
                    View Team Work Items
                  </Link>
                </div>

                {/* Add Member Bar */}
                <form
                  onSubmit={handleAddMemberSubmit}
                  className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-3"
                >
                  <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                    <UserPlus className="w-4 h-4 text-indigo-600" />
                    <span>Add Member to {selectedTeam.name}</span>
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-12 gap-2">
                    <div className="sm:col-span-6">
                      <select
                        value={addMemberUserId}
                        onChange={(e) => setAddMemberUserId(e.target.value)}
                        className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                      >
                        <option value="">Select operator to add...</option>
                        {eligibleUsers.map((u) => (
                          <option key={u.id} value={u.id}>
                            {u.displayName} ({u.email})
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="sm:col-span-4">
                      <select
                        value={addMemberRole}
                        onChange={(e) => setAddMemberRole(e.target.value)}
                        className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                      >
                        {Object.values(TEAM_ROLE).map((r) => (
                          <option key={r} value={r}>
                            {r}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="sm:col-span-2">
                      <button
                        type="submit"
                        disabled={addMemberMutation.isPending || !addMemberUserId}
                        className="w-full py-2 px-3 text-xs font-semibold rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white disabled:opacity-50 transition"
                      >
                        {addMemberMutation.isPending ? 'Adding...' : 'Add'}
                      </button>
                    </div>
                  </div>
                </form>

                {/* Members List */}
                <div>
                  <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">
                    Active Roster ({members.length})
                  </h3>

                  {membersLoading && <LoadingSpinner message="Loading team members..." />}
                  {membersError && <ErrorMessage error={membersError} onRetry={refetchMembers} />}

                  {!membersLoading && !membersError && (
                    <>
                      {members.length === 0 ? (
                        <EmptyState
                          title="No members enrolled"
                          description="Add operators using the selection bar above."
                        />
                      ) : (
                        <div className="divide-y divide-slate-100 border border-slate-200 rounded-xl overflow-hidden shadow-xs">
                          {members.map((m) => (
                            <div
                              key={m.membershipId}
                              className="p-3.5 flex items-center justify-between hover:bg-slate-50 transition"
                            >
                              <div className="flex items-center space-x-3">
                                <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-xs">
                                  {m.displayName?.charAt(0).toUpperCase()}
                                </div>
                                <div>
                                  <div className="text-xs font-bold text-slate-800 flex items-center gap-2">
                                    {m.displayName}
                                    <span
                                      className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${
                                        m.role === 'TEAM_LEAD'
                                          ? 'bg-purple-100 text-purple-700 border border-purple-200'
                                          : m.role === 'OBSERVER'
                                          ? 'bg-slate-100 text-slate-600'
                                          : 'bg-blue-100 text-blue-700'
                                      }`}
                                    >
                                      {m.role}
                                    </span>
                                  </div>
                                  <div className="text-[11px] text-slate-400">{m.email}</div>
                                </div>
                              </div>

                              <button
                                onClick={() => removeMemberMutation.mutate(m.userId)}
                                disabled={removeMemberMutation.isPending}
                                title="Remove member from team"
                                className="text-slate-400 hover:text-red-600 p-1.5 rounded-lg hover:bg-red-50 transition"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </div>
                          ))}
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            ) : (
              <EmptyState title="No team selected" description="Select a team from the list." />
            )}
          </div>
        </div>
      )}

      {/* Create Team Modal */}
      {createTeamModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <h3 className="text-base font-bold text-slate-900">Create New Team</h3>
            <form onSubmit={handleCreateTeamSubmit} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Team Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={newTeamName}
                  onChange={(e) => setNewTeamName(e.target.value)}
                  placeholder="e.g. SRE Platform Ops"
                  required
                  className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Description
                </label>
                <textarea
                  rows={2}
                  value={newTeamDesc}
                  onChange={(e) => setNewTeamDesc(e.target.value)}
                  placeholder="Core mission or operational scope..."
                  className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setCreateTeamModalOpen(false)}
                  className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createTeamMutation.isPending || !newTeamName.trim()}
                  className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white disabled:opacity-50"
                >
                  {createTeamMutation.isPending ? 'Creating...' : 'Create Team'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
