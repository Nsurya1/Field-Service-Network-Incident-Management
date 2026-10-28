import React from 'react';
import { AlertTriangle, Clock, CheckCircle2, ShieldAlert, Activity, ArrowUpRight } from 'lucide-react';

export default function StatsDashboard({ stats, loading }) {
  if (loading || !stats) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
        {[...Array(6)].map((_, i) => (
          <div key={i} className="h-24 bg-white rounded-xl border border-slate-200 animate-pulse p-4"></div>
        ))}
      </div>
    );
  }

  const cards = [
    {
      label: 'Total Incidents',
      value: stats.totalIncidents,
      icon: Activity,
      color: 'text-indigo-600',
      bgColor: 'bg-indigo-50',
      borderColor: 'border-indigo-100',
    },
    {
      label: 'Active Queue',
      value: stats.activeIncidents,
      icon: Clock,
      color: 'text-amber-600',
      bgColor: 'bg-amber-50',
      borderColor: 'border-amber-100',
    },
    {
      label: 'SLA Breached',
      value: stats.slaBreachedIncidents,
      icon: AlertTriangle,
      color: stats.slaBreachedIncidents > 0 ? 'text-rose-600' : 'text-slate-400',
      bgColor: stats.slaBreachedIncidents > 0 ? 'bg-rose-50' : 'bg-slate-50',
      borderColor: stats.slaBreachedIncidents > 0 ? 'border-rose-200 ring-2 ring-rose-500/20' : 'border-slate-100',
    },
    {
      label: 'Open',
      value: stats.openIncidents,
      icon: ShieldAlert,
      color: 'text-blue-600',
      bgColor: 'bg-blue-50',
      borderColor: 'border-blue-100',
    },
    {
      label: 'In Progress',
      value: stats.inProgressIncidents,
      icon: ArrowUpRight,
      color: 'text-purple-600',
      bgColor: 'bg-purple-50',
      borderColor: 'border-purple-100',
    },
    {
      label: 'Resolved',
      value: stats.resolvedIncidents,
      icon: CheckCircle2,
      color: 'text-emerald-600',
      bgColor: 'bg-emerald-50',
      borderColor: 'border-emerald-100',
    },
  ];

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
      {cards.map((c) => {
        const Icon = c.icon;
        return (
          <div
            key={c.label}
            className={`bg-white rounded-xl p-4 border ${c.borderColor} shadow-xs hover:shadow-sm transition`}
          >
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-medium text-slate-500">{c.label}</span>
              <div className={`p-1.5 rounded-lg ${c.bgColor} ${c.color}`}>
                <Icon className="w-4 h-4" />
              </div>
            </div>
            <div className={`text-2xl font-bold tracking-tight ${c.color === 'text-rose-600' ? 'text-rose-600' : 'text-slate-900'}`}>
              {c.value}
            </div>
          </div>
        );
      })}
    </div>
  );
}
