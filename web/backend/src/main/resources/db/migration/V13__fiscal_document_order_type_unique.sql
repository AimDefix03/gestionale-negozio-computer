alter table fiscal_documents
    add constraint uk_fiscal_documents_order_type unique (related_order_code, type);
