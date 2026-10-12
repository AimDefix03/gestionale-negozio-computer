alter table products
    add constraint chk_products_code_not_blank check (char_length(trim(code)) > 0);

alter table products
    add constraint chk_products_name_not_blank check (char_length(trim(name)) > 0);

alter table products
    add constraint chk_products_description_not_blank check (char_length(trim(description)) > 0);

alter table products
    add constraint chk_products_brand_not_blank check (char_length(trim(brand)) > 0);

alter table products
    add constraint chk_products_product_type_not_blank check (char_length(trim(product_type)) > 0);

alter table products
    add constraint chk_products_quantity_not_negative check (quantity >= 0);

alter table products
    add constraint chk_products_price_not_negative check (price >= 0);

alter table products
    add constraint chk_products_discount_range check (discount >= 0 and discount <= 100);

alter table user_accounts
    add constraint chk_user_accounts_username_not_blank check (char_length(trim(username)) > 0);

alter table user_accounts
    add constraint chk_user_accounts_password_hash_not_blank check (char_length(trim(password_hash)) > 0);

alter table customer_orders
    add constraint chk_customer_orders_code_not_blank check (char_length(trim(code)) > 0);

alter table customer_orders
    add constraint chk_customer_orders_customer_not_blank check (char_length(trim(customer)) > 0);

alter table customer_orders
    add constraint chk_customer_orders_payment_method_not_blank check (char_length(trim(payment_method)) > 0);

alter table customer_orders
    add constraint chk_customer_orders_total_not_negative check (total >= 0);

alter table order_items
    add constraint chk_order_items_product_code_not_blank check (char_length(trim(product_code)) > 0);

alter table order_items
    add constraint chk_order_items_product_name_not_blank check (char_length(trim(product_name)) > 0);

alter table order_items
    add constraint chk_order_items_quantity_positive check (quantity > 0);

alter table order_items
    add constraint chk_order_items_unit_price_not_negative check (unit_price >= 0);

alter table order_items
    add constraint chk_order_items_line_total_not_negative check (line_total >= 0);

alter table stock_movements
    add constraint chk_stock_movements_actor_not_blank check (char_length(trim(actor)) > 0);

alter table stock_movements
    add constraint chk_stock_movements_role_not_blank check (char_length(trim(role)) > 0);

alter table stock_movements
    add constraint chk_stock_movements_product_code_not_blank check (char_length(trim(product_code)) > 0);

alter table stock_movements
    add constraint chk_stock_movements_product_name_not_blank check (char_length(trim(product_name)) > 0);

alter table stock_movements
    add constraint chk_stock_movements_quantity_positive check (quantity > 0);

alter table stock_movements
    add constraint chk_stock_movements_previous_quantity_not_negative check (previous_quantity >= 0);

alter table stock_movements
    add constraint chk_stock_movements_new_quantity_not_negative check (new_quantity >= 0);

alter table stock_movements
    add constraint chk_stock_movements_reason_not_blank check (char_length(trim(reason)) > 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_code_not_blank check (char_length(trim(code)) > 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_related_order_not_blank check (char_length(trim(related_order_code)) > 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_customer_not_blank check (char_length(trim(customer)) > 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_payment_method_not_blank check (char_length(trim(payment_method)) > 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_taxable_not_negative check (taxable_amount >= 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_vat_rate_range check (vat_rate >= 0 and vat_rate <= 1);

alter table fiscal_documents
    add constraint chk_fiscal_documents_vat_amount_not_negative check (vat_amount >= 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_total_not_negative check (total_amount >= 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_created_by_not_blank check (char_length(trim(created_by)) > 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_created_by_role_not_blank check (char_length(trim(created_by_role)) > 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_reason_not_blank check (char_length(trim(reason)) > 0);

alter table fiscal_documents
    add constraint chk_fiscal_documents_disclaimer_not_blank check (char_length(trim(disclaimer)) > 0);

alter table fiscal_document_lines
    add constraint chk_fiscal_document_lines_product_code_not_blank check (char_length(trim(product_code)) > 0);

alter table fiscal_document_lines
    add constraint chk_fiscal_document_lines_description_not_blank check (char_length(trim(description)) > 0);

alter table fiscal_document_lines
    add constraint chk_fiscal_document_lines_quantity_positive check (quantity > 0);

alter table fiscal_document_lines
    add constraint chk_fiscal_document_lines_unit_price_not_negative check (unit_price >= 0);

alter table fiscal_document_lines
    add constraint chk_fiscal_document_lines_line_total_not_negative check (line_total >= 0);

alter table business_partners
    add constraint chk_business_partners_code_not_blank check (char_length(trim(code)) > 0);

alter table business_partners
    add constraint chk_business_partners_display_name_not_blank check (char_length(trim(display_name)) > 0);
