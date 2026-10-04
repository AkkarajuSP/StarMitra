import { get, post } from '../api/client';

/** M20 surface — M12/M13 contracts only; scope resolved server-side. */

export interface JudgeProfile { judgeId: string; status: string }
export interface Assignment {
  id: string; competitionId: string; categoryId?: string; roundId?: string; status: string;
}
export interface ScopedSubmission {
  id: string; participantId: string; competitionId: string; roundId: string; state: string;
}
export interface CriterionView {
  id: string; name: string; weight: number; maxScore: number; sortOrder?: number;
}
export interface RubricVersionView {
  id: string; templateId: string; versionNo: number; status: string;
  criteria: CriterionView[]; scaleMin?: number; scaleMax?: number;
}
export interface ScoreItem { criterionId: string; score: number; comment?: string }
export interface Evaluation {
  id: string; judgeId: string; submissionId: string;
  rubricVersionId: string; status: string; submittedAt?: string;
}
export interface NotificationItem {
  id: string; type: string; body: string; deepLink?: string; state: string; createdAt: string;
}
export interface NotificationPage { items: NotificationItem[] }

export const myProfile = () => get<JudgeProfile>('/api/v1/judges/me');
export const myAssignments = () => get<Assignment[]>('/api/v1/judges/me/assignments');
export const mySubmissions = (competitionId?: string) =>
  get<ScopedSubmission[]>(`/api/v1/judges/me/submissions${
    competitionId ? `?competitionId=${competitionId}` : ''}`);
export const myRubric = (roundId: string) =>
  get<RubricVersionView>(`/api/v1/judges/me/rubrics/${roundId}`);
export const submitEvaluation = (submissionId: string, rubricVersionId: string,
                                 scores: ScoreItem[]) =>
  post<Evaluation>('/api/v1/evaluations', { submissionId, rubricVersionId, scores });
export const myNotifications = () =>
  get<NotificationPage>('/api/v1/notifications?limit=50');
