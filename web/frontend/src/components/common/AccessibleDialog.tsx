import { ReactNode, RefObject, useEffect, useRef } from 'react';

type Props = {
  labelledBy: string;
  describedBy?: string;
  className?: string;
  initialFocusRef?: RefObject<HTMLElement | null>;
  onEscape?: () => void;
  children: ReactNode;
};

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(',');

export default function AccessibleDialog({ labelledBy, describedBy, className = 'modal-card', initialFocusRef, onEscape, children }: Props) {
  const dialogRef = useRef<HTMLElement>(null);
  const escapeHandlerRef = useRef(onEscape);
  const initialFocusRefRef = useRef(initialFocusRef);
  escapeHandlerRef.current = onEscape;
  initialFocusRefRef.current = initialFocusRef;

  useEffect(() => {
    const previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const dialog = dialogRef.current;
    if (!dialog) return;

    const focusable = focusableElements(dialog);
    (initialFocusRefRef.current?.current ?? focusable[0] ?? dialog).focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault();
        event.stopImmediatePropagation();
        escapeHandlerRef.current?.();
        return;
      }
      if (event.key !== 'Tab') return;

      const elements = focusableElements(dialog!);
      if (elements.length === 0) {
        event.preventDefault();
        dialog!.focus();
        return;
      }

      const first = elements[0];
      const last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener('keydown', handleKeyDown, true);
    return () => {
      document.removeEventListener('keydown', handleKeyDown, true);
      window.setTimeout(() => {
        if (previouslyFocused?.isConnected && previouslyFocused !== document.body && previouslyFocused !== document.documentElement) {
          previouslyFocused.focus();
          return;
        }
        document.querySelector<HTMLElement>('[aria-current="page"], main button, a[href]')?.focus();
      }, 0);
    };
  }, []);

  return (
    <div className="modal-backdrop" role="presentation">
      <section
        ref={dialogRef}
        className={className}
        role="dialog"
        aria-modal="true"
        aria-labelledby={labelledBy}
        aria-describedby={describedBy}
        tabIndex={-1}
      >
        {children}
      </section>
    </div>
  );
}

function focusableElements(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR))
    .filter((element) => !element.hidden && element.getAttribute('aria-hidden') !== 'true');
}
