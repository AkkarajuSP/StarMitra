import { useState } from 'react';
import { post } from '../../api/client';
import { Confirm } from '../shared/ui';

type PubAction = 'PUBLISH' | 'HIDE' | 'ARCHIVE';

/** M16 — leaderboard publication controls. M19 orchestrates; M16 owns publication. */
export default function LeaderboardsPage() {
  const [competitionId, setCompetitionId] = useState('');
  const [pending, setPending] = useState<PubAction | null>(null);
  const [msg, setMsg] = useState('');
  return (
    <>
      <h1 className="page-title">Leaderboards</h1>
      <div className="panel">
        <h2>Publication</h2>
        <p className="derived">M16 owns publication state — ranks/scores come from M14, never edited here.</p>
        <div className="field">
          <label htmlFor="lb-comp">Competition ID</label>
          <input id="lb-comp" value={competitionId} onChange={(e) => setCompetitionId(e.target.value)} />
        </div>
        <div className="filters">
          {(['PUBLISH', 'HIDE', 'ARCHIVE'] as PubAction[]).map((a) => (
            <button key={a} className="btn primary" disabled={!competitionId}
                    onClick={() => setPending(a)}>{a}</button>
          ))}
        </div>
        {msg && <p>{msg}</p>}
      </div>
      {pending && (
        <Confirm
          title={`${pending} leaderboard`}
          warn={pending === 'HIDE' || pending === 'ARCHIVE'
            ? 'This changes public visibility of the leaderboard.'
            : 'This rebuilds the projection from authoritative M14 results.'}
          confirmLabel={pending}
          onCancel={() => setPending(null)}
          onConfirm={async () => {
            await post('/api/v1/leaderboards/publications',
              { competitionId, action: pending });
            setMsg(`${pending} applied.`); setPending(null);
          }}
          body={<p>Competition: <strong>{competitionId}</strong></p>}
        />
      )}
    </>
  );
}
