import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import AccessibleDialog from './AccessibleDialog';

function DialogHarness({ onEscape = vi.fn() }: { onEscape?: () => void }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>Apri dialog</button>
      {open && (
        <AccessibleDialog labelledBy="dialog-title" onEscape={() => { onEscape(); setOpen(false); }}>
          <h2 id="dialog-title">Conferma operazione</h2>
          <button type="button">Prima azione</button>
          <button type="button" onClick={() => setOpen(false)}>Chiudi</button>
        </AccessibleDialog>
      )}
    </>
  );
}

describe('AccessibleDialog', () => {
  it('intrappola il focus, gestisce Escape e ripristina il controllo di origine', async () => {
    const user = userEvent.setup();
    const onEscape = vi.fn();
    const backgroundEscapeHandler = vi.fn();
    document.addEventListener('keydown', backgroundEscapeHandler);
    render(<DialogHarness onEscape={onEscape} />);

    const trigger = screen.getByRole('button', { name: 'Apri dialog' });
    await user.click(trigger);
    const first = screen.getByRole('button', { name: 'Prima azione' });
    const last = screen.getByRole('button', { name: 'Chiudi' });
    expect(first).toHaveFocus();

    last.focus();
    fireEvent.keyDown(document, { key: 'Tab' });
    expect(first).toHaveFocus();
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true });
    expect(last).toHaveFocus();

    backgroundEscapeHandler.mockClear();
    await user.keyboard('{Escape}');
    expect(onEscape).toHaveBeenCalledOnce();
    expect(backgroundEscapeHandler).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await waitFor(() => expect(trigger).toHaveFocus());
    document.removeEventListener('keydown', backgroundEscapeHandler);
  });

  it('ripristina il focus sulla scheda attiva se il controllo di origine viene rimosso', async () => {
    const user = userEvent.setup();

    function UnmountedTriggerHarness() {
      const [open, setOpen] = useState(false);
      const [openedOnce, setOpenedOnce] = useState(false);
      return (
        <>
          <button aria-current="page" type="button">Catalogo prodotti</button>
          {!openedOnce && <button type="button" onClick={() => { setOpenedOnce(true); setOpen(true); }}>Apri da menu</button>}
          {open && (
            <AccessibleDialog labelledBy="fallback-title" onEscape={() => setOpen(false)}>
              <h2 id="fallback-title">Bozza aperta</h2>
              <button type="button">Resta qui</button>
            </AccessibleDialog>
          )}
        </>
      );
    }

    render(<UnmountedTriggerHarness />);
    await user.click(screen.getByRole('button', { name: 'Apri da menu' }));
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Catalogo prodotti' })).toHaveFocus());
  });
});
