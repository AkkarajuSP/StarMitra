/** M01 owns identity/system-role truth. Role administration is performed via
 *  M01-owned APIs when exposed; M19 never duplicates them. */
export default function UsersPage() {
  return (
    <>
      <h1 className="page-title">Users &amp; Roles</h1>
      <div className="panel">
        <h2>System roles</h2>
        <p>Roles: <strong>ADMIN · SUPER_ADMIN · JUDGE · CREATOR · AUDIENCE</strong></p>
        <p className="derived">
          System roles are permissions; talent skills are not. Role administration uses
          M01-owned APIs — no dedicated admin endpoint is currently exposed by M01, so
          no role mutations are surfaced here.
        </p>
      </div>
    </>
  );
}
