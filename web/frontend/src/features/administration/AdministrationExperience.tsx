import { FormEvent } from 'react';
import AccountsPage from '../../pages/AccountsPage';
import AuditPage from '../../pages/AuditPage';
import CompanySettingsPage from '../../pages/CompanySettingsPage';
import MonitoringPage from '../../pages/MonitoringPage';
import { errorMessage } from '../../utils/errors';
import type { AdministrationFlowController } from './useAdministrationFlow';

type Props = {
  view: 'accounts' | 'company' | 'monitoring' | 'audit';
  flow: AdministrationFlowController;
  isSuperAdmin: boolean;
  busy: boolean;
};

export default function AdministrationExperience({ view, flow, isSuperAdmin, busy }: Props) {
  if (view === 'accounts') return renderAccounts();
  if (view === 'company') return renderCompany();
  if (view === 'monitoring') return renderMonitoring();
  return renderAudit();

  function renderAccounts() {
    const resource = flow.accounts;
    const retry = () => void resource.refresh().catch(() => undefined);
    if (resource.loading && resource.page.content.length === 0) {
      return <ResourceState area="Account" title="Caricamento account" message="Sto recuperando utenti, ruoli e stato degli accessi." />;
    }
    if (resource.error && resource.page.content.length === 0) {
      return <ResourceState area="Account" title="Account non disponibili" message={errorMessage(resource.error)} onRetry={retry} />;
    }

    function submit(event: FormEvent<HTMLFormElement>) {
      event.preventDefault();
      void resource.create();
    }

    return (
      <>
        {resource.error && <ResourceState compact area="Account" title="Aggiornamento non riuscito" message={`${errorMessage(resource.error)} Gli account gia caricati restano disponibili.`} onRetry={retry} />}
        <AccountsPage
          page={resource.page}
          query={resource.query}
          form={resource.form}
          reauthPassword={resource.reauthPassword}
          isSuperAdmin={isSuperAdmin}
          busy={busy || resource.refreshing}
          pageSize={resource.pageSize}
          loading={resource.loading}
          refreshing={resource.refreshing}
          onQueryChange={resource.setQuery}
          onFormChange={resource.setForm}
          onReauthPasswordChange={resource.setReauthPassword}
          onSubmit={submit}
          canManage={resource.canManage}
          protectionLabel={resource.protectionLabel}
          onDisable={(username, reason) => void resource.disable(username, reason)}
          onEnable={(username, reason) => void resource.enable(username, reason)}
          onResetPassword={(username, password, reason) => void resource.resetPassword(username, password, reason)}
          onRevokeSessions={(username, reason) => void resource.revokeSessions(username, reason)}
          onRoleChange={(username, role, reason) => void resource.changeRole(username, role, reason)}
        />
      </>
    );
  }

  function renderAudit() {
    const resource = flow.audit;
    const retry = () => void resource.refresh().catch(() => undefined);
    if (resource.loading && resource.page.content.length === 0) {
      return <ResourceState area="Audit" title="Caricamento audit log" message="Sto recuperando le operazioni sensibili registrate dal sistema." />;
    }
    if (resource.error && resource.page.content.length === 0) {
      return <ResourceState area="Audit" title="Audit log non disponibile" message={errorMessage(resource.error)} onRetry={retry} />;
    }
    return (
      <>
        {resource.error && <ResourceState compact area="Audit" title="Aggiornamento non riuscito" message={`${errorMessage(resource.error)} Gli eventi gia caricati restano disponibili.`} onRetry={retry} />}
        <AuditPage page={resource.page} query={resource.query} pageSize={resource.pageSize} onQueryChange={resource.setQuery} />
      </>
    );
  }

  function renderCompany() {
    const resource = flow.company;
    const retry = () => void resource.refresh().catch(() => undefined);
    if (resource.loading && !resource.settings) {
      return <ResourceState area="Configurazione" title="Caricamento configurazione" message="Sto recuperando dati aziendali, IVA e numerazione documentale." />;
    }
    if (resource.error && !resource.settings) {
      return <ResourceState area="Configurazione" title="Configurazione non disponibile" message={errorMessage(resource.error)} onRetry={retry} />;
    }
    return (
      <>
        {resource.error && <ResourceState compact area="Configurazione" title="Aggiornamento non riuscito" message={`${errorMessage(resource.error)} La configurazione gia caricata resta disponibile.`} onRetry={retry} />}
        <CompanySettingsPage settings={resource.settings} busy={busy || resource.refreshing} onSave={resource.save} onReload={retry} />
      </>
    );
  }

  function renderMonitoring() {
    const resource = flow.monitoring;
    const retry = () => void resource.refresh().catch(() => undefined);
    if (resource.loading && !resource.status) {
      return <ResourceState area="Monitoraggio" title="Caricamento stato sistema" message="Sto verificando backend, database, sessioni ed eventi sensibili." />;
    }
    if (resource.error && !resource.status) {
      return <ResourceState area="Monitoraggio" title="Stato sistema non disponibile" message={errorMessage(resource.error)} onRetry={retry} />;
    }
    return (
      <>
        {resource.error && <ResourceState compact area="Monitoraggio" title="Aggiornamento non riuscito" message={`${errorMessage(resource.error)} Lo stato precedente resta disponibile.`} onRetry={retry} />}
        <MonitoringPage status={resource.status} onRefresh={retry} />
      </>
    );
  }
}

type ResourceStateProps = {
  area: string;
  title: string;
  message: string;
  compact?: boolean;
  onRetry?: () => void;
};

function ResourceState({ area, title, message, compact = false, onRetry }: ResourceStateProps) {
  return (
    <section className={`panel administration-resource-state${compact ? ' compact' : ''}`} role={compact ? 'alert' : 'status'}>
      <div className="section-heading compact">
        <span>{area}</span>
        <h2>{title}</h2>
        <p>{message}</p>
      </div>
      {onRetry && <button className="button secondary" type="button" onClick={onRetry}>Riprova</button>}
    </section>
  );
}
