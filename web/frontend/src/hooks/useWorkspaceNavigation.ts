import { useState } from 'react';
import { View } from '../types/ui';

export default function useWorkspaceNavigation() {
  const [activeView, setActiveView] = useState<View>('dashboard');
  const [openTabs, setOpenTabs] = useState<View[]>(['dashboard']);
  const [openMenu, setOpenMenu] = useState<string | null>(null);

  function openTab(target: View) {
    setOpenTabs((current) => current.includes(target) ? current : [...current, target]);
    setActiveView(target);
    setOpenMenu(null);
  }

  function closeTab(target: View) {
    if (target === 'dashboard') return;
    setOpenTabs((current) => {
      const targetIndex = current.indexOf(target);
      const nextTabs = current.filter((tab) => tab !== target);
      if (activeView === target) {
        setActiveView(nextTabs[targetIndex - 1] ?? nextTabs[targetIndex] ?? 'dashboard');
      }
      return nextTabs.length ? nextTabs : ['dashboard'];
    });
  }

  function resetNavigation() {
    setOpenTabs(['dashboard']);
    setActiveView('dashboard');
    setOpenMenu(null);
  }

  return {
    activeView,
    openTabs,
    openMenu,
    setActiveView,
    setOpenMenu,
    openTab,
    closeTab,
    resetNavigation
  };
}
