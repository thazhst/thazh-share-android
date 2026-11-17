const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'gen-lang-client-0774062405';

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: '127.0.0.1',
      port: 8085,
      rules: fs.readFileSync('firestore.rules', 'utf8'),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

function buildValidLinkPayload(userId, now, overrides = {}) {
  return {
    userId,
    fileName: 'thazh_archive.zip',
    fileSizeBytes: 2048576,
    mimeType: 'application/zip',
    url: 'https://files.catbox.moe/abc123.zip',
    expiryLabel: 'PERMANENT',
    expiresAtEpochMs: 0,
    note: 'Bản sao lưu dự án',
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function buildValidProfilePayload(userId, now, overrides = {}) {
  return {
    userId,
    deviceName: 'Galaxy S24 Ultra',
    email: 'user@example.com',
    updatedAt: now,
    ...overrides,
  };
}

test('Unauthenticated client is denied read and write on user profile and shared_links', async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  const profileRef = unauthedDb.doc('users/user_1');
  const linkRef = unauthedDb.doc('users/user_1/shared_links/link_1');
  const now = new Date(Date.now() - 1000);
  await assertFails(profileRef.get());
  await assertFails(profileRef.set(buildValidProfilePayload('user_1', now)));
  await assertFails(linkRef.get());
  await assertFails(linkRef.set(buildValidLinkPayload('user_1', now)));
});

test('Authenticated owner can sync deviceName in /users/{userId} and reject cross-user or invalid payloads', async () => {
  const aliceDb = testEnv.authenticatedContext('alice_uid').firestore();
  const bobDb = testEnv.authenticatedContext('bob_uid').firestore();
  const aliceProfileRef = aliceDb.doc('users/alice_uid');
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceProfileRef.set(buildValidProfilePayload('alice_uid', now))
  );

  const snap = await assertSucceeds(aliceProfileRef.get());
  assert.equal(snap.exists, true);
  assert.equal(snap.data().deviceName, 'Galaxy S24 Ultra');

  // Update synced deviceName
  await assertSucceeds(
    aliceProfileRef.update({
      deviceName: 'Thazh Phone',
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read or write Alice's synced deviceName
  const bobTargetProfile = bobDb.doc('users/alice_uid');
  await assertFails(bobTargetProfile.get());
  await assertFails(
    bobTargetProfile.set(buildValidProfilePayload('alice_uid', now))
  );

  // Empty deviceName is rejected
  await assertFails(
    aliceProfileRef.set(
      buildValidProfilePayload('alice_uid', now, { deviceName: '' })
    )
  );
});

test('Authenticated owner can create, read, list, update note, and delete their own shared link', async () => {
  const aliceDb = testEnv.authenticatedContext('alice_uid').firestore();
  const ref = aliceDb.doc('users/alice_uid/shared_links/link_101');
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(ref.set(buildValidLinkPayload('alice_uid', now)));

  const snap = await assertSucceeds(ref.get());
  assert.equal(snap.exists, true);
  assert.equal(snap.data().fileName, 'thazh_archive.zip');

  // Partial update of note + updatedAt
  await assertSucceeds(
    ref.update({
      note: 'Ghi chú đã cập nhật',
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // List query ordered by createdAt desc
  const listSnap = await assertSucceeds(
    aliceDb
      .collection('users/alice_uid/shared_links')
      .orderBy('createdAt', 'desc')
      .get()
  );
  assert.equal(listSnap.size, 1);

  await assertSucceeds(ref.delete());
});

test('Cross-user access and invalid URL or immutable field tampering are rejected', async () => {
  const aliceDb = testEnv.authenticatedContext('alice_uid').firestore();
  const bobDb = testEnv.authenticatedContext('bob_uid').firestore();
  const aliceRef = aliceDb.doc('users/alice_uid/shared_links/link_202');
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(aliceRef.set(buildValidLinkPayload('alice_uid', now)));

  // Bob cannot read or delete Alice's link
  const bobTargetRef = bobDb.doc('users/alice_uid/shared_links/link_202');
  await assertFails(bobTargetRef.get());
  await assertFails(bobTargetRef.delete());

  // Invalid non-Catbox URL is rejected
  const invalidUrlRef = aliceDb.doc('users/alice_uid/shared_links/link_bad_url');
  await assertFails(
    invalidUrlRef.set(
      buildValidLinkPayload('alice_uid', new Date(Date.now() - 500), {
        url: 'https://evil.example.com/abc123.zip',
      })
    )
  );

  // Tampering with immutable url on update is rejected
  await assertFails(
    aliceRef.update({
      url: 'https://files.catbox.moe/other999.zip',
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
