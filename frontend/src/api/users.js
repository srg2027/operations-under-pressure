import api from './axios'

export const getUsers = async (activeOnly = true) => {
  const response = await api.get('/users', {
    params: { activeOnly },
  })
  return response.data
}

export const getUser = async (id) => {
  const response = await api.get(`/users/${id}`)
  return response.data
}

export const createUser = async ({ displayName, email, active = true }) => {
  const response = await api.post('/users', { displayName, email, active })
  return response.data
}
