import React, { useState, useEffect, useCallback } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { incidentService } from './services/incidentService';
import Navbar from './components/Navbar';
import StatsDashboard from './components/StatsDashboard';
import IncidentTable from './components/IncidentTable';
import IncidentDetailModal from './components/IncidentDetailModal';
import CreateIncidentModal from './components/CreateIncidentModal';
import AssignEngineerModal from './components/AssignEngineerModal';
import StatusTransitionModal from './components/StatusTransitionModal';
import EngineerManagementModal from './components/EngineerManagementModal';
import EditIncidentModal from './components/EditIncidentModal';
import AuthView from './components/AuthView';
import { Filter, RefreshCw, AlertCircle, Search, AlertTriangle } from 'lucide-react';

function IncidentDashboard() {
  const { user } = useAuth();

  // State
  const [stats, setStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(true);

  const [incidents, setIncidents] = useState([]);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [tableLoading, setTableLoading] = useState(true);

  // Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [breachedOnly, setBreachedOnly] = useState(false);

  // Modals
  const [detailIncident, setDetailIncident] = useState(null);
  const [editIncident, setEditIncident] = useState(null);
  const [assignIncident, setAssignIncident] = useState(null);
  const [statusIncident, setStatusIncident] = useState(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isEngineersOpen, setIsEngineersOpen] = useState(false);

  // Error state
  const [fetchError, setFetchError] = useState('');

  const loadStats = useCallback(async () => {
    try {
      setStatsLoading(true);
      const data = await incidentService.getStats();
      setStats(data);
    } catch (err) {
      console.error('Failed to load stats', err);
    } finally {
      setStatsLoading(false);
    }
  }, []);

  const loadIncidents = useCallback(async () => {
    try {
      setTableLoading(true);
      setFetchError('');
      const data = await incidentService.getIncidents({
        search: searchTerm.trim() || null,
        status: statusFilter || null,
        priority: priorityFilter || null,
        breachedOnly: breachedOnly ? true : null,
      }, null, page, pageSize);
      setIncidents(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setFetchError('Failed to fetch incident records from server.');
      console.error(err);
    } finally {
      setTableLoading(false);
    }
  }, [searchTerm, statusFilter, priorityFilter, breachedOnly, page, pageSize]);

  const refreshAll = useCallback(() => {
    loadStats();
    loadIncidents();
  }, [loadStats, loadIncidents]);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  useEffect(() => {
    loadIncidents();
  }, [loadIncidents]);

  const hasActiveFilters = Boolean(searchTerm.trim() || statusFilter || priorityFilter || breachedOnly);

  const resetFilters = () => {
    setSearchTerm('');
    setStatusFilter('');
    setPriorityFilter('');
    setBreachedOnly(false);
    setPage(0);
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar
        onOpenCreate={() => setIsCreateOpen(true)}
        onOpenEngineers={() => setIsEngineersOpen(true)}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Metric widgets */}
        <StatsDashboard stats={stats} loading={statsLoading} />

        {/* Action & Filter Bar */}
        <div className="bg-white rounded-xl border border-slate-200 p-4 mb-4 shadow-xs flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-3 flex-1 min-w-[280px]">
            <div className="flex items-center text-xs font-semibold text-slate-500 mr-1">
              <Filter className="w-3.5 h-3.5 mr-1.5" />
              Filters:
            </div>

            {/* Free-text Search */}
            <div className="relative flex-1 min-w-[180px] max-w-xs">
              <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
                placeholder="Search title, description, location..."
                className="w-full pl-8 pr-3 py-1.5 border border-slate-300 rounded-lg text-xs font-medium text-slate-700 bg-white placeholder:text-slate-400 focus:ring-2 focus:ring-indigo-500 outline-none"
              />
            </div>

            {/* Status Dropdown */}
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
              className="px-3 py-1.5 border border-slate-300 rounded-lg text-xs font-medium text-slate-700 bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
            >
              <option value="">All Statuses</option>
              <option value="OPEN">OPEN</option>
              <option value="ASSIGNED">ASSIGNED</option>
              <option value="IN_PROGRESS">IN PROGRESS</option>
              <option value="RESOLVED">RESOLVED</option>
              <option value="CLOSED">CLOSED</option>
            </select>

            {/* Priority Dropdown */}
            <select
              value={priorityFilter}
              onChange={(e) => { setPriorityFilter(e.target.value); setPage(0); }}
              className="px-3 py-1.5 border border-slate-300 rounded-lg text-xs font-medium text-slate-700 bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
            >
              <option value="">All Priorities</option>
              <option value="CRITICAL">CRITICAL</option>
              <option value="HIGH">HIGH</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="LOW">LOW</option>
            </select>

            {/* Breached Only Filter */}
            <button
              onClick={() => { setBreachedOnly(!breachedOnly); setPage(0); }}
              className={`inline-flex items-center px-2.5 py-1.5 rounded-lg text-xs font-medium border transition cursor-pointer ${
                breachedOnly
                  ? 'bg-rose-50 border-rose-300 text-rose-700 font-semibold'
                  : 'bg-white border-slate-300 text-slate-600 hover:bg-slate-50'
              }`}
            >
              <AlertTriangle className={`w-3.5 h-3.5 mr-1.5 ${breachedOnly ? 'text-rose-600' : 'text-slate-400'}`} />
              Breached Only
            </button>

            {hasActiveFilters && (
              <button
                onClick={resetFilters}
                className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold underline underline-offset-2 ml-1 cursor-pointer"
              >
                Reset filters
              </button>
            )}
          </div>

          <button
            onClick={refreshAll}
            className="inline-flex items-center px-3 py-1.5 border border-slate-300 rounded-lg text-xs font-medium text-slate-600 bg-white hover:bg-slate-50 transition cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5 mr-1.5" />
            Refresh
          </button>
        </div>

        {fetchError && (
          <div className="mb-4 p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs flex items-center">
            <AlertCircle className="w-4 h-4 mr-2 shrink-0" />
            <span>{fetchError}</span>
          </div>
        )}

        {/* Incident Table */}
        <IncidentTable
          incidents={incidents}
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={setPage}
          onSelectIncident={setDetailIncident}
          onOpenAssign={setAssignIncident}
          onOpenStatus={setStatusIncident}
          loading={tableLoading}
        />
      </main>

      {/* Modals */}
      <CreateIncidentModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={refreshAll}
      />

      <IncidentDetailModal
        incident={detailIncident}
        onClose={() => setDetailIncident(null)}
        onEdit={(inc) => {
          setDetailIncident(null);
          setEditIncident(inc);
        }}
      />

      <EditIncidentModal
        incident={editIncident}
        isOpen={!!editIncident}
        onClose={() => setEditIncident(null)}
        onSuccess={refreshAll}
      />

      <AssignEngineerModal
        incident={assignIncident}
        isOpen={!!assignIncident}
        onClose={() => setAssignIncident(null)}
        onSuccess={refreshAll}
      />

      <StatusTransitionModal
        incident={statusIncident}
        isOpen={!!statusIncident}
        onClose={() => setStatusIncident(null)}
        onSuccess={refreshAll}
      />

      <EngineerManagementModal
        isOpen={isEngineersOpen}
        onClose={() => setIsEngineersOpen(false)}
      />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

function AppContent() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!user) {
    return <AuthView />;
  }

  return <IncidentDashboard />;
}
