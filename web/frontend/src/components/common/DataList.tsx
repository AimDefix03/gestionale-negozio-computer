import { ReactNode } from 'react';

type Props = {
  title: string;
  columns: string[];
  rows: ReactNode[][];
  actions?: (key: string) => ReactNode;
  children?: ReactNode;
  footer?: ReactNode;
  loading?: boolean;
  refreshing?: boolean;
  error?: string;
  onRetry?: () => void;
};

export default function DataList({ title, columns, rows, actions, children, footer, loading = false, refreshing = false, error = '', onRetry }: Props) {
  return (
    <section className="panel">
      <div className="section-heading compact"><span>Lista</span><h2>{title}</h2></div>
      {children}
      {loading && <div className="resource-status" role="status">Caricamento dati...</div>}
      {refreshing && <div className="resource-status compact" role="status">Aggiornamento in corso...</div>}
      {error && <div className="resource-error" role="alert"><span>{error}</span>{onRetry && <button className="button secondary compact-button" type="button" onClick={onRetry}>Riprova</button>}</div>}
      {(!loading || rows.length > 0) && <div className="table-shell" aria-busy={refreshing}>
        <table>
          <caption className="sr-only">{title}</caption>
          <thead>
            <tr>
              {columns.map((column) => <th scope="col" key={column}>{column}</th>)}
              {actions && <th scope="col">Azioni</th>}
            </tr>
          </thead>
          <tbody>
            {rows.length ? rows.map((row, rowIndex) => (
              <tr key={`${String(row[0])}-${rowIndex}`}>
                {row.map((cell, cellIndex) => cellIndex === 0
                  ? <th scope="row" key={`${String(row[0])}-${cellIndex}`}>{cell}</th>
                  : <td key={`${String(row[0])}-${cellIndex}`}>{cell}</td>)}
                {actions && <td>{actions(String(row[0]))}</td>}
              </tr>
            )) : <tr><td colSpan={columns.length + (actions ? 1 : 0)}>Nessun dato disponibile.</td></tr>}
          </tbody>
        </table>
      </div>}
      {!loading && footer}
    </section>
  );
}
