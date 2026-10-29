import React, { useState, useEffect, useCallback } from 'react';
import { reportService } from '../services/reportService';
import {
  Clock,
  ShieldCheck,
  TrendingUp,
  Download,
  Calendar,
  BarChart2,
  Users,
  AlertCircle,
  RefreshCw,
  FileSpreadsheet
} from 'lucide-react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell
} from 'recharts';

const PRIORITY_COLORS = {
  CRITICAL: '#e11d48', // rose-600
  HIGH: '#ea580c',     // orange-600
  MEDIUM: '#d97706',   // amber-600
  LOW: '#059669',      // emerald-600
};

export default function ReportsPage() {
  const [rangePreset, setRangePreset] = useState('30d');
  const [customFrom, setCustomFrom] = useState('');
  const [customTo, setCustomTo] = useState('');
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState('');

  const calculateDates = useCallback(() => {
    const to = new Date();
    let from = new Date();

    if (rangePreset === '7d') {
      from.setDate(to.getDate() - 7);
    } else if (rangePreset === '30d') {
      from.setDate(to.getDate() - 30);
    } else if (rangePreset === '90d') {
      from.setDate(to.getDate() - 90);
    } else if (rangePreset === 'custom' && customFrom && customTo) {
      return {
        fromIso: new Date(customFrom).toISOString(),
        toIso: new Date(customTo + 'T23:59:59').toISOString(),
      };
    }

    return {
      fromIso: from.toISOString(),
      toIso: to.toISOString(),
    };
  }, [rangePreset, customFrom, customTo]);

  const loadReport = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const { fromIso, toIso } = calculateDates();
      const data = await reportService.getSummary(fromIso, toIso);
      setSummary(data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load report summary.');
    } finally {
      setLoading(false);
    }
  }, [calculateDates]);

  useEffect(() => {
    loadReport();
  }, [loadReport]);

  const handleExportCsv = async () => {
    try {
      setExporting(true);
      const { fromIso, toIso } = calculateDates();
      await reportService.exportCsv(fromIso, toIso);
    } catch (err) {
      alert('Failed to export CSV: ' + (err.response?.data?.message || err.message));
    } finally {
      setExporting(false);
    }
  };

  const priorityData = summary?.incidentsByPriority
    ? Object.entries(summary.incidentsByPriority).map(([priority, count]) => ({
        name: priority,
        value: Number(count),
        color: PRIORITY_COLORS[priority] || '#64748b',
      }))
    : [];

  const engineerData = summary?.engineerPerformance
    ? summary.engineerPerformance.map((ep) => ({
        name: ep.engineerName,
        resolved: Number(ep.resolvedCount),
        avgHours: Number(ep.avgResolutionTimeHours),
      }))
    : [];

  return (
    <div className="space-y-6">
      {/* Header and Controls */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            <BarChart2 className="w-5 h-5 text-indigo-600" />
            Operational Intelligence & MTTR Reports
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Real-time aggregate analytics, SLA compliance performance, and engineer resolution velocity
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Preset Buttons */}
          <div className="inline-flex rounded-lg border border-slate-200 p-0.5 bg-slate-50 text-xs font-semibold text-slate-600">
            <button
              onClick={() => setRangePreset('7d')}
              className={`px-3 py-1 rounded-md transition ${rangePreset === '7d' ? 'bg-white shadow-xs text-indigo-600' : 'hover:text-slate-900'}`}
            >
              7 Days
            </button>
            <button
              onClick={() => setRangePreset('30d')}
              className={`px-3 py-1 rounded-md transition ${rangePreset === '30d' ? 'bg-white shadow-xs text-indigo-600' : 'hover:text-slate-900'}`}
            >
              30 Days
            </button>
            <button
              onClick={() => setRangePreset('90d')}
              className={`px-3 py-1 rounded-md transition ${rangePreset === '90d' ? 'bg-white shadow-xs text-indigo-600' : 'hover:text-slate-900'}`}
            >
              90 Days
            </button>
            <button
              onClick={() => setRangePreset('custom')}
              className={`px-3 py-1 rounded-md transition ${rangePreset === 'custom' ? 'bg-white shadow-xs text-indigo-600' : 'hover:text-slate-900'}`}
            >
              Custom
            </button>
          </div>

          {rangePreset === 'custom' && (
            <div className="flex items-center gap-1.5 text-xs">
              <input
                type="date"
                value={customFrom}
                onChange={(e) => setCustomFrom(e.target.value)}
                className="px-2 py-1 border border-slate-200 rounded-lg text-slate-700 bg-white"
              />
              <span className="text-slate-400">to</span>
              <input
                type="date"
                value={customTo}
                onChange={(e) => setCustomTo(e.target.value)}
                className="px-2 py-1 border border-slate-200 rounded-lg text-slate-700 bg-white"
              />
            </div>
          )}

          <button
            onClick={loadReport}
            title="Refresh analytics"
            className="p-2 border border-slate-200 rounded-lg text-slate-600 hover:bg-slate-50 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={handleExportCsv}
            disabled={exporting}
            className="inline-flex items-center px-3.5 py-1.5 border border-emerald-600 text-xs font-semibold rounded-lg text-white bg-emerald-600 hover:bg-emerald-700 shadow-xs transition disabled:opacity-50"
          >
            <FileSpreadsheet className="w-4 h-4 mr-1.5" />
            {exporting ? 'Exporting...' : 'Export CSV'}
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs flex items-center">
          <AlertCircle className="w-4 h-4 mr-2 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* KPI Headline Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* MTTR */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Mean Time To Resolution</span>
            <div className="p-2 bg-indigo-50 text-indigo-600 rounded-xl">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900 tracking-tight">
              {loading ? '--' : summary?.mttrHours ?? '0.00'}
            </span>
            <span className="text-xs font-semibold text-slate-500">hours</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">
            Average span from ticket creation to technical resolution
          </p>
        </div>

        {/* SLA Compliance */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">SLA Compliance</span>
            <div className="p-2 bg-emerald-50 text-emerald-600 rounded-xl">
              <ShieldCheck className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900 tracking-tight">
              {loading ? '--' : `${summary?.slaCompliancePercentage ?? '100'}%`}
            </span>
          </div>
          <div className="mt-2 w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                (summary?.slaCompliancePercentage ?? 100) >= 90 ? 'bg-emerald-500' : (summary?.slaCompliancePercentage ?? 100) >= 75 ? 'bg-amber-500' : 'bg-rose-500'
              }`}
              style={{ width: `${Math.min(100, Math.max(0, summary?.slaCompliancePercentage ?? 100))}%` }}
            ></div>
          </div>
        </div>

        {/* Total Volume */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Total Created</span>
            <div className="p-2 bg-blue-50 text-blue-600 rounded-xl">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900 tracking-tight">
              {loading ? '--' : summary?.totalIncidents ?? 0}
            </span>
            <span className="text-xs font-semibold text-slate-500">tickets</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">Total volume logged during reporting window</p>
        </div>

        {/* Total Resolved */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Resolved Volume</span>
            <div className="p-2 bg-purple-50 text-purple-600 rounded-xl">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900 tracking-tight">
              {loading ? '--' : summary?.resolvedIncidents ?? 0}
            </span>
            <span className="text-xs font-semibold text-slate-500">resolved</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">Total tickets successfully closed or resolved</p>
        </div>
      </div>

      {/* Main Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Daily Created vs Resolved Trend (2 Columns) */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-bold text-slate-900">Created vs. Resolved Velocity</h3>
              <p className="text-xs text-slate-500">Daily cohort trajectory over the reporting timeline</p>
            </div>
          </div>

          <div className="h-72 w-full">
            {loading ? (
              <div className="h-full flex items-center justify-center text-slate-400 text-xs">
                Loading time-series data...
              </div>
            ) : summary?.dailyCreatedVsResolved?.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={summary.dailyCreatedVsResolved} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="createdGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#6366f1" stopOpacity={0.0} />
                    </linearGradient>
                    <linearGradient id="resolvedGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0.0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#94a3b8' }} />
                  <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} allowDecimals={false} />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', fontSize: '12px' }}
                  />
                  <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} />
                  <Area type="monotone" dataKey="createdCount" name="Created Tickets" stroke="#6366f1" strokeWidth={2} fillOpacity={1} fill="url(#createdGrad)" />
                  <Area type="monotone" dataKey="resolvedCount" name="Resolved Tickets" stroke="#10b981" strokeWidth={2} fillOpacity={1} fill="url(#resolvedGrad)" />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center text-slate-400 text-xs">
                No incident activity recorded in this period.
              </div>
            )}
          </div>
        </div>

        {/* Priority Breakdown (1 Column) */}
        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs flex flex-col">
          <div className="mb-4">
            <h3 className="text-sm font-bold text-slate-900">Incidents by Priority</h3>
            <p className="text-xs text-slate-500">Distribution across severity levels</p>
          </div>

          <div className="h-56 w-full flex items-center justify-center">
            {loading ? (
              <div className="text-slate-400 text-xs">Loading breakdown...</div>
            ) : priorityData.some((p) => p.value > 0) ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={priorityData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={75}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {priorityData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{ backgroundColor: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', fontSize: '12px' }}
                  />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="text-slate-400 text-xs">No priority data in period.</div>
            )}
          </div>

          <div className="mt-auto grid grid-cols-2 gap-2 pt-2 border-t border-slate-100 text-xs">
            {priorityData.map((p) => (
              <div key={p.name} className="flex items-center justify-between p-1.5 rounded-lg bg-slate-50">
                <div className="flex items-center space-x-1.5">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: p.color }}></span>
                  <span className="font-semibold text-slate-700">{p.name}</span>
                </div>
                <span className="font-bold text-slate-900">{p.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Engineer Performance Leaderboard */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Users className="w-4 h-4 text-indigo-600" />
              Field Engineer Resolution Performance
            </h3>
            <p className="text-xs text-slate-500">Resolved tickets count and average resolution time per technician</p>
          </div>
        </div>

        {loading ? (
          <div className="py-8 text-center text-slate-400 text-xs">Loading engineer performance metrics...</div>
        ) : engineerData.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 uppercase tracking-wider font-semibold border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3">Field Engineer</th>
                  <th className="px-4 py-3">Resolved Count</th>
                  <th className="px-4 py-3">Average Resolution Time</th>
                  <th className="px-4 py-3">Resolution Velocity Bar</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {engineerData.map((eng) => (
                  <tr key={eng.name} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-3 font-semibold text-slate-800">{eng.name}</td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                        {eng.resolved} tickets
                      </span>
                    </td>
                    <td className="px-4 py-3 font-mono font-medium text-slate-700">
                      {eng.avgHours > 0 ? `${eng.avgHours} hrs` : 'N/A'}
                    </td>
                    <td className="px-4 py-3 w-1/3">
                      <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                        <div
                          className="bg-indigo-600 h-full rounded-full"
                          style={{
                            width: `${Math.min(100, (eng.resolved / Math.max(...engineerData.map((e) => e.resolved || 1))) * 100)}%`,
                          }}
                        ></div>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-8 text-center text-slate-400 text-xs">
            No resolved tickets assigned to engineers during this period.
          </div>
        )}
      </div>
    </div>
  );
}
