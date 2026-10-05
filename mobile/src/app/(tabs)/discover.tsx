import { useState } from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';
import { Btn, Card, Empty, ErrorBox, Field, Loading } from '../../components/ui';
import { discover, search, type SearchItem } from '../../lib/endpoints';
import { useApi } from '../../lib/useApi';
import { C } from '../../lib/theme';

/** M05 — search + discovery over moderated public content. */
export default function Discover() {
  const [q, setQ] = useState('');
  const [results, setResults] = useState<SearchItem[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const d = useApi(() => discover(), []);

  const runSearch = async () => {
    if (!q.trim()) { setResults(null); return; }
    setBusy(true); setError(null);
    try { setResults((await search(q.trim())).items); }
    catch (e) { setError(e); }
    finally { setBusy(false); }
  };

  const items = results ?? d.data?.items ?? null;
  return (
    <FlatList
      style={{ flex: 1, backgroundColor: C.bg }}
      contentContainerStyle={{ padding: 16, gap: 12 }}
      data={items}
      keyExtractor={(item, i) => String(item.id ?? i)}
      ListHeaderComponent={
        <View style={{ gap: 8 }}>
          <Field placeholder="Search talents, skills, competitions…"
            value={q} onChangeText={setQ} returnKeyType="search"
            onSubmitEditing={runSearch} />
          <Btn title="Search" onPress={runSearch} loading={busy} />
        </View>
      }
      renderItem={({ item }) => (
        <Card>
          <Text style={styles.type}>
            {String(item.type ?? 'RESULT').replace(/_/g, ' ')}</Text>
          {Object.entries(item).slice(0, 5).map(([k, v]) =>
            k !== 'type' && v != null ? (
              <Text key={k} style={styles.line}>
                {k}: {typeof v === 'object' ? JSON.stringify(v) : String(v)}
              </Text>
            ) : null)}
        </Card>
      )}
      ListEmptyComponent={
        !d.loading ? <Empty title="Nothing found"
          detail="Try a different search or check back later." /> : null
      }
      ListFooterComponent={
        <>
          {d.loading || busy ? <Loading /> : null}
          {error != null ? <ErrorBox error={error} /> : null}
          {d.error != null ? <ErrorBox error={d.error} /> : null}
        </>
      }
    />
  );
}

const styles = StyleSheet.create({
  type: { fontSize: 11, fontWeight: '800', color: C.purple,
    letterSpacing: 0.8, marginBottom: 6 },
  line: { fontSize: 13, color: C.ink, marginTop: 2 },
});
