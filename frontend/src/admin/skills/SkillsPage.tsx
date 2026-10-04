import { useState } from 'react';
import { get, post } from '../../api/client';
import { useApi, State, Badge, Confirm } from '../shared/ui';

interface Skill { id: string; name: string; status: string; parentSkillId?: string }
interface SkillPage { items: Skill[] }

/** M03 — talent taxonomy admin. Skills are NOT permissions. */
export default function SkillsPage() {
  const { data, loading, error, reload } = useApi(() =>
    get<SkillPage>('/api/v1/skills?limit=100'), []);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [deactivating, setDeactivating] = useState<Skill | null>(null);
  return (
    <>
      <h1 className="page-title">Skill Taxonomy</h1>
      <p className="derived">Talent skills are domain taxonomy — never system permissions.</p>
      <div className="panel">
        <h2>Add skill</h2>
        <div className="filters">
          <div className="field">
            <label htmlFor="sk-name">Name</label>
            <input id="sk-name" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="sk-desc">Description</label>
            <input id="sk-desc" value={description}
                   onChange={(e) => setDescription(e.target.value)} />
          </div>
          <button className="btn primary" disabled={!name} onClick={async () => {
            await post('/api/v1/skills', { name, description: description || undefined });
            setName(''); setDescription(''); reload();
          }}>Create</button>
        </div>
      </div>
      <div className="panel">
        <h2>Skills</h2>
        <State loading={loading} error={error} empty={!data?.items.length}>
          <div className="table-wrap">
            <table className="list">
              <thead><tr><th>Name</th><th>Status</th><th /></tr></thead>
              <tbody>
                {data?.items.map((s) => (
                  <tr key={s.id}>
                    <td>{s.name}</td>
                    <td><Badge v={s.status} /></td>
                    <td><button className="btn" onClick={() => setDeactivating(s)}>Deactivate</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </State>
      </div>
      {deactivating && (
        <Confirm
          title="Deactivate skill"
          warn={`"${deactivating.name}" will no longer be assignable. Existing user skills are preserved.`}
          onCancel={() => setDeactivating(null)}
          onConfirm={async () => {
            await post(`/api/v1/skills/${deactivating.id}/deactivate`);
            setDeactivating(null); reload();
          }}
          body={<p>Deactivate <strong>{deactivating.name}</strong>?</p>}
        />
      )}
    </>
  );
}
