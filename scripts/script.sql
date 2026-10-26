-- MarketBuddies (CP6) · DDL das tabelas no Azure SQL Database (SQL Server)
-- Gerado pelo Hibernate a partir das entidades JPA (profile "schema"), o mesmo DDL que criou o banco em nuvem.
-- Tabelas: TB_USERS, TB_ADDRESSES (N:1 usuário), TB_PRODUCTS, TB_DELIVERY_ZONES (faixas de CEP),
--          TB_ORDERS (N:1 usuário, N:1 endereço opcional + cópia do endereço), TB_ORDER_ITEMS (N:1 pedido, N:1 produto), TB_EMAILS (N:1 pedido)


    create table tb_addresses (
        uf varchar(2) not null,
        cep varchar(8) not null,
        created_at datetime2(7) not null,
        id bigint identity not null,
        user_id bigint not null,
        number varchar(10) not null,
        label varchar(40),
        city varchar(60) not null,
        complement varchar(60),
        district varchar(60) not null,
        street varchar(120) not null,
        primary key (id)
    );

    create table tb_delivery_zones (
        delivery_days int not null,
        fee numeric(15,2) not null,
        cep_end varchar(8) not null,
        cep_start varchar(8) not null,
        id bigint identity not null,
        name varchar(60) not null,
        primary key (id)
    );

    create table tb_emails (
        id bigint identity not null,
        order_id bigint,
        send_date_email datetime2(7) not null,
        status_email varchar(20) not null check ((status_email in ('SENT','ERROR'))),
        email_from varchar(160) not null,
        email_to varchar(160) not null,
        subject varchar(200) not null,
        error_message varchar(500),
        text varchar(max),
        primary key (id)
    );

    create table tb_order_items (
        quantity int not null,
        unit_cost numeric(15,2) not null,
        unit_price numeric(15,2) not null,
        id bigint identity not null,
        order_id bigint not null,
        product_id bigint,
        category varchar(20) not null check ((category in ('PRODUCE','CLEANING','BEVERAGES','BAKERY','GROCERY'))),
        product_name varchar(80) not null,
        primary key (id)
    );

    create table tb_orders (
        delivery_days int,
        delivery_uf varchar(2),
        shipping_fee numeric(15,2),
        total_amount numeric(15,2) not null,
        address_id bigint,
        created_at datetime2(7) not null,
        delivery_cep varchar(8),
        delivery_updated_at datetime2(7),
        id bigint identity not null,
        responded_at datetime2(7),
        user_id bigint not null,
        delivery_number varchar(10),
        origin varchar(10) not null check ((origin in ('WEB','API'))),
        delivery_status varchar(20) check ((delivery_status in ('EM_SEPARACAO','SAIU_PARA_ENTREGA','ENTREGUE'))),
        status varchar(30) not null check ((status in ('AWAITING_PAYMENT','APPROVED','REFUSED','CANCELED'))),
        authorization_code varchar(40),
        delivery_city varchar(60),
        delivery_complement varchar(60),
        delivery_district varchar(60),
        delivery_street varchar(120),
        refusal_reason varchar(200),
        primary key (id)
    );

    create table tb_products (
        cost numeric(15,2) not null,
        discount_percentage numeric(5,2) not null,
        markup_percentage numeric(7,2) not null,
        price numeric(15,2) not null,
        stock int not null,
        id bigint identity not null,
        version bigint not null,
        category varchar(20) not null check ((category in ('PRODUCE','CLEANING','BEVERAGES','BAKERY','GROCERY'))),
        name varchar(80) not null,
        image_path varchar(120),
        description varchar(500),
        primary key (id)
    );

    create table tb_users (
        created_at datetime2(7) not null,
        id bigint identity not null,
        role varchar(20) not null check ((role in ('CUSTOMER','ADMIN'))),
        password varchar(100) not null,
        full_name varchar(120) not null,
        email varchar(160) not null,
        primary key (id)
    );

    alter table tb_delivery_zones 
       add constraint UKmel0wwtcm4ysc2gb1ih6rl40f unique (name);

    alter table tb_users 
       add constraint UKgrd22228p1miaivbn9yg178pm unique (email);

    alter table tb_addresses 
       add constraint FK6w37kk7q0gqtrp2pg965khe6l 
       foreign key (user_id) 
       references tb_users;

    alter table tb_emails 
       add constraint FK2oo8d7rqt6f0yononvgt57d96 
       foreign key (order_id) 
       references tb_orders;

    alter table tb_order_items 
       add constraint FKr9uuphipacrs0n0qi7vyxba4q 
       foreign key (order_id) 
       references tb_orders;

    alter table tb_order_items 
       add constraint FKsfeg5hsfxv6v5vlcham57bibu 
       foreign key (product_id) 
       references tb_products;

    alter table tb_orders 
       add constraint FKk531vffddjav0so73efnvv95q 
       foreign key (address_id) 
       references tb_addresses;

    alter table tb_orders 
       add constraint FKgprqgp552scklsm3rk6uyvh 
       foreign key (user_id) 
       references tb_users;
