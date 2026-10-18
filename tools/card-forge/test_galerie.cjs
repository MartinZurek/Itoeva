// Fuehrt das echte Galerie-Script aus; DOM-Stubs ersetzen keine visuelle Browserabnahme.
// node tools/card-forge/test_galerie.cjs
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');

const html = fs.readFileSync(path.join(__dirname, 'out/galerie.html'), 'utf8');
const expected = JSON.parse(fs.readFileSync(path.join(__dirname, 'out/karten.json'), 'utf8'));
const script = html.split('<script>')[1].split('</script>')[0];
const elements = new Map();
const document = {
  querySelector(selector) {
    if (!elements.has(selector)) elements.set(selector, {
      value: selector === '#so' ? 'kosten' : '', checked: false, hidden: true,
      innerHTML: '', textContent: '', addEventListener() {}, showModal() {}, close() {},
    });
    return elements.get(selector);
  },
  createElement() {
    return { getContext() { return { fillRect() {} }; }, toDataURL() { return 'data:image/png;base64,TEST'; } };
  },
};
const context = vm.createContext({ document });
vm.runInContext(script, context);
const ids = vm.runInContext('K.map(k => k.id)', context);
assert.equal(ids.length, expected.karten.length);
assert.ok(ids.length > 0);
for (const id of ids) {
  context.detail(id);
  assert.match(document.querySelector('#sheet').innerHTML, /Angriff/);
  assert.doesNotMatch(document.querySelector('#sheet').innerHTML, /Private Faktor/);
}
assert.equal(document.querySelector('#firmtog').hidden, true);

// Dieselbe Vorlage muss ihre privaten Daten nur optional brauchen und sicher darstellen.
vm.runInContext(`
  D.firmen = {};
  D.firmen[K[0].id] = '<img src=x onerror=alert(1)>';
  D.faktoren = {};
  D.faktoren[K[0].id] = {momentum: 1};
`, context);
document.querySelector('#ff').checked = true;
context.detail(ids[0]);
assert.match(document.querySelector('#sheet').innerHTML, /&lt;img/);
assert.doesNotMatch(document.querySelector('#sheet').innerHTML, /<img src=x/);
assert.match(document.querySelector('#sheet').innerHTML, /Private Faktor/);
assert.match(context.cardHTML(expected.karten[0]), /&lt;img/);
console.log(`OK: ${ids.length} Kartendetails ohne Rohfaktoren; private Zusatzansicht und HTML-Escaping.`);
