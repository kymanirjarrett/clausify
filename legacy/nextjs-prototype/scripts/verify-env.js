// ============================================
// FILE: scripts/verify-env.js
// ============================================
// Run with: node scripts/verify-env.js
// Verifies that all required environment variables are set

import dotenv from 'dotenv';

const requiredVars = [
  'NEXT_PUBLIC_SUPABASE_URL',
  'NEXT_PUBLIC_SUPABASE_ANON_KEY',
  'GROQ_API_KEY',
];

const optionalVars = [
  'GEMINI_API_KEY',
  'NEXT_PUBLIC_APP_URL',
];

console.log('🔍 Verifying environment variables...\n');

dotenv.config({ path: '.env.local' });

let hasErrors = false;

console.log('Required variables:');
requiredVars.forEach(varName => {
  const value = process.env[varName];
  if (value) {
    console.log(`  ✅ ${varName}: Set`);
  } else {
    console.log(`  ❌ ${varName}: Missing`);
    hasErrors = true;
  }
});

console.log('\nOptional variables:');
optionalVars.forEach(varName => {
  const value = process.env[varName];
  if (value) {
    console.log(`  ✅ ${varName}: Set`);
  } else {
    console.log(`  ⚠️  ${varName}: Not set (optional)`);
  }
});

if (hasErrors) {
  console.log('\n❌ Some required variables are missing!');
  console.log('Run: node scripts/setup-env.js');
  process.exit(1);
} else {
  console.log('\n✅ All required environment variables are set!');
  process.exit(0);
}