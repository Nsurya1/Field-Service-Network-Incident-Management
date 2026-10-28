import React, { useState } from 'react';
import { incidentService } from '../services/incidentService';
import { X, AlertCircle, ArrowRight, CheckCircle2 } from 'lucide-react';

export default function StatusTransitionModal({ incident, isOpen, onClose, onSuccess }) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen || !incident) return null;

  // State machine transition definitions
  const getAllowedTransitions = (currentStatus) => {
    switch (currentStatus) {
      case 'OPEN':
        return [
          { status: 'IN_PROGRESS', label: 'Start Work (IN_PROGRESS)', color: 'bg-amber-600 hover:bg-amber-700' },
          { status: 'CLOSED', label: 'Close Ticket (CLOSED)', color: 'bg-slate-700 hover:bg-slate-800' },
        ];
      case 'ASSIGNED':
        return [
          { status: 'IN_PROGRESS', label: 'Commence Field Work (IN_PROGRESS)', color: 'bg-amber-600 hover:bg-amber-700' },
          { status: 'OPEN', label: 'Unassign / Re-Open (OPEN)', color: 'bg-blue-600 hover:bg-blue-700' },
          { status: 'CLOSED', label: 'Cancel Ticket (CLOSED)', color: 'bg-slate-700 hover:bg-slate-800' },
        ];
      case 'IN_PROGRESS':
        return [
          { status: 'RESOLVED', label: 'Resolve Incident (RESOLVED)', color: 'bg-emerald-600 hover:bg-emerald-700' },
          { status: 'ASSIGNED', label: 'Revert to Assigned (ASSIGNED)', color: 'bg-purple-600 hover:bg-purple-700' },
          { status: 'CLOSED', label: 'Close Ticket (CLOSED)', color: 'bg-slate-700 hover:bg-slate-800' },
        ];
      case 'RESOLVED':
        return [
          { status: 'CLOSED', label: 'Sign-Off & Close (CLOSED)', color: 'bg-slate-800 hover:bg-slate-900' },
          { status: 'IN_PROGRESS', label: 'Re-Open for Investigation (IN_PROGRESS)', color: 'bg-amber-600 hover:bg-amber-700' },
        ];
      default:
        return [];
    }
  };

  const allowedTransitions = getAllowedTransitions(incident.status);

  const handleTransition = async (targetStatus) => {
    setSubmitting(true);
    setError('');
    try {
      await incidentService.updateStatus(incident.id, targetStatus);
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update incident status.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div>
            <h3 className="font-bold text-base text-slate-900">Advance Incident Lifecycle</h3>
            <p className="text-xs text-slate-500">Incident #{incident.id}: {incident.title}</p>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200/50 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-xs flex items-start">
              <AlertCircle className="w-4 h-4 mr-2 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <div className="flex items-center justify-between bg-slate-50 p-3 rounded-xl border border-slate-200">
            <span className="text-xs font-semibold text-slate-500">Current Status</span>
            <span className="text-xs font-bold px-2 py-0.5 bg-indigo-100 text-indigo-700 rounded-md border border-indigo-200">
              {incident.status}
            </span>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-2">Available Next States</label>
            {allowedTransitions.length === 0 ? (
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-500 text-center italic">
                This incident is in a terminal state ({incident.status}) and cannot transition further.
              </div>
            ) : (
              <div className="space-y-2">
                {allowedTransitions.map((t) => (
                  <button
                    key={t.status}
                    type="button"
                    disabled={submitting}
                    onClick={() => handleTransition(t.status)}
                    className={`w-full flex items-center justify-between px-4 py-3 text-white text-xs font-semibold rounded-xl transition shadow-xs disabled:opacity-50 ${t.color}`}
                  >
                    <span>{t.label}</span>
                    <ArrowRight className="w-4 h-4 ml-2" />
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="pt-2 text-right">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-slate-300 text-slate-700 rounded-lg text-xs font-medium hover:bg-slate-50 transition"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
