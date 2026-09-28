import { useEffect, useMemo, useState } from 'react';
import {
  approvePhysicalInventory,
  cancelPhysicalInventory,
  createPhysicalInventory,
  CreatePhysicalInventoryPayload,
  fetchPhysicalInventory,
  fetchPhysicalInventoryPage,
  PageResponse,
  PhysicalInventoryCountPayload,
  PhysicalInventoryQuery,
  PhysicalInventorySession,
  recordPhysicalInventoryCount,
  submitPhysicalInventory
} from '../../api';
import { type CommandExecutor } from '../../hooks/useCommandExecution';
import usePaginatedResource from '../../hooks/usePaginatedResource';

const PAGE_SIZE = 6;
const INITIAL_QUERY: PhysicalInventoryQuery = { page: 0, size: PAGE_SIZE };

type Options = {
  enabled: boolean;
  executeCommand: CommandExecutor;
  onInventoryChanged: () => Promise<void>;
};

export default function usePhysicalInventoryFlow({ enabled, executeCommand, onInventoryChanged }: Options) {
  const [query, setQuery] = useState<PhysicalInventoryQuery>(INITIAL_QUERY);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<PhysicalInventorySession | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<unknown>(null);
  const emptySessions = useMemo(() => emptyPage<PhysicalInventorySession>(), []);
  const sessions = usePaginatedResource({ query, enabled, initialData: emptySessions, loader: fetchPhysicalInventoryPage });

  useEffect(() => {
    if (!enabled || selectedId === null) {
      setDetail(null);
      setDetailError(null);
      return;
    }
    const controller = new AbortController();
    setDetailLoading(true);
    setDetailError(null);
    fetchPhysicalInventory(selectedId, controller.signal)
      .then(setDetail)
      .catch((error) => {
        if (!(error instanceof DOMException && error.name === 'AbortError')) setDetailError(error);
      })
      .finally(() => {
        if (!controller.signal.aborted) setDetailLoading(false);
      });
    return () => controller.abort();
  }, [enabled, selectedId]);

  async function create(payload: CreatePhysicalInventoryPayload) {
    await executeCommand({
      key: `physical-inventory:create:${payload.productCodes.join(',') || 'all'}`,
      command: () => createPhysicalInventory(payload),
      applyResponse: (session) => {
        setSelectedId(session.id);
        setDetail(session);
      },
      refresh: refreshSessions,
      successMessage: 'Sessione di inventario aperta. Il conteggio può iniziare.'
    });
  }

  async function count(itemId: number, payload: PhysicalInventoryCountPayload) {
    if (!detail) return;
    await executeMutation(
      `physical-inventory:count:${detail.id}:${itemId}`,
      () => recordPhysicalInventoryCount(detail.id, itemId, payload),
      'Conteggio registrato e differenza ricalcolata.'
    );
  }

  async function submit() {
    if (!detail) return;
    await executeMutation(
      `physical-inventory:submit:${detail.id}`,
      () => submitPhysicalInventory(detail.id),
      'Conteggio inviato al responsabile per l’approvazione.'
    );
  }

  async function approve(reason: string) {
    if (!detail) return;
    await executeMutation(
      `physical-inventory:approve:${detail.id}`,
      () => approvePhysicalInventory(detail.id, { reason }),
      'Inventario approvato: le differenze sono state registrate nel ledger.',
      onInventoryChanged
    );
  }

  async function cancel(reason: string) {
    if (!detail) return;
    await executeMutation(
      `physical-inventory:cancel:${detail.id}`,
      () => cancelPhysicalInventory(detail.id, { reason }),
      'Sessione annullata senza modificare le giacenze.'
    );
  }

  async function executeMutation(key: string, command: () => Promise<PhysicalInventorySession>, successMessage: string, afterConfirmed?: () => Promise<void>) {
    await executeCommand({
      key,
      command,
      applyResponse: setDetail,
      refresh: refreshSessions,
      afterConfirmed,
      successMessage
    });
  }

  async function refreshSessions() {
    await sessions.refresh();
  }

  async function refreshDetail() {
    if (selectedId === null) return;
    setDetail(await fetchPhysicalInventory(selectedId));
  }

  async function refreshResources() {
    await Promise.all([refreshSessions(), refreshDetail()]);
  }

  function reset() {
    setQuery(INITIAL_QUERY);
    setSelectedId(null);
    setDetail(null);
    setDetailError(null);
    sessions.setData(emptySessions);
  }

  return {
    query,
    page: sessions.data,
    loading: sessions.loading,
    refreshing: sessions.refreshing,
    error: sessions.error,
    pageSize: PAGE_SIZE,
    selectedId,
    detail,
    detailLoading,
    detailError,
    setQuery,
    setSelectedId,
    create,
    count,
    submit,
    approve,
    cancel,
    refreshResources,
    reset
  };
}

export type PhysicalInventoryFlowController = ReturnType<typeof usePhysicalInventoryFlow>;

function emptyPage<T>(): PageResponse<T> {
  return { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0, first: true, last: true };
}
