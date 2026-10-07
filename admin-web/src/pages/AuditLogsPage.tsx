import { useState, useEffect, useCallback } from 'react';
import { Search, ScrollText, AlertCircle, RefreshCw, X, Code, Shield } from 'lucide-react';
import DataTable from '../components/DataTable';
import type { Column } from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import apiClient from '../api/client';

interface AuditLogEntry {
  id: number;
  actorUserId?: string;
  actorRole: string;
  actionType: string;
  entityName: string;
  entityId: string;
  deltaState?: string;
  timestamp: string;
}

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);

  const [actionTypeFilter, setActionTypeFilter] = useState('All');
  const [entityFilter, setEntityFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const [selectedLog, setSelectedLog] = useState<AuditLogEntry | null>(null);

  const fetchAuditLogs = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const params: Record<string, any> = {
        page: currentPage - 1,
        size: pageSize,
      };
      if (actionTypeFilter !== 'All') {
        params.actionType = actionTypeFilter;
      }
      if (entityFilter.trim()) {
        params.entityName = entityFilter.trim();
      }

      const res = await apiClient.get('/admin/audit-logs', { params });
      if (res.data && Array.isArray(res.data.content)) {
        setLogs(res.data.content);
        setTotalElements(res.data.totalElements || res.data.content.length);
        setTotalPages(res.data.totalPages || 1);
      } else {
        setLogs([]);
        setTotalElements(0);
        setTotalPages(1);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Could not load audit logs from backend';
      setApiError(msg);
      setLogs([]);
      setTotalElements(0);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, actionTypeFilter, entityFilter]);

  useEffect(() => {
    fetchAuditLogs();
  }, [fetchAuditLogs]);

  const columns: Column<AuditLogEntry>[] = [
    {
      key: 'id',
      label: 'ID',
      render: (val: number) => <span className="font-mono text-xs text-gray-500">#{val}</span>,
    },
    {
      key: 'timestamp',
      label: 'Timestamp',
      render: (val: string) => (
        <span className="text-xs text-gray-600 whitespace-nowrap">
          {val ? new Date(val).toLocaleString('en-IN') : '—'}
        </span>
      ),
    },
    {
      key: 'actorRole',
      label: 'Actor Role',
      render: (val: string) => <StatusBadge status={val} />,
    },
    {
      key: 'actionType',
      label: 'Action',
      render: (val: string) => (
        <span className="font-semibold text-xs text-gray-800 bg-gray-100 px-2 py-0.5 rounded">
          {val}
        </span>
      ),
    },
    {
      key: 'entityName',
      label: 'Entity',
      render: (val: string, row: AuditLogEntry) => (
        <div>
          <span className="text-sm font-medium text-gray-800">{val}</span>
          <p className="font-mono text-[11px] text-gray-400 truncate max-w-[140px]">{row.entityId}</p>
        </div>
      ),
    },
    {
      key: 'actorUserId',
      label: 'Actor User ID',
      render: (val?: string) => (
        <span className="font-mono text-xs text-gray-500 truncate max-w-[120px] block">
          {val || 'SYSTEM'}
        </span>
      ),
    },
    {
      key: 'deltaState',
      label: 'Delta',
      render: (val?: string) => (
        val ? (
          <span className="text-xs text-primary-600 font-mono bg-primary-50 px-2 py-0.5 rounded">
            JSON
          </span>
        ) : (
          <span className="text-xs text-gray-400">—</span>
        )
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <ScrollText className="w-5 h-5 text-primary-600" />
            <h1 className="text-lg font-semibold text-gray-900">System Audit Logs</h1>
          </div>
          <p className="text-sm text-gray-500">
            Authoritative, immutable event log stored in PostgreSQL database ({totalElements} recorded events)
          </p>
        </div>
        <button
          onClick={() => fetchAuditLogs()}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Backend Error Banner */}
      {apiError && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>Backend error: {apiError}</span>
          </div>
          <button
            onClick={() => fetchAuditLogs()}
            className="px-3 py-1 bg-red-100 hover:bg-red-200 text-red-800 font-medium rounded text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            value={entityFilter}
            onChange={(e) => {
              setEntityFilter(e.target.value);
              setCurrentPage(1);
            }}
            placeholder="Filter by entity name..."
            className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20"
          />
        </div>
        <select
          value={actionTypeFilter}
          onChange={(e) => {
            setActionTypeFilter(e.target.value);
            setCurrentPage(1);
          }}
          className="text-sm px-3 py-2 bg-white border border-gray-200 rounded-lg"
        >
          <option value="All">All Action Types</option>
          <option value="USER_STATUS_CHANGE">USER_STATUS_CHANGE</option>
          <option value="ORGANIZATION_VERIFICATION">ORGANIZATION_VERIFICATION</option>
          <option value="PICKUP_ASSIGNED">PICKUP_ASSIGNED</option>
          <option value="DONATION_CREATED">DONATION_CREATED</option>
          <option value="REQUEST_CREATED">REQUEST_CREATED</option>
        </select>
      </div>

      {/* Table */}
      <DataTable
        columns={columns}
        data={logs}
        emptyMessage={loading ? 'Loading audit records from database...' : 'No audit records found'}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={(page) => setCurrentPage(page)}
        onRowClick={(row) => setSelectedLog(row)}
      />

      {/* Detail Drawer */}
      {selectedLog && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-black/20" onClick={() => setSelectedLog(null)} />
          <div className="relative w-full max-w-lg bg-white shadow-xl overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between z-10">
              <div>
                <p className="text-xs font-mono text-gray-500">Record #{selectedLog.id}</p>
                <h2 className="text-lg font-semibold text-gray-900">{selectedLog.actionType}</h2>
              </div>
              <button onClick={() => setSelectedLog(null)} className="p-2 hover:bg-gray-100 rounded-lg">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <Shield className="w-4 h-4 text-primary-500" /> Audit Context
                </h4>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <span className="text-xs text-gray-400 block">Actor Role</span>
                    <StatusBadge status={selectedLog.actorRole} />
                  </div>
                  <div>
                    <span className="text-xs text-gray-400 block">Timestamp</span>
                    <span className="text-xs text-gray-700 font-medium">
                      {new Date(selectedLog.timestamp).toLocaleString('en-IN')}
                    </span>
                  </div>
                  <div>
                    <span className="text-xs text-gray-400 block">Entity Name</span>
                    <span className="text-sm text-gray-800 font-medium">{selectedLog.entityName}</span>
                  </div>
                  <div>
                    <span className="text-xs text-gray-400 block">Entity ID</span>
                    <span className="text-xs font-mono text-gray-700 break-all">{selectedLog.entityId}</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-xs text-gray-400 block">Actor User ID</span>
                    <span className="text-xs font-mono text-gray-700 break-all">
                      {selectedLog.actorUserId || 'SYSTEM'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Delta State */}
              <div>
                <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2 mb-2">
                  <Code className="w-4 h-4 text-primary-500" /> Delta State
                </h4>
                {selectedLog.deltaState ? (
                  <pre className="bg-gray-900 text-gray-100 p-4 rounded-xl text-xs font-mono overflow-x-auto max-h-96">
                    {(() => {
                      try {
                        return JSON.stringify(JSON.parse(selectedLog.deltaState), null, 2);
                      } catch {
                        return selectedLog.deltaState;
                      }
                    })()}
                  </pre>
                ) : (
                  <div className="p-4 bg-gray-50 rounded-xl text-xs text-gray-400 text-center">
                    No JSON state delta captured for this event
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
