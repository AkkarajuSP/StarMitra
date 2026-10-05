import { FlatList, StyleSheet, Text, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { Empty, ErrorBox, Loading } from '../../components/ui';
import { leaderboard } from '../../lib/endpoints';
import { useApi } from '../../lib/useApi';
import { C } from '../../lib/theme';

/** M16 — published leaderboard; ranking is server-authoritative. */
export default function Leaderboard() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data, loading, error } = useApi(() => leaderboard(id), [id]);
  if (loading) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: C.bg }}
      contentContainerStyle={{ padding: 16, gap: 8 }}
      data={data?.items ?? []}
      keyExtractor={(e) => e.entryRef}
      renderItem={({ item }) => (
        <View style={[styles.row, item.rank === 1 && styles.first]}>
          <Text style={styles.rank}>#{item.rank}</Text>
          <Text style={styles.name}>
            {String(item.display?.name ?? `${item.entryRef.slice(0, 8)}…`)}
          </Text>
          {item.display?.score != null &&
            <Text style={styles.score}>{String(item.display.score)}</Text>}
          {item.display?.qualification != null &&
            <Text style={styles.qual}>{String(item.display.qualification)}</Text>}
        </View>
      )}
      ListEmptyComponent={
        <Empty title="Leaderboard not published"
          detail="Rankings appear once the round leaderboard is published." />
      }
    />
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 12,
    backgroundColor: '#fff', borderRadius: 12, padding: 14,
    borderWidth: 1, borderColor: C.line },
  first: { borderColor: C.gold, borderWidth: 2 },
  rank: { fontSize: 16, fontWeight: '800', color: C.gold, width: 42 },
  name: { flex: 1, fontSize: 14.5, fontWeight: '700', color: C.ink },
  score: { fontSize: 14, fontWeight: '700', color: C.navy },
  qual: { fontSize: 12, fontWeight: '700', color: C.purple },
});
