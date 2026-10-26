# JSONs dos CRUDs

Um arquivo por operação de escrita. Cada arquivo é o corpo exato da request de mesmo nome na coleção
[`postman/marketbuddies.postman_collection.json`](../postman/marketbuddies.postman_collection.json).

- Base URL: `https://marketbuddies-rm563925.azurewebsites.net` (local: `http://localhost:8080`).
- Token: `POST /api/auth/login` devolve `accessToken`; as outras rotas pedem `Authorization: Bearer <token>`.
- Ids: `addressId` e `productId` valem `1` nos arquivos. Troque pelos ids criados (na coleção viram `{{addressId}}` e
  `{{productId}}`).
- Senha do ADMIN: `login-admin.json` traz `{{adminPassword}}`. Local é `admin123`; na nuvem é a variável secreta
  `ADMIN_PASSWORD` da pipeline.
- Cliente de teste: a coleção troca o e-mail de `signup.json`, `login-cliente-teste.json`, `put-me.json` e
  `put-admin.json` por um e-mail com timestamp, para rodar várias vezes. O `PUT /me` usa esse cliente, e não a conta
  do seed, para não trocar a senha `cliente123`.
- Valores monetários vão como número JSON (`24.90`). CEP aceita com ou sem hífen e é gravado só com dígitos.
- Erros voltam com `status`, `message`, `path` e `errors` por campo.

## Autenticação — `TB_USERS`

| Arquivo | Método e rota | Token | Resposta esperada |
|---|---|---|---|
| `auth/login-cliente.json` | `POST /api/auth/login` | — | 200 `accessToken` |
| `auth/login-admin.json` | `POST /api/auth/login` | — | 200 `accessToken` |
| `auth/login-senha-errada.json` | `POST /api/auth/login` | — | 401 "Usuário ou senha inválidos" |
| `auth/signup.json` | `POST /api/auth/signup` | — | 201 `UserDto` com `role` `CUSTOMER` |
| `auth/signup-email-duplicado.json` | `POST /api/auth/signup` | — | 409 `errors.email` |
| `auth/login-cliente-teste.json` | `POST /api/auth/login` | — | 200 `accessToken` do cliente de teste |
| — | `GET /api/auth/me` | qualquer | 200 `UserDto` do token |

## Produtos — `TB_PRODUCTS`

| Arquivo | Método e rota | Token | Resposta esperada |
|---|---|---|---|
| `products/post.json` | `POST /api/products` | ADMIN | 201 `ProductDto`, `price` 15.00 · 403 com token de cliente |
| — | `GET /api/products?search=&category=` | — | 200 lista do catálogo |
| — | `GET /api/products/{id}` | — | 200 · 404 |
| `products/put.json` | `PUT /api/products/{id}` | ADMIN | 200 `ProductDto`, `finalPrice` 13.50 com 10% de oferta |
| — | `DELETE /api/products/{id}` | ADMIN | 204; os itens de pedido mantêm o nome |

`category`: `PRODUCE`, `CLEANING`, `BEVERAGES`, `BAKERY` ou `GROCERY`. `id` e `version` do `ProductFormDto` são
opcionais: o id vem da rota, e `version` só é conferido quando enviado (409 se o produto mudou).

## Endereços — `TB_ADDRESSES`

| Arquivo | Método e rota | Token | Resposta esperada |
|---|---|---|---|
| — | `GET /api/addresses/cep/{cep}` | CUSTOMER | 200 endereço do ViaCEP · 404 · 502 ViaCEP fora |
| `addresses/post.json` | `POST /api/addresses` | CUSTOMER | 201 `AddressDto`, `cep` `01310100` · 403 com token de admin |
| — | `GET /api/addresses` | CUSTOMER | 200 endereços do cliente logado |
| — | `GET /api/addresses/{id}` | CUSTOMER | 200 · 404 se é de outro cliente |
| `addresses/put.json` | `PUT /api/addresses/{id}` | CUSTOMER | 200 `AddressDto` |
| — | `DELETE /api/addresses/{id}` | CUSTOMER | 204; os pedidos mantêm a cópia do endereço |

## Zonas de entrega — `TB_DELIVERY_ZONES`

| Arquivo | Método e rota | Token | Resposta esperada |
|---|---|---|---|
| `delivery-zones/post.json` | `POST /api/delivery-zones` | ADMIN | 201 `DeliveryZoneDto` · 409 `errors.name` se repetir o nome |
| `delivery-zones/post-sobreposta.json` | `POST /api/delivery-zones` | ADMIN | 400 `errors.cepStart` "A faixa sobrepõe a zona …" |
| — | `GET /api/delivery-zones` | ADMIN | 200 lista ordenada por `cepStart` |
| — | `GET /api/delivery-zones/{id}` | ADMIN | 200 · 404 |
| `delivery-zones/put.json` | `PUT /api/delivery-zones/{id}` | ADMIN | 200 `fee` 19.90, `deliveryDays` 2 |
| — | `DELETE /api/delivery-zones/{id}` | ADMIN | 204; os pedidos guardam o frete já cobrado |

`post-sobreposta.json` só dá 400 com a zona de `post.json` cadastrada.

## Pedidos — `TB_ORDERS` e `TB_ORDER_ITEMS`

| Arquivo | Método e rota | Token | Resposta esperada |
|---|---|---|---|
| `orders/post.json` | `POST /api/orders` | CUSTOMER | 202 `AWAITING_PAYMENT`; ~3 s depois `APPROVED` · 403 com token de admin |
| `orders/post-acima-do-limite.json` | `POST /api/orders` | CUSTOMER | 202; ~3 s depois `REFUSED` "Valor acima do limite de R$ 500,00" |
| — | `GET /api/orders` | qualquer | 200 pedidos do cliente; o ADMIN vê todos |
| — | `GET /api/orders/{id}` | qualquer | 200 com `items` e `delivery` · 404 se é de outro cliente |
| — | `POST /api/orders/{id}/cancel` | CUSTOMER (dono) | 200 `CANCELED` · 422 se recusado ou já saiu para entrega |
| — | `DELETE /api/orders/{id}` | ADMIN | 204 se recusado ou cancelado · 422 nos outros status |

Com o produto de `put.json` (R$ 13,50): `post.json` dá 2 × 13,50 + frete 7,90 = R$ 34,90; `post-acima-do-limite.json`
dá 40 × 13,50 = R$ 540,00, com frete grátis (a partir de R$ 150). CEP fora de todas as zonas → 422.

## E-mails — `TB_EMAILS`

| Arquivo | Método e rota | Token | Resposta esperada |
|---|---|---|---|
| — | `GET /api/emails` | ADMIN | 200 tentativas de e-mail de confirmação (`SENT` ou `ERROR`) |

## Usuários — `TB_USERS`

| Arquivo | Método e rota | Token | Resposta esperada |
|---|---|---|---|
| — | `GET /api/users` | ADMIN | 200 lista · 403 com token de cliente |
| — | `GET /api/users/{id}` | ADMIN | 200 · 404 |
| `users/put-me.json` | `PUT /api/users/me` | qualquer | 200 `{ user, accessToken, tokenType, expiresIn }` |
| `users/put-me-senha-errada.json` | `PUT /api/users/me` | qualquer | 400 `errors.currentPassword` "Senha atual incorreta." |
| `users/put-admin.json` | `PUT /api/users/{id}` | ADMIN | 200 `UserDto`; só nome e e-mail mudam |
| — | `DELETE /api/users/{id}` | ADMIN | 204 · 409 cliente com pedidos ou a própria conta |

No `PUT /me`, `newPassword` e `confirmPassword` vazios mantêm a senha atual. O cadastro de usuário é o
`auth/signup.json`.
