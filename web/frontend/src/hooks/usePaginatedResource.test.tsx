import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ApiRequestError, SessionExpiredError } from '../api';
import usePaginatedResource from './usePaginatedResource';

describe('usePaginatedResource', () => {
  it('espone loading, data e refreshing senza perdere i dati correnti', async () => {
    let finishRefresh: (value: string[]) => void = () => undefined;
    const loader = vi.fn()
      .mockResolvedValueOnce(['prima pagina'])
      .mockImplementationOnce(() => new Promise<string[]>((resolve) => { finishRefresh = resolve; }));
    const { result } = renderHook(() => usePaginatedResource({ query: { page: 0 }, enabled: true, initialData: [], loader }));

    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.data).toEqual(['prima pagina']));

    let refreshPromise: Promise<string[] | undefined> = Promise.resolve(undefined);
    act(() => { refreshPromise = result.current.refresh(); });
    expect(result.current.refreshing).toBe(true);
    expect(result.current.data).toEqual(['prima pagina']);

    finishRefresh(['pagina aggiornata']);
    await act(async () => { await refreshPromise; });
    expect(result.current).toMatchObject({ data: ['pagina aggiornata'], loading: false, refreshing: false, error: null });
  });

  it.each([
    ['401', new SessionExpiredError('Sessione scaduta.')],
    ['403', new ApiRequestError('Permesso negato.', 403, 'ACCESS_DENIED')],
    ['500', new ApiRequestError('Errore interno.', 500, 'INTERNAL_ERROR')],
    ['offline', new TypeError('Failed to fetch')]
  ])('espone l errore %s senza rejection non gestite', async (_label, expectedError) => {
    const loader = vi.fn().mockRejectedValue(expectedError);
    const { result } = renderHook(() => usePaginatedResource({ query: { page: 0 }, enabled: true, initialData: [], loader }));

    await waitFor(() => expect(result.current.error).toBe(expectedError));
    expect(result.current.loading).toBe(false);
    expect(result.current.refreshing).toBe(false);
  });

  it('applica sempre l ultima risposta anche se la precedente termina dopo', async () => {
    const resolvers = new Map<number, (value: string[]) => void>();
    const loader = vi.fn((query: { page: number }) => new Promise<string[]>((resolve) => resolvers.set(query.page, resolve)));
    const { result, rerender } = renderHook(
      ({ page }) => usePaginatedResource({ query: { page }, enabled: true, initialData: [], loader }),
      { initialProps: { page: 0 } }
    );
    await waitFor(() => expect(loader).toHaveBeenCalledTimes(1));

    rerender({ page: 1 });
    await waitFor(() => expect(loader).toHaveBeenCalledTimes(2));
    act(() => resolvers.get(1)?.(['risposta nuova']));
    await waitFor(() => expect(result.current.data).toEqual(['risposta nuova']));
    act(() => resolvers.get(0)?.(['risposta vecchia']));

    await waitFor(() => expect(result.current.data).toEqual(['risposta nuova']));
  });

  it('annulla la richiesta precedente quando cambia query', async () => {
    const signals: AbortSignal[] = [];
    const loader = vi.fn((_query: { page: number }, signal: AbortSignal) => {
      signals.push(signal);
      return new Promise<string[]>(() => undefined);
    });
    const { rerender } = renderHook(
      ({ page }) => usePaginatedResource({ query: { page }, enabled: true, initialData: [], loader }),
      { initialProps: { page: 0 } }
    );
    await waitFor(() => expect(signals).toHaveLength(1));

    rerender({ page: 1 });

    await waitFor(() => expect(signals).toHaveLength(2));
    expect(signals[0].aborted).toBe(true);
    expect(signals[1].aborted).toBe(false);
  });
});
