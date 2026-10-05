import { FlatList, StyleSheet, Text, View } from 'react-native';
import { Badge, Card, Empty, ErrorBox, Loading } from '../components/ui';
import { rooms } from '../lib/endpoints';
import { useApi } from '../lib/useApi';
import { C } from '../lib/theme';

/** M07 — Creative Rooms list (visibility-scoped by backend). */
export default function Rooms() {
  const { data, loading, error, reload } = useApi(() => rooms(), []);
  if (loading) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: C.bg }}
      contentContainerStyle={{ padding: 16, gap: 12 }}
      data={data?.items ?? []}
      keyExtractor={(r) => r.id}
      onRefresh={reload}
      refreshing={false}
      renderItem={({ item }) => (
        <Card>
          <View style={styles.head}>
            <Text style={styles.name}>{item.name}</Text>
            <Badge v={item.status} />
          </View>
          {item.description ? <Text style={styles.desc}>{item.description}</Text> : null}
          <Text style={styles.vis}>Visibility: {item.visibility ?? '—'}</Text>
        </Card>
      )}
      ListEmptyComponent={
        <Empty title="No rooms"
          detail="Creative Rooms you're invited to will appear here." />
      }
    />
  );
}

const styles = StyleSheet.create({
  head: { flexDirection: 'row', justifyContent: 'space-between',
    gap: 10, alignItems: 'center' },
  name: { fontSize: 16, fontWeight: '700', color: C.navy, flex: 1 },
  desc: { fontSize: 13, color: C.ink, marginTop: 6 },
  vis: { fontSize: 12, color: C.muted, marginTop: 8 },
});
