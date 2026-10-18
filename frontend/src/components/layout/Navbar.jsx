import React, { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useSession } from '../../context/SessionContext'
import {
  Layers,
  LayoutDashboard,
  CheckSquare,
  Users,
  UserCheck,
  LogOut,
  Menu,
  X,
  Info,
} from 'lucide-react'

export const Navbar = () => {
  const { currentUser, clearSession } = useSession()
  const location = useLocation()
  const navigate = useNavigate()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const navLinks = [
    { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { name: 'Work Items', path: '/work-items', icon: CheckSquare },
    { name: 'Teams', path: '/teams', icon: Users },
  ]

  const isActive = (path) => location.pathname === path

  const handleSwitchUser = () => {
    clearSession()
    navigate('/')
  }

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-40">
      {/* Demo Identity Disclaimer Banner */}
      <div className="bg-indigo-900 text-indigo-100 text-[11px] py-1 px-4 text-center flex items-center justify-center space-x-1.5 font-medium">
        <Info className="w-3.5 h-3.5 shrink-0 text-indigo-300" />
        <span>
          <strong>Demo Identity Mode:</strong> The backend has no authentication service. Operations are executed using the selected user ID.
        </span>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          {/* Logo & Primary Nav */}
          <div className="flex items-center space-x-8">
            <Link to="/dashboard" className="flex items-center space-x-2.5">
              <div className="w-9 h-9 rounded-lg bg-indigo-600 flex items-center justify-center text-white shadow-sm">
                <Layers className="w-5 h-5" />
              </div>
              <div className="flex flex-col">
                <span className="font-bold text-slate-900 text-sm sm:text-base leading-tight tracking-tight">
                  Newtonite Operations
                </span>
                <span className="text-[10px] text-indigo-600 font-semibold uppercase tracking-wider">
                  Under Pressure
                </span>
              </div>
            </Link>

            {currentUser && (
              <nav className="hidden md:flex space-x-1">
                {navLinks.map((link) => {
                  const Icon = link.icon
                  const active = isActive(link.path)
                  return (
                    <Link
                      key={link.path}
                      to={link.path}
                      className={`inline-flex items-center px-3 py-2 rounded-md text-xs font-medium transition-colors ${
                        active
                          ? 'bg-indigo-50 text-indigo-700 font-semibold'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                      }`}
                    >
                      <Icon className="w-4 h-4 mr-1.5" />
                      {link.name}
                    </Link>
                  )
                })}
              </nav>
            )}
          </div>

          {/* User Session Bar & Mobile Button */}
          <div className="flex items-center space-x-3">
            {currentUser ? (
              <div className="hidden sm:flex items-center space-x-3 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5">
                <div className="w-7 h-7 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs">
                  {currentUser.displayName ? currentUser.displayName.charAt(0).toUpperCase() : 'U'}
                </div>
                <div className="text-left text-xs">
                  <div className="font-semibold text-slate-800 leading-none flex items-center gap-1.5">
                    {currentUser.displayName}
                    <span className="bg-amber-100 text-amber-800 text-[10px] px-1.5 py-0.2 rounded font-medium">
                      Demo User
                    </span>
                  </div>
                  <div className="text-[10px] text-slate-500 mt-0.5 leading-none">
                    {currentUser.email}
                  </div>
                </div>
                <button
                  onClick={handleSwitchUser}
                  title="Switch user identity"
                  className="p-1 hover:bg-slate-200 rounded text-slate-500 hover:text-slate-800 transition"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <Link
                to="/"
                className="inline-flex items-center px-3 py-1.5 text-xs font-semibold rounded-md bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm transition"
              >
                <UserCheck className="w-3.5 h-3.5 mr-1" />
                Select Identity
              </Link>
            )}

            {/* Mobile Hamburger */}
            {currentUser && (
              <div className="flex md:hidden">
                <button
                  onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                  className="p-2 rounded-md text-slate-500 hover:text-slate-700 hover:bg-slate-100 focus:outline-none"
                >
                  {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {currentUser && mobileMenuOpen && (
        <div className="md:hidden border-t border-slate-200 bg-white px-4 pt-2 pb-4 space-y-2">
          {/* Mobile User Profile */}
          <div className="py-2 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-sm">
                {currentUser.displayName ? currentUser.displayName.charAt(0).toUpperCase() : 'U'}
              </div>
              <div>
                <p className="text-xs font-semibold text-slate-800">{currentUser.displayName}</p>
                <p className="text-[10px] text-slate-500">{currentUser.email}</p>
              </div>
            </div>
            <button
              onClick={() => {
                setMobileMenuOpen(false)
                handleSwitchUser()
              }}
              className="text-xs text-indigo-600 font-semibold"
            >
              Switch
            </button>
          </div>

          <div className="space-y-1">
            {navLinks.map((link) => {
              const Icon = link.icon
              const active = isActive(link.path)
              return (
                <Link
                  key={link.path}
                  to={link.path}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`flex items-center px-3 py-2 rounded-md text-sm font-medium ${
                    active ? 'bg-indigo-50 text-indigo-700 font-bold' : 'text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <Icon className="w-4 h-4 mr-2" />
                  {link.name}
                </Link>
              )
            })}
          </div>
        </div>
      )}
    </header>
  )
}
