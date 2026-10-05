import { useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { myRubric, submitEvaluation, type ScoreItem } from '../api';
import { useApi, State, Confirm, Badge } from '../../admin/shared/ui';
import { ApiProblem } from '../../api/client';

/** M13 evaluation — published rubric rendered read-only; scores validated
 *  against criterion maxScore; submit is a single POST (server idempotent). */
export default function EvaluatePage() {
  const { submissionId = '' } = useParams();
  const [params] = useSearchParams();
  const roundId = params.get('round') ?? '';
  const { data: rubric, loading, error } = useApi(
    () => myRubric(roundId), [roundId]);
  const [scores, setScores] = useState<
    Record<string, { criterionId: string; score?: number; comment?: string }>>({});
  const [notes, setNotes] = useState('');
  const [review, setReview] = useState(false);
  const [done, setDone] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const criteria = rubric?.criteria ?? [];
  const missing = criteria.filter((c) => scores[c.id]?.score === undefined);
  const outOfRange = criteria.filter((c) => {
    const s = scores[c.id]?.score;
    return s !== undefined && c.maxScore != null && (s < 0 || s > c.maxScore);
  });
  const ready = criteria.length > 0 && missing.length === 0 && outOfRange.length === 0;

  const doSubmit = async () => {
    try {
      await submitEvaluation(submissionId, rubric!.id,
        criteria.map((c) => scores[c.id] as ScoreItem));
      setDone(true); setReview(false);
    } catch (e) {
      setReview(false);
      setSubmitError(e instanceof ApiProblem
        ? (e.status === 409 ? 'Already submitted — evaluation is locked.' : e.message)
        : 'Submission failed.');
    }
  };

  if (done) return (
    <div className="panel eval-done" role="status">
      <div className="empty-state">
        <img src="/brand/mobileAppIconDark.png" alt="" aria-hidden="true" />
        <div className="t">Evaluation submitted</div>
        <div className="d">Recorded against rubric v{rubric?.versionNo} —
          locked per the evaluation lifecycle.</div>
      </div>
      <p style={{ textAlign: 'center' }}>
        <Link className="btn" to="/judge/submissions">Back to submissions</Link>
      </p>
    </div>
  );

  return (
    <>
      <p style={{ margin: '0 0 10px' }}>
        <Link to="/judge/submissions" className="link-btn">← Back to submissions</Link>
      </p>
      <div className="eval-head">
        <div>
          <h1 className="page-title">Evaluate submission</h1>
          <p className="page-sub">Submission {submissionId.slice(0, 8)}…</p>
        </div>
        <Badge v={rubric?.status ?? 'DRAFT'} />
      </div>
      {submitError && <div className="state error" role="alert">{submitError}</div>}
      <State loading={loading} error={error}>
        <div className="eval-grid">
          {/* LEFT — preview + submission details */}
          <div>
            <div className="panel media-preview">
              <img src="/brand/mobileAppIconLight.png" alt="Submission media placeholder"
                   className="preview-art" />
              <p className="derived" style={{ margin: 0 }}>
                Media preview — attachment delivery is wired through M04;
                thumbnails render here when media is attached.
              </p>
            </div>
            <div className="panel">
              <h2>Submission details</h2>
              <dl className="detail-grid">
                <dt>Submission</dt><dd>{submissionId.slice(0, 8)}…</dd>
                <dt>Round</dt><dd>{roundId.slice(0, 8)}…</dd>
                <dt>Rubric</dt><dd>v{rubric?.versionNo} — {rubric?.status}</dd>
              </dl>
            </div>
          </div>

          {/* RIGHT — rubric */}
          <div className="panel">
            <h2>Evaluation rubric</h2>
            {criteria.map((c) => (
              <div className="rubric-row" key={c.id}>
                <div>
                  <div className="crit">{c.name}</div>
                  <div className="max">Weight {c.weight}% · Max {c.maxScore}</div>
                </div>
                <div>
                  <input className="score-input"
                    aria-label={`Score for ${c.name}`}
                    type="number" min={0} max={c.maxScore}
                    value={scores[c.id]?.score ?? ''}
                    onChange={(e) => setScores({
                      ...scores,
                      [c.id]: { criterionId: c.id,
                                score: e.target.value === '' ? undefined
                                  : Number(e.target.value),
                                comment: scores[c.id]?.comment },
                    })}
                  />
                  <div className="max" style={{ textAlign: 'center' }}>
                    / {c.maxScore}
                  </div>
                </div>
                <input
                  className="crit-note"
                  aria-label={`Comment for ${c.name}`}
                  placeholder="Comment (optional)"
                  value={scores[c.id]?.comment ?? ''}
                  onChange={(e) => setScores({
                    ...scores,
                    [c.id]: { criterionId: c.id,
                              score: scores[c.id]?.score ?? 0,
                              comment: e.target.value },
                  })}
                />
              </div>
            ))}

            <div className="field" style={{ marginTop: 14 }}>
              <label htmlFor="eval-notes">Overall comments</label>
              <textarea id="eval-notes" rows={3} value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Optional overall notes (not part of scored criteria)" />
            </div>

            {missing.length > 0 &&
              <p className="derived">{missing.length} criterion/criteria unscored.</p>}
            {outOfRange.length > 0 &&
              <p className="state error" role="alert"
                 style={{ padding: 8, textAlign: 'left' }}>
                Scores must be within 0–max for each criterion.</p>}

            <button className="btn primary" disabled={!ready}
                    onClick={() => setReview(true)}>
              Review &amp; submit
            </button>
          </div>
        </div>
      </State>
      {review && (
        <Confirm
          title="Submit evaluation"
          warn="Submitted evaluations are locked per the backend lifecycle — duplicate submission is rejected."
          confirmLabel="Submit evaluation"
          onCancel={() => setReview(false)}
          onConfirm={doSubmit}
          body={
            <ul>
              {criteria.map((c) => (
                <li key={c.id}>{c.name}: <strong>{scores[c.id]?.score}</strong>/{c.maxScore}</li>
              ))}
            </ul>
          }
        />
      )}
    </>
  );
}
