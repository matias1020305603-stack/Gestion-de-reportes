const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_uid";
const BOB_UID = "bob_uid";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("reports").get());
});

test("Unauthenticated user: cannot create report", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("reports").add({
    userId: "someone",
    title: "Leak",
  }));
});

test("Authenticated user: cannot read another user's report", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("reports").doc("bob_report").set({
      userId: BOB_UID,
      code: "REP-101",
      title: "Problema Eléctrico",
      description: "Cortocircuito en aula 302",
      location: "Aula 302",
      category: "Eléctrico",
      severity: "ALTA",
      status: "ABIERTO",
      authorName: "Bob Smith",
      authorRole: "Profesor",
      authorEmail: "bob@school.edu",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("reports").doc("bob_report").get());
});

test("Authenticated user: can query own reports with userId filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("reports").doc("alice_report").set({
      userId: ALICE_UID,
      code: "REP-102",
      title: "Fuga de Agua",
      description: "Fuga en baño segundo piso",
      location: "Baño Piso 2",
      category: "Fontanería",
      severity: "MEDIA",
      status: "ABIERTO",
      authorName: "Alice Doe",
      authorRole: "Estudiante",
      authorEmail: "alice@school.edu",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("reports").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: cannot query all reports without filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("reports").get());
});
