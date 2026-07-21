# FE Prompt Spec for ABMS

## 1) Objective

Use this document as the **single source of truth** to generate the frontend for **ABMS (Apartment Building Management System)**.

Frontend scope in this phase is limited to:
- **Authentication**
- **Vehicle management**
- **Billing (invoices & payments)**

The goal is to produce frontend code that is:
- accurate with the current backend APIs
- clean and maintainable
- easy to run locally
- easy to extend later

---

## 2) Project context

- Backend is already implemented at a basic working level.
- The backend modules currently relevant for frontend are:
  - `auth-service`
  - `vehicle-service`
  - `automated-finance-service` (billing & payment)
  - `apartment-service` (apartment list / active residence lookup used by billing UI)
- The frontend should **consume existing APIs**, not redesign backend contracts.
- If some business rule is unclear, prefer a safe UI assumption and keep the code easy to adjust.

---

## 3) Mandatory implementation scope

### 3.1 Authentication
Implement:
- Register page
- Login page
- Persistent authentication state
- Logout
- Protected routes
- Automatic bearer token injection for authenticated API calls

### 3.2 Vehicle management
Implement:
- Vehicle registration form
- Vehicle list by apartment ID
- Approve vehicle action
- Reject vehicle action

### 3.3 Billing management
Implement:
- Invoice list with filters (apartment, billing month, status)
- Create invoice from electricity/water meter readings (staff/manager/admin)
- VietQR view for unpaid invoices
- Confirm VietQR payment (elevated roles)
- Record cash payment with collector tracking (elevated roles)
- VNPay sandbox payment for unpaid invoices (resident + elevated): create payment URL → redirect → return/IPN updates invoice
- Payment history list (all payments for elevated roles; apartment-filtered for residents)

Do **not** implement unrelated modules such as maintenance or notification in this phase.

---

## 4) Recommended frontend stack

Preferred stack:
- **ReactJS**
- **Vite** if creating a new project
- **React Router**
- **Axios**
- **React Hook Form**
- **Zod** or **Yup**
- **Context API** for auth state

Styling:
- Can use plain CSS, CSS Modules, SCSS, or Tailwind
- Prioritize simplicity and maintainability over heavy UI abstraction

Recommended language:
- **TypeScript preferred**
- If plain JavaScript is used, keep data shapes explicit and consistent

---

## 5) Important assumptions

These assumptions should be followed by the frontend unless backend says otherwise:

1. `token` returned by auth APIs is a JWT-like bearer token.
2. Authenticated requests should send:

```http
Authorization: Bearer <token>
```

3. `roleName` can be used for conditional UI rendering.
4. Vehicle approve/reject actions may be restricted by role in real usage, but current frontend can:
   - either always render buttons after login
   - or only render them for elevated roles such as `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF`
5. `apartmentId`, `ownerId`, and `vehicleId` are UUID strings.
6. Frontend should keep route structure simple and explicit.
7. If backend error shape is unclear, frontend should handle errors defensively and display a generic fallback message.

---

## 6) Backend API contract

### 6.1 Base URL

Use environment variable for backend base URL.

For Vite:

```env
VITE_API_BASE_URL=http://localhost:8080
```

For Create React App:

```env
REACT_APP_API_BASE_URL=http://localhost:8080
```

The frontend must not hardcode the full backend domain directly in components.

---

### 6.2 Auth APIs

Base path:

```text
/api/v1/auth
```

#### A. Register

```http
POST /api/v1/auth/register
Content-Type: application/json
```

Request body:

```json
{
  "email": "user@example.com",
  "password": "123456",
  "fullName": "Nguyen Van A",
  "phone": "0123456789",
  "idCard": "079123456789"
}
```

Validation from backend:
- `email`: required, valid email
- `password`: required
- `fullName`: required
- `phone`: optional
- `idCard`: optional

Expected response:

```json
{
  "token": "jwt-token-string",
  "roleName": "ROLE_USER",
  "email": "user@example.com"
}
```

Frontend behavior:
- validate before submit
- on success: persist auth data and redirect to authenticated area
- on failure: show API error message if available, otherwise fallback message

#### B. Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

Validation from backend:
- `email`: required, valid email
- `password`: required

Expected response:

```json
{
  "token": "jwt-token-string",
  "roleName": "ROLE_USER",
  "email": "user@example.com"
}
```

Frontend behavior:
- validate before submit
- on success: persist auth data and redirect to authenticated area
- on failure: show invalid credential / generic error message

---

### 6.3 Vehicle APIs

Base path:

```text
/api/v1/vehicles
```

#### A. Register vehicle

```http
POST /api/v1/vehicles/
Content-Type: application/json
Authorization: Bearer <token>
```

Request body:

```json
{
  "apartmentId": "11111111-1111-1111-1111-111111111111",
  "ownerId": "22222222-2222-2222-2222-222222222222",
  "licensePlate": "59A-12345",
  "type": "MOTORBIKE",
  "brand": "Honda"
}
```

Validation from backend:
- `apartmentId`: required, UUID
- `ownerId`: required, UUID
- `licensePlate`: required
- `type`: required
- `brand`: required

Expected success response:

```json
{
  "vehicleId": "33333333-3333-3333-3333-333333333333",
  "apartmentId": "11111111-1111-1111-1111-111111111111",
  "ownerId": "22222222-2222-2222-2222-222222222222",
  "licensePlate": "59A-12345",
  "type": "MOTORBIKE",
  "brand": "Honda",
  "status": "PENDING"
}
```

Frontend behavior:
- validate form before submit
- disable submit while loading
- show success message after creation
- optionally reset form after success

#### B. Get vehicles by apartment

```http
GET /api/v1/vehicles/apartment/{apartmentId}
Authorization: Bearer <token>
```

Expected response:

```json
[
  {
    "vehicleId": "33333333-3333-3333-3333-333333333333",
    "apartmentId": "11111111-1111-1111-1111-111111111111",
    "ownerId": "22222222-2222-2222-2222-222222222222",
    "licensePlate": "59A-12345",
    "type": "MOTORBIKE",
    "brand": "Honda",
    "status": "PENDING"
  }
]
```

Frontend behavior:
- allow user to input `apartmentId`
- fetch data on submit/click
- render loading state
- render empty state when array is empty
- render error state when request fails

#### C. Approve vehicle

```http
POST /api/v1/vehicles/{vehicleId}/approve
Authorization: Bearer <token>
```

Expected response:

```json
{
  "vehicleId": "33333333-3333-3333-3333-333333333333",
  "apartmentId": "11111111-1111-1111-1111-111111111111",
  "ownerId": "22222222-2222-2222-2222-222222222222",
  "licensePlate": "59A-12345",
  "type": "MOTORBIKE",
  "brand": "Honda",
  "status": "APPROVED"
}
```

Frontend behavior:
- update the changed item immediately after success
- prevent duplicate clicks while request is pending

#### D. Reject vehicle

```http
POST /api/v1/vehicles/{vehicleId}/reject
Authorization: Bearer <token>
```

Expected response:

```json
{
  "vehicleId": "33333333-3333-3333-3333-333333333333",
  "apartmentId": "11111111-1111-1111-1111-111111111111",
  "ownerId": "22222222-2222-2222-2222-222222222222",
  "licensePlate": "59A-12345",
  "type": "MOTORBIKE",
  "brand": "Honda",
  "status": "REJECTED"
}
```

Frontend behavior:
- update the changed item immediately after success
- prevent duplicate clicks while request is pending

---

### 6.4 Billing APIs

Base paths:

```text
/api/v1/services
/api/v1/invoices
/api/v1/payments
```

Supporting apartment APIs (for FE dropdowns / resident mode):

```text
GET /api/v1/apartments
GET /api/v1/apartments/residents/user/{userId}/active
```

#### A. List billing services

```http
GET /api/v1/services
Authorization: Bearer <token>
```

Expected response:

```json
[
  {
    "serviceId": 1,
    "name": "Electricity",
    "unitPrice": 3500.0,
    "unit": "kWh"
  }
]
```

#### B. Create invoice from meter readings

```http
POST /api/v1/invoices/meter-readings
Content-Type: application/json
Authorization: Bearer <token>
```

Request body:

```json
{
  "apartmentId": "a101b202-c303-4d04-8e05-f606a707b808",
  "billingMonth": "2026-07-01",
  "readings": [
    { "serviceId": 1, "oldIndex": 100, "newIndex": 150 },
    { "serviceId": 2, "oldIndex": 20, "newIndex": 25 }
  ]
}
```

Expected response includes `invoiceId`, `invoiceCode`, `totalAmount`, `paidAmount`, `remainingAmount`, `status`, `displayStatus`, and `details[]`.

Frontend behavior:
- elevated roles only for create
- validate `newIndex >= oldIndex`
- refresh invoice list after success

#### C. List / filter invoices

```http
GET /api/v1/invoices?apartmentId={uuid}&billingMonth=2026-07-01&status=UNPAID
GET /api/v1/invoices/apartment/{apartmentId}
GET /api/v1/invoices/{invoiceId}
Authorization: Bearer <token>
```

Statuses stored: `UNPAID` | `PARTIAL` | `PAID`.  
`displayStatus` may be `OVERDUE` when billing month is past and not paid.

#### D. VietQR (test)

```http
GET /api/v1/payments/vietqr/{invoiceId}
Authorization: Bearer <token>
```

Response includes `qrImageUrl`, `amount`, `transferContent` (= invoice code), bank account fields.

```http
POST /api/v1/payments/vietqr/confirm
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "invoiceId": "........",
  "payerId": "4d5e6f70-8192-4abc-d345-e6f7890a1b2c",
  "paidAmount": 265000
}
```

#### E. Cash payment

```http
POST /api/v1/payments/cash
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "invoiceId": "........",
  "payerId": "4d5e6f70-8192-4abc-d345-e6f7890a1b2c",
  "collectorId": "708192a3-b4c5-4def-a678-90a1b2c3d4e5",
  "paidAmount": 100000
}
```

#### F. VNPay sandbox

Register sandbox credentials at http://sandbox.vnpayment.vn/devreg/ and set env `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` on `automated-finance-service`. Do not commit real secrets.

```http
POST /api/v1/payments/vnpay/create
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "invoiceId": "........",
  "payerId": "4d5e6f70-8192-4abc-d345-e6f7890a1b2c"
}
```

Response:

```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "txnRef": "....",
  "amount": 265000
}
```

Frontend should redirect the browser to `paymentUrl`. Amount is the full remaining balance (no partial VNPay in this demo).

Public callbacks (no JWT; whitelisted on API gateway):

```http
GET /api/v1/payments/vnpay/return?...vnp_*
```

Verifies HMAC, applies payment idempotently by `vnp_TxnRef`, then **302** redirects to:

`http://localhost:5173/app/payments/vnpay-result?success=true&invoiceCode=...&txnRef=...&message=...`

```http
GET /api/v1/payments/vnpay/ipn?...vnp_*
```

Same verify + apply logic; responds `{ "RspCode": "00", "Message": "Confirm Success" }` (or `97`/`01`/`02`/`04` per VNPay rules). Local demo works via return URL without ngrok; IPN needs a public tunnel if you register it on the VNPay portal.

Payment method stored: `VNPAY`.

#### G. Payment history

```http
GET /api/v1/payments
GET /api/v1/payments?apartmentId={uuid}
GET /api/v1/payments/invoice/{invoiceId}
Authorization: Bearer <token>
```

---

## 7) Frontend data models

Use these shapes exactly.

### TypeScript version

```ts
export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  idCard?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  roleName: string;
  email: string;
}

export interface VehicleRequest {
  apartmentId: string;
  ownerId: string;
  licensePlate: string;
  type: string;
  brand: string;
}

export interface VehicleResponse {
  vehicleId: string;
  apartmentId: string;
  ownerId: string;
  licensePlate: string;
  type: string;
  brand: string;
  status: string;
}

export interface InvoiceDetailResponse {
  detailId: string;
  serviceId: number;
  serviceName: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  amount: number;
}

export interface InvoiceResponse {
  invoiceId: string;
  apartmentId: string;
  invoiceCode: string;
  billingMonth: string;
  totalAmount: number;
  paidAmount: number;
  remainingAmount: number;
  status: string;
  displayStatus: string;
  details: InvoiceDetailResponse[];
}

export interface PaymentResponse {
  paymentId: string;
  invoiceId: string;
  invoiceCode: string;
  payerId: string;
  collectorId?: string | null;
  paidAmount: number;
  paymentMethod: string;
  paymentTime: string;
}

export interface VietQrResponse {
  invoiceId: string;
  invoiceCode: string;
  amount: number;
  bankBin: string;
  accountNo: string;
  accountName: string;
  transferContent: string;
  qrImageUrl: string;
}
```

### Auth storage shape

```ts
export interface AuthStorage {
  token: string;
  roleName: string;
  email: string;
}
```

---

## 8) Required pages

Create these pages:

1. `LoginPage`
2. `RegisterPage`
3. `DashboardPage`
4. `VehicleListPage`
5. `VehicleCreatePage`
6. `InvoiceListPage`
7. `PaymentListPage`
8. `NotFoundPage`

Optional:
9. `UnauthorizedPage`

---

## 9) Required route behavior

Recommended routes:

```text
/login
/register
/
/vehicles
/vehicles/create
/app/invoices
/app/payments
/app/payments/vnpay-result
/unauthorized (optional)
*
```

Rules:
- `/login` and `/register` are public routes
- `/`, `/vehicles`, `/vehicles/create`, `/app/invoices`, `/app/payments`, `/app/payments/vnpay-result` are private routes
- unauthenticated access to private routes should redirect to `/login`
- unknown routes should go to `NotFoundPage`

Optional role rule:
- approve/reject buttons can be hidden unless role is one of:
  - `ROLE_ADMIN`
  - `ROLE_MANAGER`
  - `ROLE_STAFF`

If role policy is uncertain, make that condition centralized and easy to modify.

---

## 10) Required application behavior

### 10.1 Authentication flow

#### Register flow
- user enters form data
- frontend validates fields
- submit `POST /api/v1/auth/register`
- if success:
  - save auth data
  - navigate to dashboard or vehicle page
- if failure:
  - show error message

#### Login flow
- user enters email and password
- frontend validates fields
- submit `POST /api/v1/auth/login`
- if success:
  - save auth data
  - navigate to dashboard or vehicle page
- if failure:
  - show error message

#### Logout flow
- clear local auth state
- remove local storage data
- redirect to `/login`

#### App startup behavior
- on app load, restore auth state from local storage if present
- if token exists, keep user signed in locally

---

### 10.2 Vehicle flow

#### Vehicle list flow
- user enters apartment ID
- frontend requests vehicles by apartment ID
- frontend renders result list/table
- each item should show:
  - vehicleId
  - apartmentId
  - ownerId
  - licensePlate
  - type
  - brand
  - status

#### Vehicle create flow
- frontend renders form with:
  - apartmentId
  - ownerId
  - licensePlate
  - type
  - brand
- submit request
- show success/error feedback

#### Approve/Reject flow
- action buttons should be shown per item
- clicking action should call the corresponding API
- update row/card status without requiring full page reload if practical

---

### 10.3 Billing flow

#### Invoice list flow
- elevated roles: load all invoices (optional filters apartment / month / status)
- residents: resolve active residence via apartment API, then load invoices by apartment
- each row shows code, apartment, month, amounts, status/displayStatus, line details
- staff can create invoices from electricity/water meter readings on the same page
- unpaid rows expose VietQR and VNPay; elevated roles also get Confirm QR and Cash actions
- VNPay: call create → redirect to sandbox → return lands on `/app/payments/vnpay-result`

#### Payment list flow
- elevated roles: load all payments
- residents: load payments filtered by apartmentId
- show method (`CASH` / `VIETQR` / `VNPAY`), amount, invoice reference, payer, collector, time

---

## 11) Validation requirements

Frontend validation should mirror backend as closely as possible.

### Register
- `email`: required, email format
- `password`: required
- `fullName`: required
- `phone`: optional
- `idCard`: optional

### Login
- `email`: required, email format
- `password`: required

### Vehicle create
- `apartmentId`: required
- `ownerId`: required
- `licensePlate`: required
- `type`: required
- `brand`: required

### Meter invoice create
- `apartmentId`: required UUID
- `billingMonth`: required
- electricity/water old & new indexes: required, `new >= old`, `>= 0`

### Cash / VietQR confirm
- `invoiceId`, `payerId`: required UUID
- `collectorId`: required UUID for cash
- `paidAmount`: required, `> 0`

Optional enhancement:
- validate UUID format for `apartmentId` and `ownerId`

---

## 12) Error handling requirements

Frontend must handle gracefully:
- invalid login credentials
- form validation errors
- missing token / unauthorized access
- expired or invalid token
- empty vehicle result
- network/server failure

Fallback UX behavior:
- show inline error on forms when possible
- show generic message if backend error body is unknown
- avoid blank screen on API failure

Recommended fallback messages:
- `Something went wrong. Please try again.`
- `Unable to load vehicles.`
- `Invalid email or password.`

---

## 13) State management requirements

At minimum, auth state should manage:
- `token`
- `roleName`
- `email`
- `isAuthenticated`

Recommended auth actions:
- `login()`
- `register()`
- `logout()`
- `restoreSession()`

Vehicle page local state can manage:
- list data
- loading state
- submit/action loading
- current filter apartmentId

---

## 14) API layer requirements

The generated frontend should include:

1. a shared Axios client
2. request interceptor to attach bearer token
3. optional response interceptor for unauthorized handling
4. separated API modules:
   - `authApi`
   - `vehicleApi`
   - `invoiceService` / `paymentService`
   - `apartmentService` (for apartment dropdown + active residence)

Avoid calling axios directly inside page components if possible.

---

## 15) Suggested folder structure

If using TypeScript:

```text
src/
  api/
    axiosClient.ts
    authApi.ts
    vehicleApi.ts
  components/
    common/
      Button.tsx
      Input.tsx
      Loading.tsx
      ProtectedRoute.tsx
      EmptyState.tsx
      ErrorState.tsx
    layout/
      MainLayout.tsx
      Navbar.tsx
  context/
    AuthContext.tsx
  hooks/
    useAuth.ts
  pages/
    auth/
      LoginPage.tsx
      RegisterPage.tsx
    vehicle/
      VehicleListPage.tsx
      VehicleCreatePage.tsx
    DashboardPage.tsx
    NotFoundPage.tsx
    UnauthorizedPage.tsx
  routes/
    AppRoutes.tsx
  types/
    auth.ts
    vehicle.ts
  utils/
    storage.ts
    constants.ts
    auth.ts
  App.tsx
  main.tsx
```

If using JavaScript, keep the same structure with `.js/.jsx`.

---

## 16) UI requirements

The UI should be:
- simple
- clean
- readable
- desktop-friendly
- acceptable on smaller screens

Minimum UX requirements:
- loading indicators
- disabled submit/action button while pending
- empty state for no vehicles
- visible error feedback
- clear navigation between dashboard and vehicle pages

---

## 17) Coding constraints

The generated code must:
- follow React best practices
- keep business logic out of presentational components where reasonable
- avoid duplicated form and API logic
- use reusable components where it makes sense
- keep auth logic centralized
- keep route config explicit
- use environment variables for API base URL

Do not:
- include unrelated mock modules
- invent backend endpoints not listed here
- hardcode fake response data as final implementation

---

## 18) Acceptance criteria

Consider the frontend implementation correct only if all conditions below are met:

### Authentication
- user can register successfully via backend API
- user can login successfully via backend API
- token is persisted locally
- protected routes block unauthenticated access
- logout clears session and redirects properly

### Vehicle
- user can create/register a vehicle
- user can fetch vehicles by apartment ID
- vehicle list renders all expected fields
- approve action updates status correctly
- reject action updates status correctly

### Billing
- staff can create invoice from meter readings
- invoice list supports elevated vs resident modes
- VietQR can be generated for unpaid invoices
- cash and VietQR confirm update paid amount / status
- VNPay sandbox create + return updates invoice / creates `VNPAY` payment (idempotent)
- payment list shows transaction history

### Technical
- API base URL comes from env config
- forms are validated on frontend
- loading/error/empty states exist
- project structure is modular and maintainable

---

## 19) Output contract for the AI that generates FE code

When generating the frontend, the AI should output in this order:

1. short architecture summary
2. folder structure
3. dependency list
4. environment file example
5. source code files
6. run instructions
7. notes about assumptions

Code output requirements:
- produce complete files
- avoid partial snippets when possible
- ensure imports are consistent
- ensure routes and components connect correctly

---

## 20) Recommended prompt to use directly

Copy and use the prompt below when asking an AI to generate the frontend.

```text
Build a frontend for ABMS (Apartment Building Management System) using ReactJS.

Important: only implement the Authentication and Vehicle Management modules.

Tech requirements:
- Prefer Vite + React + TypeScript
- Use React Router
- Use Axios
- Use React Hook Form
- Use Zod or Yup for validation
- Use Context API for authentication state
- Use a modular, scalable folder structure

Backend contract must be followed exactly.

Base API URL must come from environment variable.
Example:
VITE_API_BASE_URL=http://localhost:8080

Auth APIs:
1) POST /api/v1/auth/register
Request body:
{
  "email": "user@example.com",
  "password": "123456",
  "fullName": "Nguyen Van A",
  "phone": "0123456789",
  "idCard": "079123456789"
}
Validation:
- email required and valid email
- password required
- fullName required
- phone optional
- idCard optional
Response:
{
  "token": "jwt-token-string",
  "roleName": "ROLE_USER",
  "email": "user@example.com"
}

2) POST /api/v1/auth/login
Request body:
{
  "email": "user@example.com",
  "password": "123456"
}
Validation:
- email required and valid email
- password required
Response:
{
  "token": "jwt-token-string",
  "roleName": "ROLE_USER",
  "email": "user@example.com"
}

Vehicle APIs:
3) POST /api/v1/vehicles/
Request body:
{
  "apartmentId": "11111111-1111-1111-1111-111111111111",
  "ownerId": "22222222-2222-2222-2222-222222222222",
  "licensePlate": "59A-12345",
  "type": "MOTORBIKE",
  "brand": "Honda"
}
Validation:
- apartmentId required
- ownerId required
- licensePlate required
- type required
- brand required
Response:
{
  "vehicleId": "33333333-3333-3333-3333-333333333333",
  "apartmentId": "11111111-1111-1111-1111-111111111111",
  "ownerId": "22222222-2222-2222-2222-222222222222",
  "licensePlate": "59A-12345",
  "type": "MOTORBIKE",
  "brand": "Honda",
  "status": "PENDING"
}

4) GET /api/v1/vehicles/apartment/{apartmentId}
Response item fields:
- vehicleId
- apartmentId
- ownerId
- licensePlate
- type
- brand
- status

5) POST /api/v1/vehicles/{vehicleId}/approve
Response: VehicleResponse

6) POST /api/v1/vehicles/{vehicleId}/reject
Response: VehicleResponse

Required pages:
- LoginPage
- RegisterPage
- DashboardPage
- VehicleListPage
- VehicleCreatePage
- NotFoundPage

Behavior requirements:
- persist token, roleName, email after login/register
- restore auth session on app startup
- protect private routes
- attach bearer token automatically in axios interceptor
- logout clears auth state and storage
- vehicle list page can search by apartmentId
- approve/reject should update UI immediately after success
- implement loading, empty, and error states

Suggested route structure:
- /login
- /register
- /
- /vehicles
- /vehicles/create

Suggested architecture:
- api layer
- auth context
- protected routes
- reusable common components
- page-based modules

Important constraints:
- do not invent extra backend endpoints
- do not include unrelated modules
- keep code modular and production-friendly

Output format:
1. architecture summary
2. folder structure
3. dependency list
4. .env example
5. complete source files
6. run instructions
```

---

## 21) Optional Vietnamese prompt version

```text
Hãy tạo frontend ReactJS cho hệ thống ABMS.

Phạm vi chỉ gồm:
- Authentication
- Vehicle Management

Ưu tiên dùng:
- Vite + React + TypeScript
- React Router
- Axios
- React Hook Form
- Zod hoặc Yup
- Context API cho auth state

Phải bám đúng API backend hiện có, không tự nghĩ ra endpoint mới.

Base URL backend lấy từ biến môi trường:
VITE_API_BASE_URL=http://localhost:8080

Auth API:
- POST /api/v1/auth/register
- POST /api/v1/auth/login

Vehicle API:
- POST /api/v1/vehicles/
- GET /api/v1/vehicles/apartment/{apartmentId}
- POST /api/v1/vehicles/{vehicleId}/approve
- POST /api/v1/vehicles/{vehicleId}/reject

Yêu cầu màn hình:
- LoginPage
- RegisterPage
- DashboardPage
- VehicleListPage
- VehicleCreatePage
- NotFoundPage

Yêu cầu hành vi:
- lưu token, roleName, email sau login/register
- restore session khi reload app
- protected routes
- tự động gắn bearer token vào request
- logout xóa session
- vehicle list tìm theo apartmentId
- approve/reject cập nhật UI ngay sau khi thành công
- có loading, empty, error states

Hãy output theo thứ tự:
1. tóm tắt kiến trúc
2. cây thư mục
3. dependencies
4. file .env mẫu
5. code đầy đủ theo file
6. hướng dẫn chạy
```