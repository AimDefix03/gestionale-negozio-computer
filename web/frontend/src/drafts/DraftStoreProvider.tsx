import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { View } from '../types/ui';

export type DraftRegistration = {
  id: string;
  view: View;
  label: string;
  dirty: boolean;
  discard: () => void;
};

type DraftStoreContextValue = {
  dirtyViews: View[];
  hasDirtyDrafts: (view?: View) => boolean;
  labelsForView: (view: View) => string[];
  registerDraft: (registration: DraftRegistration) => () => void;
  markDraftClean: (id: string) => void;
  discardViewDrafts: (view: View) => void;
  discardAllDrafts: () => void;
};

const DraftStoreContext = createContext<DraftStoreContextValue | null>(null);

export function DraftStoreProvider({ children }: { children: ReactNode }) {
  const [registrations, setRegistrations] = useState<Record<string, DraftRegistration>>({});
  const registrationsRef = useRef<Record<string, DraftRegistration>>({});

  const replaceRegistrations = useCallback((next: Record<string, DraftRegistration>) => {
    registrationsRef.current = next;
    setRegistrations(next);
  }, []);

  const registerDraft = useCallback((registration: DraftRegistration) => {
    replaceRegistrations({ ...registrationsRef.current, [registration.id]: registration });
    return () => {
      if (registrationsRef.current[registration.id]?.discard !== registration.discard) return;
      const next = { ...registrationsRef.current };
      delete next[registration.id];
      replaceRegistrations(next);
    };
  }, [replaceRegistrations]);

  const markDraftClean = useCallback((id: string) => {
    const registration = registrationsRef.current[id];
    if (!registration || !registration.dirty) return;
    replaceRegistrations({
      ...registrationsRef.current,
      [id]: { ...registration, dirty: false }
    });
  }, [replaceRegistrations]);

  const activeDrafts = useMemo(() => Object.values(registrations).filter((draft) => draft.dirty), [registrations]);
  const dirtyViews = useMemo(() => Array.from(new Set(activeDrafts.map((draft) => draft.view))), [activeDrafts]);
  const hasDirtyDrafts = useCallback((view?: View) => Object.values(registrationsRef.current).some((draft) => draft.dirty && (!view || draft.view === view)), []);
  const labelsForView = useCallback((view: View) => Object.values(registrationsRef.current).filter((draft) => draft.dirty && draft.view === view).map((draft) => draft.label), []);
  const discardViewDrafts = useCallback((view: View) => {
    Object.values(registrationsRef.current).filter((draft) => draft.view === view).forEach((draft) => draft.discard());
  }, []);
  const discardAllDrafts = useCallback(() => {
    Object.values(registrationsRef.current).forEach((draft) => draft.discard());
  }, []);

  useEffect(() => {
    if (activeDrafts.length === 0) return;
    const protectDrafts = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', protectDrafts);
    return () => window.removeEventListener('beforeunload', protectDrafts);
  }, [activeDrafts.length]);

  const value = useMemo<DraftStoreContextValue>(() => ({
    dirtyViews,
    hasDirtyDrafts,
    labelsForView,
    registerDraft,
    markDraftClean,
    discardViewDrafts,
    discardAllDrafts
  }), [dirtyViews, discardAllDrafts, discardViewDrafts, hasDirtyDrafts, labelsForView, markDraftClean, registerDraft]);

  return <DraftStoreContext.Provider value={value}>{children}</DraftStoreContext.Provider>;
}

export function useDraftStore(): DraftStoreContextValue {
  const context = useContext(DraftStoreContext);
  if (!context) throw new Error('useDraftStore deve essere usato dentro DraftStoreProvider.');
  return context;
}
