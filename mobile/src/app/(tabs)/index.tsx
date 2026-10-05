import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { Link, router } from 'expo-router';
import { Card, Empty, ErrorBox, Loading, Mark, Badge } from '../../components/ui';
import { feed, competitions, type Competition } from '../../lib/endpoints';
import { useApi } from '../../lib/useApi';
import { useSession } from '../../lib/session';
import { C } from '../../lib/theme';

/** Home — personalized feed + featured competitions (M05/M09 data). */
export default function Home() {
  const { session } = useSession();
  const f = useApi(() => feed(), []);
  const comps = useApi(() => competitions(), []);

  return (
    <FlatList
      style={{ flex: 1, backgroundColor: C.bg }}
      contentContainerStyle={{ padding: 16, gap: 14 }}
      data={f.data?.items ?? []}
      keyExtractor={(item, i) => String(item.id ?? item.submissionId ?? i)}
      ListHeaderComponent={
        <View style={{ gap: 14 }}>
          <View style={styles.welcome}>
            <Mark size={40} surface="dark" />
            <View style={{ flex: 1 }}>
              <Text style={styles.hi}>Welcome back</Text>
              <Text style={styles.mail} numberOfLines={1}>{session?.email}</Text>
            </View>
            <Pressable accessibilityLabel="Notifications"
              onPress={() => router.push('/notifications')}>
              <Text style={styles.bell}>🔔</Text>
            </Pressable>
          </View>

          <Text style={styles.section}>Competitions</Text>
          {comps.loading ? <Loading /> : comps.error ? <ErrorBox error={comps.error} />
            : (comps.data?.items ?? []).slice(0, 3).map((c) => (
                <CompetitionCard key={c.id} c={c} />
              ))}

          <Text style={styles.section}>Your feed</Text>
        </View>
      }
      renderItem={({ item }) => (
        <Card>
          <Text style={styles.feedType}>
            {String(item.type ?? 'ITEM').replace(/_/g, ' ')}</Text>
          {Object.entries(item).slice(0, 4).map(([k, v]) =>
            k !== 'type' && v != null ? (
              <Text key={k} style={styles.feedLine}>
                {k}: {typeof v === 'object' ? JSON.stringify(v) : String(v)}
              </Text>
            ) : null)}
        </Card>
      )}
      ListEmptyComponent={
        !f.loading && !f.error
          ? <Empty title="Your feed is quiet"
              detail="Follow creators and join competitions to fill it up." />
          : null
      }
      ListFooterComponent={
        f.loading ? <Loading /> : f.error ? <ErrorBox error={f.error} /> : null
      }
    />
  );
}

function CompetitionCard({ c }: { c: Competition }) {
  return (
    <Link href={`/competition/${c.id}`} asChild>
      <Pressable>
        <Card style={styles.compCard}>
          <View style={styles.compHead}>
            <Text style={styles.compTitle} numberOfLines={2}>{c.title}</Text>
            <Badge v={c.participationStatus ?? c.configStatus} />
          </View>
          <Text style={styles.compCta}>View details →</Text>
        </Card>
      </Pressable>
    </Link>
  );
}

const styles = StyleSheet.create({
  welcome: { flexDirection: 'row', alignItems: 'center', gap: 12,
    backgroundColor: C.navy, borderRadius: 16, padding: 14 },
  hi: { color: '#fff', fontWeight: '800', fontSize: 16 },
  mail: { color: C.lavender, fontSize: 12 },
  bell: { fontSize: 20 },
  section: { fontSize: 17, fontWeight: '800', color: C.ink, marginTop: 4 },
  compCard: { borderTopWidth: 4, borderTopColor: C.gold },
  compHead: { flexDirection: 'row', justifyContent: 'space-between',
    gap: 10, alignItems: 'flex-start' },
  compTitle: { fontSize: 16, fontWeight: '700', color: C.navy, flex: 1 },
  compCta: { marginTop: 8, color: C.purple, fontWeight: '700', fontSize: 13 },
  feedType: { fontSize: 11, fontWeight: '800', color: C.purple,
    letterSpacing: 0.8, marginBottom: 6 },
  feedLine: { fontSize: 13, color: C.ink, marginTop: 2 },
});
