import { Order } from '../api';
import { View } from '../types/ui';

export const money = new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' });
export const dateTime = new Intl.DateTimeFormat('it-IT', { dateStyle: 'short', timeStyle: 'short' });

export function orderStatusClass(status: Order['status']) {
  return ({ DRAFT: 'draft', CONFIRMED: 'confirmed', FULFILLED: 'fulfilled', CANCELED: 'canceled' } satisfies Record<Order['status'], string>)[status];
}

export function formatBytes(bytes: number) {
  if (bytes <= 0) return '0 MB';
  const units = ['B', 'KB', 'MB', 'GB'];
  const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / Math.pow(1024, exponent);
  return `${value.toFixed(exponent === 0 ? 0 : 1)} ${units[exponent]}`;
}

export function formatDuration(ms: number) {
  const minutes = Math.max(1, Math.floor(ms / 60000));
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return remainingMinutes ? `${hours} h ${remainingMinutes} min` : `${hours} h`;
}

export function titleForView(view: View) {
  return ({ dashboard: 'Dashboard operativa', catalog: 'Catalogo prodotti', partners: 'Anagrafiche', inventory: 'Magazzino', orders: 'Ordini', documents: 'Documenti simulati', reports: 'Report', accounts: 'Account', company: 'Configurazione azienda', monitoring: 'Monitoraggio', audit: 'Audit log' } satisfies Record<View, string>)[view];
}

export function subtitleForView(view: View) {
  return ({ dashboard: 'Panoramica su catalogo, scorte e ordini.', catalog: 'Gestione prodotti, brand, tipo prodotto e disponibilita.', partners: 'Clienti e fornitori con dati strutturati.', inventory: 'Carichi, scarichi e tracciamento giacenze.', orders: 'Ordini cliente, stati e totale acquisti.', documents: 'Fatture e note credito simulate.', reports: 'Analisi operativa di vendite, incassi e magazzino.', accounts: 'Gestione utenti e ruoli.', company: 'Dati emittente, aliquota predefinita e numerazioni annuali.', monitoring: 'Stato tecnico, sessioni, database e anomalie recenti.', audit: 'Traccia delle operazioni rilevanti.' } satisfies Record<View, string>)[view];
}
