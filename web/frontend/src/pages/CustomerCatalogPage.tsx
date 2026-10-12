import { CustomerProduct, PageResponse, ProductQuery } from '../api';
import CustomerProductTable from '../components/CustomerProductTable';

type Props = {
  page: PageResponse<CustomerProduct>;
  query: ProductQuery;
  busy: boolean;
  onQueryChange: (query: ProductQuery) => void;
  onPageChange: (page: number) => void;
};

export default function CustomerCatalogPage(props: Props) {
  return <CustomerProductTable page={props.page} query={props.query} busy={props.busy} onQueryChange={props.onQueryChange} onPageChange={props.onPageChange} />;
}
