# Brief cập nhật SRS và SDD cho ChatGPT Plus

## Mục đích

Dùng toàn bộ file này làm brief khi yêu cầu ChatGPT Plus viết lại hoặc chỉnh sửa tài liệu SRS và SDD cho dự án:

> Apartment Management System (ABMS) - Hệ thống quản lý chung cư

Mục tiêu là làm cho SRS/SDD nhất quán với nghiệp vụ thực tế, entity, DTO, API và database. Không được sửa tài liệu chỉ để hợp thức hóa các lỗi của code hiện tại.

## Cách sử dụng

Khi gửi cho ChatGPT Plus:

1. Đính kèm SRS hiện tại.
2. Đính kèm SDD hiện tại nếu có.
3. Đính kèm file này.
4. Yêu cầu ChatGPT Plus trả về:
   - bản SRS đã chỉnh sửa;
   - bản SDD đã chỉnh sửa;
   - bảng traceability từ SRS -> SDD -> API/entity/database;
   - danh sách assumption và điểm cần xác nhận.

Nếu chưa có SDD, phải tạo SDD mới dựa trên SRS sau khi đã chuẩn hóa.

## Nguyên tắc bắt buộc khi viết lại tài liệu

- SRS mô tả **hệ thống phải làm gì** và acceptance criteria.
- SDD mô tả **hệ thống sẽ làm như thế nào**.
- Không trộn implementation detail vào SRS nếu không cần thiết.
- Không tự thêm nghiệp vụ ngoài phạm vi nếu chưa ghi rõ là assumption.
- Nếu yêu cầu hiện tại mâu thuẫn, phải nêu mâu thuẫn và chọn một phương án nhất quán.
- Không dùng các cụm mơ hồ như “nếu cần”, “có thể”, “tùy role” trong business rule quan trọng.
- Tất cả status, role, field name, API parameter và table name phải thống nhất giữa SRS, SDD và ERD.
- Không đánh dấu feature là “complete” nếu chưa có acceptance criteria hoặc chưa có thiết kế dữ liệu tương ứng.

## Bối cảnh hệ thống cần giữ

### Actors chuẩn hóa

Không dùng lẫn “Admin”, “System Administrator” và “Building Manager”. SRS phải có actor matrix rõ ràng:

| Actor nghiệp vụ | Role/API key | Phạm vi |
|---|---|---|
| System Administrator | `ADMIN` | Toàn hệ thống hoặc các building được cấp quyền |
| Building Manager | `MANAGER` | Building được phân công |
| Staff/Lễ tân | `STAFF` | Nghiệp vụ được cấp quyền |
| Technician | `TECHNICIAN` | Maintenance được phân công |
| Resident/Cư dân | `RESIDENT` | Căn hộ, hợp đồng, invoice và notification của bản thân |
| System Scheduler | System actor | Job tự động, không phải người dùng đăng nhập |

Nếu muốn dùng tên `BUILDING_MANAGER` thay cho `MANAGER`, phải ghi rõ mapping và sửa thống nhất ở auth, gateway, service, frontend và tài liệu.

### Phạm vi module

- Authentication và authorization.
- Apartment, resident và contract.
- Vehicle registration/approval.
- Billing: services, meter readings, invoices, payments.
- Maintenance request.
- Feedback.
- Notification và monthly invoice notification.
- Dashboard và financial report.

## Các nội dung bắt buộc phải sửa trong SRS

### 1. Actor, permission và building scope

SRS phải quy định:

- Actor nào được tạo/approve/reject/cancel announcement.
- Actor nào được xem notification history.
- Actor nào được generate/export report.
- Admin có toàn quyền hay cũng bị giới hạn theo building.
- Manager không được đọc dữ liệu building khác.
- Resident chỉ xem dữ liệu của mình/căn hộ của mình.
- Direct service port có được gọi ngoài gateway không.

Mọi use case có dữ liệu nghiệp vụ phải ghi rõ `scope = building/user/system`.

### 2. Notification approval workflow

UC-NOTIF-01 hiện ghi “approval if required by role”, nội dung này phải được quyết định rõ.

SRS nên dùng state machine:

```text
PENDING_APPROVAL
    -> APPROVED
    -> SCHEDULED
    -> PROCESSING
    -> SENT
    -> PARTIAL_FAILED
    -> FAILED

PENDING_APPROVAL / APPROVED / SCHEDULED -> REJECTED hoặc CANCELLED
```

Phải quy định:

- Notification chưa approve không được gửi và không được resident nhìn thấy.
- Reject phải lưu reason.
- Có cho edit/resubmit sau reject hay không.
- Người tạo có được tự approve hay không.
- `scheduledAt` dùng timezone nào.
- Cách xử lý scheduled time đã nằm trong quá khứ.
- Notification có expiry hay không.

Không nên chỉ dùng `PENDING`, `SENT`, `FAILED`, `CANCELLED` nếu `PENDING` vừa mang nghĩa pending approval vừa mang nghĩa waiting to dispatch.

### 3. Recipient và channel

SRS phải định nghĩa rõ target type:

```text
ALL
ROLE
USERS
```

Phải ghi rõ:

- Recipient phải active không.
- Recipient có phải thuộc cùng building không.
- `recipientIds` có giới hạn số lượng không.
- IN_APP và EMAIL có trạng thái độc lập không.
- Email thất bại nhưng IN_APP thành công được tính là `PARTIAL_FAILED`.
- Retry tối đa bao nhiêu lần.
- Retry delay/backoff bao nhiêu.
- Khi nào chuyển dead-letter hoặc cần admin xử lý.

### 4. Automatic Monthly Invoice Notification

UC-NOTIF-03 phải sửa từ “gửi cho all residents with active contracts” thành rule có thể kiểm chứng:

> Hệ thống chỉ gửi notification cho các resident có invoice đã được generate trong billing period, invoice không bị cancel và resident vẫn hợp lệ tại thời điểm gửi.

SRS phải chốt:

- Job chạy cho kỳ hiện tại hay kỳ trước.
- Invoice phải ở status nào mới được gửi.
- Recipient được lấy từ invoice/apartment/contract như thế nào.
- Template phải có `invoiceId`, amount, due date và payment link.
- Mỗi invoice/user/channel có một delivery record.
- Job chạy lại không gửi trùng.
- Job đang chạy dở khi service restart được recover như thế nào.
- Job log có resident, invoice, channel, status và attempt hay chỉ có tổng counter.

Nếu SRS yêu cầu retry sau 30 phút tối đa 3 lần, SDD và configuration phải thể hiện đúng giá trị đó; không dùng một delay khác mà không giải thích.

### 5. Notification history

UC-NOTIF-02 phải thống nhất:

- Date filter là `createdAt`, `scheduledAt` hay `sentAt`.
- Recipient filter là recipient name, email, user ID, group hay building.
- Resident có được filter status `PENDING` không.
- Có sort theo tất cả cột không.
- Audit log lưu thao tác view/mark-read thế nào.

### 6. ERD và database business tables

Không gọi ERD 11 bảng là “complete” nếu hệ thống đã có notification/report.

SRS phải bổ sung hoặc ghi rõ ownership cho:

```text
notifications
notification_recipients
notification_deliveries
invoice_notification_job_executions
notification_audit_logs
report_jobs/report_files (nếu export async)
```

Các field cần thống nhất:

- `billing_month` hoặc `billing_period`, chỉ chọn một.
- `services.name` hoặc `services.service_name`, chỉ chọn một.
- Invoice cần `due_date`, `issue_date`, `status` đầy đủ.
- Payment cần `status`, `paid_at`, `transaction_reference`.
- Apartment cần `building_id`, status và quan hệ resident/contract.
- Vehicle cần owner/user reference.
- Contract cần status và rule không cho overlap active contract.

Invoice status tối thiểu phải thống nhất với business:

```text
UNPAID / PENDING / PARTIALLY_PAID / PAID / OVERDUE / CANCELLED
```

### 7. Dashboard

UC-DASH-01 đang yêu cầu occupancy, revenue, pending invoices, maintenance và alerts.

SRS phải ghi rõ:

- Đây là một Dashboard Aggregation API hay mỗi module cung cấp một phần dữ liệu.
- Dữ liệu có thể stale bao lâu.
- Khi một subsystem lỗi thì dashboard hiển thị partial data thế nào.
- Occupancy tính theo active contract hay apartment status.
- Revenue là invoiced hay collected.
- Alert nào là urgent.
- Quyền xem theo building.

Nếu Dashboard chỉ là phase sau, phải ghi rõ `Out of scope` thay vì mô tả như feature đã hoàn thiện.

### 8. Export Dashboard Report

UC-DASH-02 hiện yêu cầu from/to date, async generation, lưu file và gửi email nếu quá 10 giây.

SRS phải chọn một trong hai:

#### MVP synchronous

- Chỉ export khi request hoàn tất trong thời gian ngắn.
- Trả trực tiếp PDF/XLSX.
- Không cam kết email hoặc background job.

#### Production asynchronous

- Có report job.
- Có trạng thái `QUEUED/PROCESSING/SUCCEEDED/FAILED`.
- Có file storage và download URL.
- Có email khi hoàn tất.
- Có retention policy.

Không được ghi async trong SRS nhưng code chỉ trả byte đồng bộ mà không có assumption.

### 9. Financial Report

UC-REPORT-01 phải định nghĩa accounting semantics:

- `totalInvoiced`: theo billing period hay issue date.
- `totalCollected`: theo payment date hay invoice period.
- `pending/overdue`: tính tại cuối kỳ (`asOfDate`) hay ngày chạy.
- Partial payment, refund, cancelled invoice, overpayment.
- Revenue breakdown là invoice allocation hay payment allocation.
- Currency, timezone và building scope.
- Không có dữ liệu: trả empty report hay lỗi nghiệp vụ.
- Historical report có reproducible không.

SRS cũng phải thống nhất với UI:

- Report type.
- Period.
- Format PDF/XLSX.
- Có chart thật hay chỉ bảng số liệu.
- Có lưu report và download link hay trả trực tiếp.

## Các nội dung bắt buộc phải sửa trong SDD

### 1. Architecture

SDD phải thể hiện:

- Gateway.
- Auth service.
- Apartment/resident/contract.
- Billing data owner.
- Notification service.
- Report service.
- Database ownership hoặc shared database contract.

Nếu notification/report query trực tiếp bảng do billing service sở hữu, phải ghi rõ dependency và schema contract.

### 2. Notification design

SDD phải có:

- State transition diagram.
- ERD notification và delivery.
- Sequence create/approve/schedule/dispatch.
- Sequence invoice job và recipient resolution.
- Retry/backoff/idempotency.
- Lock hoặc optimistic locking khi nhiều instance chạy job.
- `building_id`, `@Version`, UTC timestamps và indexes.
- Email provider, timeout, retry và provider message ID.
- Audit event format.

### 3. Report design

SDD phải có:

- Query contract cho invoices/payments/details/services.
- `asOfDate` và accounting basis.
- Query filter theo building và period range.
- Snapshot/isolation strategy để tổng report nhất quán.
- Authorization tại gateway và service.
- Sync/async export decision.
- File retention và download security.
- Audit generate/export.

### 4. API contract

SDD phải ghi rõ request/response/error cho:

- Create announcement.
- Approve/reject/cancel.
- List/detail/mark-read.
- Retry notification.
- Run/retry/history invoice job.
- Financial preview.
- Financial export.

DTO phải có validation và cross-field rule tương ứng. Không để SRS nói có field nhưng API/DTO không có field đó.

## Traceability bắt buộc

ChatGPT Plus phải tạo bảng dạng sau:

| Requirement ID | SRS behavior | SDD component | API | Entity/table | Test |
|---|---|---|---|---|---|
| BR-NOTIF-01 | Chưa approve không được gửi | Approval/dispatch state machine | approve/dispatch API | notifications | notification approval test |
| BR-NOTIF-02 | Invoice template có amount/due/link | Invoice notification handler | invoice job API | invoice + delivery | invoice recipient test |
| BR-REPORT-01 | Collected theo payment date | Financial query design | financial preview | payments | historical report test |

Mỗi requirement High Priority phải có ít nhất một test hoặc acceptance scenario.

## Bộ acceptance test tối thiểu

1. Scheduled notification chưa approve không được gửi.
2. Resident không thấy `PENDING_APPROVAL`, `REJECTED`, `CANCELLED`.
3. Manager building A không xem dữ liệu building B.
4. Recipient target `ALL/ROLE/USERS` validate đúng.
5. Email fail và IN_APP success tạo partial delivery.
6. Invoice notification chỉ gửi đến user có invoice.
7. Retry không gửi trùng và vẫn giữ lịch sử execution.
8. Job restart không để trạng thái `RUNNING` vĩnh viễn.
9. Historical report không thay đổi chỉ vì `CURRENT_DATE` thay đổi.
10. Cash basis và invoice basis được phân biệt rõ.
11. Preview/export validate month, year, period và format.
12. PDF/XLSX mở được và số liệu khớp preview.

## Format output yêu cầu ChatGPT Plus

Khi viết lại tài liệu, ChatGPT Plus phải trả về theo thứ tự:

1. `Assumptions and unresolved decisions`.
2. `Updated SRS`.
3. `Updated SDD`.
4. `Updated ERD/data dictionary`.
5. `Updated state/sequence diagrams` bằng Mermaid hoặc PlantUML.
6. `API contract and validation matrix`.
7. `SRS-SDD-code-test traceability matrix`.
8. `Migration and implementation checklist`.
9. `Known out-of-scope items`.

ChatGPT Plus không được:

- Tự đổi actor hoặc accounting rule mà không ghi assumption.
- Giữ lại requirement async nếu SDD không có job/storage/email design.
- Gọi ERD là complete khi còn thiếu bảng notification/report.
- Ghi `SENT` cho channel chưa có delivery evidence.
- Dùng `CURRENT_DATE` cho historical report nếu chưa được SRS cho phép.
- Đánh dấu implementation hoàn thiện chỉ vì Maven compile thành công.

## Definition of done cho tài liệu

- Actor và role không mâu thuẫn.
- Tất cả UC có precondition, postcondition, main flow, alternative flow và business rule.
- Notification state machine rõ và không bypass approval.
- Invoice recipient rule xác định được bằng query/relationship.
- Report accounting semantics có thể kiểm tra bằng dữ liệu test.
- ERD, DTO, API và SDD dùng cùng field/status/table name.
- Có building scope và security design.
- Có retry, idempotency và failure behavior.
- Có traceability từ requirement đến test.
- Tất cả assumption chưa chốt được liệt kê riêng.

## Implementation status after backend review

The following P0 changes are now implemented in the source code:

- Notification workflow uses explicit states (`PENDING_APPROVAL`, `APPROVED`, `SCHEDULED`, `PROCESSING`, `SENT`, `PARTIAL_FAILED`, `FAILED`, `REJECTED`, `CANCELLED`). Approval is required before dispatch; reject and cancel endpoints are available.
- Resident notification list/detail/read operations expose only sent notifications and require the building scope forwarded by the gateway (`X-Building-Id`). Manager operations can be building-scoped; an administrator without a building header is treated as global scope.
- Gateway forwards `buildingId` from the JWT. Notification records persist `building_id` and use optimistic versioning (`version`).
- Notification DTO validation covers title/content/group/channel limits and future scheduling. Report preview/export validates role, month/year and PDF/Excel format.
- Financial report queries accept an optional building scope. The canonical invoice field used by report/notification SQL is `billing_period`.
- `report-service` and `notification-service` are now configured for Microsoft SQL Server (`mssql-jdbc`, SQL Server JDBC URLs and SQL Server-compatible SQL/mappings). Auth, apartment and vehicle services remain PostgreSQL-backed unless their own configuration is changed.
- Scheduling is enabled in notification-service and failed notification retry is restricted to the selected building when a building header is present.
- Invoice notification execution is idempotent by period through a notification `source_key`; a forced retry reuses the existing failed/partial notification instead of creating another one.
- Invoice recipient resolution is fail-closed: only users joined to a non-cancelled invoice for the requested period are eligible. An empty/incompatible result is skipped or failed; it no longer falls back to broadcasting to every active resident.

This does not mean the modules are production-complete. The following items still block a “Done” claim and must be resolved before updating SRS/SDD as implemented:

- The email gateway is still a logging stub; there is no provider delivery evidence, per-channel delivery table, provider message ID or real retry/backoff.
- The invoice job still creates one grouped notification and its template does not yet carry `invoiceId`, amount, due date and payment link per resident/channel. The billing schema must be shared as an explicit contract instead of fallback SQL variants.
- Invoice job execution/history is still system-wide rather than one execution per building; manager-level monitoring therefore needs a finalized building ownership rule before production use.
- Financial report pending/overdue values now use the selected period end as `asOfDate` and expose that date in the preview/export data. Collected revenue and fee breakdown are now based on successful payment `paid_at` in the selected month, with proportional allocation for partial payments; refund/overpayment rules still need explicit tests.
- Dashboard aggregation, asynchronous report jobs, file storage/retention and email-on-completion are not implemented by these services.
- Notification integration tests require PostgreSQL at the configured `DB_URL` (default `localhost:5433`). Maven package succeeds; the application-context test remains blocked when that database is unavailable.
