import api from './axios'

export const getWorkItems = async (teamId, status) => {
  const params = { teamId }
  if (status) {
    params.status = status
  }
  const response = await api.get('/work-items', { params })
  return response.data
}

export const getAssignedWorkItems = async (userId, status) => {
  const params = status ? { status } : {}
  const response = await api.get(`/work-items/assigned/${userId}`, { params })
  return response.data
}

export const getWorkItem = async (id) => {
  const response = await api.get(`/work-items/${id}`)
  return response.data
}

export const createWorkItem = async ({ title, description, priority, teamId, creatorId }) => {
  const response = await api.post('/work-items', {
    title,
    description,
    priority,
    teamId,
    creatorId,
  })
  return response.data
}

export const claimWorkItem = async (id, userId) => {
  const response = await api.post(`/work-items/${id}/claim`, null, {
    params: { userId },
  })
  return response.data
}

export const releaseWorkItem = async (id, userId) => {
  const response = await api.post(`/work-items/${id}/release`, null, {
    params: { userId },
  })
  return response.data
}

export const completeWorkItem = async (id, userId) => {
  const response = await api.post(`/work-items/${id}/complete`, null, {
    params: { userId },
  })
  return response.data
}

export const reassignWorkItem = async (id, targetUserId, actingUserId) => {
  const response = await api.post(`/work-items/${id}/reassign`, {
    targetUserId,
    actingUserId,
  })
  return response.data
}

export const changePriority = async (id, priority, actorId) => {
  const response = await api.patch(`/work-items/${id}/priority`, {
    priority,
    actorId,
  })
  return response.data
}

export const addNote = async (id, note, actorId) => {
  const response = await api.post(`/work-items/${id}/notes`, {
    note,
    actorId,
  })
  return response.data
}

export const getActivityHistory = async (id) => {
  const response = await api.get(`/work-items/${id}/history`)
  return response.data
}
