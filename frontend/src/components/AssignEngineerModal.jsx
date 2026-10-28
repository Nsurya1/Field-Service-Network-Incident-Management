import React, { useEffect, useState } from 'react';
import { engineerService } from '../services/engineerService';
import { incidentService } from '../services/incidentService';
import { X, AlertCircle, UserCheck, Wrench } from 'lucide-react';

export default function AssignEngineerModal({ incident, isOpen, onClose, onSuccess }) {
  const [engineers, setEngineers] = useState([]);
  const [selectedEngineerId, setSelectedEngineerId] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setError('');
    const loadEngineers = async () => {
      try {
        const data = await engineerService.getEngineers();
        setEngineers(data);
        if (incident?.assignedEngineerId) {
          setSelectedEngineerId(String(incident.assignedEngineerId));
        } else if (data.length > 0) {
          setSelectedEngineerId(String(data[0].id));
        }
      } catch (err) {
        setError('Failed to fetch available engineers.');
      } finally {
        setLoading(false);
      }
    };
    loadEngineers();
  }, [isOpen, incident]);

  if (!isOpen || !incident) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedEngineerId) return;

    setSubmitting(true);
    setError('');
    try {
      await incidentService.assignIncident(incident.id, Number(selectedEngineerId));
      onSuccess();
      onClose();
    } catch (err) {
      // Handles 409 Conflict (e.g. reached 3 active tickets) or other business validation
      setError(err.response?.data?.message || 'Assignment failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div>
            <h3 className="font-bold text-base text-slate-900">Assign Field Engineer</h3>
            <p className="text-xs text-slate-500">Incident #{incident.id}: {incident.title}</p>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200/50 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-xs flex items-start">
              <AlertCircle className="w-4 h-4 mr-2 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <div className="bg-indigo-50/60 p-3 rounded-xl border border-indigo-100 text-xs text-indigo-900">
            <strong>Workload Guardrail:</strong> Each technician can have at most{' '}
            <span className="font-bold">3 active incidents</span> (ASSIGNED or IN_PROGRESS) concurrently.
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-2">Select Field Engineer</label>
            {loading ? (
              <div className="py-4 text-center text-xs text-slate-500">Loading technician pool...</div>
            ) : engineers.length === 0 ? (
              <div className="py-4 text-center text-xs text-slate-500 italic">No engineers registered yet.</div>
            ) : (
              <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                {engineers.map((eng) => (
                  <label
                    key={eng.id}
                    className={`flex items-center justify-between p-3 rounded-xl border cursor-pointer transition ${
                      selectedEngineerId === String(eng.id)
                        ? 'border-indigo-600 bg-indigo-50/30 ring-1 ring-indigo-600'
                        : 'border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-center space-x-3">
                      <input
                        type="radio"
                        name="engineer"
                        value={eng.id}
                        checked={selectedEngineerId === String(eng.id)}
                        onChange={() => setSelectedEngineerId(String(eng.id))}
                        className="text-indigo-600 focus:ring-indigo-500"
                      />
                      <div>
                        <div className="text-sm font-semibold text-slate-900">{eng.name}</div>
                        <div className="text-xs text-slate-500 flex items-center space-x-2">
                          <span>{eng.email}</span>
                          <span>•</span>
                          <span className="inline-flex items-center text-slate-600">
                            <Wrench className="w-3 h-3 mr-1 text-slate-400" />
                            {eng.specialization}
                          </span>
                        </div>
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>

          <div className="pt-2 flex items-center justify-end space-x-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-slate-300 text-slate-700 rounded-lg text-xs font-medium hover:bg-slate-50 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || engineers.length === 0}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-medium shadow-xs disabled:opacity-50 transition flex items-center"
            >
              <UserCheck className="w-4 h-4 mr-1.5" />
              {submitting ? 'Assigning...' : 'Confirm Assignment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
