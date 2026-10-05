import { useState } from 'react';
import { FlatList, KeyboardAvoidingView, Platform, StyleSheet, Text, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { Btn, ErrorBox, Field, Loading } from '../../components/ui';
import { markRead, messages, sendMessage } from '../../lib/endpoints';
import { useApi } from '../../lib/useApi';
import { useSession } from '../../lib/session';
import { C } from '../../lib/theme';

/** M06 — conversation messages; send + read-mark. */
export default function ConversationScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { session } = useSession();
  const { data, loading, error, reload } = useApi(() => messages(id), [id]);
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  const [sendErr, setSendErr] = useState<unknown>(null);

  const send = async () => {
    if (!draft.trim()) return;
    setBusy(true); setSendErr(null);
    try {
      await sendMessage(id, draft.trim());
      setDraft('');
      await reload();
      const last = data?.items.at(-1)?.sequence;
      if (last !== undefined) await markRead(id, last);
    } catch (e) { setSendErr(e); } finally { setBusy(false); }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: C.bg }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={90}>
      <FlatList
        contentContainerStyle={{ padding: 16, gap: 8 }}
        data={data?.items ?? []}
        keyExtractor={(m) => m.id}
        renderItem={({ item }) => {
          const mine = item.senderId === session?.userId;
          return (
            <View style={[styles.bubble, mine ? styles.mine : styles.theirs]}>
              <Text style={[styles.body, mine && { color: '#fff' }]}>
                {item.body ?? '(media)'}
              </Text>
              <Text style={[styles.seq, mine && { color: C.lavender }]}>
                #{item.sequence}
              </Text>
            </View>
          );
        }}
        ListHeaderComponent={
          <>{loading ? <Loading /> : error ? <ErrorBox error={error} /> : null}</>
        }
      />
      {sendErr != null && <ErrorBox error={sendErr} />}
      <View style={styles.composer}>
        <View style={{ flex: 1 }}>
          <Field placeholder="Message…" value={draft} onChangeText={setDraft} />
        </View>
        <Btn title="Send" onPress={send} loading={busy} />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  bubble: { borderRadius: 14, padding: 10, maxWidth: '80%' },
  mine: { backgroundColor: C.purple, alignSelf: 'flex-end',
    borderBottomRightRadius: 4 },
  theirs: { backgroundColor: '#fff', alignSelf: 'flex-start',
    borderBottomLeftRadius: 4, borderWidth: 1, borderColor: C.line },
  body: { fontSize: 14.5, color: C.ink },
  seq: { fontSize: 10, color: C.muted, marginTop: 4, alignSelf: 'flex-end' },
  composer: { flexDirection: 'row', gap: 10, padding: 12,
    backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: C.line,
    alignItems: 'center' },
});
