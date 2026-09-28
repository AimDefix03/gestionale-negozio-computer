import { PageResponse } from '../api';

type Props = {
  page: PageResponse<unknown>;
  onPageChange: (page: number) => void;
};

export default function PaginationControls({ page, onPageChange }: Props) {
  const from = page.totalElements === 0 ? 0 : page.page * page.size + 1;
  const to = Math.min((page.page + 1) * page.size, page.totalElements);

  return (
    <div className="pagination-bar">
      <span>{from}-{to} di {page.totalElements}</span>
      <div className="pagination-actions">
        <button className="button secondary compact-button" type="button" disabled={page.first} onClick={() => onPageChange(0)}>Prima</button>
        <button className="button secondary compact-button" type="button" disabled={page.first} onClick={() => onPageChange(page.page - 1)}>Indietro</button>
        <strong>Pagina {page.totalPages === 0 ? 0 : page.page + 1} / {page.totalPages}</strong>
        <button className="button secondary compact-button" type="button" disabled={page.last || page.totalPages === 0} onClick={() => onPageChange(page.page + 1)}>Avanti</button>
      </div>
    </div>
  );
}
