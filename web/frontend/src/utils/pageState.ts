import { PageResponse } from '../api';

export function upsertPageItem<T>(page: PageResponse<T>, item: T, identity: (value: T) => string | number, previousIdentity?: string | number): PageResponse<T> {
  const currentIdentity = identity(item);
  const matchIdentity = previousIdentity ?? currentIdentity;
  const existingIndex = page.content.findIndex((value) => identity(value) === matchIdentity);

  if (existingIndex >= 0) {
    const content = [...page.content];
    content[existingIndex] = item;
    return { ...page, content };
  }

  if (!page.first) return page;

  const totalElements = page.totalElements + 1;
  const content = [item, ...page.content].slice(0, page.size);
  return {
    ...page,
    content,
    totalElements,
    totalPages: page.size > 0 ? Math.ceil(totalElements / page.size) : 0,
    last: totalElements <= page.size
  };
}

export function removePageItems<T>(page: PageResponse<T>, identities: ReadonlySet<string | number>, identity: (value: T) => string | number): PageResponse<T> {
  const removed = page.content.filter((value) => identities.has(identity(value))).length;
  if (removed === 0) return page;

  const totalElements = Math.max(0, page.totalElements - removed);
  return {
    ...page,
    content: page.content.filter((value) => !identities.has(identity(value))),
    totalElements,
    totalPages: page.size > 0 ? Math.ceil(totalElements / page.size) : 0,
    last: page.page + 1 >= Math.ceil(totalElements / page.size)
  };
}
