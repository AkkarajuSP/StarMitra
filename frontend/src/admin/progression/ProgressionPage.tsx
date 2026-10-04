import { useState } from 'react';
import { post } from '../../api/client';
import { Confirm } from '../shared/ui';

/** M15 — progression administration. M14 decides qualification; M15 derives
 *  advancement. M19 never manufactures outcomes. */
export default function ProgressionPage() {
  const [competitionId, setCompetitionId] = useState('');
  const [roundId, setRoundId] = useState('');
  const [confirming, setConfirming] = useState<'CALCULATE' | 'FINALIZE' | null>(null);
  const [msg, setMsg] = useState('');
  return (
    <>
      <h1 className="page-title">Round Progression</h1>
      <p className="derived">Progression requires sealed M14 results — M15 derives, never invents.</p>
      <div className="panel">
        <div className="field">
          <label htmlFor="pg-comp">Competition ID</label>
          <input id="pg-comp" value={competitionId}
                 onChange={(e) => setCompetitionId(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="pg-round">Round ID</label>
          <input id="pg-round" value={roundId} onChange={(e) => setRoundId(e.target.value)} />
        </div>
        <div className="filters">
          <button className="btn primary" disabled={!competitionId || !roundId}
                  onClick={() => setConfirming('CALCULATE')}>Calculate</button>
          <button className="btn danger" disabled={!competitionId || !roundId}
                  onClick={() => setConfirming('FINALIZE')}>Finalize</button>
        </div>
        {msg && <p>{msg}</p>}
      </div>
      {confirming && (
        <Confirm
          title={`${confirming === 'CALCULATE' ? 'Calculate' : 'Finalize'} progression`}
          warn={confirming === 'FINALIZE'
            ? 'Advancement/elimination becomes authoritative — irreversible.'
            : 'Preview — no records written until finalize.'}
          confirmLabel={confirming}
          onCancel={() => setConfirming(null)}
          onConfirm={async () => {
            await post(`/api/v1/progression/${confirming.toLowerCase()}`,
              { competitionId, roundId });
            setMsg(`${confirming} applied.`); setConfirming(null);
          }}
          body={<p>Competition: <strong>{competitionId}</strong><br/>
                  Round: <strong>{roundId}</strong></p>}
        />
      )}
    </>
  );
}
