import { describe, expect, it } from 'vitest';
import { PageResponse } from '../api';
import { removePageItems, upsertPageItem } from './pageState';

type Row = { id: number; name: string };

const page: PageResponse<Row> = {
  content: [{ id: 1, name: 'Uno' }],
  page: 0,
  size: 2,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true
};

describe('pageState', () => {
  it('replaces an existing row using the command response', () => {
    const updated = upsertPageItem(page, { id: 1, name: 'Aggiornato' }, (row) => row.id);
    expect(updated.content).toEqual([{ id: 1, name: 'Aggiornato' }]);
    expect(updated.totalElements).toBe(1);
  });

  it('inserts a new row on the first page', () => {
    const updated = upsertPageItem(page, { id: 2, name: 'Due' }, (row) => row.id);
    expect(updated.content[0]).toEqual({ id: 2, name: 'Due' });
    expect(updated.totalElements).toBe(2);
  });

  it('removes confirmed rows from the current page', () => {
    const updated = removePageItems(page, new Set([1]), (row) => row.id);
    expect(updated.content).toEqual([]);
    expect(updated.totalElements).toBe(0);
  });
});
