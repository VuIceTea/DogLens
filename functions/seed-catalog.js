"use strict";

const path = require("node:path");
const { isDeepStrictEqual } = require("node:util");
const catalog = require("./seed-catalog.json");
const verifiedBreeds = require("./verified-breeds.json");
const editorialArticles = require("./editorial-articles.json");
const modelBreeds = require("../app/src/main/assets/recognition/breed-catalog.json");
const modelMetadata = require("../app/src/main/assets/recognition/model-metadata.json");
const fciGroups = "https://www.fci.be/en/nomenclature/";
const careSource = "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/health";
const allowedCollections = new Set([
  "dogs", "dogSizeCategories", "dogImages", "traits", "dogTraits", "careTips", "dogCareTips",
  "articles", "articleCategories", "articleSections", "explore", "exploreCategories", "featuredTopics", "dogUseCases"
]);

function documents(legacy = false) {
  const existingDogIds = new Set(catalog.dogs.map((dog) => dog.id));
  const mappedBreeds = modelBreeds.filter((breed) => breed !== null);
  if (modelBreeds.length !== modelMetadata.breeds.length ||
      new Set(mappedBreeds.map((breed) => breed.id)).size !== mappedBreeds.length) {
    throw new Error("Invalid recognition breed mapping");
  }
  if (Object.keys(verifiedBreeds).length !== mappedBreeds.length ||
      mappedBreeds.some(({ id }) => !Object.keys(verifiedBreeds[id]?.sources ?? {}).length)) {
    throw new Error("Each recognition breed needs an editorial record with sources");
  }
  const newBreeds = mappedBreeds
    .filter(({ id }) => !existingDogIds.has(id))
    .map(({ id, name }, index) => ({
      id,
      name,
      sizeCategoryId: "unknown",
      sortOrder: 1000 + index,
      description: "Thông tin chi tiết về giống chó này đang được bổ sung."
    }));
  const completeCatalog = { ...catalog, dogs: [...catalog.dogs, ...newBreeds] };
  if (!legacy) {
    completeCatalog.dogs = completeCatalog.dogs.map((dog) => {
      const verified = verifiedBreeds[dog.id];
      return { ...dog, ...verified, sources: { ...verified.sources, "RSPCA · Chăm sóc chung": careSource } };
    });
    const covers = {
      care_coat: "golden_retriever", puppy_food: "labrador_retriever",
      basic_training: "corgi", popular_breeds: "french_bulldog", common_diseases: "siberian_husky"
    };
    completeCatalog.articles = catalog.articles.map((article) => {
      const { sections, ...edited } = editorialArticles[article.id];
      const words = sections.flat().join(" ").trim().split(/\s+/).length;
      const cover = verifiedBreeds[covers[article.id]];
      const photoCredits = Object.fromEntries(Object.entries(cover.sources)
        .filter(([label]) => label.startsWith("Ảnh · ") || label === "Giấy phép ảnh"));
      return {
        ...article, ...edited,
        ...(cover.imageUrl ? { imageUrl: cover.imageUrl } : {}),
        sources: { ...edited.sources, ...photoCredits },
        publishedAt: "26/11/2026",
        readMinutes: Math.ceil(words / 180)
      };
    });
    completeCatalog.articleSections = catalog.articleSections.map((section) => {
      const [heading, body] = editorialArticles[section.articleId].sections[section.sortIndex - 1];
      return { ...section, heading, body };
    });
    for (const key of ["dogImages", "dogTraits", "dogCareTips"]) {
      completeCatalog[key] = catalog[key].map((row) => ({ ...row, editorialVerified: false }));
    }
    const temperaments = [
      ["golden_retriever", "friendly", "FCI mô tả tính khí thân thiện, tự tin."],
      ["golden_retriever", "smart", "Tiêu chuẩn FCI nêu khả năng học và làm việc."],
      ["siberian_husky", "friendly", "FCI mô tả giống thân thiện và cởi mở."],
      ["siberian_husky", "gentle", "Tính khí dịu dàng là đặc điểm được FCI mô tả."],
      ["corgi", "friendly", "FCI mô tả giống cởi mở và thân thiện."],
      ["french_bulldog", "active", "FCI mô tả giống hoạt bát và thích chơi."],
      ["poodle", "loyal", "Lòng trung thành được ghi trong tiêu chuẩn FCI."],
      ["labrador_retriever", "smart", "FCI mô tả giống thông minh và dễ tiếp thu."],
      ["flat_coated_retriever", "friendly", "Giống chó thân thiện, vui vẻ và tự tin theo tiêu chuẩn giống."]
    ];
    for (const [index, [dogId, traitId, description]] of temperaments.entries()) {
      completeCatalog.dogTraits.push({
        id: `${dogId}_fci_${traitId}`, dogId, traitId, description, sortOrder: index,
        sourceUrl: verifiedBreeds[dogId].sources["FCI · Tiêu chuẩn giống"]
      });
    }
    for (const { id: dogId } of mappedBreeds) {
      completeCatalog.dogCareTips.push(
        { id: `${dogId}_general_grooming`, dogId, careTipId: "grooming", sortOrder: 0,
          description: "Chọn lịch chải và dụng cụ phù hợp với bộ lông.", sourceUrl: careSource },
        { id: `${dogId}_general_exercise`, dogId, careTipId: "exercise", sortOrder: 1,
          description: "Điều chỉnh vận động theo tuổi, sức khỏe và nhu cầu của từng con.", sourceUrl: careSource }
      );
    }
    const topics = [
      { title: "Các giống chó tha mồi", dogIds: ["golden_retriever", "labrador_retriever", "flat_coated_retriever"] },
      { title: "Các giống chó chăn đàn", dogIds: ["border_collie", "german_shepherd", "australian_shepherd", "belgian_malinois"] },
      { title: "Các giống chó đồng hành", dogIds: ["french_bulldog", "shih_tzu", "pug", "toy_poodle", "papillon"] }
    ];
    completeCatalog.featuredTopics = catalog.featuredTopics.map((topic, index) =>
      ({ ...topic, ...topics[index], sourceUrl: fciGroups }));
    completeCatalog.exploreCategories = catalog.exploreCategories.map((row) =>
      row.id === "long_hair_dog"
        ? { ...row, dogIds: ["pomeranian", "lhasa_apso", "shih_tzu"], sourceUrl: fciGroups }
        : row.id === "short_hair_dog" ? { ...row, sourceUrl: fciGroups } : row);
    completeCatalog.dogUseCases = catalog.dogUseCases.map((row) => ({ ...row, sourceUrl: fciGroups }));
  }
  const rows = Object.entries(completeCatalog).flatMap(([collection, items]) => {
    if (!allowedCollections.has(collection)) throw new Error("Invalid catalog collection");
    return items.map(({ id, ...data }) => {
      if (!/^[a-z0-9_-]+$/.test(id)) throw new Error("Invalid catalog ID");
      return [`${collection}/${id}`, data];
    });
  });
  if (new Set(rows.map(([key]) => key)).size !== rows.length || rows.length > 500) {
    throw new Error("Duplicate catalog ID or batch too large");
  }
  return rows;
}

function firestoreValue(value) {
  if (value === null) return { nullValue: null };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "number") {
    return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  }
  if (Array.isArray(value)) return { arrayValue: { values: value.map(firestoreValue) } };
  if (value && typeof value === "object") return { mapValue: { fields: firestoreFields(value) } };
  throw new Error("Unsupported catalog value");
}

function firestoreFields(data) {
  return Object.fromEntries(Object.entries(data).map(([field, value]) => [field, firestoreValue(value)]));
}

function reviewPatch(previous, desired, current) {
  const updates = {};
  let conflicts = 0;
  for (const [field, value] of Object.entries(desired)) {
    if (isDeepStrictEqual(previous[field], value) || isDeepStrictEqual(current[field], value)) continue;
    if (field === "sources" && current[field]) {
      const typed = Boolean(value.mapValue);
      const incoming = typed ? value.mapValue.fields : value;
      const existing = typed ? current[field].mapValue?.fields : current[field];
      if (existing && typeof existing === "object" && !Array.isArray(existing)) {
        const merged = { ...existing };
        for (const [label, url] of Object.entries(incoming)) {
          if (!(label in existing)) merged[label] = url;
          else if (!isDeepStrictEqual(existing[label], url)) conflicts++;
        }
        if (!isDeepStrictEqual(existing, merged)) {
          updates[field] = typed ? { mapValue: { fields: merged } } : merged;
        }
        continue;
      }
    }
    if (field === "editorialVerified" &&
        Object.entries(previous).some(([key, before]) => !isDeepStrictEqual(current[key], before))) {
      conflicts++;
      continue;
    }
    if (current[field] === undefined || isDeepStrictEqual(current[field], previous[field])) {
      updates[field] = value;
    } else {
      conflicts++;
    }
  }
  return { updates, conflicts };
}

async function fromFirebaseCli(projectId, rows, write, moduleRoot, legacyRows) {
  const lib = path.join(moduleRoot, "lib");
  const { getProjectDefaultAccount } = require(path.join(lib, "auth"));
  const { requireAuth } = require(path.join(lib, "requireAuth"));
  const { Client } = require(path.join(lib, "apiv2"));
  const account = getProjectDefaultAccount(path.resolve(__dirname, ".."));
  if (!account) throw new Error("Run firebase login before seeding");
  await requireAuth({ ...account, nonInteractive: true });

  const client = new Client({ urlPrefix: "https://firestore.googleapis.com" });
  const base = `projects/${projectId}/databases/(default)/documents`;
  const lookup = (await client.post(`/v1/${base}:batchGet`, {
    documents: rows.map(([key]) => `${base}/${key}`)
  })).body;
  if (!Array.isArray(lookup) || lookup.length !== rows.length) throw new Error("Unexpected Firestore lookup response");
  const snapshots = new Map(lookup.map((result) => [result.missing ?? result.found?.name, result.found]));
  const summary = { created: 0, updated: 0, conflicts: 0 };
  const changes = [];
  for (const [key, data] of rows) {
    const name = `${base}/${key}`;
    const fields = firestoreFields(data);
    const current = snapshots.get(name);
    if (!snapshots.has(name)) throw new Error(`No lookup result for ${key}`);
    if (!current) {
      summary.created++;
      changes.push({ update: { name, fields }, currentDocument: { exists: false } });
    } else if (legacyRows) {
      const previous = firestoreFields(legacyRows.get(key) ?? {});
      const { updates, conflicts } = reviewPatch(previous, fields, current.fields ?? {});
      summary.conflicts += conflicts;
      if (!Object.keys(updates).length) continue;
      summary.updated++;
      changes.push({
        update: { name, fields: updates },
        updateMask: { fieldPaths: Object.keys(updates) },
        currentDocument: { updateTime: current.updateTime }
      });
    }
  }
  if (write && changes.length) await client.post(`/v1/${base}:commit`, { writes: changes });
  return summary;
}

async function fromApplicationDefaultCredentials(projectId, rows, write, legacyRows) {
  const { initializeApp, applicationDefault } = require("firebase-admin/app");
  const { getFirestore } = require("firebase-admin/firestore");
  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const references = rows.map(([key]) => db.doc(key));
  const snapshots = await db.getAll(...references);
  const summary = { created: 0, updated: 0, conflicts: 0 };
  const batch = db.batch();
  for (const [index, [key, data]] of rows.entries()) {
    const snapshot = snapshots[index];
    if (!snapshot.exists) {
      summary.created++;
      batch.create(references[index], data);
    } else if (legacyRows) {
      const { updates, conflicts } = reviewPatch(legacyRows.get(key) ?? {}, data, snapshot.data());
      summary.conflicts += conflicts;
      if (!Object.keys(updates).length) continue;
      summary.updated++;
      batch.update(references[index], updates, { lastUpdateTime: snapshot.updateTime });
    }
  }
  if (write && (summary.created || summary.updated)) await batch.commit();
  return summary;
}

async function main() {
  const [projectId, ...options] = process.argv.slice(2);
  if (!/^[a-z][a-z0-9-]+$/.test(projectId ?? "") ||
      options.some((option) => !["--write", "--verified"].includes(option))) {
    throw new Error("Usage: node functions/seed-catalog.js <project-id> [--verified] [--write]");
  }
  const rows = documents();
  const write = options.includes("--write");
  const legacyRows = options.includes("--verified") ? new Map(documents(true)) : null;
  const { created, updated, conflicts } = process.env.DOG_LENS_FIREBASE_CLI_PACKAGE
    ? await fromFirebaseCli(projectId, rows, write, process.env.DOG_LENS_FIREBASE_CLI_PACKAGE, legacyRows)
    : await fromApplicationDefaultCredentials(projectId, rows, write, legacyRows);
  console.log(`${projectId} (${write ? "committed" : "dry run"}): ${created} new, ${updated} updated, ${conflicts} edited fields preserved.`);
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
