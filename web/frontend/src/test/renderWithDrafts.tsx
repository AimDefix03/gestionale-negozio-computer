import { ReactElement, ReactNode } from 'react';
import { render, renderHook, RenderHookOptions, RenderOptions } from '@testing-library/react';
import { DraftStoreProvider } from '../drafts/DraftStoreProvider';

function DraftWrapper({ children }: { children: ReactNode }) {
  return <DraftStoreProvider>{children}</DraftStoreProvider>;
}

export function renderWithDrafts(ui: ReactElement, options?: Omit<RenderOptions, 'wrapper'>) {
  return render(ui, { wrapper: DraftWrapper, ...options });
}

export function renderHookWithDrafts<Result, Props>(
  callback: (props: Props) => Result,
  options?: Omit<RenderHookOptions<Props>, 'wrapper'>
) {
  return renderHook(callback, { wrapper: DraftWrapper, ...options });
}
