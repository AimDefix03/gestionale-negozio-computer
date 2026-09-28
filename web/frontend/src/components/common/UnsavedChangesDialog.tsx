import { useRef } from 'react';
import AccessibleDialog from './AccessibleDialog';

type Props = {
  title: string;
  message: string;
  confirmLabel: string;
  draftLabels: string[];
  destructive?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
};

export default function UnsavedChangesDialog({ title, message, confirmLabel, draftLabels, destructive = false, onCancel, onConfirm }: Props) {
  const cancelRef = useRef<HTMLButtonElement>(null);

  return (
    <AccessibleDialog
      labelledBy="unsaved-changes-title"
      describedBy="unsaved-changes-description"
      initialFocusRef={cancelRef}
      onEscape={onCancel}
    >
      <div className="section-heading compact">
        <span>Bozza non completata</span>
        <h2 id="unsaved-changes-title">{title}</h2>
        <p id="unsaved-changes-description">{message}</p>
      </div>
      <ul className="draft-list">
        {draftLabels.map((label) => <li key={label}>{label}</li>)}
      </ul>
      <div className="form-actions">
        <button ref={cancelRef} className="button secondary" type="button" onClick={onCancel}>Resta qui</button>
        <button className={`button ${destructive ? 'danger' : 'primary'}`} type="button" onClick={onConfirm}>{confirmLabel}</button>
      </div>
    </AccessibleDialog>
  );
}
