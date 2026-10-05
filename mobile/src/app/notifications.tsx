import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { Btn, Card, Empty, ErrorBox, Loading } from '../components/ui';
import { markAllRead, markNotificationRead, notifications } from '../lib/endpoints';
import { useApi } from '../lib/useApi';
import { C } from '../lib/theme';

/** M17 — notification inbox; mark read / mark all read. */
export default function Notifications() {
  const { data, loading, error, reload } = useApi(() => notifications(), []);
  const items = data?.items ?? [];
  const unread = items.filter((n) => n.state === 'UNREAD').length;

  return (
    <FlatList
      style={{ flex: 1, backgroundColor: C.bg }}
      contentContainerStyle={{ padding: 16, gap: 10 }}
      data={items}
      keyExtractor={(n) => n.id}
      onRefresh={reload}
      refreshing={false}
      ListHeaderComponent={
        <View style={styles.head}>
          <Text style={styles.count}>{unread} unread</Text>
          <Btn kind="ghost" title="Mark all read"
            onPress={async () => { await markAllRead().catch(() => {}); reload(); }} />
        </View>
      }
      renderItem={({ item }) => (
        <Pressable
          onPress={async () => {
            if (item.state === 'UNREAD') {
              await markNotificationRead(item.id).catch(() => {});
              reload();
            }
          }}>
          <Card style={[styles.row, item.state === 'UNREAD' && styles.unread]}>
            <View style={styles.dot} />
            <View style={{ flex: 1 }}>
              <Text style={styles.type}>{item.type.replace(/_/g, ' ')}</Text>
              <Text style={styles.body}>{item.body}</Text>
              <Text style={styles.when}>{item.createdAt.slice(0, 10)}</Text>
            </View>
          </Card>
        </Pressable>
      )}
      ListEmptyComponent={
        !loading && !error ? <Empty title="No notifications" /> : null
      }
      ListFooterComponent={
        <>{loading ? <Loading /> : error ? <ErrorBox error={error} /> : null}</>
      }
    />
  );
}

const styles = StyleSheet.create({
  head: { flexDirection: 'row', justifyContent: 'space-between',
    alignItems: 'center', marginBottom: 4 },
  count: { fontSize: 14, fontWeight: '700', color: C.navy },
  row: { flexDirection: 'row', gap: 10, alignItems: 'flex-start' },
  unread: { borderLeftWidth: 4, borderLeftColor: C.gold },
  dot: { width: 8, height: 8, borderRadius: 4, backgroundColor: C.gold,
    marginTop: 6 },
  type: { fontSize: 11, fontWeight: '800', color: C.purple,
    letterSpacing: 0.6 },
  body: { fontSize: 13.5, color: C.ink, marginTop: 2 },
  when: { fontSize: 11, color: C.muted, marginTop: 4 },
});
