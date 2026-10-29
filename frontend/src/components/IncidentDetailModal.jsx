import React, { useEffect, useState } from 'react';
import { incidentService } from '../services/incidentService';
import { X, Clock, MapPin, User, Calendar, History, ShieldAlert, Wrench, CheckCircle2 } from 'lucide-react';

export default function IncidentDetailModal({ incident, onClose }) {
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(true);
  const [visits, setVisits] = useState([]);
  const [loadingVisits, setLoadingVisits] = useState(true);
  const [showVisitForm, setShowVisitForm] = useState(false);
  const [workNotes, setWorkNotes] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [submittingVisit, setSubmittingVisit] = useState(false);
  const [visitError, setVisitError] = useState('');

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

    const fetchVisits = async () => {
      try {
        const data = await incidentService.getVisits(incident.id);
        setVisits(data);
      } catch (err) {
        console.error('Failed to load visits', err);
      } finally {
        setLoadingVisits(false);
      }
    };

    fetchHistory();
    fetchVisits();
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
      case 'SERVICE_VISIT':
        return 'bg-emerald-100 text-emerald-700 border-emerald-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  const handleRecordVisit = async (e) => {
    e.preventDefault();
    if (!workNotes.trim() || workNotes.trim().length < 5) {
      setVisitError('Work notes must be at least 5 characters');
      return;
    }

    setSubmittingVisit(true);
    setVisitError('');
    try {
      const now = new Date().toISOString();
      const newVisit = await incidentService.createVisit(incident.id, {
        workNotes: workNotes.trim(),
        resolutionNotes: resolutionNotes.trim() || null,
        checkInAt: now,
        checkOutAt: now
      });
      setVisits((prev) => [...prev, newVisit]);
      setWorkNotes('');
      setResolutionNotes('');
      setShowVisitForm(false);
      // Refresh history to include visit event
      const updatedHistory = await incidentService.getHistory(incident.id);
      setHistory(updatedHistory);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to record service visit';
      setVisitError(msg);
    } finally {
      setSubmittingVisit(false);
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

          {/* Resolution Details Card (if resolved) */}
          {incident.resolutionNotes && (
            <div className="bg-emerald-50 rounded-xl p-4 border border-emerald-200">
              <div className="flex items-center space-x-1.5 mb-1 text-emerald-800">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <h4 className="text-xs font-bold uppercase tracking-wider">Resolution Details</h4>
                {incident.rootCause && (
                  <span className="ml-2 text-[10px] font-mono font-semibold px-2 py-0.5 rounded bg-white text-emerald-700 border border-emerald-300">
                    Root Cause: {incident.rootCause}
                  </span>
                )}
              </div>
              <p className="text-sm text-emerald-950 leading-relaxed whitespace-pre-wrap mt-1">{incident.resolutionNotes}</p>
            </div>
          )}

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

          {/* Field Service Visits Section */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <Wrench className="w-4 h-4 text-emerald-600" />
                <h3 className="text-sm font-semibold text-slate-900">On-Site Service Visits ({visits.length})</h3>
              </div>
              {incident.status !== 'CLOSED' && (
                <button
                  onClick={() => setShowVisitForm(!showVisitForm)}
                  className="text-xs font-medium text-indigo-600 hover:text-indigo-800 transition"
                >
                  {showVisitForm ? 'Cancel' : '+ Record Visit'}
                </button>
              )}
            </div>

            {/* Record Visit Form */}
            {showVisitForm && (
              <form onSubmit={handleRecordVisit} className="p-4 bg-slate-50 border border-slate-200 rounded-xl mb-4 space-y-3">
                <h4 className="text-xs font-bold text-slate-700">Record Field Service Visit</h4>
                {visitError && (
                  <div className="text-xs text-rose-600 bg-rose-50 border border-rose-200 p-2 rounded">
                    {visitError}
                  </div>
                )}
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">Work Notes (min 5 chars) *</label>
                  <textarea
                    rows={2}
                    value={workNotes}
                    onChange={(e) => setWorkNotes(e.target.value)}
                    placeholder="Describe diagnostic checks, equipment tested, or cables replaced..."
                    className="w-full text-xs p-2.5 border border-slate-300 rounded-lg focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">Resolution Findings (optional)</label>
                  <input
                    type="text"
                    value={resolutionNotes}
                    onChange={(e) => setResolutionNotes(e.target.value)}
                    placeholder="Optional root cause or final notes..."
                    className="w-full text-xs p-2 border border-slate-300 rounded-lg focus:ring-1 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>
                <div className="flex justify-end space-x-2">
                  <button
                    type="button"
                    onClick={() => setShowVisitForm(false)}
                    className="px-3 py-1.5 text-xs text-slate-600 bg-white border border-slate-300 rounded-lg hover:bg-slate-100"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submittingVisit}
                    className="px-3 py-1.5 text-xs text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 disabled:opacity-50"
                  >
                    {submittingVisit ? 'Saving...' : 'Save Visit'}
                  </button>
                </div>
              </form>
            )}

            {loadingVisits ? (
              <div className="py-4 text-center text-xs text-slate-500">Loading service visits...</div>
            ) : visits.length === 0 ? (
              <div className="py-3 px-4 bg-slate-50 border border-slate-200 rounded-xl text-center text-xs text-slate-400 italic">
                No on-site service visits recorded yet.
              </div>
            ) : (
              <div className="space-y-3">
                {visits.map((visit) => (
                  <div key={visit.id} className="p-3 bg-white border border-slate-200 rounded-xl shadow-xs space-y-1.5">
                    <div className="flex items-center justify-between text-xs">
                      <div className="flex items-center space-x-2">
                        <span className="font-semibold text-slate-800">{visit.engineerName || `Engineer #${visit.engineerId}`}</span>
                        <span className="text-[10px] text-slate-400 font-mono">Date: {visit.visitDate}</span>
                      </div>
                      <span className="text-[10px] bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded font-mono">
                        Visit #{visit.id}
                      </span>
                    </div>
                    <p className="text-xs text-slate-700"><span className="font-medium text-slate-500">Work Notes:</span> {visit.workNotes}</p>
                    {visit.resolutionNotes && (
                      <p className="text-xs text-emerald-800 bg-emerald-50/60 p-1.5 rounded border border-emerald-100">
                        <span className="font-semibold text-emerald-700">Resolution:</span> {visit.resolutionNotes}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
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
