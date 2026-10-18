import axios from 'axios'

// Base Axios instance — all /api/* calls go through the Vite proxy to :8080
const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

export default api
