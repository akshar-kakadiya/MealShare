const fs = require('fs');
const path = require('path');

const e2ePath = path.join(__dirname, 'test', 'app.e2e-spec.ts');
const content = fs.readFileSync(e2ePath, 'utf8');

const lines = content.split('\n');

// 1. We need to remove the injected code between line 48 and 144.
// Let's find exactly where 'volunteerProfile: volunteerProfilesDb.get(user.id),' is.
const startIdx = lines.findIndex(l => l.includes('volunteerProfile: volunteerProfilesDb.get(user.id),'));
console.log('startIdx', startIdx); // should be 46 (0-indexed)

// And the matching `      }),` that resumes `user` mocking.
const endIdx = lines.findIndex((l, i) => i > startIdx && l === '      }),');
console.log('endIdx', endIdx); // should be 144

if (startIdx > -1 && endIdx > -1) {
  // We want to replace everything from startIdx + 1 to endIdx - 1 with `        });`
  const newLines = [
    ...lines.slice(0, startIdx + 1),
    '        });',
    ...lines.slice(endIdx)
  ];
  fs.writeFileSync(e2ePath, newLines.join('\n'));
  console.log('Fixed the accidental injection.');
}
