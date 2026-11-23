import test from 'node:test';
import assert from 'node:assert/strict';
import { companyContacts, contactsGuide } from '../src/contacts.mjs';
import { conversationMessages } from '../src/worker.mjs';

test('official email aliases are public configuration, not sending credentials', () => {
  for (const key of ['support','general','privacy','billing','partnerships','executive','sales','admin','careers','hello']) {
    assert.match(companyContacts[key], /^[a-z]+@lxobsidianportal\.co\.za$/);
  }
  assert.equal(companyContacts.privacy, 'info@lxobsidianportal.co.za');
  assert.ok(!JSON.stringify(companyContacts).includes('gmail.com'));
});
test('assistant help uses official contacts without claiming support actions completed', () => {
  const messages = conversationMessages({mode:'reply',message:'How do I report a login problem?', context:'', spoilers:false});
  assert.ok(messages[0].content.includes(companyContacts.support));
  assert.ok(messages[0].content.includes(companyContacts.supportPage));
  assert.ok(contactsGuide.includes('Never claim an email has been sent'));
});
