import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { myRubric, submitEvaluation, type ScoreItem } from '../api';
import { useApi, State, Confirm } from '../../admin/shared/ui';
import { ApiProblem } from '../../api/client';

/** M13 evaluation — published rubric rendered read-only; scores validated
 *  against criterion maxScore; submit is a single POST (server idempotent). */
export default function EvaluatePage() {
  const { submissionId = '' } = useParams();
  const [params] = useSearchParams();
  const roundId = params.get('round') ?? '';
  const { data: rubric, loading, error } = useApi(
    () => myRubric(roundId), [roundId]);
  const [scores, setScores] = useState<Record<string, ScoreItem>>({});
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
      await submitEvaluation(submissionId, rubric!.id, criteria.map((c) => scores[c.id]));
      setDone(true); setReview(false);
    } catch (e) {
      setReview(false);
      setSubmitError(e instanceof ApiProblem
        ? (e.status === 409 ? 'Already submitted — evaluation is locked.' : e.message)
        : 'Submission failed.');
    }
  };

  if (done) return (
    <div className="panel" role="status">
      <h2>Evaluation submitted</h2>
      <p>Recorded against rubric v{rubric?.versionNo}. Locked per backend lifecycle.</p>
    </div>
  );

  return (
    <>
      <h1 className="page-title">Evaluate submission</h1>
      {submitError && <div className="state error" role="alert">{submitError}</div>}
      <State loading={loading} error={error}>
        <div className="panel">
          <h2>Rubric v{rubric?.versionNo} — {rubric?.status}</h2>
          <div className="table-wrap">
            <table className="list" aria-label="Rubric criteria">
              <thead><tr>
                <th>Criterion</th><th>Weight</th><th>Max</th>
                <th>Score</th><th>Comment</th>
              </tr></thead>
              <tbody>
                {criteria.map((c) => (
                  <tr key={c.id}>
                    <td>{c.name}</td>
                    <td>{c.weight}%</td>
                    <td>{c.maxScore}</td>
                    <td>
                      <input
                        aria-label={`Score for ${c.name}`}
                        type="number" min={0} max={c.maxScore}
                        value={scores[c.id]?.score ?? ''}
                        onChange={(e) => setScores({
                          ...scores,
                          [c.id]: { criterionId: c.id,
                                    score: Number(e.target.value),
                                    comment: scores[c.id]?.comment },
                        })}
                      />
                    </td>
                    <td>
                      <input
                        aria-label={`Comment for ${c.name}`}
                        value={scores[c.id]?.comment ?? ''}
                        onChange={(e) => setScores({
                          ...scores,
                          [c.id]: { criterionId: c.id,
                                    score: scores[c.id]?.score ?? 0,
                                    comment: e.target.value },
                        })}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {missing.length > 0 && <p className="derived">{missing.length} criterion/criteria unscored.</p>}
          {outOfRange.length > 0 &&
            <p className="state error" role="alert">Scores must be within 0–max.</p>}
          <button className="btn primary" disabled={!ready} onClick={() => setReview(true)}>
            Review &amp; submit
          </button>
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
