import { Dispatch, SetStateAction, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDraftStore } from '../drafts/DraftStoreProvider';
import { View } from '../types/ui';

type Options<T> = {
  key: string;
  view: View;
  label: string;
  initialValue: T;
  enabled?: boolean;
};

type DraftState<T> = {
  value: T;
  setValue: Dispatch<SetStateAction<T>>;
  dirty: boolean;
  clear: () => void;
};

const STORAGE_PREFIX = 'gestionale:draft:v1:';

export default function useDraftState<T>({ key, view, label, initialValue, enabled = true }: Options<T>): DraftState<T> {
  const storageKey = `${STORAGE_PREFIX}${key}`;
  const initialSerialized = useMemo(() => serialize(initialValue), [initialValue]);
  const baselineRef = useRef({ key: storageKey, value: initialValue, serialized: initialSerialized });
  const [value, setValue] = useState<T>(() => enabled ? readDraft(storageKey, initialValue) : initialValue);
  const { markDraftClean, registerDraft } = useDraftStore();

  useEffect(() => {
    baselineRef.current = { key: storageKey, value: initialValue, serialized: initialSerialized };
    setValue(enabled ? readDraft(storageKey, initialValue) : initialValue);
  }, [enabled, initialSerialized, storageKey]);

  const dirty = enabled && serialize(value) !== baselineRef.current.serialized;

  const clear = useCallback(() => {
    removeDraft(storageKey);
    markDraftClean(storageKey);
    setValue(baselineRef.current.value);
  }, [markDraftClean, storageKey]);

  useEffect(() => {
    if (!enabled || !dirty) {
      removeDraft(storageKey);
      return;
    }
    writeDraft(storageKey, value);
  }, [dirty, enabled, storageKey, value]);

  useEffect(() => registerDraft({ id: storageKey, view, label, dirty, discard: clear }), [clear, dirty, label, registerDraft, storageKey, view]);

  return { value, setValue, dirty, clear };
}

function readDraft<T>(key: string, fallback: T): T {
  try {
    const stored = window.sessionStorage.getItem(key);
    return stored ? JSON.parse(stored) as T : fallback;
  } catch {
    removeDraft(key);
    return fallback;
  }
}

function writeDraft<T>(key: string, value: T) {
  try {
    window.sessionStorage.setItem(key, serialize(value));
  } catch {
    return;
  }
}

function removeDraft(key: string) {
  try {
    window.sessionStorage.removeItem(key);
  } catch {
    return;
  }
}

function serialize(value: unknown): string {
  return JSON.stringify(value);
}
