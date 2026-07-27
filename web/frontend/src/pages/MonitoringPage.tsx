import { SystemStatus } from '../api';
import StatCard from '../components/common/StatCard';
import { dateTime, formatBytes, formatDuration } from '../utils/formatters';

type Props = {
  status: SystemStatus | null;
  onRefresh: () => void;
};

export default function MonitoringPage({ status, onRefresh }: Props) {
  if (!status) {
    return (
      <section className="panel">
        <div className="section-heading compact"><span>Monitoraggio</span><h2>Stato sistema</h2><p>Carica lo stato operativo del backend, del database e della sicurezza.</p></div>
        <button className="button primary compact-button" type="button" onClick={onRefresh}>Carica stato</button>
      </section>
    );
  }

  const memoryUsage = status.runtime.maxMemoryBytes > 0 ? Math.round((status.runtime.usedMemoryBytes / status.runtime.maxMemoryBytes) * 100) : 0;

  return (
    <>
      <section className="panel monitoring-toolbar">
        <div className="section-heading compact"><span>Monitoraggio</span><h2>Stato sistema</h2><p>Ultimo aggiornamento {dateTime.format(new Date(status.timestamp))} · Profilo {status.activeProfile}</p></div>
        <button className="button secondary compact-button" type="button" onClick={onRefresh}>Aggiorna</button>
      </section>
      <section className="stats-grid monitoring-stats">
        <StatCard label="Sistema" value={status.status} caption={`${status.application} · ${formatDuration(status.uptimeMs)}`} />
        <StatCard label="Database" value={`${status.database.status} · ${status.database.latencyMs} ms`} caption={status.database.name || status.database.message} />
        <StatCard label="Sessioni attive" value={String(status.security.activeSessions)} caption={`${status.security.lockedLoginAttempts} blocchi login`} />
        <StatCard label="Audit 24h" value={String(status.audit.criticalLast24h + status.audit.warningsLast24h)} caption={`${status.audit.criticalLast24h} critical · ${status.audit.warningsLast24h} warning`} />
      </section>
      <div className="content-grid two">
        <section className="panel">
          <div className="section-heading compact"><span>Runtime</span><h2>Risorse applicative</h2><p>Indicatori leggeri per capire se l'applicazione sta lavorando in modo regolare.</p></div>
          <div className="metric-grid">
            <div><span>Memoria usata</span><strong>{formatBytes(status.runtime.usedMemoryBytes)}</strong><small>{memoryUsage}% del massimo</small></div>
            <div><span>Memoria massima</span><strong>{formatBytes(status.runtime.maxMemoryBytes)}</strong><small>JVM</small></div>
            <div><span>Processori</span><strong>{status.runtime.availableProcessors}</strong><small>Disponibili</small></div>
            <div><span>Tentativi login recenti</span><strong>{status.security.recentLoginAttempts}</strong><small>Ultima ora</small></div>
          </div>
        </section>
        <section className="panel">
          <div className="section-heading compact"><span>Errori API</span><h2>Anomalie recenti</h2><p>Ultimi errori interni tracciati con codice richiesta.</p></div>
          {status.recentErrors.length ? (
            <ul className="system-feed">{status.recentErrors.map((error) => <li key={error.requestId}><div><span className="status-badge out">{error.status}</span><strong>{error.code}</strong></div><p>{error.message}</p><small>{error.path} · {error.requestId} · {dateTime.format(new Date(error.timestamp))}</small></li>)}</ul>
          ) : <div className="empty-state">Nessun errore interno recente.</div>}
        </section>
      </div>
      <section className="panel">
        <div className="section-heading compact"><span>Audit</span><h2>Eventi sensibili recenti</h2><p>Eventi warning e critical utili per controllare operazioni delicate.</p></div>
        {status.audit.recentImportantEvents.length ? (
          <ul className="system-feed">{status.audit.recentImportantEvents.map((event) => <li key={event.id}><div><span className={`status-badge ${event.severity.toLowerCase()}`}>{event.severity}</span><strong>{event.action}</strong></div><p>{event.target} · {event.details}</p><small>{event.actor} · {event.entityType} · {event.requestId} · {dateTime.format(new Date(event.timestamp))}</small></li>)}</ul>
        ) : <div className="empty-state">Nessun evento warning o critical recente.</div>}
      </section>
    </>
  );
}
