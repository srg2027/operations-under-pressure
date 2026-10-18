import api from './axios'

export const getTeams = async () => {
  const response = await api.get('/teams')
  return response.data
}

export const getTeam = async (id) => {
  const response = await api.get(`/teams/${id}`)
  return response.data
}

export const createTeam = async ({ name, description }) => {
  const response = await api.post('/teams', { name, description })
  return response.data
}

export const getTeamMembers = async (teamId) => {
  const response = await api.get(`/teams/${teamId}/members`)
  return response.data
}

export const addTeamMember = async (teamId, { userId, role }, actingUserId) => {
  const params = actingUserId ? { actingUserId } : {}
  const response = await api.post(`/teams/${teamId}/members`, { userId, role }, { params })
  return response.data
}

export const removeTeamMember = async (teamId, userId, actingUserId) => {
  const response = await api.delete(`/teams/${teamId}/members/${userId}`, {
    params: { actingUserId },
  })
  return response.data
}

export const removeMember = removeTeamMember

