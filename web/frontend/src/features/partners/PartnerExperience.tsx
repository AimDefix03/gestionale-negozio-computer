import { FormEvent } from 'react';
import PartnersPage from '../../pages/PartnersPage';
import { errorMessage } from '../../utils/errors';
import type { PartnerFlowController } from './usePartnerFlow';

type Props = {
  flow: PartnerFlowController;
  canManage: boolean;
  canLinkAccounts: boolean;
  busy: boolean;
};

export default function PartnerExperience({ flow, canManage, canLinkAccounts, busy }: Props) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void flow.submitPartner();
  }

  if (flow.loading && flow.page.content.length === 0) {
    return <PartnerState title="Caricamento anagrafiche" message="Sto recuperando clienti, fornitori e collegamenti account." />;
  }

  if (flow.error && flow.page.content.length === 0) {
    return <PartnerState title="Anagrafiche non disponibili" message={errorMessage(flow.error)} onRetry={() => void flow.refreshResources()} />;
  }

  return (
    <>
      {flow.error && (
        <PartnerState
          compact
          title="Aggiornamento non riuscito"
          message={`${errorMessage(flow.error)} I dati gia caricati restano disponibili.`}
          onRetry={() => void flow.refreshResources()}
        />
      )}
      <PartnersPage
        page={flow.page}
        query={flow.query}
        form={flow.form}
        formDirty={flow.formDirty}
        editingPartner={flow.editingPartner}
        customerAccounts={flow.customerAccounts}
        canManage={canManage}
        canLinkAccounts={canLinkAccounts}
        busy={busy || flow.refreshing}
        pageSize={flow.pageSize}
        onQueryChange={flow.setQuery}
        onFormChange={flow.setForm}
        onEdit={flow.editPartner}
        onDeactivate={(code) => void flow.deactivate(code)}
        onLinkAccount={(code, accountId) => void flow.linkAccount(code, accountId)}
        onUnlinkAccount={(code) => void flow.unlinkAccount(code)}
        onCancelEdit={flow.cancelEdit}
        onSubmit={handleSubmit}
      />
    </>
  );
}

type PartnerStateProps = {
  title: string;
  message: string;
  compact?: boolean;
  onRetry?: () => void;
};

function PartnerState({ title, message, compact = false, onRetry }: PartnerStateProps) {
  return (
    <section className={`panel partner-resource-state${compact ? ' compact' : ''}`} role="status">
      <div className="section-heading compact">
        <span>Anagrafiche</span>
        <h2>{title}</h2>
        <p>{message}</p>
      </div>
      {onRetry && <button className="button secondary" type="button" onClick={onRetry}>Riprova</button>}
    </section>
  );
}
