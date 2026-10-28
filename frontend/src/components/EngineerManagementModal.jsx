import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { engineerService } from '../services/engineerService';
import { X, Plus, AlertCircle, CheckCircle2, User, Wrench, Mail, Calendar } from 'lucide-react';

export default function EngineerManagementModal({ isOpen, onClose }) {
  const { hasRole } = useAuth();
  const [engineers, setEngineers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddForm, setShowAddForm] = useState(false);
  const [formData, setFormData] = useState({ name: '', email: '', specialization: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchEngineers = async () => {
    setLoading(true);
    try {
      const data = await engineerService.getEngineers();
      setEngineers(data);
    } catch (err) {
      setError('Failed to fetch engineers.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isOpen) return;
    setError('');
    setSuccess('');
    setShowAddForm(false);
    fetchEngineers();
  }, [isOpen]);

  if (!isOpen) return null;

  const handleCreate = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSubmitting(true);
    try {
      await engineerService.createEngineer(formData);
      setSuccess('Engineer added successfully!');
      setFormData({ name: '', email: '', specialization: '' });
      setShowAddForm(false);
      fetchEngineers();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create engineer.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[85vh] flex flex-col shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div>
            <h3 className="font-bold text-base text-slate-900">Field Engineer Registry</h3>
            <p className="text-xs text-slate-500">Manage dispatch technician pool</p>
          </div>
          <button onClick={onClose} className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200/50 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 overflow-y-auto space-y-4">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-xs flex items-center">
              <AlertCircle className="w-4 h-4 mr-2 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-700 text-xs flex items-center">
              <CheckCircle2 className="w-4 h-4 mr-2 shrink-0" />
              <span>{success}</span>
            </div>
          )}

          {hasRole('ROLE_ADMIN') && (
            <div className="flex justify-end">
              <button
                onClick={() => setShowAddForm(!showAddForm)}
                className="inline-flex items-center px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-medium transition"
              >
                <Plus className="w-3.5 h-3.5 mr-1" />
                {showAddForm ? 'Cancel Add' : 'Register New Engineer'}
              </button>
            </div>
          )}

          {showAddForm && (
            <form onSubmit={handleCreate} className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700">Add Field Technician</h4>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <input
                  type="text"
                  required
                  placeholder="Full Name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="px-3 py-1.5 bg-white border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500 outline-none"
                />
                <input
                  type="email"
                  required
                  placeholder="Email Address"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="px-3 py-1.5 bg-white border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500 outline-none"
                />
                <input
                  type="text"
                  placeholder="Specialization (e.g. Fiber)"
                  value={formData.specialization}
                  onChange={(e) => setFormData({ ...formData, specialization: e.target.value })}
                  className="px-3 py-1.5 bg-white border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500 outline-none"
                />
              </div>
              <div className="text-right">
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold disabled:opacity-50 transition"
                >
                  {submitting ? 'Saving...' : 'Save Engineer'}
                </button>
              </div>
            </form>
          )}

          {/* Engineer list table */}
          {loading ? (
            <div className="py-8 text-center text-xs text-slate-500">Loading technician records...</div>
          ) : engineers.length === 0 ? (
            <div className="py-8 text-center text-xs text-slate-400 italic">No engineers registered yet.</div>
          ) : (
            <div className="divide-y divide-slate-200 border border-slate-200 rounded-xl overflow-hidden">
              {engineers.map((eng) => (
                <div key={eng.id} className="p-4 bg-white flex items-center justify-between hover:bg-slate-50 transition">
                  <div className="flex items-center space-x-3">
                    <div className="w-9 h-9 rounded-full bg-indigo-50 border border-indigo-200 flex items-center justify-center text-indigo-700 font-bold text-xs">
                      {eng.name.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-slate-900">{eng.name}</div>
                      <div className="text-xs text-slate-500 flex items-center space-x-2">
                        <span className="flex items-center">
                          <Mail className="w-3 h-3 mr-1 text-slate-400" />
                          {eng.email}
                        </span>
                        <span>•</span>
                        <span className="flex items-center text-slate-600">
                          <Wrench className="w-3 h-3 mr-1 text-slate-400" />
                          {eng.specialization || 'General Network'}
                        </span>
                      </div>
                    </div>
                  </div>
                  <span className="text-[11px] font-mono text-slate-400">ID #{eng.id}</span>
                </div>
              ))}
            </div>
          )}
        </div>

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
