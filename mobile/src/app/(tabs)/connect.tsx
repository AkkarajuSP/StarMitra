import { FlatList, Pressable, StyleSheet, Text } from 'react-native';
import { Link } from 'expo-router';
import { Card, Empty, ErrorBox, Loading } from '../../components/ui';
import { conversations } from '../../lib/endpoints';
import { useApi } from '../../lib/useApi';
import { C } from '../../lib/theme';

/** M06 Connect — conversation list; messages at /conversation/[id]. */
export default function Connect() {
  const { data, loading, error, reload } = useApi(() => conversations(), []);
  if (loading) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: C.bg }}
      contentContainerStyle={{ padding: 16, gap: 10 }}
      data={data?.items ?? []}
      keyExtractor={(c) => c.id}
      onRefresh={reload}
      refreshing={false}
      renderItem={({ item }) => (
        <Link href={`/conversation/${item.id}`} asChild>
          <Pressable>
            <Card style={styles.row}>
              <Text style={styles.type}>{item.type.replace(/_/g, ' ')}</Text>
              <Text style={styles.id} numberOfLines={1}>
                Conversation {item.id.slice(0, 8)}…
              </Text>
            </Card>
          </Pressable>
        </Link>
      )}
      ListEmptyComponent={
        <Empty title="No conversations"
          detail="Direct and room conversations will appear here." />
      }
    />
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 12,
    paddingVertical: 14 },
  type: { fontSize: 11, fontWeight: '800', color: C.purple,
    letterSpacing: 0.6 },
  id: { flex: 1, fontSize: 14, color: C.ink, fontWeight: '600' },
});
