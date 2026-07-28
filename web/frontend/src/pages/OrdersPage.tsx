import { useState } from 'react';
import { Order, OrderQuery, PageResponse, ReceiptPayload, ReturnRefundPayload, ReturnRequestPayload, UserAccount } from '../api';
import PaginationControls from '../components/PaginationControls';
import DataList from '../components/common/DataList';
import OrderOperationsPanel from '../components/orders/OrderOperationsPanel';
import { dateTime, money, orderStatusClass } from '../utils/formatters';

type Props = {
  page: PageResponse<Order>;
  query: OrderQuery;
  currentUser: UserAccount;
  canManageDocuments: boolean;
  canRecordPayments: boolean;
  canRequestReturns: boolean;
  canManageReturns: boolean;
  canRefundPayments: boolean;
  busy: boolean;
  pageSize: number;
  canConfirm: (order: Order) => boolean;
  canFulfill: (order: Order) => boolean;
  canCancel: (order: Order) => boolean;
  onQueryChange: (query: OrderQuery) => void;
  onConfirm: (code: string) => void;
  onFulfill: (code: string) => void;
  onCancel: (code: string) => void;
  onInvoice: (code: string) => void;
  onReceipt: (code: string, payload: ReceiptPayload) => void;
  onRequestReturn: (code: string, payload: ReturnRequestPayload) => void;
  onApproveReturn: (orderCode: string, returnCode: string, note: string) => void;
  onRejectReturn: (orderCode: string, returnCode: string, note: string) => void;
  onReceiveReturn: (orderCode: string, returnCode: string) => void;
  onRefundReturn: (orderCode: string, returnCode: string, payload: ReturnRefundPayload) => void;
};

export default function OrdersPage(props: Props) {
  const [selectedCode, setSelectedCode] = useState('');
  const selectedOrder = props.page.content.find((order) => order.code === selectedCode) ?? null;

  return (
    <>
      <DataList
      title="Ordini"
      rows={props.page.content.map((order) => [order.code, order.customerCode || '-', order.customer, <span className={`status-badge ${orderStatusClass(order.status)}`}>{order.statusLabel}</span>, money.format(order.total), <span>{order.payment.methodLabel}<small className="cell-note">{order.payment.statusLabel}</small></span>, dateTime.format(new Date(order.timestamp))])}
      actions={(orderCode) => {
        const order = props.page.content.find((item) => item.code === orderCode);
        if (!order) return null;
        return (
          <div className="row-actions">
            {props.canConfirm(order) && <button className="link-button" onClick={() => props.onConfirm(order.code)}>Conferma</button>}
            {props.canFulfill(order) && <button className="link-button" onClick={() => props.onFulfill(order.code)}>Evadi</button>}
            {props.canCancel(order) && <button className="link-button danger-text" onClick={() => props.onCancel(order.code)}>Annulla</button>}
            {props.canManageDocuments && order.status === 'FULFILLED' && <button className="link-button" onClick={() => props.onInvoice(order.code)}>Fattura</button>}
            <button className="link-button" onClick={() => setSelectedCode(order.code)}>Operazioni</button>
          </div>
        );
      }}
      footer={<PaginationControls page={props.page} onPageChange={(page) => props.onQueryChange({ ...props.query, page })} />}
    >
      <div className="list-filters">
        <label>Cerca<input value={props.query.q ?? ''} placeholder="Codice ordine, cliente o metodo" onChange={(event) => props.onQueryChange({ ...props.query, q: event.target.value, page: 0 })} /></label>
        {props.currentUser.role !== 'CUSTOMER' && <label>Cliente<input value={props.query.customer ?? ''} placeholder="Filtra cliente" onChange={(event) => props.onQueryChange({ ...props.query, customer: event.target.value, page: 0 })} /></label>}
        <button className="button secondary compact-button" type="button" onClick={() => props.onQueryChange({ page: 0, size: props.pageSize })}>Reset</button>
      </div>
      </DataList>
      <OrderOperationsPanel
        order={selectedOrder}
        busy={props.busy}
        canRecordPayments={props.canRecordPayments}
        canRequestReturns={props.canRequestReturns}
        canManageReturns={props.canManageReturns}
        canRefundPayments={props.canRefundPayments}
        onReceipt={props.onReceipt}
        onRequestReturn={props.onRequestReturn}
        onApproveReturn={props.onApproveReturn}
        onRejectReturn={props.onRejectReturn}
        onReceiveReturn={props.onReceiveReturn}
        onRefundReturn={props.onRefundReturn}
      />
    </>
  );
}
