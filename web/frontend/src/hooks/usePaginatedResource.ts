import { Dispatch, SetStateAction, useCallback, useEffect, useRef, useState } from 'react';

export type PaginatedResourceState<TData> = {
  data: TData;
  loading: boolean;
  refreshing: boolean;
  error: unknown;
};

type Options<TQuery, TData> = {
  query: TQuery;
  enabled: boolean;
  initialData: TData;
  loader: (query: TQuery, signal: AbortSignal) => Promise<TData>;
};

type PaginatedResource<TData> = PaginatedResourceState<TData> & {
  refresh: () => Promise<TData | undefined>;
  setData: Dispatch<SetStateAction<TData>>;
};

export default function usePaginatedResource<TQuery, TData>({ query, enabled, initialData, loader }: Options<TQuery, TData>): PaginatedResource<TData> {
  const [state, setState] = useState<PaginatedResourceState<TData>>({ data: initialData, loading: false, refreshing: false, error: null });
  const requestSequence = useRef(0);
  const activeController = useRef<AbortController | null>(null);
  const hasLoaded = useRef(false);
  const queryRef = useRef(query);
  const initialDataRef = useRef(initialData);
  queryRef.current = query;
  initialDataRef.current = initialData;
  const queryKey = JSON.stringify(query);

  const execute = useCallback(async (requestedQuery: TQuery): Promise<TData | undefined> => {
    const sequence = requestSequence.current + 1;
    requestSequence.current = sequence;
    activeController.current?.abort();
    const controller = new AbortController();
    activeController.current = controller;

    setState((current) => ({
      ...current,
      loading: !hasLoaded.current,
      refreshing: hasLoaded.current,
      error: null
    }));

    try {
      const data = await loader(requestedQuery, controller.signal);
      if (sequence !== requestSequence.current) return undefined;
      hasLoaded.current = true;
      setState({ data, loading: false, refreshing: false, error: null });
      return data;
    } catch (error) {
      if (sequence !== requestSequence.current || isAbortError(error)) return undefined;
      setState((current) => ({ ...current, loading: false, refreshing: false, error }));
      throw error;
    }
  }, [loader]);

  useEffect(() => {
    if (!enabled) {
      requestSequence.current += 1;
      activeController.current?.abort();
      activeController.current = null;
      hasLoaded.current = false;
      setState({ data: initialDataRef.current, loading: false, refreshing: false, error: null });
      return;
    }

    void execute(queryRef.current).catch(() => undefined);
    return () => activeController.current?.abort();
  }, [enabled, execute, queryKey]);

  const refresh = useCallback(() => {
    if (!enabled) return Promise.resolve(undefined);
    return execute(queryRef.current);
  }, [enabled, execute]);

  const setData = useCallback<Dispatch<SetStateAction<TData>>>((value) => {
    hasLoaded.current = true;
    setState((current) => ({
      ...current,
      data: typeof value === 'function' ? (value as (previous: TData) => TData)(current.data) : value
    }));
  }, []);

  return { ...state, refresh, setData };
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError';
}
