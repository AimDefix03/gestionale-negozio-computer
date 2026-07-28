import { ReactNode } from 'react';
import { UserAccount } from '../../api';
import { MenuGroup, View } from '../../types/ui';
import { dateTime, subtitleForView, titleForView } from '../../utils/formatters';

type Props = {
  currentUser: UserAccount;
  sessionExpiresAt: string;
  menuGroups: MenuGroup[];
  openMenu: string | null;
  openTabs: View[];
  activeView: View;
  message: string;
  children: ReactNode;
  onMenuChange: (menu: string | null) => void;
  onOpenTab: (view: View) => void;
  onCloseTab: (view: View) => void;
  onActiveViewChange: (view: View) => void;
  onRefresh: () => void;
  onLogout: () => void;
};

export default function WorkspaceChrome(props: Props) {
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
            <button className={`menu-trigger ${props.openMenu === group.id ? 'active' : ''}`} type="button" aria-expanded={props.openMenu === group.id} onClick={() => props.onMenuChange(props.openMenu === group.id ? null : group.id)}><span>{group.label}</span><small>⌄</small></button>
            {props.openMenu === group.id && (
              <div className="menu-panel">
                {group.items.map((item) => (
                  <button key={item.label} className="menu-entry" type="button" disabled={item.disabled} onClick={() => {
                    if (item.view) props.onOpenTab(item.view);
                    item.action?.();
                    props.onMenuChange(null);
                  }}><span>{item.label}</span><small>{item.description}</small></button>
                ))}
              </div>
            )}
          </div>
        ))}
      </section>
      <section className="tab-strip" aria-label="Schede aperte">
        {props.openTabs.map((tab) => (
          <div key={tab} className={`browser-tab ${props.activeView === tab ? 'active' : ''}`}>
            <button className="tab-main" type="button" onClick={() => props.onActiveViewChange(tab)}>{titleForView(tab)}</button>
            {tab !== 'dashboard' && <button className="tab-close" type="button" aria-label={`Chiudi ${titleForView(tab)}`} onClick={() => props.onCloseTab(tab)}>x</button>}
          </div>
        ))}
      </section>
      <section className="workspace chrome-workspace">
        <header className="workspace-header">
          <div><span className="eyebrow">{props.activeView === 'dashboard' ? 'Workspace' : 'Scheda attiva'}</span><h1>{titleForView(props.activeView)}</h1><p>{subtitleForView(props.activeView)}</p></div>
          <div className="workspace-meta"><span>Sessione</span><strong>{props.currentUser.username}</strong>{props.sessionExpiresAt && <small>Scade {dateTime.format(new Date(props.sessionExpiresAt))}</small>}</div>
        </header>
        {props.message && <div className="alert" role="alert">{props.message}</div>}
        {props.children}
      </section>
    </main>
  );
}
