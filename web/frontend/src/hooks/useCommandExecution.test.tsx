import { act, renderHook } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import useCommandExecution, { commandOutcome } from './useCommandExecution';

describe('useCommandExecution', () => {
  it('espone esiti tipizzati distinguendo persistenza, warning e fallimento', () => {
    expect(commandOutcome({ status: 'saved', value: true })).toEqual({ status: 'success', saved: true });
    expect(commandOutcome({ status: 'saved-refresh-failed', value: true, refreshError: new Error('refresh') })).toEqual({ status: 'warning', saved: true });
    expect(commandOutcome({ status: 'duplicate' })).toEqual({ status: 'warning', saved: false });
    expect(commandOutcome({ status: 'failed', error: new Error('failed') })).toEqual({ status: 'error', saved: false });
  });

  it('keeps a command confirmed when the targeted refresh fails', async () => {
    const onNotice = vi.fn();
    const onCommandError = vi.fn();
    const applyResponse = vi.fn();
    const afterConfirmed = vi.fn();
    const { result } = renderHook(() => useCommandExecution({ onNotice, onCommandError }));

    let execution;
    await act(async () => {
      execution = await result.current.executeCommand({
        key: 'product:create',
        command: async () => ({ code: 'P-1' }),
        applyResponse,
        afterConfirmed,
        refresh: async () => { throw new Error('GET 500'); },
        successMessage: 'Prodotto creato.'
      });
    });

    expect(execution).toMatchObject({ status: 'saved-refresh-failed', value: { code: 'P-1' } });
    expect(applyResponse).toHaveBeenCalledWith({ code: 'P-1' });
    expect(afterConfirmed).toHaveBeenCalledWith({ code: 'P-1' });
    expect(onCommandError).not.toHaveBeenCalled();
    expect(onNotice).toHaveBeenLastCalledWith(expect.objectContaining({ tone: 'warning', title: 'Operazione salvata' }));
  });

  it('preserves local state when the command times out', async () => {
    const onNotice = vi.fn();
    const onCommandError = vi.fn();
    const applyResponse = vi.fn();
    const afterConfirmed = vi.fn();
    const refresh = vi.fn();
    const timeout = new Error('Timeout');
    const { result } = renderHook(() => useCommandExecution({ onNotice, onCommandError }));

    let execution;
    await act(async () => {
      execution = await result.current.executeCommand({
        key: 'order:create',
        command: async () => { throw timeout; },
        applyResponse,
        afterConfirmed,
        refresh,
        successMessage: 'Ordine creato.'
      });
    });

    expect(execution).toEqual({ status: 'failed', error: timeout });
    expect(applyResponse).not.toHaveBeenCalled();
    expect(afterConfirmed).not.toHaveBeenCalled();
    expect(refresh).not.toHaveBeenCalled();
    expect(onCommandError).toHaveBeenCalledWith(timeout);
  });

  it('allows an intentional retry after a failed command', async () => {
    const command = vi.fn()
      .mockRejectedValueOnce(new Error('Timeout'))
      .mockResolvedValueOnce({ code: 'ORD-1' });
    const applyResponse = vi.fn();
    const { result } = renderHook(() => useCommandExecution({ onNotice: vi.fn(), onCommandError: vi.fn() }));

    await act(async () => {
      await result.current.executeCommand({ key: 'order:create', command, applyResponse, successMessage: 'Ordine creato.' });
      await result.current.executeCommand({ key: 'order:create', command, applyResponse, successMessage: 'Ordine creato.' });
    });

    expect(command).toHaveBeenCalledTimes(2);
    expect(applyResponse).toHaveBeenCalledWith({ code: 'ORD-1' });
  });

  it('blocks a duplicate submission while the first command is pending', async () => {
    let resolveCommand: ((value: { code: string }) => void) | undefined;
    const command = vi.fn(() => new Promise<{ code: string }>((resolve) => { resolveCommand = resolve; }));
    const onNotice = vi.fn();
    const { result } = renderHook(() => useCommandExecution({ onNotice, onCommandError: vi.fn() }));
    const options = { key: 'product:create', command, applyResponse: vi.fn(), successMessage: 'Prodotto creato.' };

    let firstExecution!: Promise<unknown>;
    let secondExecution!: Promise<unknown>;
    act(() => {
      firstExecution = result.current.executeCommand(options);
      secondExecution = result.current.executeCommand(options);
    });

    await expect(secondExecution).resolves.toEqual({ status: 'duplicate' });
    expect(command).toHaveBeenCalledTimes(1);
    expect(onNotice).toHaveBeenCalledWith(expect.objectContaining({ tone: 'info', title: 'Operazione in corso' }));

    await act(async () => {
      resolveCommand?.({ code: 'P-1' });
      await firstExecution;
    });
  });
});
