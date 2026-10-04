import { myNotifications } from '../api';
import { useApi, State, Badge } from '../../admin/shared/ui';

/** M17 inbox — judge receives assignment/evaluation events via M17 contract. */
export default function JudgeNotificationsPage() {
  const { data, loading, error } = useApi(() => myNotifications(), []);
  return (
    <>
      <h1 className="page-title">Notifications</h1>
      <div className="panel">
        <State loading={loading} error={error} empty={!data?.items.length}>
          <div className="table-wrap">
            <table className="list">
              <thead><tr><th>Type</th><th>Message</th><th>Status</th><th>When</th></tr></thead>
              <tbody>
                {data?.items.map((n) => (
                  <tr key={n.id}>
                    <td>{n.type}</td>
                    <td>{n.body}</td>
                    <td><Badge v={n.state} /></td>
                    <td>{new Date(n.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </State>
      </div>
    </>
  );
}
