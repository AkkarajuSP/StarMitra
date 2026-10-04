/** M17 owns notifications. Admin announcements are emitted via the M17
 *  NotificationContract when product wiring enables ADMIN_ANNOUNCEMENT — no
 *  M19-side row insertion ever. */
export default function NotificationsPage() {
  return (
    <>
      <h1 className="page-title">Announcements</h1>
      <div className="panel">
        <h2>Notification administration</h2>
        <p className="derived">
          M17 owns notification state/delivery. Broadcast announcements are raised
          through M17's contract; no M17 API for announcements is currently exposed,
          so no mutation surface is presented here.
        </p>
      </div>
    </>
  );
}
