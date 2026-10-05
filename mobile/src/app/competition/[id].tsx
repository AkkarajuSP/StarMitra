import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { Badge, Btn, Card, Empty, ErrorBox, Loading, Mark } from '../../components/ui';
import {
  competition, competitionSubmissions, leaderboard, castVote, voteCount,
  registerParticipant, participants,
} from '../../lib/endpoints';
import { useApi } from '../../lib/useApi';
import { C } from '../../lib/theme';
import { randomUUID } from 'expo-crypto';

/** M09/M10/M11/M16 — competition detail, participation, voting, leaderboard. */
export default function CompetitionDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const c = useApi(() => competition(id), [id]);
  const subs = useApi(() => competitionSubmissions(id).catch(() => null), [id]);
  const parts = useApi(() => participants(id).catch(() => null), [id]);
  const lb = useApi(() => leaderboard(id).catch(() => null), [id]);
  const [err, setErr] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [voted, setVoted] = useState<Record<string, number>>({});

  if (c.loading) return <Loading />;
  if (c.error || !c.data) return <ErrorBox error={c.error ?? new Error('Not found')} />;
  const comp = c.data;

  const register = async () => {
    setBusy(true); setErr(null);
    try { await registerParticipant(id); parts.reload(); }
    catch (e) { setErr(e); } finally { setBusy(false); }
  };

  const vote = async (submissionId: string) => {
    setBusy(true); setErr(null);
    try {
      await castVote(submissionId, randomUUID());
      const vc = await voteCount(submissionId);
      setVoted((m) => ({ ...m, [submissionId]: vc.count }));
    } catch (e) { setErr(e); } finally { setBusy(false); }
  };

  return (
    <ScrollView style={{ flex: 1, backgroundColor: C.bg }}
      contentContainerStyle={{ padding: 16, gap: 14 }}>
      {err != null && <ErrorBox error={err} />}
      <Card style={styles.banner}>
        <Mark size={54} surface="dark" />
        <Text style={styles.title}>{comp.title}</Text>
        <View style={styles.badgeRow}>
          <Badge v={comp.configStatus} />
          <Badge v={comp.participationStatus} />
          <Badge v={comp.roundState} />
        </View>
      </Card>

      <Card>
        <Text style={styles.section}>Participate</Text>
        <Text style={styles.hint}>
          Register as an individual participant to enter this competition.
        </Text>
        <Btn title="Register" onPress={register} loading={busy} />
        <Text style={styles.count}>
          {(parts.data?.items ?? []).length} participant(s) registered
        </Text>
      </Card>

      <Card>
        <Text style={styles.section}>Submissions</Text>
        {(subs.data?.items ?? []).length === 0
          ? <Text style={styles.hint}>No submissions yet.</Text>
          : (subs.data?.items ?? []).map((s) => (
              <View key={s.id} style={styles.subRow}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.subTitle}>Submission {s.id.slice(0, 8)}…</Text>
                  <Badge v={s.state} />
                </View>
                <Btn kind="gold" disabled={busy || s.state !== 'FINALIZED'}
                  title={voted[s.id] !== undefined ? `Voted (${voted[s.id]})` : 'Vote'}
                  onPress={() => vote(s.id)} />
              </View>
            ))}
      </Card>

      <Card>
        <View style={styles.lbHead}>
          <Text style={styles.section}>Leaderboard</Text>
          <Pressable onPress={() => router.push(`/leaderboard/${id}`)}>
            <Text style={styles.link}>Full view →</Text>
          </Pressable>
        </View>
        {(lb.data?.items ?? []).length === 0
          ? <Text style={styles.hint}>Not published yet.</Text>
          : (lb.data?.items ?? []).slice(0, 5).map((e) => (
              <View key={e.entryRef} style={styles.lbRow}>
                <Text style={styles.rank}>#{e.rank}</Text>
                <Text style={styles.lbRef}>{String(e.display?.name ?? e.entryRef.slice(0, 8) + '…')}</Text>
                {e.display?.score != null &&
                  <Text style={styles.lbScore}>{String(e.display.score)}</Text>}
              </View>
            ))}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  banner: { alignItems: 'center', backgroundColor: C.navy, borderWidth: 0 },
  title: { color: '#fff', fontSize: 19, fontWeight: '800', marginTop: 10,
    textAlign: 'center' },
  badgeRow: { flexDirection: 'row', gap: 8, marginTop: 10, flexWrap: 'wrap' },
  section: { fontSize: 16, fontWeight: '800', color: C.navy, marginBottom: 8 },
  hint: { fontSize: 12.5, color: C.muted, marginBottom: 10 },
  count: { fontSize: 12.5, color: C.muted, marginTop: 10 },
  subRow: { flexDirection: 'row', alignItems: 'center', gap: 10,
    paddingVertical: 10, borderTopWidth: 1, borderTopColor: C.line },
  subTitle: { fontSize: 14, fontWeight: '700', color: C.ink, marginBottom: 5 },
  lbHead: { flexDirection: 'row', justifyContent: 'space-between',
    alignItems: 'center' },
  link: { color: C.purple, fontWeight: '700', fontSize: 13 },
  lbRow: { flexDirection: 'row', alignItems: 'center', gap: 10,
    paddingVertical: 8, borderTopWidth: 1, borderTopColor: C.line },
  rank: { fontSize: 15, fontWeight: '800', color: C.gold, width: 36 },
  lbRef: { flex: 1, fontSize: 13.5, color: C.ink, fontWeight: '600' },
  lbScore: { fontSize: 13, color: C.muted, fontWeight: '700' },
});
