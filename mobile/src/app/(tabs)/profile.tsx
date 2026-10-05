import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { Avatar, Badge, Btn, Card, Empty, ErrorBox, Field, Loading, Mark } from '../../components/ui';
import {
  addMySkill, myPortfolio, myPortfolioItems, myProfile, following,
  removeMySkill, skills as allSkills, updateProfile,
} from '../../lib/endpoints';
import { useApi } from '../../lib/useApi';
import { useSession } from '../../lib/session';
import { C } from '../../lib/theme';

/** M02/M03/M08/M21 — own profile, skills, portfolio, social + sign-out. */
export default function Profile() {
  const { session, signOut } = useSession();
  const p = useApi(() => myProfile(), []);
  const sk = useApi(() => allSkills(), []);
  const fol = useApi(() => following(), []);
  const pf = useApi(() => myPortfolio().catch(() => null), []);
  const items = useApi(() => myPortfolioItems().catch(() => []), []);
  const [edit, setEdit] = useState(false);
  const [name, setName] = useState('');
  const [bio, setBio] = useState('');
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<unknown>(null);

  if (p.loading) return <Loading />;
  const prof = p.data;

  const startEdit = () => {
    setName(prof?.displayName ?? ''); setBio(prof?.bio ?? ''); setEdit(true);
  };
  const save = async () => {
    setSaving(true); setErr(null);
    try { await updateProfile({ displayName: name, bio }); setEdit(false); p.reload(); }
    catch (e) { setErr(e); } finally { setSaving(false); }
  };
  const toggleSkill = async (skillId: string, has: boolean) => {
    try {
      if (has) await removeMySkill(skillId);
      else await addMySkill(skillId, 'INTERMEDIATE');
      p.reload();
    } catch (e) { setErr(e); }
  };

  return (
    <ScrollView style={{ flex: 1, backgroundColor: C.bg }}
      contentContainerStyle={{ padding: 16, gap: 14 }}>
      {err != null && <ErrorBox error={err} />}

      <Card style={styles.head}>
        <View style={{ flexDirection: 'row', gap: 14, alignItems: 'center' }}>
          <Avatar name={prof?.displayName ?? session?.email} size={56} />
          <View style={{ flex: 1 }}>
            <Text style={styles.name}>{prof?.displayName ?? session?.email}</Text>
            {prof?.location ? <Text style={styles.loc}>{prof.location}</Text> : null}
            <Badge v={prof?.visibilityState ?? 'PUBLIC'} />
          </View>
        </View>
        {prof?.bio ? <Text style={styles.bio}>{prof.bio}</Text> : null}
        <View style={styles.counts}>
          <Text style={styles.count}>{fol.data?.items.length ?? 0} following</Text>
        </View>
        <Btn kind={edit ? 'ghost' : 'primary'} title={edit ? 'Editing…' : 'Edit profile'}
          onPress={startEdit} />
      </Card>

      {edit && (
        <Card>
          <Field label="Display name" value={name} onChangeText={setName} />
          <Field label="Bio" value={bio} onChangeText={setBio} multiline
            numberOfLines={3} />
          <Btn title="Save" onPress={save} loading={saving} />
        </Card>
      )}

      <Card>
        <Text style={styles.section}>Talent skills</Text>
        <View style={styles.chips}>
          {(sk.data?.items ?? []).map((s) => {
            const has = prof?.skills.some((u) => u.skillId === s.id);
            return (
              <Pressable key={s.id}
                onPress={() => toggleSkill(s.id, !!has)}
                accessibilityRole="button"
                style={[styles.chip, has && styles.chipOn]}>
                <Text style={[styles.chipText, has && styles.chipTextOn]}>
                  {s.name}{has ? ` · ${prof?.skills.find((u) => u.skillId === s.id)?.proficiency}` : ''}
                </Text>
              </Pressable>
            );
          })}
        </View>
        <Text style={styles.hint}>Tap a skill to add/remove (INTERMEDIATE default).</Text>
      </Card>

      <Card>
        <Text style={styles.section}>Portfolio</Text>
        <Text style={styles.hint}>{pf.data?.title ?? 'Untitled portfolio'}</Text>
        {items.loading ? <Loading /> : (items.data ?? []).length === 0
          ? <Text style={styles.hint}>No portfolio items yet.</Text>
          : (items.data ?? []).map((i) => (
              <View key={i.id} style={styles.item}>
                <Text style={styles.itemTitle}>{i.title}</Text>
                {i.description ? <Text style={styles.hint}>{i.description}</Text> : null}
              </View>
            ))}
      </Card>

      <View style={{ gap: 10 }}>
        <Pressable style={styles.link} onPress={() => router.push('/rooms')}>
          <Mark size={22} /><Text style={styles.linkText}>Creative Rooms</Text>
        </Pressable>
        <Pressable style={styles.link} onPress={() => router.push('/notifications')}>
          <Mark size={22} /><Text style={styles.linkText}>Notifications</Text>
        </Pressable>
        <Btn kind="danger" title="Sign out" onPress={async () => {
          await signOut(); router.replace('/login');
        }} />
      </View>
      {p.error != null && <ErrorBox error={p.error} />}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  head: { gap: 12 },
  name: { fontSize: 19, fontWeight: '800', color: C.ink },
  loc: { fontSize: 12.5, color: C.muted, marginBottom: 4 },
  bio: { fontSize: 13.5, color: C.ink, lineHeight: 20 },
  counts: { flexDirection: 'row', gap: 14 },
  count: { fontSize: 13, color: C.muted, fontWeight: '600' },
  section: { fontSize: 16, fontWeight: '800', color: C.navy, marginBottom: 10 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { borderWidth: 1.5, borderColor: C.line, borderRadius: 999,
    paddingHorizontal: 13, paddingVertical: 8, backgroundColor: '#fff' },
  chipOn: { backgroundColor: C.purple, borderColor: C.purple },
  chipText: { fontSize: 13, fontWeight: '600', color: C.ink },
  chipTextOn: { color: '#fff' },
  hint: { fontSize: 12.5, color: C.muted, marginTop: 8 },
  item: { borderTopWidth: 1, borderTopColor: C.line, paddingTop: 10, marginTop: 10 },
  itemTitle: { fontSize: 14, fontWeight: '700', color: C.ink },
  link: { flexDirection: 'row', alignItems: 'center', gap: 10,
    backgroundColor: '#fff', borderRadius: 12, padding: 14,
    borderWidth: 1, borderColor: C.line },
  linkText: { fontSize: 14.5, fontWeight: '700', color: C.navy },
});
