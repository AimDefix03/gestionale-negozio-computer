import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { useDraftStore } from '../drafts/DraftStoreProvider';
import { renderWithDrafts } from '../test/renderWithDrafts';
import useDraftState from './useDraftState';

function Harness() {
  const draft = useDraftState({
    key: 'catalog:new',
    view: 'catalog',
    label: 'Nuovo prodotto',
    initialValue: { code: '' }
  });
  const store = useDraftStore();
  return (
    <>
      <label>Codice<input value={draft.value.code} onChange={(event) => draft.setValue({ code: event.target.value })} /></label>
      <output>{draft.dirty ? 'Bozza attiva' : 'Nessuna bozza'}</output>
      <button type="button" onClick={() => store.discardViewDrafts('catalog')}>Scarta bozza</button>
    </>
  );
}

function ImmediateCleanHarness({ onClean }: { onClean: (dirty: boolean, labels: string[]) => void }) {
  const draft = useDraftState({
    key: 'catalog:immediate',
    view: 'catalog',
    label: 'Prodotto immediato',
    initialValue: { code: '' }
  });
  const store = useDraftStore();
  return (
    <>
      <label>Codice immediato<input value={draft.value.code} onChange={(event) => draft.setValue({ code: event.target.value })} /></label>
      <button type="button" onClick={() => {
        draft.clear();
        onClean(store.hasDirtyDrafts('catalog'), store.labelsForView('catalog'));
      }}>Salva e verifica</button>
    </>
  );
}

describe('useDraftState', () => {
  beforeEach(() => window.sessionStorage.clear());

  it('salva per vista, ripristina dopo il remount e protegge l uscita', async () => {
    const user = userEvent.setup();
    const first = renderWithDrafts(<Harness />);
    await user.type(screen.getByLabelText('Codice'), 'GPU-5090');

    expect(screen.getByText('Bozza attiva')).toBeInTheDocument();
    expect(window.sessionStorage.getItem('gestionale:draft:v1:catalog:new')).toContain('GPU-5090');
    const beforeUnload = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(beforeUnload);
    expect(beforeUnload.defaultPrevented).toBe(true);

    first.unmount();
    renderWithDrafts(<Harness />);
    expect(screen.getByLabelText('Codice')).toHaveValue('GPU-5090');
    await user.click(screen.getByRole('button', { name: 'Scarta bozza' }));
    expect(screen.getByLabelText('Codice')).toHaveValue('');
    expect(window.sessionStorage.getItem('gestionale:draft:v1:catalog:new')).toBeNull();
  });

  it('ignora una bozza corrotta senza bloccare il form', () => {
    window.sessionStorage.setItem('gestionale:draft:v1:catalog:new', '{invalido');
    renderWithDrafts(<Harness />);
    expect(screen.getByLabelText('Codice')).toHaveValue('');
    expect(window.sessionStorage.getItem('gestionale:draft:v1:catalog:new')).toBeNull();
  });

  it('rende subito pulito il registro dopo un salvataggio confermato', async () => {
    const user = userEvent.setup();
    const observations: Array<{ dirty: boolean; labels: string[] }> = [];
    renderWithDrafts(<ImmediateCleanHarness onClean={(dirty, labels) => observations.push({ dirty, labels })} />);

    await user.type(screen.getByLabelText('Codice immediato'), 'GPU-5090');
    await user.click(screen.getByRole('button', { name: 'Salva e verifica' }));

    expect(observations).toEqual([{ dirty: false, labels: [] }]);
  });
});
