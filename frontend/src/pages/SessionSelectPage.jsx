import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSession } from '../context/SessionContext'
import { createUser } from '../api/users'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { Users, UserPlus, ShieldAlert, ArrowRight, CheckCircle2 } from 'lucide-react'
import toast from 'react-hot-toast'

export const SessionSelectPage = () => {
  const { users, currentUser, selectUser, isLoading, error, refreshUsers } = useSession()
  const navigate = useNavigate()

  const [isCreating, setIsCreating] = useState(false)
  const [newDisplayName, setNewDisplayName] = useState('')
  const [newEmail, setNewEmail] = useState('')
  const [createLoading, setCreateLoading] = useState(false)
  const [createError, setCreateError] = useState(null)

  const handleSelect = (user) => {
    selectUser(user)
    toast.success(`Identity active: ${user.displayName}`)
    navigate('/dashboard')
  }

  const handleCreateUser = async (e) => {
    e.preventDefault()
    if (!newDisplayName.trim() || !newEmail.trim()) {
      setCreateError('Please provide both display name and email')
      return
    }

    setCreateLoading(true)
    setCreateError(null)

    try {
      const created = await createUser({
        displayName: newDisplayName.trim(),
        email: newEmail.trim(),
        active: true,
      })
      await refreshUsers()
      toast.success(`Created operator: ${created.displayName}`)
      setIsCreating(false)
      setNewDisplayName('')
      setNewEmail('')
      handleSelect(created)
    } catch (err) {
      setCreateError(err?.response?.data?.message || err.message || 'Failed to create user')
    } finally {
      setCreateLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="w-12 h-12 bg-indigo-600 rounded-xl mx-auto flex items-center justify-center text-white shadow-md mb-3">
          <Users className="w-6 h-6" />
        </div>
        <h2 className="text-center text-2xl font-extrabold text-slate-900 tracking-tight">
          Who are you?
        </h2>
        <p className="mt-1 text-center text-xs font-semibold uppercase tracking-wider text-indigo-600">
          Demo Identity Selection
        </p>

        {/* Prominent Architectural Disclaimer */}
        <div className="mt-4 bg-amber-50 border border-amber-200 rounded-lg p-3 text-xs text-amber-800 flex items-start space-x-2">
          <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
          <div>
            <span className="font-bold">Honest Architecture Notice:</span> The backend application does not implement Spring Security, JWT, or password authentication. Selecting a user simulates authentication by persisting the User ID in localStorage and supplying it to API requests.
          </div>
        </div>
      </div>

      <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-lg">
        <div className="bg-white py-8 px-4 shadow-xl sm:rounded-2xl sm:px-8 border border-slate-200">
          {isLoading && <LoadingSpinner message="Querying active operators from backend..." />}

          {error && (
            <ErrorMessage
              error={`Could not load users: ${error}. Ensure Spring Boot is running on http://localhost:8080.`}
              onRetry={refreshUsers}
            />
          )}

          {!isLoading && !error && (
            <>
              {users.length === 0 ? (
                <div className="text-center py-6">
                  <p className="text-sm text-slate-600">No active operators found in the database.</p>
                  <p className="text-xs text-slate-400 mt-1">Create an operator below to proceed.</p>
                </div>
              ) : (
                <div className="space-y-3">
                  <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                    Select an Active Operator
                  </h3>
                  <div className="divide-y divide-slate-100 border border-slate-200 rounded-xl overflow-hidden shadow-sm">
                    {users.map((user) => {
                      const isCurrent = currentUser?.id === user.id
                      return (
                        <div
                          key={user.id}
                          className={`p-3.5 flex items-center justify-between transition-colors ${
                            isCurrent ? 'bg-indigo-50/70' : 'hover:bg-slate-50'
                          }`}
                        >
                          <div className="flex items-center space-x-3">
                            <div className="w-9 h-9 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-sm shrink-0">
                              {user.displayName.charAt(0).toUpperCase()}
                            </div>
                            <div>
                              <div className="text-sm font-semibold text-slate-800 flex items-center gap-2">
                                {user.displayName}
                                {isCurrent && (
                                  <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-indigo-700 bg-indigo-100 px-1.5 py-0.5 rounded">
                                    <CheckCircle2 className="w-3 h-3" /> Active
                                  </span>
                                )}
                              </div>
                              <div className="text-xs text-slate-500">{user.email}</div>
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={() => handleSelect(user)}
                            className="inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm transition"
                          >
                            <span>Select</span>
                            <ArrowRight className="w-3 h-3 ml-1" />
                          </button>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )}

              {/* Toggle Create User Section */}
              <div className="mt-6 pt-5 border-t border-slate-200">
                {!isCreating ? (
                  <button
                    type="button"
                    onClick={() => setIsCreating(true)}
                    className="w-full inline-flex items-center justify-center py-2.5 px-4 border border-dashed border-slate-300 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                  >
                    <UserPlus className="w-4 h-4 mr-1.5 text-indigo-600" />
                    + Create New Operator Profile
                  </button>
                ) : (
                  <form onSubmit={handleCreateUser} className="space-y-3 bg-slate-50 p-4 rounded-xl border border-slate-200">
                    <div className="flex items-center justify-between">
                      <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                        New Operator Details
                      </h4>
                      <button
                        type="button"
                        onClick={() => setIsCreating(false)}
                        className="text-xs text-slate-400 hover:text-slate-600"
                      >
                        Cancel
                      </button>
                    </div>

                    {createError && <ErrorMessage error={createError} />}

                    <div>
                      <label className="block text-xs font-medium text-slate-700 mb-1">
                        Display Name
                      </label>
                      <input
                        type="text"
                        value={newDisplayName}
                        onChange={(e) => setNewDisplayName(e.target.value)}
                        placeholder="e.g. Maya Lin"
                        required
                        className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-medium text-slate-700 mb-1">
                        Email Address
                      </label>
                      <input
                        type="email"
                        value={newEmail}
                        onChange={(e) => setNewEmail(e.target.value)}
                        placeholder="e.g. maya@example.com"
                        required
                        className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      />
                    </div>

                    <button
                      type="submit"
                      disabled={createLoading}
                      className="w-full py-2 px-4 rounded-lg bg-indigo-600 text-white text-xs font-semibold hover:bg-indigo-700 disabled:opacity-50 transition"
                    >
                      {createLoading ? 'Creating Operator...' : 'Create and Switch'}
                    </button>
                  </form>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
