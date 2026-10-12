import { KeyboardEvent, ReactNode, useEffect, useRef } from 'react';
import { UserAccount } from '../../api';
import { type UiNotice } from '../../hooks/useCommandExecution';
import { MenuGroup, View } from '../../types/ui';
import { dateTime, subtitleForView, titleForView } from '../../utils/formatters';

type Props = {
  currentUser: UserAccount;
  sessionExpiresAt: string;
  menuGroups: MenuGroup[];
  openMenu: string | null;
  openTabs: View[];
  dirtyViews: View[];
  activeView: View;
  notice: UiNotice | null;
  children: ReactNode;
  onMenuChange: (menu: string | null) => void;
  onOpenTab: (view: View) => boolean;
  onCloseTab: (view: View) => void;
  onActiveViewChange: (view: View) => void;
  onRefresh: () => void;
  onLogout: () => void;
};

export default function WorkspaceChrome(props: Props) {
  const menuTriggerRefs = useRef<Record<string, HTMLButtonElement | null>>({});
  const tabRefs = useRef<Record<string, HTMLButtonElement | null>>({});
  const viewTitle = (view: View) => view === 'sales' && props.currentUser.role === 'CUSTOMER' ? 'Nuovo ordine' : titleForView(view);

  useEffect(() => {
    if (!props.openMenu) return;
    const handleEscape = (event: globalThis.KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      const trigger = menuTriggerRefs.current[props.openMenu!];
      props.onMenuChange(null);
      window.setTimeout(() => trigger?.focus(), 0);
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [props.openMenu, props.onMenuChange]);

  function handleTabKeyDown(event: KeyboardEvent<HTMLButtonElement>, tab: View) {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    const currentIndex = props.openTabs.indexOf(tab);
    const nextIndex = event.key === 'Home'
      ? 0
      : event.key === 'End'
        ? props.openTabs.length - 1
        : (currentIndex + (event.key === 'ArrowRight' ? 1 : -1) + props.openTabs.length) % props.openTabs.length;
    const nextTab = props.openTabs[nextIndex];
    props.onActiveViewChange(nextTab);
    window.setTimeout(() => tabRefs.current[nextTab]?.focus(), 0);
  }

  return (
    <main className="desktop-shell">
      <header className="topbar">
        <div className="topbar-brand"><div className="prompt-mark">&gt;_</div><div><span>Gestionale Computer</span><strong>Workspace operativo</strong></div></div>
        <div className="topbar-actions">
          <div className="session-pill"><span>{props.currentUser.roleLabel}</span><strong>{props.currentUser.username}</strong></div>
          <button className="button secondary compact-button" onClick={props.onRefresh}>Aggiorna</button>
          <button className="button primary compact-button" onClick={props.onLogout}>Logout</button>
        </div>
      </header>
      <section className="command-bar" aria-label="Menu principale">
        {props.menuGroups.map((group) => (
          <div className="menu-group" key={group.id}>
            <button ref={(element) => { menuTriggerRefs.current[group.id] = element; }} id={`menu-trigger-${group.id}`} aria-controls={`menu-panel-${group.id}`} aria-haspopup="true" className={`menu-trigger ${props.openMenu === group.id ? 'active' : ''}`} type="button" aria-expanded={props.openMenu === group.id} onClick={() => props.onMenuChange(props.openMenu === group.id ? null : group.id)}><span>{group.label}</span><small aria-hidden="true">⌄</small></button>
            {props.openMenu === group.id && (
              <div className="menu-panel" id={`menu-panel-${group.id}`} role="group" aria-labelledby={`menu-trigger-${group.id}`}>
                {group.items.map((item) => (
                  <button key={item.label} className="menu-entry" type="button" disabled={item.disabled} onClick={() => {
                    const navigationCompleted = item.view ? props.onOpenTab(item.view) : true;
                    item.action?.();
                    if (navigationCompleted) props.onMenuChange(null);
                  }}><span>{item.label}</span><small>{item.description}</small></button>
                ))}
              </div>
            )}
          </div>
        ))}
      </section>
      <nav className="tab-strip" aria-label="Schede aperte">
        {props.openTabs.map((tab) => (
          <div key={tab} className={`browser-tab ${props.activeView === tab ? 'active' : ''}`}>
            <button
              ref={(element) => { tabRefs.current[tab] = element; }}
              id={`workspace-tab-${tab}`}
              className="tab-main"
              type="button"
              aria-current={props.activeView === tab ? 'page' : undefined}
              aria-label={`${viewTitle(tab)}${props.dirtyViews.includes(tab) ? ', bozza non completata' : ''}`}
              tabIndex={props.activeView === tab ? 0 : -1}
              onKeyDown={(event) => handleTabKeyDown(event, tab)}
              onClick={() => props.onActiveViewChange(tab)}
            >
              {viewTitle(tab)}
              {props.dirtyViews.includes(tab) && <span className="dirty-indicator" aria-hidden="true">•</span>}
            </button>
            {tab !== 'dashboard' && <button className="tab-close" type="button" aria-label={`Chiudi ${viewTitle(tab)}`} onClick={() => props.onCloseTab(tab)}>x</button>}
          </div>
        ))}
      </nav>
      <section id="workspace-panel" className="workspace chrome-workspace" aria-label={viewTitle(props.activeView)}>
        <header className="workspace-header">
          <div><span className="eyebrow">{props.activeView === 'dashboard' ? 'Workspace' : 'Scheda attiva'}</span><h1>{viewTitle(props.activeView)}</h1><p>{subtitleForView(props.activeView)}</p></div>
          <div className="workspace-meta"><span>Sessione</span><strong>{props.currentUser.username}</strong>{props.sessionExpiresAt && <small>Scade {dateTime.format(new Date(props.sessionExpiresAt))}</small>}</div>
        </header>
        {props.notice && (
          <div className={`alert ${props.notice.tone}`} role={props.notice.tone === 'error' ? 'alert' : 'status'}>
            <strong>{props.notice.title}</strong>
            <span>{props.notice.message}</span>
          </div>
        )}
        {props.children}
      </section>
    </main>
  );
}
