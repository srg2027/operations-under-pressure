import React, { createContext, useContext, useState, useEffect } from 'react'
import { getUsers, getUser } from '../api/users'

const SessionContext = createContext(null)

const STORAGE_KEY = 'operations_demo_user_id'

export const SessionProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null)
  const [users, setUsers] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadUsersAndSession = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const activeUsers = await getUsers(true)
      setUsers(activeUsers || [])

      const savedUserId = localStorage.getItem(STORAGE_KEY)
      if (savedUserId && activeUsers && activeUsers.length > 0) {
        const found = activeUsers.find((u) => String(u.id) === String(savedUserId))
        if (found) {
          setCurrentUser(found)
        } else {
          // Attempt to fetch user individually if not in active list
          try {
            const fetched = await getUser(savedUserId)
            setCurrentUser(fetched)
          } catch {
            localStorage.removeItem(STORAGE_KEY)
            setCurrentUser(null)
          }
        }
      } else {
        setCurrentUser(null)
      }
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to connect to backend')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadUsersAndSession()
  }, [])

  const selectUser = (user) => {
    if (user && user.id) {
      localStorage.setItem(STORAGE_KEY, String(user.id))
      setCurrentUser(user)
    }
  }

  const clearSession = () => {
    localStorage.removeItem(STORAGE_KEY)
    setCurrentUser(null)
  }

  return (
    <SessionContext.Provider
      value={{
        currentUser,
        users,
        isLoading,
        error,
        selectUser,
        clearSession,
        refreshUsers: loadUsersAndSession,
        isDemoIdentityMode: true,
      }}
    >
      {children}
    </SessionContext.Provider>
  )
}

export const useSession = () => {
  const context = useContext(SessionContext)
  if (!context) {
    throw new Error('useSession must be used within a SessionProvider')
  }
  return context
}
