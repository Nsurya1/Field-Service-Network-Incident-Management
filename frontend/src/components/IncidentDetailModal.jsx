import React, { useEffect, useState } from 'react';
import { incidentService } from '../services/incidentService';
import { useAuth } from '../context/AuthContext';
import { X, Clock, MapPin, User, Calendar, History, ShieldAlert, Wrench, CheckCircle2, MessageSquare, Send, Edit3 } from 'lucide-react';

export default function IncidentDetailModal({ incident, onClose, onEdit }) {
  const { user: currentUser } = useAuth();
  const canEdit = currentUser?.role === 'ROLE_ADMIN' || currentUser?.role === 'ROLE_MANAGER';
  const [timeline, setTimeline] = useState([]);
  const [loadingTimeline, setLoadingTimeline] = useState(true);
  const [visits, setVisits] = useState([]);
  const [loadingVisits, setLoadingVisits] = useState(true);
  const [showVisitForm, setShowVisitForm] = useState(false);
  const [workNotes, setWorkNotes] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [submittingVisit, setSubmittingVisit] = useState(false);
  const [visitError, setVisitError] = useState('');
  const [commentText, setCommentText] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);
  const [commentError, setCommentError] = useState('');

  const fetchTimeline = async () => {
    try {
      const data = await incidentService.getTimeline(incident.id);
      setTimeline(data);
    } catch (err) {
      console.error('Failed to load timeline', err);
    } finally {
      setLoadingTimeline(false);
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

  useEffect(() => {
    if (!incident) return;
    fetchTimeline();
    fetchVisits();
  }, [incident]);

  if (!incident) return null;

  const formatDate = (isoString) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString();
  };

  const getActionBadgeColor = (action, itemType) => {
    if (itemType === 'COMMENT') {
      return 'bg-blue-100 text-blue-700 border-blue-200';
    }
    if (itemType === 'VISIT') {
      return 'bg-emerald-100 text-emerald-700 border-emerald-200';
    }
    switch (action) {
      case 'CREATED':
        return 'bg-indigo-100 text-indigo-700 border-indigo-200';
      case 'ASSIGNED':
        return 'bg-purple-100 text-purple-700 border-purple-200';
      case 'UNASSIGNED':
        return 'bg-rose-100 text-rose-700 border-rose-200';
      case 'STATUS_CHANGED':
        return 'bg-amber-100 text-amber-700 border-amber-200';
      case 'COMMENT_ADDED':
        return 'bg-cyan-100 text-cyan-700 border-cyan-200';
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
      fetchTimeline();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to record service visit';
      setVisitError(msg);
    } finally {
      setSubmittingVisit(false);
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!commentText.trim()) return;

    setSubmittingComment(true);
    setCommentError('');
    try {
      await incidentService.addComment(incident.id, commentText.trim());
      setCommentText('');
      fetchTimeline();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to post comment';
      setCommentError(msg);
    } finally {
      setSubmittingComment(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl overflow-hidden border border-slate-200 animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-xs font-mono font-medium text-slate-500">#{incident.id}</span>
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                incident.status === 'OPEN' ? 'bg-amber-50 text-amber-700 border-amber-200' :
                incident.status === 'ASSIGNED' ? 'bg-blue-50 text-blue-700 border-blue-200' :
                incident.status === 'IN_PROGRESS' ? 'bg-indigo-50 text-indigo-700 border-indigo-200' :
                incident.status === 'RESOLVED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                'bg-slate-50 text-slate-700 border-slate-200'
              }`}>
                {incident.status}
              </span>
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                incident.priority === 'CRITICAL' ? 'bg-rose-50 text-rose-700 border-rose-200' :
                incident.priority === 'HIGH' ? 'bg-orange-50 text-orange-700 border-orange-200' :
                incident.priority === 'MEDIUM' ? 'bg-amber-50 text-amber-700 border-amber-200' :
                'bg-emerald-50 text-emerald-700 border-emerald-200'
              }`}>
                {incident.priority}
              </span>
              {incident.slaBreached && (
                <span className="flex items-center space-x-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-rose-100 text-rose-800 border border-rose-200 animate-pulse">
                  <ShieldAlert className="w-3 h-3 text-rose-600" />
                  <span>SLA BREACHED</span>
                </span>
              )}
            </div>
            <h2 className="text-lg font-bold text-slate-900 mt-1">{incident.title}</h2>
          </div>
          <div className="flex items-center space-x-2">
            {canEdit && onEdit && incident.status !== 'CLOSED' && (
              <button
                onClick={() => onEdit(incident)}
                className="inline-flex items-center px-2.5 py-1 text-xs font-semibold rounded-lg border border-slate-200 text-indigo-600 bg-indigo-50/50 hover:bg-indigo-50 transition"
              >
                <Edit3 className="w-3.5 h-3.5 mr-1" />
                Edit
              </button>
            )}
            <button
              onClick={onClose}
              className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Content Body */}
        <div className="p-6 space-y-6 overflow-y-auto flex-1">
          {/* Metadata Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 p-3 bg-slate-50/80 rounded-xl border border-slate-100 text-xs">
            <div className="flex items-center space-x-2 text-slate-600">
              <MapPin className="w-4 h-4 text-slate-400" />
              <div>
                <span className="block text-[10px] text-slate-400 uppercase tracking-wider font-semibold">Location</span>
                <span className="font-medium text-slate-800">{incident.location || 'Unspecified'}</span>
              </div>
            </div>

            <div className="flex items-center space-x-2 text-slate-600">
              <User className="w-4 h-4 text-slate-400" />
              <div>
                <span className="block text-[10px] text-slate-400 uppercase tracking-wider font-semibold">Assigned Tech</span>
                <span className="font-medium text-slate-800">{incident.assignedEngineerName || 'Unassigned'}</span>
              </div>
            </div>

            <div className="flex items-center space-x-2 text-slate-600">
              <Clock className="w-4 h-4 text-slate-400" />
              <div>
                <span className="block text-[10px] text-slate-400 uppercase tracking-wider font-semibold">SLA Deadline</span>
                <span className={`font-medium ${incident.slaBreached ? 'text-rose-600 font-bold' : 'text-slate-800'}`}>
                  {formatDate(incident.slaDeadline)}
                </span>
              </div>
            </div>

            <div className="flex items-center space-x-2 text-slate-600">
              <Calendar className="w-4 h-4 text-slate-400" />
              <div>
                <span className="block text-[10px] text-slate-400 uppercase tracking-wider font-semibold">Reported At</span>
                <span className="font-medium text-slate-800">{formatDate(incident.createdAt)}</span>
              </div>
            </div>

            {incident.resolvedAt && (
              <div className="flex items-center space-x-2 text-slate-600">
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                <div>
                  <span className="block text-[10px] text-slate-400 uppercase tracking-wider font-semibold">Resolved At</span>
                  <span className="font-medium text-emerald-800">{formatDate(incident.resolvedAt)}</span>
                </div>
              </div>
            )}
          </div>

          {/* Description */}
          <div>
            <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">Description</h3>
            <div className="p-3 bg-white border border-slate-200 rounded-xl text-slate-700 text-xs leading-relaxed whitespace-pre-wrap">
              {incident.description || 'No description provided.'}
            </div>
          </div>

          {/* Resolution Details Card */}
          {(incident.resolutionNotes || incident.rootCause) && (
            <div className="p-3 bg-emerald-50/50 border border-emerald-200 rounded-xl space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-emerald-800 flex items-center space-x-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Resolution Details</span>
                </span>
                {incident.rootCause && (
                  <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
                    Root Cause: {incident.rootCause}
                  </span>
                )}
              </div>
              {incident.resolutionNotes && (
                <p className="text-xs text-emerald-900 leading-relaxed pl-5">
                  {incident.resolutionNotes}
                </p>
              )}
            </div>
          )}

          {/* On-Site Service Visits */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <Wrench className="w-4 h-4 text-indigo-600" />
                <h3 className="text-sm font-semibold text-slate-900">On-Site Service Visits</h3>
                <span className="text-[11px] bg-slate-100 text-slate-600 px-1.5 py-0.2 rounded-full font-mono">
                  {visits.length}
                </span>
              </div>
              {incident.status !== 'CLOSED' && (
                <button
                  type="button"
                  onClick={() => setShowVisitForm(!showVisitForm)}
                  className="text-xs text-indigo-600 hover:text-indigo-800 font-medium"
                >
                  {showVisitForm ? 'Cancel' : '+ Record Visit'}
                </button>
              )}
            </div>

            {/* Visit Form */}
            {showVisitForm && (
              <form onSubmit={handleRecordVisit} className="mb-4 p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-3">
                <h4 className="text-xs font-semibold text-slate-800">Record Field Service Visit</h4>
                {visitError && (
                  <div className="p-2 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg">
                    {visitError}
                  </div>
                )}
                <div>
                  <label className="block text-[11px] text-slate-600 font-medium mb-1">Work Notes *</label>
                  <textarea
                    rows={2}
                    value={workNotes}
                    onChange={(e) => setWorkNotes(e.target.value)}
                    placeholder="Describe actions taken during the on-site visit..."
                    className="w-full text-xs p-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    required
                  />
                </div>
                <div>
                  <label className="block text-[11px] text-slate-600 font-medium mb-1">Resolution Notes (Optional)</label>
                  <input
                    type="text"
                    value={resolutionNotes}
                    onChange={(e) => setResolutionNotes(e.target.value)}
                    placeholder="Optional details if work resolved the fault..."
                    className="w-full text-xs p-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                </div>
                <div className="flex justify-end space-x-2">
                  <button
                    type="button"
                    onClick={() => setShowVisitForm(false)}
                    className="px-3 py-1.5 text-xs text-slate-600 hover:text-slate-800"
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

          {/* Comments & Activity Timeline */}
          <div>
            <div className="flex items-center space-x-2 mb-3">
              <MessageSquare className="w-4 h-4 text-slate-500" />
              <h3 className="text-sm font-semibold text-slate-900">Activity, Comments & Audit Timeline</h3>
            </div>

            {/* Post Comment Input */}
            {incident.status !== 'CLOSED' && (
              <form onSubmit={handleAddComment} className="mb-4">
                {commentError && (
                  <div className="p-2 mb-2 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg">
                    {commentError}
                  </div>
                )}
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    placeholder="Add an update note or comment on this incident..."
                    className="flex-1 text-xs p-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                  <button
                    type="submit"
                    disabled={submittingComment || !commentText.trim()}
                    className="px-3 py-2 bg-indigo-600 text-white text-xs font-medium rounded-lg hover:bg-indigo-700 disabled:opacity-50 flex items-center space-x-1"
                  >
                    <Send className="w-3 h-3" />
                    <span>{submittingComment ? 'Posting...' : 'Comment'}</span>
                  </button>
                </div>
              </form>
            )}

            {/* Timeline Stream */}
            {loadingTimeline ? (
              <div className="py-6 text-center text-xs text-slate-500">Loading timeline events...</div>
            ) : timeline.length === 0 ? (
              <div className="py-6 text-center text-xs text-slate-400 italic">No activity recorded yet.</div>
            ) : (
              <div className="relative pl-6 border-l-2 border-slate-200 space-y-4 my-2">
                {timeline.map((item) => (
                  <div key={item.id} className="relative group">
                    <div className={`absolute -left-[31px] top-1.5 w-3 h-3 rounded-full border-2 ${
                      item.itemType === 'COMMENT' ? 'bg-blue-500 border-white ring-2 ring-blue-200' :
                      item.itemType === 'VISIT' ? 'bg-emerald-500 border-white ring-2 ring-emerald-200' :
                      'bg-white border-indigo-600'
                    }`}></div>
                    <div className="bg-white p-2.5 rounded-xl border border-slate-100 shadow-2xs space-y-1">
                      <div className="flex items-center justify-between text-xs">
                        <div className="flex items-center space-x-2">
                          <span className={`text-[10px] font-bold px-1.5 py-0.2 rounded border uppercase tracking-wider ${getActionBadgeColor(item.action, item.itemType)}`}>
                            {item.action}
                          </span>
                          <span className="text-[11px] font-semibold text-slate-700">{item.actor || 'system'}</span>
                        </div>
                        <span className="text-[10px] text-slate-400 font-mono">{formatDate(item.timestamp)}</span>
                      </div>
                      <p className="text-xs text-slate-700 mt-1 whitespace-pre-wrap">{item.content}</p>
                      {item.details && (
                        <p className="text-[11px] text-emerald-800 bg-emerald-50 p-1.5 rounded border border-emerald-100">
                          <span className="font-semibold">Resolution:</span> {item.details}
                        </p>
                      )}
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
