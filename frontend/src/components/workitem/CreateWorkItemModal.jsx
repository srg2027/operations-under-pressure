import React, { useState } from 'react'
import { createWorkItem } from '../../api/workItems'
import { WORK_ITEM_PRIORITY } from '../../constants'
import { ErrorMessage } from '../common/ErrorMessage'
import { X, PlusCircle } from 'lucide-react'
import toast from 'react-hot-toast'

export const CreateWorkItemModal = ({ isOpen, onClose, teams, currentUser, onSuccess }) => {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState(WORK_ITEM_PRIORITY.MEDIUM)
  const [teamId, setTeamId] = useState(teams && teams.length > 0 ? teams[0].id : '')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState(null)

  const selectedOrFirstTeamId = teamId || (teams && teams.length > 0 ? String(teams[0].id) : '')

  if (!isOpen) return null

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!title.trim() || !description.trim() || !selectedOrFirstTeamId) {
      setError('Title, description, and team are required.')
      return
    }

    setIsSubmitting(true)
    setError(null)

    try {
      const created = await createWorkItem({
        title: title.trim(),
        description: description.trim(),
        priority,
        teamId: Number(selectedOrFirstTeamId),
        creatorId: currentUser.id,
      })
      toast.success(`Work item #${created.id} created!`)
      setTitle('')
      setDescription('')
      setPriority(WORK_ITEM_PRIORITY.MEDIUM)
      if (onSuccess) onSuccess(created)
      onClose()
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to create work item')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 relative animate-in fade-in zoom-in-95 duration-150">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100">
          <div className="flex items-center space-x-2">
            <PlusCircle className="w-5 h-5 text-indigo-600" />
            <h3 className="text-base font-bold text-slate-900">Create New Work Item</h3>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {error && <ErrorMessage error={error} />}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Title <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Incident #402: High latency on auth gateway"
              required
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Description <span className="text-red-500">*</span>
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe the operational issue, symptoms, or requirements..."
              required
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Priority
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
              >
                {Object.values(WORK_ITEM_PRIORITY).map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Assigned Team <span className="text-red-500">*</span>
              </label>
              <select
                value={selectedOrFirstTeamId}
                onChange={(e) => setTeamId(e.target.value)}
                required
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
              >
                {teams && teams.length > 0 ? (
                  teams.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))
                ) : (
                  <option value="">No teams available</option>
                )}
              </select>
            </div>
          </div>

          <div className="pt-2 text-[11px] text-slate-500 flex items-center justify-between">
            <span>
              Author: <strong>{currentUser?.displayName}</strong>
            </span>
            <span className="text-slate-400">Status will initialize as OPEN</span>
          </div>

          <div className="flex items-center justify-end space-x-2 pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 rounded-lg transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !teams || teams.length === 0}
              className="px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-sm disabled:opacity-50 transition"
            >
              {isSubmitting ? 'Creating...' : 'Create Work Item'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
