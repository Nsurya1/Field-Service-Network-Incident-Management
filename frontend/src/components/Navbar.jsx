import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Shield, User, LogOut, Plus, Users, Activity } from 'lucide-react';

export default function Navbar({ onOpenCreate, onOpenEngineers, activeTab = 'incidents', onSelectTab }) {
  const { user, logout, hasRole } = useAuth();

  const getRoleBadgeColor = (role) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'bg-purple-100 text-purple-700 border-purple-200';
      case 'ROLE_MANAGER':
        return 'bg-blue-100 text-blue-700 border-blue-200';
      default:
        return 'bg-emerald-100 text-emerald-700 border-emerald-200';
    }
  };

  const formatRole = (role) => {
    if (!role) return '';
    return role.replace('ROLE_', '');
  };

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-indigo-600 rounded-lg text-white shadow-sm">
              <Activity className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-lg text-slate-900 tracking-tight">FieldService</span>
              <span className="hidden sm:inline-block ml-2 text-xs font-medium text-slate-500 border border-slate-200 rounded px-1.5 py-0.5">
                Incident Dispatch
              </span>
            </div>

            {onSelectTab && (
              <nav className="hidden sm:flex items-center space-x-1 ml-6 border-l border-slate-200 pl-6">
                <button
                  onClick={() => onSelectTab('incidents')}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer ${
                    activeTab === 'incidents'
                      ? 'bg-indigo-50 text-indigo-700'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Incidents
                </button>
                <button
                  onClick={() => onSelectTab('reports')}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer ${
                    activeTab === 'reports'
                      ? 'bg-indigo-50 text-indigo-700'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Reports & MTTR
                </button>
              </nav>
            )}
          </div>

          <div className="flex items-center space-x-3">
            {hasRole('ROLE_ADMIN', 'ROLE_MANAGER') && (
              <button
                onClick={onOpenCreate}
                className="inline-flex items-center px-3.5 py-2 border border-transparent text-sm font-medium rounded-lg text-white bg-indigo-600 hover:bg-indigo-700 shadow-xs transition"
              >
                <Plus className="w-4 h-4 mr-1.5" />
                New Incident
              </button>
            )}

            {hasRole('ROLE_ADMIN', 'ROLE_MANAGER') && (
              <button
                onClick={onOpenEngineers}
                className="inline-flex items-center px-3 py-2 border border-slate-300 text-sm font-medium rounded-lg text-slate-700 bg-white hover:bg-slate-50 transition"
              >
                <Users className="w-4 h-4 mr-1.5 text-slate-500" />
                Engineers
              </button>
            )}

            <div className="h-6 w-px bg-slate-200 mx-1"></div>

            <div className="flex items-center space-x-2 pl-1">
              <div className="flex items-center space-x-2">
                <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-600 font-semibold text-xs">
                  {user?.username?.substring(0, 2).toUpperCase() || 'U'}
                </div>
                <div className="hidden md:block text-left">
                  <div className="text-sm font-medium text-slate-800 leading-tight">{user?.username}</div>
                  <span className={`inline-block text-[10px] font-semibold px-1.5 py-0.2 rounded border ${getRoleBadgeColor(user?.role)}`}>
                    {formatRole(user?.role)}
                  </span>
                </div>
              </div>

              <button
                onClick={logout}
                title="Log out"
                className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
