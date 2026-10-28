import React, { useEffect, useState } from 'react';
import { incidentService } from '../services/incidentService';
import { X, Clock, MapPin, User, Calendar, History, ShieldAlert } from 'lucide-react';

export default function IncidentDetailModal({ incident, onClose }) {
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(true);

  useEffect(() => {
    if (!incident) return;
    const fetchHistory = async () => {
      try {
        const data = await incidentService.getHistory(incident.id);
        setHistory(data);
      } catch (err) {
        console.error('Failed to load history', err);
      } finally {
        setLoadingHistory(false);
      }
    };
    fetchHistory();
  }, [incident]);

  if (!incident) return null;

  const formatDate = (isoString) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString();
  };

  const getActionBadgeColor = (action) => {
    switch (action) {
      case 'CREATED':
        return 'bg-blue-100 text-blue-700 border-blue-200';
      case 'ASSIGNED':
        return 'bg-purple-100 text-purple-700 border-purple-200';
      case 'STATUS_CHANGED':
        return 'bg-amber-100 text-amber-700 border-amber-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div className="flex items-center space-x-2">
            <span className="font-mono text-xs font-bold text-slate-500 bg-white border border-slate-200 px-2 py-0.5 rounded">
              INCIDENT #{incident.id}
            </span>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-200">
              {incident.status}
            </span>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200/50 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content body */}
        <div className="p-6 overflow-y-auto space-y-6">
          <div>
            <h2 className="text-xl font-bold text-slate-900 leading-snug">{incident.title}</h2>
            <div className="flex flex-wrap items-center gap-y-2 gap-x-4 text-xs text-slate-500 mt-2">
              <div className="flex items-center">
                <MapPin className="w-3.5 h-3.5 mr-1 text-slate-400" />
                {incident.location || 'Remote'}
              </div>
              <div className="flex items-center">
                <ShieldAlert className="w-3.5 h-3.5 mr-1 text-slate-400" />
                Priority: <span className="font-semibold text-slate-800 ml-1">{incident.priority}</span>
              </div>
              <div className="flex items-center">
                <Clock className="w-3.5 h-3.5 mr-1 text-slate-400" />
                SLA: <span className="font-semibold text-slate-800 ml-1">{formatDate(incident.slaDeadline)}</span>
                {incident.slaBreached && (
                  <span className="ml-1 text-[10px] font-bold text-rose-600 bg-rose-50 border border-rose-200 rounded px-1">
                    BREACHED
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1">Description</h4>
            <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-wrap">{incident.description || 'No description provided.'}</p>
          </div>

          {/* Assigned Engineer details */}
          <div className="bg-white rounded-xl p-4 border border-slate-200 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-xl">
                <User className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs text-slate-500 font-medium">Assigned Field Engineer</div>
                <div className="text-sm font-semibold text-slate-900">
                  {incident.assignedEngineerName ? `${incident.assignedEngineerName} (ID: #${incident.assignedEngineerId})` : 'Unassigned'}
                </div>
              </div>
            </div>
          </div>

          {/* Audit History Timeline */}
          <div>
            <div className="flex items-center space-x-2 mb-3">
              <History className="w-4 h-4 text-slate-500" />
              <h3 className="text-sm font-semibold text-slate-900">Audit History & Timeline</h3>
            </div>

            {loadingHistory ? (
              <div className="py-6 text-center text-xs text-slate-500">Loading audit events...</div>
            ) : history.length === 0 ? (
              <div className="py-6 text-center text-xs text-slate-400 italic">No audit records found.</div>
            ) : (
              <div className="relative pl-6 border-l-2 border-slate-200 space-y-4 my-2">
                {history.map((entry) => (
                  <div key={entry.id} className="relative group">
                    <div className="absolute -left-[31px] top-1.5 w-3 h-3 bg-white border-2 border-indigo-600 rounded-full"></div>
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className={`text-[10px] font-bold px-1.5 py-0.2 rounded border uppercase tracking-wider ${getActionBadgeColor(entry.action)}`}>
                          {entry.action}
                        </span>
                        <span className="text-[11px] text-slate-400 font-mono">{formatDate(entry.timestamp)}</span>
                      </div>
                      <p className="text-xs text-slate-700 mt-1">{entry.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-200 bg-slate-50 text-right">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-white border border-slate-300 text-slate-700 font-medium text-xs rounded-lg hover:bg-slate-50 transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
