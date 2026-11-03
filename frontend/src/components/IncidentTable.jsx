import React from 'react';
import { useAuth } from '../context/AuthContext';
import { AlertCircle, UserCheck, Eye, RefreshCw, ChevronLeft, ChevronRight, Clock, MapPin } from 'lucide-react';

export default function IncidentTable({
  incidents,
  page,
  totalPages,
  totalElements,
  onPageChange,
  onSelectIncident,
  onOpenAssign,
  onOpenStatus,
  loading,
}) {
  const { hasRole } = useAuth();

  const getPriorityBadge = (priority) => {
    switch (priority) {
      case 'CRITICAL':
        return 'bg-red-50 text-red-700 border-red-200 ring-1 ring-red-500/20';
      case 'HIGH':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'MEDIUM':
        return 'bg-yellow-50 text-yellow-700 border-yellow-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'OPEN':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'ASSIGNED':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'IN_PROGRESS':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'RESOLVED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'CLOSED':
        return 'bg-slate-100 text-slate-600 border-slate-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  };

  const formatDeadline = (isoString) => {
    if (!isoString) return 'None';
    const date = new Date(isoString);
    return date.toLocaleString([], {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-12 text-center shadow-xs">
        <div className="inline-flex items-center justify-center p-3 bg-indigo-50 text-indigo-600 rounded-full mb-3 animate-spin">
          <RefreshCw className="w-6 h-6" />
        </div>
        <p className="text-sm font-medium text-slate-600">Loading incident records...</p>
      </div>
    );
  }

  if (!incidents || incidents.length === 0) {
    const isEngineerOnly = hasRole('ROLE_ENGINEER') && !hasRole('ROLE_ADMIN', 'ROLE_MANAGER');
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-12 text-center shadow-xs">
        <div className="inline-flex items-center justify-center p-3 bg-slate-100 text-slate-500 rounded-full mb-3">
          <AlertCircle className="w-6 h-6" />
        </div>
        <h3 className="text-base font-semibold text-slate-900">
          {isEngineerOnly ? 'No Incidents Assigned to You' : 'No Incidents Found'}
        </h3>
        <p className="text-sm text-slate-500 mt-1 max-w-md mx-auto">
          {isEngineerOnly
            ? 'You are logged in as a Field Technician. Only incidents specifically dispatched and assigned to your profile appear here. When a manager or dispatcher assigns an incident to you, it will display here automatically.'
            : 'There are no incident records matching your active filters.'}
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
          <thead className="bg-slate-50 text-slate-600 font-semibold text-xs uppercase tracking-wider">
            <tr>
              <th scope="col" className="px-5 py-3.5">ID</th>
              <th scope="col" className="px-5 py-3.5">Incident & Location</th>
              <th scope="col" className="px-4 py-3.5">Priority</th>
              <th scope="col" className="px-4 py-3.5">Status</th>
              <th scope="col" className="px-5 py-3.5">SLA Target</th>
              <th scope="col" className="px-5 py-3.5">Assigned Engineer</th>
              <th scope="col" className="px-5 py-3.5 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 bg-white">
            {incidents.map((inc) => (
              <tr key={inc.id} className="hover:bg-slate-50/70 transition">
                <td className="px-5 py-4 font-mono text-xs font-semibold text-slate-500">
                  #{inc.id}
                </td>
                <td className="px-5 py-4 max-w-xs">
                  <div className="font-medium text-slate-900 truncate">{inc.title}</div>
                  <div className="flex items-center text-xs text-slate-500 mt-0.5 truncate">
                    <MapPin className="w-3.5 h-3.5 mr-1 text-slate-400 shrink-0" />
                    <span>{inc.location || 'Remote / Unspecified'}</span>
                  </div>
                </td>
                <td className="px-4 py-4 whitespace-nowrap">
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium border ${getPriorityBadge(inc.priority)}`}>
                    {inc.priority}
                  </span>
                </td>
                <td className="px-4 py-4 whitespace-nowrap">
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium border ${getStatusBadge(inc.status)}`}>
                    {inc.status}
                  </span>
                </td>
                <td className="px-5 py-4 whitespace-nowrap">
                  <div className="flex items-center space-x-1.5">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    <span className="text-xs text-slate-700">{formatDeadline(inc.slaDeadline)}</span>
                  </div>
                  {inc.slaBreached && inc.status !== 'RESOLVED' && inc.status !== 'CLOSED' && (
                    <span className="inline-block mt-1 text-[10px] font-bold uppercase tracking-wider bg-rose-100 text-rose-700 border border-rose-300 rounded px-1.5 py-0.2">
                      BREACHED
                    </span>
                  )}
                </td>
                <td className="px-5 py-4 whitespace-nowrap">
                  {inc.assignedEngineerName ? (
                    <div>
                      <div className="text-xs font-semibold text-slate-800">{inc.assignedEngineerName}</div>
                      <div className="text-[11px] text-slate-500 font-mono">ID: #{inc.assignedEngineerId}</div>
                    </div>
                  ) : (
                    <span className="text-xs italic text-slate-400">Unassigned</span>
                  )}
                </td>
                <td className="px-5 py-4 whitespace-nowrap text-right space-x-1.5">
                  <button
                    onClick={() => onSelectIncident(inc)}
                    className="p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                    title="View Timeline & Details"
                  >
                    <Eye className="w-4 h-4" />
                  </button>

                  {hasRole('ROLE_ADMIN', 'ROLE_MANAGER') && inc.status !== 'CLOSED' && (
                    <button
                      onClick={() => onOpenAssign(inc)}
                      className="p-1.5 text-slate-500 hover:text-purple-600 hover:bg-purple-50 rounded-lg transition"
                      title="Assign Engineer"
                    >
                      <UserCheck className="w-4 h-4" />
                    </button>
                  )}

                  {inc.status !== 'CLOSED' && (
                    <button
                      onClick={() => onOpenStatus(inc)}
                      className="p-1.5 text-slate-500 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition"
                      title="Advance Status"
                    >
                      <RefreshCw className="w-4 h-4" />
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination footer */}
      <div className="px-5 py-3.5 bg-slate-50 border-t border-slate-200 flex items-center justify-between">
        <div className="text-xs text-slate-500">
          Showing page <span className="font-semibold text-slate-700">{page + 1}</span> of{' '}
          <span className="font-semibold text-slate-700">{Math.max(totalPages, 1)}</span> ({totalElements} total incidents)
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => onPageChange(page - 1)}
            disabled={page === 0}
            className="p-1.5 border border-slate-300 rounded-lg bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <button
            onClick={() => onPageChange(page + 1)}
            disabled={page + 1 >= totalPages}
            className="p-1.5 border border-slate-300 rounded-lg bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
