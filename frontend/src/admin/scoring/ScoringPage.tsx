import { useState } from 'react';
import { post } from '../../api/client';
import { Confirm } from '../shared/ui';

/** M14 — scoring administration. M19 NEVER calculates scores/rank/qualification —
 *  M14 does. Overrides require reason + confirmation (M14 audits). */
export default function ScoringPage() {
  const [competitionId, setCompetitionId] = useState('');
  const [roundId, setRoundId] = useState('');
  const [confirming, setConfirming] = useState<'CALCULATE' | 'FINALIZE' | null>(null);
  const [msg, setMsg] = useState('');
  return (
    <>
      <h1 className="page-title">Scoring &amp; Ranking</h1>
      <p className="derived">M14 owns score/rank/qualification truth — this page only invokes M14 operations.</p>
      <div className="panel">
        <div className="field">
          <label htmlFor="sc-comp">Competition ID</label>
          <input id="sc-comp" value={competitionId}
                 onChange={(e) => setCompetitionId(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="sc-round">Round ID</label>
          <input id="sc-round" value={roundId} onChange={(e) => setRoundId(e.target.value)} />
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
          title={`${confirming === 'CALCULATE' ? 'Calculate' : 'Finalize'} scoring`}
          warn={confirming === 'FINALIZE'
            ? 'Finalizing SEALS scores/rankings for this round — a new ranking snapshot is captured.'
            : 'Recalculates aggregates from authoritative judge/audience inputs.'}
          confirmLabel={confirming}
          onCancel={() => setConfirming(null)}
          onConfirm={async () => {
            await post(`/api/v1/scoring/${confirming.toLowerCase()}`,
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
