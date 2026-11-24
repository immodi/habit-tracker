import { readFileSync } from 'node:fs';
import { afterAll, beforeAll, describe, it } from 'vitest';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  collection,
  setDoc,
} from 'firebase/firestore';

/** @type {import('@firebase/rules-unit-testing').RulesTestEnvironment} */
let testEnv;

const habitPath = (uid, habitId = 'h1') => `users/${uid}/habits/${habitId}`;
const logPath = (uid, id = 'h1_2026-11-23') => `users/${uid}/habitLogs/${id}`;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-habit-tracker',
    firestore: {
      rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
      host: '127.0.0.1',
      port: 8085,
    },
  });
});

afterAll(async () => {
  await testEnv?.cleanup();
});

describe('owner access', () => {
  it('lets a user create, read, update and delete their own habit', async () => {
    const db = testEnv.authenticatedContext('alice').firestore();
    await assertSucceeds(
      setDoc(doc(db, habitPath('alice')), {
        name: 'Read',
        weekdays: [1, 2, 3, 4, 5, 6, 7],
        archived: false,
      }),
    );
    await assertSucceeds(getDoc(doc(db, habitPath('alice'))));
    await assertSucceeds(
      setDoc(doc(db, habitPath('alice')), { name: 'Read more' }, { merge: true }),
    );
    await assertSucceeds(deleteDoc(doc(db, habitPath('alice'))));
  });

  it('lets a user write and read their own check-in', async () => {
    const db = testEnv.authenticatedContext('alice').firestore();
    await assertSucceeds(
      setDoc(doc(db, logPath('alice')), {
        habitId: 'h1',
        date: '2026-11-23',
      }),
    );
    await assertSucceeds(getDoc(doc(db, logPath('alice'))));
  });

  it('lets a user read and write their own settings document', async () => {
    const db = testEnv.authenticatedContext('alice').firestore();
    const ref = doc(db, 'users/alice/settings/preferences');
    await assertSucceeds(setDoc(ref, { reminderTime: '08:30' }));
    await assertSucceeds(getDoc(ref));
  });
});

describe('cross-user isolation', () => {
  beforeAll(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, habitPath('bob')), { name: 'Bob only' });
      await setDoc(doc(db, logPath('bob')), { habitId: 'h1', date: '2026-11-23' });
      await setDoc(doc(db, 'users/bob/settings/preferences'), { reminderTime: null });
    });
  });

  it('blocks reading another user habit, log, settings and habit list', async () => {
    const db = testEnv.authenticatedContext('alice').firestore();
    await assertFails(getDoc(doc(db, habitPath('bob'))));
    await assertFails(getDoc(doc(db, logPath('bob'))));
    await assertFails(getDoc(doc(db, 'users/bob/settings/preferences')));
    await assertFails(getDocs(collection(db, 'users/bob/habits')));
  });

  it('blocks writing to another user documents', async () => {
    const db = testEnv.authenticatedContext('alice').firestore();
    await assertFails(setDoc(doc(db, habitPath('bob')), { name: 'hacked' }));
    await assertFails(setDoc(doc(db, logPath('bob')), { habitId: 'h1', date: '2026-11-24' }));
    await assertFails(deleteDoc(doc(db, habitPath('bob'))));
  });
});

describe('signed-out access', () => {
  it('blocks reading everything', async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, habitPath('alice'))));
    await assertFails(getDocs(collection(db, 'users/alice/habits')));
  });

  it('blocks writing everything', async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(setDoc(doc(db, habitPath('alice')), { name: 'anon' }));
    await assertFails(setDoc(doc(db, 'public/anything'), { value: 1 }));
  });
});
