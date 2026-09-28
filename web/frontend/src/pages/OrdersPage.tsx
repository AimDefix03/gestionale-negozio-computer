import { CancellationPayload, FinancialReconciliation, Order, OrderOperationalDetail, OrderQuery, PageResponse, ReceiptPayload, ReturnRefundPayload, ReturnRequestPayload, UserAccount } from '../api';
import PaginationControls from '../components/PaginationControls';
import DataList from '../components/common/DataList';
import OrderOperationsPanel from '../components/orders/OrderOperationsPanel';
import FinancialReconciliationPanel from '../components/orders/FinancialReconciliationPanel';
import { CommandOutcome } from '../hooks/useCommandExecution';
import { dateTime, money, orderStatusClass } from '../utils/formatters';

type Props = {
  page: PageResponse<Order>;
  query: OrderQuery;
  currentUser: UserAccount;
  financialReconciliation: FinancialReconciliation | null;
  selectedDetail: OrderOperationalDetail | null;
  detailLoading: boolean;
  detailError: unknown;
  busy: boolean;
  pageSize: number;
  onQueryChange: (query: OrderQuery) => void;
  onSelect: (code: string) => void;
  onConfirm: (code: string) => void;
  onFulfill: (code: string) => void;
  onCancel: (code: string, payload: CancellationPayload) => void;
  onInvoice: (code: string) => void;
  onReceipt: (code: string, payload: ReceiptPayload) => void;
  onRequestReturn: (code: string, payload: ReturnRequestPayload) => Promise<CommandOutcome>;
  onApproveReturn: (orderCode: string, returnCode: string, note: string) => Promise<CommandOutcome>;
  onRejectReturn: (orderCode: string, returnCode: string, note: string) => Promise<CommandOutcome>;
  onReceiveReturn: (orderCode: string, returnCode: string) => Promise<CommandOutcome>;
  onRefundReturn: (orderCode: string, returnCode: string, payload: ReturnRefundPayload) => Promise<CommandOutcome>;
};

export default function OrdersPage(props: Props) {
  const selectedOrder = props.selectedDetail?.order ?? null;

  return (
    <>
      {props.financialReconciliation && <FinancialReconciliationPanel reconciliation={props.financialReconciliation} />}
      <DataList
      title="Ordini"
      columns={['Codice', 'Codice cliente', 'Cliente', 'Stato', 'Totale', 'Pagamento', 'Data']}
      rows={props.page.content.map((order) => [order.code, order.customerCode || '-', order.customer, <span className={`status-badge ${orderStatusClass(order.status)}`}>{order.statusLabel}</span>, money.format(order.total), <span>{order.payment.methodLabel}<small className="cell-note">{order.payment.statusLabel}</small></span>, dateTime.format(new Date(order.timestamp))])}
      actions={(orderCode) => {
        const order = props.page.content.find((item) => item.code === orderCode);
        if (!order) return null;
        return (
          <div className="row-actions">
            {order.capabilities.canConfirm && <button className="link-button" disabled={props.busy} onClick={() => props.onConfirm(order.code)}>Conferma</button>}
            {order.capabilities.canFulfill && <button className="link-button" disabled={props.busy} onClick={() => props.onFulfill(order.code)}>Evadi</button>}
            <button className="link-button" onClick={() => props.onSelect(order.code)}>Operazioni</button>
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
        capabilities={selectedOrder?.capabilities ?? null}
        documents={props.selectedDetail?.documents ?? null}
        loading={props.detailLoading}
        error={props.detailError}
        busy={props.busy}
        onInvoice={props.onInvoice}
        onCancel={props.onCancel}
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
