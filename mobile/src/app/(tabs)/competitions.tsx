import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { Link } from 'expo-router';
import { Badge, Card, Empty, ErrorBox, Loading } from '../../components/ui';
import { competitions } from '../../lib/endpoints';
import { useApi } from '../../lib/useApi';
import { C } from '../../lib/theme';

/** M09 — competition list; detail handled at /competition/[id]. */
export default function Competitions() {
  const { data, loading, error, reload } = useApi(() => competitions(), []);
  if (loading) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: C.bg }}
      contentContainerStyle={{ padding: 16, gap: 12 }}
      data={data?.items ?? []}
      keyExtractor={(c) => c.id}
      onRefresh={reload}
      refreshing={false}
      renderItem={({ item }) => (
        <Link href={`/competition/${item.id}`} asChild>
          <Pressable>
            <Card style={styles.card}>
              <View style={styles.head}>
                <Text style={styles.title} numberOfLines={2}>{item.title}</Text>
                <Badge v={item.participationStatus ?? item.configStatus} />
              </View>
              <Text style={styles.cta}>Categories, rounds & leaderboard →</Text>
            </Card>
          </Pressable>
        </Link>
      )}
      ListEmptyComponent={
        <Empty title="No competitions yet"
          detail="Published competitions will appear here." />
      }
    />
  );
}

const styles = StyleSheet.create({
  card: { borderTopWidth: 4, borderTopColor: C.gold },
  head: { flexDirection: 'row', justifyContent: 'space-between',
    gap: 10, alignItems: 'flex-start' },
  title: { fontSize: 16, fontWeight: '700', color: C.navy, flex: 1 },
  cta: { marginTop: 8, color: C.purple, fontWeight: '700', fontSize: 13 },
});
