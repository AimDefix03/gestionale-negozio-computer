import { ReactNode } from 'react';

type Props = {
  title: string;
  rows: ReactNode[][];
  actions?: (key: string) => ReactNode;
  children?: ReactNode;
  footer?: ReactNode;
};

export default function DataList({ title, rows, actions, children, footer }: Props) {
  return (
    <section className="panel">
      <div className="section-heading compact"><span>Lista</span><h2>{title}</h2></div>
      {children}
      <div className="table-shell">
        <table>
          <tbody>
            {rows.length ? rows.map((row, rowIndex) => (
              <tr key={`${String(row[0])}-${rowIndex}`}>
                {row.map((cell, cellIndex) => <td key={`${String(row[0])}-${cellIndex}`}>{cell}</td>)}
                {actions && <td>{actions(String(row[0]))}</td>}
              </tr>
            )) : <tr><td>Nessun dato disponibile.</td></tr>}
          </tbody>
        </table>
      </div>
      {footer}
    </section>
  );
}
