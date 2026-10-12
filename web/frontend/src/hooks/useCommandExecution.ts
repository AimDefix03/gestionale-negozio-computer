import { useRef, useState } from 'react';

export type NoticeTone = 'info' | 'success' | 'warning' | 'error';

export type UiNotice = {
  tone: NoticeTone;
  title: string;
  message: string;
};

export type CommandExecutionResult<T> =
  | { status: 'saved'; value: T }
  | { status: 'saved-refresh-failed'; value: T; refreshError: unknown }
  | { status: 'failed'; error: unknown }
  | { status: 'duplicate' };

export type CommandOutcome =
  | { status: 'success'; saved: true }
  | { status: 'warning'; saved: boolean }
  | { status: 'error'; saved: false };

export type CommandExecutionOptions<T> = {
  key: string;
  command: () => Promise<T>;
  applyResponse: (value: T) => void;
  afterConfirmed?: (value: T) => void | Promise<void>;
  refresh?: () => Promise<void>;
  successMessage: string;
  refreshFailureMessage?: string;
};

export type CommandExecutor = <T>(options: CommandExecutionOptions<T>) => Promise<CommandExecutionResult<T>>;

type Options = {
  onNotice: (notice: UiNotice | null) => void;
  onCommandError: (error: unknown) => void;
};

export function commandWasSaved<T>(result: CommandExecutionResult<T>): result is Extract<CommandExecutionResult<T>, { status: 'saved' | 'saved-refresh-failed' }> {
  return result.status === 'saved' || result.status === 'saved-refresh-failed';
}

export function commandOutcome<T>(result: CommandExecutionResult<T>): CommandOutcome {
  if (result.status === 'saved') return { status: 'success', saved: true };
  if (result.status === 'saved-refresh-failed') return { status: 'warning', saved: true };
  if (result.status === 'duplicate') return { status: 'warning', saved: false };
  return { status: 'error', saved: false };
}

export default function useCommandExecution({ onNotice, onCommandError }: Options) {
  const activeKeys = useRef(new Set<string>());
  const [pendingCount, setPendingCount] = useState(0);

  async function executeCommand<T>(options: CommandExecutionOptions<T>): Promise<CommandExecutionResult<T>> {
    if (activeKeys.current.has(options.key)) {
      onNotice({
        tone: 'info',
        title: 'Operazione in corso',
        message: 'Il comando e gia stato inviato. Attendi l\'esito prima di riprovare.'
      });
      return { status: 'duplicate' };
    }

    activeKeys.current.add(options.key);
    setPendingCount((current) => current + 1);
    onNotice(null);

    try {
      let value: T;
      try {
        value = await options.command();
      } catch (error) {
        onCommandError(error);
        return { status: 'failed', error };
      }

      let presentationError: unknown;
      try {
        options.applyResponse(value);
        await options.afterConfirmed?.(value);
      } catch (error) {
        presentationError = error;
      }

      try {
        await options.refresh?.();
      } catch (error) {
        presentationError = error;
      }

      if (presentationError) {
        onNotice({
          tone: 'warning',
          title: 'Operazione salvata',
          message: options.refreshFailureMessage ?? 'Il comando e stato registrato, ma l\'aggiornamento automatico dei dati non e riuscito. Usa Aggiorna dati senza ripetere l\'operazione.'
        });
        return { status: 'saved-refresh-failed', value, refreshError: presentationError };
      }

      onNotice({ tone: 'success', title: 'Operazione salvata', message: options.successMessage });
      return { status: 'saved', value };
    } finally {
      activeKeys.current.delete(options.key);
      setPendingCount((current) => Math.max(0, current - 1));
    }
  }

  return { executeCommand, commandBusy: pendingCount > 0 };
}
